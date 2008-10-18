package com.google.gwt.examples.i18n;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;

public class NumberFormatConstantsExample implements EntryPoint {

  public void useNumberFormatConstants() {
    NumberFormatConstants constants = (NumberFormatConstants) GWT.create(NumberFormatConstants.class);
    String decimalSep = constants.decimalSeparator();
    String thousandsSep = constants.thousandsSeparator();
    String msg = "Decimals are separated using '" + decimalSep + "'";
    msg += ", and thousands are separated using '" + thousandsSep + "'";
    showMessage(msg);
  }

  /**
   * Not intended for use in doc; only here as a check that gwt.key works.
   */
  public void useNumberFormatConstantsWithAltKey() {
    NumberFormatConstantsWithAltKey constants = (NumberFormatConstantsWithAltKey) GWT.create(NumberFormatConstantsWithAltKey.class);
    String decimalSep = constants.decimalSeparator();
    String thousandsSep = constants.thousandsSeparator();
    String msg = "[using gwt.key] Decimals are separated using '" + decimalSep
      + "'";
    msg += ", and thousands are separated using '" + thousandsSep + "'";
    showMessage(msg);
  }

  private static native void showMessage(String msg) /*-{
   var el = $doc.createElement("div"); 
   el.innerHTML = msg;
   $doc.body.appendChild(el);
   }-*/;

  public void onModuleLoad() {
    useNumberFormatConstants();
    useNumberFormatConstantsWithAltKey();
    useNumberFormatConstantsWithLookup();
  }

  private void useNumberFormatConstantsWithLookup() {
    NumberFormatConstantsWithLookup constants = (NumberFormatConstantsWithLookup) GWT.create(NumberFormatConstantsWithLookup.class);
    String decimalSep = constants.getString("decimalSeparator");
    String thousandsSep = constants.getString("thousandsSeparator");
    String msg = "Decimals are separated using '" + decimalSep + "'";
    msg += ", and thousands are separated using '" + thousandsSep + "'";
    showMessage(msg);
  }
}
