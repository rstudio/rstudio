// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.msg;

public final class FormatterForLong extends Formatter {

  public String format(Object toFormat) {
    return ((Long)toFormat).toString();
  }

}
