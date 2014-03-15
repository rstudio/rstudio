/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.javac.mediatortest;

/**
 * A class that contains fields of various types. <p>
 *
 * This code must be kept in sync with {@link com.google.gwt.dev.javac.TypeOracleUpdaterTestBase}.
 */
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
