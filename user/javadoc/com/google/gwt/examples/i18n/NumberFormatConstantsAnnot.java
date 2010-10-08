// Copyright 2006 Google Inc.  All Rights Reserved.
package com.google.gwt.examples.i18n;

import com.google.gwt.i18n.client.Constants;

public interface NumberFormatConstantsAnnot extends Constants {
  /**
   * Returns the localized decimal separator.
   */
  @DefaultStringValue(".")
  String decimalSeparator();

  /**
   * Returns the localized thousands separator.
   */
  @DefaultStringValue(",")
  String thousandsSeparator();
}
