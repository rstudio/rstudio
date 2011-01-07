/**
 * This code must be kept in sync with {@link com.google.gwt.dev.javac.TypeOracleMediatorTestBase}
 */
package com.google.gwt.dev.javac.mediatortest;

import java.util.List;

public class GenericOuter<V> {
   public class Inner {
     private V field;
     private List<V> list;
   }
}
