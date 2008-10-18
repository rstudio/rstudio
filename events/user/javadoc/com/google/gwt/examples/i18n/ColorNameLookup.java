package com.google.gwt.examples.i18n;

import com.google.gwt.i18n.client.Localizable;

public class ColorNameLookup implements Localizable {
  public String lookupColorName(String htmlColorValue) {
    return "?unlocalized " + htmlColorValue + "?";
  }
}
