# EuroCentralBankData
API which reads the EU central bank data and exposes various useful functions.

### Requirements
The API uses Java 11.

### Running tests
Execute `mvn test`

### Building
Execute `mvn build` to generate the JAR file

### Example Usage
The data is expected to be in CSV format, and wrapped in a ZIP file. 
```
	ExchangeRateTool t = new ExchangeRateTool();
	t.loadDataLiveSite();
```
If the ZIP file is already downloaded, it can be loaded as follows:
```
	ExchangeRateTool t = new ExchangeRateTool();
	t.loadDataFromZip(new File(pathToFile));
```

This will fetch the latest data from https://www.ecb.europa.eu/stats/eurofxref/eurofxref-hist.zip
Once data is loaded, the following functionality is available:

* **readDataForDate**
	* Allows an API caller to retrieve the reference rate data for a given Date for all available Currencies. 
* **convertCurrency**
	* Given a Date, source Currency (eg. JPY), target Currency (eg. GBP), and an Amount, returns the Amount given converted from the first to the second Currency as it would have been on that Date (assuming zero fees). 
* **calculateHighest**
	* Given a start Date, an end Date and a Currency, return the highest reference exchange rate that the Currency achieved for the period. 
* **calculateAverage**
	* Given a start Date, an end Date and a Currency, determine and return the average reference exchange rate of that Currency for the period. 
	* Note that there are two modes, which affect how the average is calculated
		* calculate with null values removed
		* calculate assuming null values are zero