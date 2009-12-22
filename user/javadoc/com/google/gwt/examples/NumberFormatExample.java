package com.google.gwt.examples;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.NumberFormat;

public class NumberFormatExample implements EntryPoint {

  public void onModuleLoad() {
    NumberFormat fmt = NumberFormat.getDecimalFormat();
    double value = 12345.6789;
    String formatted = fmt.format(value);
    // Prints 1,2345.6789 in the default locale
    GWT.log("Formatted string is" + formatted);

    // Turn a string back into a double
    value = NumberFormat.getDecimalFormat().parse("12345.6789");
    GWT.log("Parsed value is" + value);

    // Scientific notation
    value = 12345.6789;
    formatted = NumberFormat.getScientificFormat().format(value);
    // prints 1.2345E4 in the default locale
    GWT.log("Formatted string is" + formatted);

    // Currency
    fmt = NumberFormat.getCurrencyFormat();
    formatted = fmt.format(123456.7899);
    // prints US$123,456.79 in the default locale or $123,456.79 in the en_US
    // locale
    GWT.log("Formatted currency is" + formatted);
    
    // Custom format
    value = 12345.6789;
    formatted = NumberFormat.getFormat("000000.000000").format(value);
    // prints 012345.678900 in the default locale
    GWT.log("Formatted string is" + formatted);
  }
}
