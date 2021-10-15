package com.rr.eucentralbank.model;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Class representing a single row of data with the Date field separated out from the rates data
 * 
 * @author Robert Rodrigues
 *
 */
public class ExchangeRatesRow {

	private Date date;
	private List<Double> rates;
	protected ExchangeRates parent;
	
	public ExchangeRatesRow(Date date, List<Double> exchangeRate) {
		super();
		this.date = date;
		this.rates = exchangeRate;
	}
	
	/**
	 * Converts the current row to a Map<String, Double>
	 * 
	 * @return Map where the key is the currency name and the value is the exchange rate
	 */
	public Map<String, Double> toMap() {
		Map<String, Double> map = new HashMap<>();
		List<String> currencyNames = parent.streamNames().collect(Collectors.toList());
		for(int i=0; i<currencyNames.size(); i++) {
			map.put(currencyNames.get(i), rates.get(i));
		}
		return map;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Date=").append(date);
		builder.append(" Values=").append(rates);
		return builder.toString();
	}
	
	public Date getDate() {
		return date;
	}
	
}
