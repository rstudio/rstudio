/**
 * This code must be kept in sync with {@link com.google.gwt.dev.javac.TypeOracleMediatorTestBase}
 */
package com.google.gwt.dev.javac.mediatortest;

/**
 * Test of a parameterized interface declared inside another interface.
 *
 * @param <T>
 */
public class DeclaresGenericInnerInterface<T> {
  public interface Inner<T> {}
}
