/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.editor.client.adapters;

import com.google.gwt.editor.client.LeafValueEditor;

/**
 * A trivial implementation of LeafValueEditor than can be used for "hidden"
 * properties when composing UI-based Editors.
 * 
 * @param <T> the type of data being held
 */
public class SimpleEditor<T> implements LeafValueEditor<T> {
  /**
   * Returns a new ValueEditor that holds a {@code null} value.
   *
   * @return a SimpleEditor instance holding a {@code null} value
   */
  public static <T> SimpleEditor<T> of() {
    return new SimpleEditor<T>(null);
  }

  /**
   * Returns a new ValueEditor that holds the given value.
   *
   * @param value a data object of type T
   * @return a SimpleEditor instance holding the given value
   */
  public static <T> SimpleEditor<T> of(T value) {
    return new SimpleEditor<T>(value);
  }

  private T value;

  /**
   * Constructs a new SimpleEditor that holds the given value.
   *
   * @param value a data object of type T
   */
  protected SimpleEditor(T value) {
    this.value = value;
  }

  public T getValue() {
    return value;
  }

  public void setValue(T value) {
    this.value = value;
  }
}
