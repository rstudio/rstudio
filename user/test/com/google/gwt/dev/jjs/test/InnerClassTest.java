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
package com.google.gwt.dev.jjs.test;

import com.google.gwt.junit.client.GWTTestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO: document me.
 */
public class InnerClassTest extends GWTTestCase {

  class InnerClass {
    {
      callInner();
    }

    void callInner() {
      testAppend.append("a");
      class ReallyInnerClass {
        {
          callReallyInner();
        }

        void callReallyInner() {
          testAppend.append("b");
        }
      }
      new ReallyInnerClass();
    }
  }

  static class P1<T1> {
    class P2<T2> extends P1<T1> {
      class P3<T3> extends P2<T2> {
        P3() {
          this(1);
        }

        P3(int i) {
          P2.this.super(i);
        }
      }

      P2() {
        this(1);
      }

      P2(int i) {
        super(i);
      }
    }

    final int value;

    P1() {
      this(1);
    }

    P1(int i) {
      value = i;
    }
  }

  private StringBuffer testAppend;

  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  public void testInnerClassCtors() {
    P1<?> p1 = new P1<Object>();
    assertEquals(1, p1.value);
    assertEquals(2, new P1<Object>(2).value);
    P1<?>.P2<?> p2 = p1.new P2<Object>();
    assertEquals(1, p2.value);
    assertEquals(2, p1.new P2<Object>(2).value);
    assertEquals(1, p2.new P3<Object>().value);
    assertEquals(2, p2.new P3<Object>(2).value);
  }

  public void testInnerClassInitialization() {
    testAppend = new StringBuffer();
    new InnerClass();
    assertEquals("ab", testAppend.toString());
  }

  public void testInnerClassLoop() {
    final StringBuffer b = new StringBuffer();
    abstract class AppendToStringBuffer {
      int num;

      public AppendToStringBuffer(int i) {
        this.num = i;
      }

      public abstract void act();
    }
    List<AppendToStringBuffer> results = new ArrayList<AppendToStringBuffer>();
    for (int i = 0; i < 10; i++) {
      AppendToStringBuffer ap = new AppendToStringBuffer(i) {
        public void act() {
          b.append(num);
        }
      };
      results.add(ap);
    }
    for (AppendToStringBuffer theAp : results) {
      theAp.act();
    }
    assertEquals("0123456789", b.toString());
  }

}
