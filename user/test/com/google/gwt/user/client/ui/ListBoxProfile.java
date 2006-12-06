// Copyright 2006 Google Inc. All Rights Reserved. 

package com.google.gwt.user.client.ui;

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
      b.addItem("item" + i, "i:" + i);
    }
    timing("add(" + num + ")");
  }
}
