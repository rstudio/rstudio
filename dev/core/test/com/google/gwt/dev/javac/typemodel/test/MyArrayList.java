package com.google.gwt.dev.javac.typemodel.test;

import java.util.ArrayList;

/**
 * A subtype of ArrayList.
 */
public class MyArrayList<T> extends ArrayList<T> {

  @Override
  public boolean add(T o) {
    return super.add(o);
  }
}
