/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.dev.util.collect;

import com.google.gwt.thirdparty.guava.common.collect.Lists;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A Stack based on {@link ArrayList}. Unlike {@link java.util.ArrayDeque},
 * this one allows {@code null} values.
 *
 * @param <T> the value type
 */
public final class Stack<T> implements Iterable<T> {

  private ArrayList<T> elements = Lists.newArrayList();

  /**
   * Returns the number of elements in the stack (including pushed nulls).
   */
  public int size() {
    return elements.size();
  }

  /**
   * Returns true if the stack contains element, false otherwise.
   */
  public boolean contains(T element) {
    return elements.contains(element);
  }

  /**
   * Returns true if the stack is empty false otherwise.
   */
  public boolean isEmpty() {
    return elements.isEmpty();
  }

  @Override
  public Iterator<T> iterator() {
    return elements.iterator();
  }

  /**
   * Returns the top of the stack.
   */
  public T peek() {
    return elements.get(elements.size() - 1);
  }

  /**
   * Returns the element at location index (from bottom of stack).
   */
  public T peekAt(int index) {
    return elements.get(index);
  }

  /**
   * Returns the top of the stack and removes it.
   * @return
   */
  public T pop() {
    return elements.remove(elements.size() - 1);
  }

  /**
   * Pops {@code count} elements from the stack and returns them as a list with to top of the stack
   * last.
   */
  public List<T> pop(int count) {
    int size = elements.size();
    List<T> nodesToPop = elements.subList(size - count, size);
    List<T> result = Lists.newArrayList(nodesToPop);
    nodesToPop.clear();
    return result;
  }

  public void push(T value) {
    elements.add(value);
  }

  /**
   * Creates a new Stack for type {@code T}.
   */
  public static <T> Stack<T> create() {
    return new Stack<T>();
  }
}
