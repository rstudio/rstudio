/**
 * This code must be kept in sync with {@link com.google.gwt.dev.javac.TypeOracleMediatorTestBase}
 */
package com.google.gwt.dev.javac.mediatortest;

public class Implementations {
  public static class OuterImpl<K,V> implements OuterInt<K,V> {}
  public static class InnerImpl<V> implements OuterInt.InnerInt<V> {}
}
