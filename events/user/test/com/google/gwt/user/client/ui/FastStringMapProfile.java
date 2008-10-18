/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.user.client.ui;

import java.util.Map;

/**
 * TODO: document me.
 */
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
