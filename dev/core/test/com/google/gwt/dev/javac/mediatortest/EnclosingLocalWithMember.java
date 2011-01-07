/**
 * This code must be kept in sync with {@link com.google.gwt.dev.javac.TypeOracleMediatorTestBase}
 */
package com.google.gwt.dev.javac.mediatortest;

public class EnclosingLocalWithMember {
  public int foo;
  public Object getLocal() {
    class MyObject {
      int getFoo() {
        return foo;
      }
    }
    return new MyObject() {
    };
  }
}
