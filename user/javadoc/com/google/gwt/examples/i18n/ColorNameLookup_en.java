package com.google.gwt.examples.i18n;

public class ColorNameLookup_en extends ColorNameLookup {
  @Override
  public String lookupColorName(String htmlColorValue) {
    if ("#FF0000".equalsIgnoreCase(htmlColorValue))
      return "red";
    else if ("#00FF00".equalsIgnoreCase(htmlColorValue))
      return "green";
    else if ("#0000FF".equalsIgnoreCase(htmlColorValue))
      return "blue";
    else
      return null; // don't know this one
  }
}
