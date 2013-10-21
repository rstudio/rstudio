/**
 * This code must be kept in sync with {@link com.google.gwt.dev.javac.TypeOracleUpdaterTestBase}
 */
package com.google.gwt.dev.javac.mediatortest;

public class Enclosing {
  public static Object getLocal() {
    return new Object() { };
  }
}
