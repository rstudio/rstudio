// Copyright 2006 Google Inc. All Rights Reserved. 

package com.google.gwt.user.client.ui;

import java.util.ArrayList;
import java.util.Vector;

/**
 * Profile class.
 */
public class ArrayListAndVectorProfile extends WidgetProfile {

  private static final int UPPER_BOUND = 40000;
  private static final int LOWER_BOUND = 1024;

  public void testTiming() throws Exception {

    for (int i = LOWER_BOUND; i < UPPER_BOUND; i = i * 2) {
      testTiming(i);
    }

    throw new Exception("Finished profiling");
  }

  public void testTiming(int i) {
    arrayListTiming(i);
    vectorTiming(i);
  }

  public void vectorTiming(int num) {
    resetTimer();
    Vector v = new Vector();
    for (int i = 0; i < num; i++) {
      v.add("hello");
    }
    timing("vector | add(" + num + ")");
    resetTimer();
    for (int k = 0; k < num; k++) {
      v.get(k);
    }

    timing("vector | get(" + num  + ")");
  }

  public void arrayListTiming(int num) {
    resetTimer();
    ArrayList v = new ArrayList();
    for (int i = 0; i < num; i++) {
      v.add("hello");
    }
    timing("arrayList | add(" + num + ")");
    resetTimer();
    for (int k = 0; k < num; k++) {
      v.get(k);
    }

    timing("arrayList | get(" + num +")");
  }
}
