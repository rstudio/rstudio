package com.google.gwt.examples;

import java.util.Date;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;

public class DateTimeFormatExample implements EntryPoint {

  public void onModuleLoad() {
    Date today = new Date();

    // prints Tue Dec 18 12:01:26 GMT-500 2007 in the default locale.
    GWT.log(today.toString());

    // prints 12/18/07 in the default locale
    GWT.log(DateTimeFormat.getShortDateFormat().format(today));

    // prints December 18, 2007 in the default locale
    GWT.log(DateTimeFormat.getLongDateFormat().format(today));

    // prints 12:01 PM in the default locale
    GWT.log(DateTimeFormat.getShortTimeFormat().format(today));

    // prints 12:01:26 PM GMT-05:00 in the default locale
    GWT.log(DateTimeFormat.getLongTimeFormat().format(today));

    // prints Dec 18, 2007 12:01:26 PM in the default locale
    GWT.log(DateTimeFormat.getMediumDateTimeFormat().format(today));
    
    // A custom date format
    DateTimeFormat fmt = DateTimeFormat.getFormat("EEEE, MMMM dd, yyyy");
    // prints Monday, December 17, 2007 in the default locale
    GWT.log(fmt.format(today));
  }
}
