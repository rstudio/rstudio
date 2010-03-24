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

/**
 * TODO: document me.
 */
public class ListBoxProfile extends WidgetProfile {

  public void testTiming() throws Exception {
    testTiming(1);
    testTiming(2);
    testTiming(4);
    testTiming(8);
    testTiming(16);
    testTiming(32);
    testTiming(64);
    testTiming(128);
    testTiming(256);
    testTiming(512);
    testTiming(1024);

    throw new Exception("Finished profiling");
  }

  public void testTiming(int i) {
    addTiming(i);
  }

  public void addTiming(int num) {
    ListBox b = new ListBox();
    RootPanel.get().add(b);
    resetTimer();
    for (int i = 0; i < num; i++) {
      // CHECKSTYLE_OFF
      b.addItem("item" + i, "i:" + i);
      // CHECKSTYLE_ON
    }
    timing("add(" + num + ")");
  }
}
