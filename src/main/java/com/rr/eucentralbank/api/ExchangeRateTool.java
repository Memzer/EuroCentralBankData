package com.rr.eucentralbank.api;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.rr.eucentralbank.exception.CurrencyUnavailableException;
import com.rr.eucentralbank.model.ExchangeRates;
import com.rr.eucentralbank.model.ExchangeRatesRow;

/**
 * This class provides a set of functions which allows the calling code to easily load CSV data, and 
 * to perform a variety of useful operations on it.
 * 
 * @author Robert Rodrigues
 *
 */
public class ExchangeRateTool {
	
	//Dates in the CSV are in this format
	private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	
	//This is the main model holding he data in memory.
	private ExchangeRates exchangeRates;
	
	/**
	 * Allows an API caller to retrieve the reference rate data for a given Date for all available Currencies.
	 * 
	 * @param date a {@link Date} object to search by
	 * @return {@link HashMap} containing all available currencies and their respective rates
	 */
	public Map<String, Double> readDataForDate(Date date) {
		Optional<ExchangeRatesRow> rowData = exchangeRates.getRowData().stream().filter(e -> e.getDate().equals(date)).findAny();
		if(rowData.isPresent()) {
			return rowData.get().toMap();
		} else {
			return new HashMap<>();
		}
	}
	
	/**
	 * Given a Date, source Currency (eg. JPY), target Currency (eg. GBP), and an
	 * Amount, returns the Amount given converted from the first to the second Currency as
	 * it would have been on that Date (assuming zero fees).
	 * 
	 * @param date a {@link Date} object to search by
	 * @param amount in the source currency
	 * @param sourceCurrency String value of source currency name
	 * @param targetCurrency String value of target currency name
	 * @return Double value converted to the target currency
	 * @throws CurrencyUnavailableException
	 */
	public Double convertCurrency(Date date, Double amount, String sourceCurrency, String targetCurrency) throws CurrencyUnavailableException {
		Map<String, Double> row = readDataForDate(date);
		Double sourceToEuro = row.get(sourceCurrency);
		Double targetToEuro = row.get(targetCurrency);
		if(sourceToEuro == null) {
			throw new CurrencyUnavailableException(sourceCurrency+" not available on "+date);
		}
		if(targetToEuro == null) {
			throw new CurrencyUnavailableException(targetCurrency+" not available on "+date);
		}
		
		return amount / sourceToEuro * targetToEuro;
	}
	
	/**
	 * Given a start Date, an end Date and a Currency, return the highest reference
	 * exchange rate that the Currency achieved for the period.
	 * 
	 * @param start {@link Date} (inclusive)
	 * @param end {@link Date} (inclusive)
	 * @param currency String value of currency name
	 * @return Double the highest exchange rate within the given time period
	 * @throws CurrencyUnavailableException
	 */
	public Double calculateHighest(Date start, Date end, String currency) throws CurrencyUnavailableException {
		DoubleSummaryStatistics statistics = statistics(start, end, currency, true);
		if(statistics.getCount() == 0) {
			throw new CurrencyUnavailableException(currency+" not available between "+start+" and "+end);
		} else {
			return statistics.getMax();
		}
	}
	
	/**
	 * Given a start Date, an end Date and a Currency, determine and return the average
	 * reference exchange rate of that Currency for the period.
	 * 
	 * The removeNulls flag determines how to handle null values. 
	 * If we leave nulls in, we treat them as zero. This will lower the average
	 * over the given date range.
	 * 
	 * @param start {@link Date} (inclusive)
	 * @param end {@link Date} (inclusive)
	 * @param currency String value of currency name
	 * @param removeNulls boolean flag to decide what to do with null values
	 * @return Double the highest exchange rate within the given time period
	 * @throws CurrencyUnavailableException 
	 */
	public Double calculateAverage(Date start, Date end, String currency, boolean removeNulls) throws CurrencyUnavailableException {
		DoubleSummaryStatistics statistics = statistics(start, end, currency, removeNulls);
		if(statistics.getCount() == 0) {
			throw new CurrencyUnavailableException(currency+" not available between "+start+" and "+end);
		} else {
			return statistics.getAverage();
		}
	}
	
	/**
	 * Given a start Date, an end Date and a Currency, return
	 * statistics on the exchange rates for the period.
	 * 
	 * The removeNulls flag determines how to handle null values. This only affects the
	 * average calculation. If we leave nulls in, we treat them as zero. This will lower the average
	 * over the given date range.
	 * 
	 * @param start {@link Date} (inclusive)
	 * @param end {@link Date} (inclusive)
	 * @param currency String value of currency name
	 * @param removeNulls boolean flag to decide what to do with null values
	 * @return List<Double> the exchanges rate within the given time period
	 */
	public DoubleSummaryStatistics statistics(Date start, Date end, String currency, boolean removeNulls) {
		return exchangeRates.getRowData().stream()
			.filter(r -> r.getDate().after(start) || r.getDate().equals(start))
			.filter(r -> r.getDate().before(end) || r.getDate().equals(end))
			//At this point we have all ExchangeRateRows between the dates (inclusive)
			.map(e -> e.toMap().get(currency))
			//Now just the rate values for given currency
			.filter(removeNulls ? Objects::nonNull : v -> true)
			.map(v -> v == null ? 0 : v) //Any nulls not already removed, treat as zero. This effects the average
			.mapToDouble(v -> v)
			.summaryStatistics();
	}
	
