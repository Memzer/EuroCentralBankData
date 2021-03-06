package com.rr.eucentralbank.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * This model holds the currency data. 
 * It is structured in this way so that the number of columns can be arbitrary
 * 
 * @author Robert Rodrigues
 *
 */
public class ExchangeRates {

	//Holds list of currency names (3 letters)
	private List<String> currencyNames;
	
	//Initialise as empty list, because it will be added to
	private List<ExchangeRatesRow> rowData = new ArrayList<>();
	
	/**
	 * Default constructor which required a list of currency names
	 * @param currencyNames {@link List} of currency names
	 */
	public ExchangeRates(List<String> currencyNames) {
		super();
		this.currencyNames = currencyNames;
	}
	
	/**
	 * Returns a stream of the currency names
	 * 
	 * @return Stream of names
	 */
	public Stream<String> streamNames() {
		return currencyNames.stream();
	}
	
	/**
	 * Returns a stream of the row data
	 * 
	 * @return Stream of {@link ExchangeRatesRow}
	 */
	public Stream<ExchangeRatesRow> streamRows() {
		return rowData.stream();
	}

	/**
	 * Adds a row to the data. Also sets a reference to the parent which holds the currency names
	 * @param row an {@link ExchangeRatesRow} object
	 */
	public void addRowData(ExchangeRatesRow row) {
		row.parent = this;
		rowData.add(row);
	}
	
}
