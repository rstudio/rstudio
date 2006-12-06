// Copyright 2006 Google Inc.  All Rights Reserved.
package com.google.gwt.examples.i18n;

import com.google.gwt.i18n.client.Constants;

public interface NumberFormatConstantsWithAltKey extends Constants {
  /**
   * @gwt.key fmt.sep.decimal
   * @return the localized decimal separator
   */
  String decimalSeparator();

  /**
   * @gwt.key fmt.sep.thousands
   * @return the localized thousands separator
   */
  String thousandsSeparator();
}
