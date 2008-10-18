package com.google.gwt.examples.i18n;

import com.google.gwt.i18n.client.ConstantsWithLookup;

public interface NumberFormatConstantsWithLookup extends ConstantsWithLookup {
  /**
   * @return the localized decimal separator
   */
  String decimalSeparator();

  /**
   * @return the localized thousands separator
   */
  String thousandsSeparator();
}
