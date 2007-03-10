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

    timing("arrayList | get(" + num + ")");
  }
}
