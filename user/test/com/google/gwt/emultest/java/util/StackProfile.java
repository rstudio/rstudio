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
package com.google.gwt.emultest.java.util;

import com.google.gwt.user.client.ui.WidgetProfile;

import java.util.Stack;

/**
 * TODO: document me.
 */
public class StackProfile extends WidgetProfile {

  /**
   * Sets module name so that javascript compiler can operate.
   */
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  public void testTiming() throws Exception {
    int t = 1;
    while (true) {
      testTiming(t);
      t = t * 2;
    }
    // throw new Exception("Finished profiling");
  }

  public void testTiming(int i) {
    addTiming(i);
  }

  public void addTiming(int num) {
    Stack s = new Stack();
    resetTimer();
    for (int i = 0; i < num; i++) {
      s.push("item" + i);
    }
    timing("push(" + num + ")");
    resetTimer();
    for (int i = 0; i < num; i++) {
      s.pop();
    }
    timing("pop(" + num + ")");
  }
}
