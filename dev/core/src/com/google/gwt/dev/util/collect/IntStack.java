/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.util.collect;

import cern.colt.list.IntArrayList;

import java.io.Serializable;

/**
 * An int stack.
 * <p>
 * Because only int primitives are used performance and memory usage can surpass Object stacks.
 */
public class IntStack implements Serializable {

  private IntArrayList values = new IntArrayList();

  public boolean isEmpty() {
    return values.isEmpty();
  }

  public int pop() {
    int lastIndex = values.size() - 1;
    int value = values.get(lastIndex);
    values.remove(lastIndex);
    return value;
  }

  public void push(int value) {
    values.add(value);
  }
}
