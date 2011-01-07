/**
 * This code must be kept in sync with {@link com.google.gwt.dev.javac.TypeOracleMediatorTestBase}
 */
package com.google.gwt.dev.javac.mediatortest;

public class Methods {
    private int returnsInt() { return 0; };
    private Object returnsSomeType() { return null; }
    public static void staticMethod() { return; }
    public final void finalMethod() { return; }
    public void overloaded(int x, Object y) throws Throwable { return; }
    public Object overloaded(int x, char y) { return null; }
}
