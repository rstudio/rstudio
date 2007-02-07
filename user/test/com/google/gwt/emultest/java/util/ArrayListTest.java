/*
 * Copyright 2006 Google Inc.
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

import com.google.gwt.core.client.GWT;

import org.apache.commons.collections.TestArrayList;

import java.util.ArrayList;
import java.util.List;

/** Tests ArrayList, and, by extension AbstractList.  Uses inheritance to 
 * inherit all of Apache's TestList and TestCollection. */
public class ArrayListTest extends TestArrayList {
  public ArrayListTest() {
  }

  protected List makeEmptyList() {
    return new ArrayList();
  }

  public void testAddWatch() {
    ArrayList s = new ArrayList();
    s.add("watch");
    assertEquals(s.get(0), "watch");
  }


  public void testRemoveRange() {
    if (GWT.isScript()) {
      ArrayList l = new ArrayList();
      for (int i = 0; i < 10; i++) {
        l.add(new Integer(i));
      }
      verifyRemoveRangeWorks(l);
    }
  }

  private native void verifyRemoveRangeWorks(ArrayList l) /*-{
    var startIndex = l.@java.util.ArrayList::startIndex;
    var endIndex = l.@java.util.ArrayList::endIndex;
    var array = l.@java.util.ArrayList::array;
    l.@java.util.ArrayList::removeRange(II)(0,2);
    if (array[startIndex] !== undefined) {
      @junit.framework.Assert::fail(Ljava/lang/String;)("startIndex should be empty");
    }
    if (array[startIndex + 1] !== undefined) {
      @junit.framework.Assert::fail(Ljava/lang/String;)("startIndex + 1 should be empty");
    }
    if (array[startIndex + 2] === undefined) {
      @junit.framework.Assert::fail(Ljava/lang/String;)("startIndex + 2 should not be empty");
    }
    l.@java.util.ArrayList::removeRange(II)(6,8);
    if (array[endIndex - 3] === undefined) {
      @junit.framework.Assert::fail(Ljava/lang/String;)("endIndex - 3 should not be empty");
    }
    if (array[endIndex - 2] !== undefined) {
      @junit.framework.Assert::fail(Ljava/lang/String;)("endIndex - 2 should be empty");
    }
    if (array[endIndex - 1] !== undefined) {
      @junit.framework.Assert::fail(Ljava/lang/String;)("endIndex - 1 should be empty");
    }
  }-*/;
  
}
