// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.user.client.ui;

import java.util.Map;

public class FastStringMapProfile extends WidgetProfile {

  Map m;

  public void testTiming() throws Exception {
    m = putTiming(32000);
    timing(2000);
    timing(4000);
    timing(8000);
    timing(16000);
    timing(32000);
    throw new Exception("|browser|test case|time|");

  }

  private void timing(int s) {
    getTiming(s);
  }

  public void getTiming(int size) {
    this.resetTimer();
    for (int i = 0; i < size; i++) {
      m.get(size + ":");
    }
    this.timing("get(" + size + ")");

  }

  public FastStringMap putTiming(int size) {
    FastStringMap m1 = new FastStringMap();
    this.resetTimer();
    for (int i = 0; i < size; i++) {
      Integer iVal = new Integer(size);
      m1.put(iVal.hashCode() + "", iVal);
    }
    this.timing("put(" + size + ")");
    return m1;
  }

  public int instanceOfTiming() {
    int count = 0;
    int size = 20000;
    Object a = new String();

    this.resetTimer();
    for (int i = 0; i < size; i++) {
      if (a instanceof String) {
        ++count;
      }
    }
    this.timing("instanceOf(" + size + "," + a + ")");
    return count;
  }
}
