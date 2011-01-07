/**
 * This code must be kept in sync with {@link com.google.gwt.dev.javac.TypeOracleMediatorTestBase}
 */
package com.google.gwt.dev.javac.mediatortest;

/**
 * Test of a parameterized class declared inside another class.
 *
 * @param <T> Some type
 */
public class DeclaresGenericInnerType<T> {
  public interface Inner<T> {}
}
