package com.google.gwt.emultest.java.util;

import com.google.gwt.user.client.ui.WidgetProfile;

import java.util.Stack;

public class StackProfile extends WidgetProfile {

  /** Sets module name so that javascript compiler can operate */
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }


    public void testTiming() throws Exception {
      int t = 1;
      while (true) {
        testTiming(t);
        t = t * 2;
      }
      //throw new Exception("Finished profiling");
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

