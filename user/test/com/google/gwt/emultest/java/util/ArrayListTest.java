// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.emultest.java.util;

import org.apache.commons.collections.TestArrayList;

import java.util.ArrayList;
import java.util.List;

/** Tests ArrayList, and, by extention AbstractList */
public class ArrayListTest extends TestArrayList {
  public ArrayListTest() {
  }

  protected List makeEmptyList() {
    return new ArrayList();
  }

}
