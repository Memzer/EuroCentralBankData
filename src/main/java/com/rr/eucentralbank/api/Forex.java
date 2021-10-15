package com.rr.eucentralbank.api;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.rr.eucentralbank.exception.CurrencyUnavailableException;

public interface Forex {

	/**
	 * Allows an API caller to retrieve the reference rate data for a given Date for all available Currencies.
	 * 
	 * @param date a {@link Date} object to search by
	 * @return {@link HashMap} containing all available currencies and their respective rates
	 */
	Map<String, Double> readDataForDate(Date date);

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
	Double convertCurrency(Date date, Double amount, String sourceCurrency, String targetCurrency) throws CurrencyUnavailableException;

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
	Double calculateHighest(Date start, Date end, String currency) throws CurrencyUnavailableException;

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
	Double calculateAverage(Date start, Date end, String currency, boolean removeNulls) throws CurrencyUnavailableException;

	/**
	 * This method accepts a ZIP file, containing the CSV data to be parsed and populates the in-memory data model.
	 * 
	 * @param zipFile The input zip file which is expected to contain a csv file of the currency data.
	 * @throws IOException
	 */
	void loadDataFromZip(File zipFile) throws IOException;

	/**
	 * This method streams the data from the live site and populates the in-memory data model.
	 * 
	 * @throws IOException
	 */
	void loadDataLiveSite() throws IOException;

	/**
	 * This method streams the data and populates the in-memory data model.
	 * 
	 * @throws IOException
	 */
	void loadDataFromInputStream(InputStream in) throws IOException;

}