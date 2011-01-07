/**
 * This code must be kept in sync with {@link com.google.gwt.dev.javac.TypeOracleMediatorTestBase}
 */
package com.google.gwt.dev.javac.mediatortest;

public class BindToTypeScope {
  public static class Object { }
  // Fails when loaded from bytecode
  public static class DerivedObject extends Object { }
}
