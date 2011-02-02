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
package com.google.gwt.editor.client.testing;

import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorError;
import com.google.gwt.editor.client.EditorVisitor;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;

import java.util.Collections;
import java.util.List;

import javax.validation.ConstraintViolation;

/**
 * A no-op implementation of {@link SimpleBeanEditorDriver} that records its
 * inputs.
 * 
 * @param <T> the type being edited
 * @param <E> the Editor type
 */
public class MockSimpleBeanEditorDriver<T, E extends Editor<T>> implements
    SimpleBeanEditorDriver<T, E> {

  private E editor;
  private T object;

  /**
   * A no-op method.
   */
  public void accept(EditorVisitor visitor) {
  }

  /**
   * Records <code>object</code>.
   */
  public void edit(T object) {
    this.object = object;
  }

  /**
   * Returns <code>null</code> or the last value provided to {@link #edit}.
   */
  public T flush() {
    return object;
  }

  /**
   * Returns <code>null</code> or the last value provided to {@link #initialize}
   * .
   */
  public E getEditor() {
    return editor;
  }

  /**
   * Returns an empty list.
   */
  public List<EditorError> getErrors() {
    return Collections.emptyList();
  }

  /**
   * Returns <code>null</code> or the last value provided to {@link #edit}.
   */
  public T getObject() {
    return object;
  }

  /**
   * Returns <code>false</code>.
   */
  public boolean hasErrors() {
    return false;
  }

  /**
   * Records <code>editor</code>.
   */
  public void initialize(E editor) {
    this.editor = editor;
  }

  /**
   * Returns {@code false}.
   */
  public boolean isDirty() {
    return false;
  }

  /**
   * A no-op method that always returns false.
   */
  public boolean setConstraintViolations(
      Iterable<ConstraintViolation<?>> violations) {
    return false;
  }
}
