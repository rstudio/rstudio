// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.test;

import com.google.gwt.junit.client.GWTTestCase;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class InnerClassTest extends GWTTestCase {

  public class InnerClass {
    void callInner() {
      testAppend.append("a");
      class ReallyInnerClass {
        void callReallyInner() {
          testAppend.append("b");
        }

        {
          callReallyInner();
        }
      }
      new ReallyInnerClass();
    }

    {
      callInner();
    }
  }

  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  public void testInnerClassInitialization() {
    assertEquals("ab", testAppend.toString());
  }

  public void testInnerClassLoop() {
    final StringBuffer b = new StringBuffer();
    List results = new ArrayList();
    abstract class AppendToStringBuffer {
      public AppendToStringBuffer(int i) {
        this.num = i;
      }

      public abstract void act();

      int num;
    }
    for (int i = 0; i < 10; i++) {
      AppendToStringBuffer ap = new AppendToStringBuffer(i) {
        public void act() {
          b.append(num);
        }
      };
      results.add(ap);
    }
    for (Iterator it = results.iterator(); it.hasNext();) {
      AppendToStringBuffer theAp = (AppendToStringBuffer) it.next();
      theAp.act();
    }
    assertEquals("0123456789", b.toString());
  }

  protected void setUp() throws Exception {
    testAppend = new StringBuffer();
    new InnerClass();
  }

  private StringBuffer testAppend;

}
