/**
 * This code must be kept in sync with {@link com.google.gwt.dev.javac.TypeOracleMediatorTestBase}
 */
package com.google.gwt.dev.javac.mediatortest;

public class Fields {
  private int privateInt;
  private DefaultClass privateSomeType;
  protected int protectedInt;
  public int publicInt;
  int packageInt;
  private static int staticInt;
  private transient int transientInt;
  private volatile int volatileInt;
  public static final transient int multiInt = 0;
  private int[] intArray;
  private DefaultClass[] someTypeArray;
  private int[][] intArrayArray;
}
