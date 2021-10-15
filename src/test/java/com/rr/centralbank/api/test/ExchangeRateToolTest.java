package com.rr.centralbank.api.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;

import com.rr.eucentralbank.api.Forex;
import com.rr.eucentralbank.api.ForexImpl;
import com.rr.eucentralbank.exception.CurrencyUnavailableException;

class ExchangeRateToolTest {

	private String testData = "Date,USD,GBP,AAA,\r\n" 
							+ "2021-10-15,1.1602,0.84368,1,\r\n" 
							+ "2021-10-14,1.1602,0.84618,1,\r\n" 
							+ "2021-10-13,1.1562,0.84898,1,\r\n" 
							+ "2021-10-12,1.1555,0.84755,1,\r\n" 
							+ "2021-10-11,1.1574,0.84878,1,\r\n" 
							+ "2021-10-08,1.1569,0.8489,N/A,\r\n" 
							+ "2021-10-07,1.1562,0.85023,N/A,\r\n" 
							+ "2021-10-06,1.1542,0.8497,N/A,\r\n" 
							+ "2021-10-05,1.1602,0.85173,N/A,\r\n" 
							+ "2021-10-04,1.1636,0.8553,N/A,";

	/**
	 * Creates an in-memory stream which will be used simulate loading a real stream
	 * 
	 * @return InputStream
	 * @throws IOException
	 */
	private InputStream createDummyStream() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ZipOutputStream zos = new ZipOutputStream(baos);
		ZipEntry entry = new ZipEntry("eurofxref-hist.csv");
		entry.setSize(testData.getBytes().length);
		zos.putNextEntry(entry);
		zos.write(testData.getBytes());
		zos.closeEntry();
		zos.close();
		return new ByteArrayInputStream(baos.toByteArray());
	}

	/**
	 * Tests full row fetching
	 * 
	 * @throws IOException
	 * @throws ParseException 
	 */
	@Test
	void testFetchingRow() throws IOException, ParseException {
		Forex t = new ForexImpl();
		t.loadDataFromInputStream(createDummyStream());

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

		// Valid date
		Map<String, Double> result = t.readDataForDate(dateFormat.parse("2021-10-13"));
		assertEquals(1.1562, result.get("USD"), "Expect USD=1.1562");

		// Date doesn't exist
		Map<String, Double> emptyResult = t.readDataForDate(dateFormat.parse("2021-10-10"));
		assertTrue(emptyResult.isEmpty(), "Expect map to be empty");
	}

	/**
	 * Tests conversion calculations
	 * 
	 * @throws IOException
	 * @throws CurrencyUnavailableException
	 * @throws ParseException 
	 */
	@Test
	void testConversion() throws IOException, CurrencyUnavailableException, ParseException {
		Forex t = new ForexImpl();
		t.loadDataFromInputStream(createDummyStream());

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		
		// Valid
		Double result = t.convertCurrency(dateFormat.parse("2021-10-15"), 1D, "USD", "GBP");
		assertEquals(0.7271849681089468, result, "Expect 0.7271849681089468");

		// Date doesn't exist, expect exception
		assertThrows(CurrencyUnavailableException.class, () -> {
			t.convertCurrency(dateFormat.parse("2021-10-10"), 1D, "USD", "GBP");
		});

		// Null rate for AAA, expect exception
		assertThrows(CurrencyUnavailableException.class, () -> {
			t.convertCurrency(dateFormat.parse("2021-10-08"), 1D, "USD", "AAA");
		});
	}

	/**
	 * Tests the maximum function
	 * 
	 * @throws IOException
	 * @throws CurrencyUnavailableException
	 * @throws ParseException 
	 */
	@Test
	void testCalculateMaximum() throws IOException, CurrencyUnavailableException, ParseException {
		Forex t = new ForexImpl();
		t.loadDataFromInputStream(createDummyStream());

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

		// Valid range and currency
		Double highest = t.calculateHighest(dateFormat.parse("2021-10-04"), dateFormat.parse("2021-10-15"), "USD");
		assertEquals(1.1636, highest, "Expect 1.1636");

		// Start date is after end date, so no data. Expect exception
		assertThrows(CurrencyUnavailableException.class, () -> {
			t.calculateHighest(dateFormat.parse("2021-10-15"), dateFormat.parse("2021-10-04"), "USD");
		});

		// No data in range. Expect exception
		assertThrows(CurrencyUnavailableException.class, () -> {
			t.calculateHighest(dateFormat.parse("2021-10-04"), dateFormat.parse("2021-10-08"), "AAA");
			assertFalse(true, "Expect exception to be thrown");
		});
	}

	/**
	 * Tests the average function
	 * 
	 * @throws IOException
	 * @throws CurrencyUnavailableException
	 * @throws ParseException 
	 */
	@Test
	void testCalculateAverage() throws IOException, CurrencyUnavailableException, ParseException {
		Forex t = new ForexImpl();
		t.loadDataFromInputStream(createDummyStream());

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

		// Valid range and currency, removing nulls
		Double average1 = t.calculateAverage(dateFormat.parse("2021-10-04"), dateFormat.parse("2021-10-15"), "AAA", true);
		assertEquals(1, average1, "Expect 1");

		// Valid range and currency, without removing nulls
		Double average2 = t.calculateAverage(dateFormat.parse("2021-10-04"), dateFormat.parse("2021-10-15"), "AAA", false);
		assertEquals(0.5, average2, "Expect 0.5");

		// Start date is after end date, so no data. Expect exception
		assertThrows(CurrencyUnavailableException.class, () -> {
			t.calculateAverage(dateFormat.parse("2021-10-15"), dateFormat.parse("2021-10-04"), "USD", true);
		});

		// No data in range. Expect exception
		assertThrows(CurrencyUnavailableException.class, () -> {
			t.calculateAverage(dateFormat.parse("2021-10-04"), dateFormat.parse("2021-10-08"), "AAA", true);
			assertFalse(true, "Expect exception to be thrown");
		});
	}
}
