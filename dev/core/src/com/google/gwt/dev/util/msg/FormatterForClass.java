// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.msg;

public final class FormatterForClass extends Formatter {

  private String getNiceTypeName(Class targetType) {
    // Screen out common cases.
    // Otherwise, just pass along the class name.
    if (targetType.isArray())
      return getNiceTypeName(targetType.getComponentType()) + "[]";
    else
      return targetType.getName();
  }
  
  public String format(Object toFormat) {
    return getNiceTypeName((Class)toFormat);
  }
}
