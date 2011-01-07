/**
 * This code must be kept in sync with {@link com.google.gwt.dev.javac.TypeOracleMediatorTestBase}
 */
package com.google.gwt.dev.javac.mediatortest;

public class EnclosingLocal {
  public static Object getLocal() {
       class MyObject { }
       return new MyObject();
  }
}
