/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.dev.jjs.test;

import java.util.ArrayList;

/**
 * A subclass that overrides {@link #method(ArrayList, int)} to see what the values of its own
 * fields are from within the superclass's cstr (before our own cstr has run).
 * 
 * See {@link CompilerTest#testFieldInitializationOrder()}.
 */
class FieldInitOrderChild extends FieldInitOrderBase {

  private final int i1 = 1;
  private int i2 = 1;
  private Integer i3 = new Integer(1);
  private Integer i4;
  private final static int i5 = 1;
  private static int i6 = 1;
  private static Integer i7 = new Integer(1);

  FieldInitOrderChild(ArrayList<String> seenValues) {
    // the superclass calls method(), which will record the pre-cstr value of our fields
    super(seenValues, 2);
    recordValues(seenValues);
  }

  // invoked by the super classes before our cstr has run
  @Override
  void method(ArrayList<String> seenValues, int x) {
    recordValues(seenValues);
    // i1 is final
    i2 = x;
    i3 = new Integer(x);
    i4 = new Integer(x);
    // i5 is final
    i6 = 2;
    i7 = new Integer(x);
  }

  private void recordValues(ArrayList<String> seenValues) {
    // i3, i4 and i7 would be directly converted into strings hence show undefined instead of null.
    // String operations should take care of corner cases where a string is null or undefined
    // see issue 8257.
    seenValues.add("i1=" + i1 + ",i2=" + i2 + ",i3=" + (i3 == null ? "null" : i3) +
        ",i4=" +  (i4 == null ? "null" : i4) + ",i5=" + i5 + ",i6=" + i6
        + ",i7=" + (i7 == null ? "null" : i7));
  }
}