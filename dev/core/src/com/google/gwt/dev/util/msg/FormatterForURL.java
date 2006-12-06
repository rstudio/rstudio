// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.msg;

import java.net.URL;

public class FormatterForURL extends Formatter {

  public String format(Object toFormat) {
    return ((URL)toFormat).toString();
  }

}