	/**
	 * This method accepts a ZIP file, containing the CSV data to be parsed and populates the in-memory data model.
	 * 
	 * @param zipFile The input zip file which is expected to contain a csv file of the currency data.
	 * @throws IOException
	 */
	public void loadDataFromZip(File zipFile) throws IOException {
		try(InputStream in = new FileInputStream(zipFile)) {
			processInputStream(in);
		}
	}
	
	/**
	 * This method streams the data from the live site and populates the in-memory data model.
	 * 
	 * @throws IOException
	 */
	public void loadDataLiveSite() throws IOException {
		try(InputStream in = new BufferedInputStream(new URL("https://www.ecb.europa.eu/stats/eurofxref/eurofxref-hist.zip").openStream())) {
			processInputStream(in);
		}
	}
	
	/**
	 * This method streams the data and populates the in-memory data model.
	 * 
	 * @throws IOException
	 */
	public void loadDataFromInputStream(InputStream in) throws IOException {
		processInputStream(in);
	}

	/**
	 * Reads the input stream and extracts the data into the in-memory model
	 * 
	 * @param in the {@link InputStream} to be processed
	 * @throws IOException
	 */
	private void processInputStream(InputStream in) throws IOException {
		try (ZipInputStream zipInStream = new ZipInputStream(in)) {
			ZipEntry zipEntry;
			//Although we expect a single csv file in the zip, looping though available files
			//will catch occasions where there are multiple files.
			while((zipEntry = zipInStream.getNextEntry()) != null) {
				if(zipEntry.getName().equalsIgnoreCase("eurofxref-hist.csv")) {
					Reader reader = new InputStreamReader(zipInStream);
					CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT);
					
					parser.forEach(this::readSingleRecord);
					
					parser.close();
					reader.close();
					//At this point we've read the CSV file, so no need to loop around to any additional files in the .zip
					break;
				}
			}
		}
	}
	
	/**
	 * Process a single {@link CSVRecord}, and update the in-memory data model
	 * 
	 * @param csvRecord a single row from the csv stream
	 */
	private void readSingleRecord(CSVRecord csvRecord) {		
		List<String> recordAsList = csvRecord.stream()
				.skip(1) //Ignore the first field, which is Date
				.collect(Collectors.toList());
		
		//if the csv file has an extra empty string at the end, so filter that out
		if(recordAsList.get(recordAsList.size()-1).isEmpty()) {
			recordAsList.remove(recordAsList.size()-1);
		}
		
		if(csvRecord.getRecordNumber() == 1L) {
			//Instantiate model and keep track of the currency names from the first row of the CSV			
			exchangeRates = new ExchangeRates(recordAsList);
		} else {
			//Handle row of data
			
			//Parse the String date to Date object (yyyy-MM-dd)	
			Date date = parseDate(csvRecord.get(0));
			if(date != null) {
				//Store the actual exchange rates into the model
				List<Double> recordAsListOfDouble = convertFromString(recordAsList, ExchangeRateTool::parseDouble);

				ExchangeRatesRow rowData = new ExchangeRatesRow(date, recordAsListOfDouble);
				
				exchangeRates.addRowData(rowData);
			}	
		}
	}
	
	/**
	 * Helper function to parse a string value to a Double, or null if it is not a number
	 * 
	 * @param stringValue input string to be converted
	 * @return Double value or null
	 */
	private static Double parseDouble(String stringValue) {
		try {
			return Double.parseDouble(stringValue);
		} catch(NumberFormatException e) {
			return null;
		}
	}
	
	/**
	 * Helper for converting a List of <T> to <U> as defined by the input function
	 * 
	 * @param <T> the source type
	 * @param <U> the destination type
	 * @param source the input List
	 * @param func the conversion function
	 * @return List<U> the converted list
	 */
	private static <T, U> List<U> convertFromString(List<T> source, Function<T, U> func) {
	    return source.stream().map(func).collect(Collectors.toList());
	}
	
	/**
	 * Helper function to parse the String date to Date object
	 * 
	 * @param dateStr String date
	 * @return converted {@link Date} object 
	 */
	public Date parseDate(String dateStr) {
		try {
			return dateFormat.parse(dateStr);
		} catch (ParseException e) {
			e.printStackTrace();
			//The date could not be parsed, so skip this row
			return null;
		}
	}
}
