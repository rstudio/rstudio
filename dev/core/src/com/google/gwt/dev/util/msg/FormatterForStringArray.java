// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.msg;

public final class FormatterForStringArray extends Formatter {

  public String format(Object toFormat) {
    StringBuffer sb = new StringBuffer();
    String[] ss = (String[])toFormat;
    for (int i = 0, n = ss.length; i < n; ++i) {
      if (i > 0)
        sb.append(", ");
      sb.append(ss[i]);
    }
    return sb.toString();
  }

}
