// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.msg;

import java.io.File;

public final class FormatterForFile extends Formatter {

  public String format(Object toFormat) {
    return ((File)toFormat).getAbsolutePath();
  }

}
