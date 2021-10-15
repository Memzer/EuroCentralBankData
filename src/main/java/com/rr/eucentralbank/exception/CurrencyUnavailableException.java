package com.rr.eucentralbank.exception;

/**
 * Exception which is thrown when an attempt is made to access a currency which is not available at that time.
 * 
 * @author Robert Rodrigues
 *
 */
public class CurrencyUnavailableException extends Exception {

	private static final long serialVersionUID = 7303913302250244036L;

	public CurrencyUnavailableException(String errorMessage) {
        super(errorMessage);
    }
	
}
