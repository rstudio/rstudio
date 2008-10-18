package com.google.gwt.examples.i18n;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;

public class ColorNameLookupExample implements EntryPoint {

  public void onModuleLoad() {
    useColorNameLookup();
  }

  public void useColorNameLookup() {
    ColorNameLookup lookup = (ColorNameLookup) GWT.create(ColorNameLookup.class);
    String htmlGreen = "#00FF00";
    String localizedGreen = lookup.lookupColorName(htmlGreen);
    String msg = "The localized name for green is '" + localizedGreen + "'";
    showMessage(msg);
  }

  private static native void showMessage(String msg) /*-{
   var el = $doc.createElement("div"); 
   el.innerHTML = msg;
   $doc.body.appendChild(el);
   }-*/;
}
