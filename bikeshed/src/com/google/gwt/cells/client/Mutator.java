package com.google.gwt.cells.client;

public interface Mutator<T, C> {
  void mutate(T object, C after);
}
