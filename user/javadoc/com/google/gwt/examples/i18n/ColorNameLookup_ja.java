package com.google.gwt.examples.i18n;

public class ColorNameLookup_ja extends ColorNameLookup {
  @Override
  public String lookupColorName(String htmlColorValue) {
    if ("#FF0000".equalsIgnoreCase(htmlColorValue))
      return "あか";
    else if ("#00FF00".equalsIgnoreCase(htmlColorValue))
      return "みどり";
    else if ("#0000FF".equalsIgnoreCase(htmlColorValue))
      return "あお";
    else
      return null; // don't know this one
  }
}
