// Copyright 2006 Google Inc.  All Rights Reserved.
package com.google.gwt.examples.i18n;

import com.google.gwt.i18n.client.Constants;

public interface NumberFormatConstantsWithAltKey extends Constants {
  /**
   * Returns the localized decimal separator.
   */
  @Key("fmt.sep.decimal")
  String decimalSeparator();

  /**
   * Returns the localized thousands separator.
   */
  @Key("fmt.sep.thousands")
  String thousandsSeparator();
}
