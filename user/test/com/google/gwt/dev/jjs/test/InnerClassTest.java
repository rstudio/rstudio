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
import java.util.Iterator;
import java.util.List;

/**
 * TODO: document me.
 */
public class InnerClassTest extends GWTTestCase {

  /**
   * TODO: document me.
   */
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
