// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.msg;

import java.lang.reflect.Method;

public final class FormatterForMethod extends Formatter {

  public String format(Object toFormat) {
    return ((Method)toFormat).getName();
  }

}
