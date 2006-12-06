// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.msg;

public abstract class Formatter {
  /**
   * Transforms the specified object into a string format.
   * 
   * @param toFormat the object to format; the caller is responsible for 
   * ensuring that the type of the passed-in object is the type expected 
   * by the subclass
   * 
   * @throws ClassCastException if <code>toFormat</code> is not of the type expected by the subclass
   */
  public abstract String format(Object toFormat);
}
