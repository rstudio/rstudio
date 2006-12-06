// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.emultest.java.util;

import org.apache.commons.collections.TestComparator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ComparatorTest extends TestComparator {
  public Comparator makeComparator() {
    return new DummyComparator();
  }

  public List getComparableObjectsOrdered() {
    List l = new ArrayList();
    l.add("x");
    l.add("y");
    l.add("z");
    return l;
  }
}

/** list comparator for testing */
class DummyComparator implements Comparator {
  /** compares returns reverse hash order */
  public int compare(Object arg0, Object arg1) {
    int a = arg0.hashCode();
    int b = arg1.hashCode();
    int accum;
    if (a == b) {
      accum = 0;
    } else if (a > b) {
      accum = 1;
    } else {
      accum = -1;
    }
    return accum;
  }
}