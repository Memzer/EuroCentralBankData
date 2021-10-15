package com.rr.eucentralbank.model;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	
	public Map<String, Double> toMap() {
		Map<String, Double> map = new HashMap<>();
		for(int i=0; i<parent.getCurrencyNames().size(); i++) {
			map.put(parent.getCurrencyNames().get(i), rates.get(i));
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
	public List<Double> getRates() {
		return rates;
	}
	
}
