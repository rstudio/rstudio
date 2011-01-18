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
package com.google.gwt.editor.client.impl;

import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorError;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;

import java.util.ArrayList;
import java.util.List;

import javax.validation.ConstraintViolation;

/**
 * A base implementation class for generated SimpleBeanEditorDriver
 * implementations.
 * 
 * @param <T> the type being edited
 * @param <E> the Editor type
 */
public abstract class AbstractSimpleBeanEditorDriver<T, E extends Editor<T>>
    implements SimpleBeanEditorDriver<T, E> {

  private SimpleBeanEditorDelegate<T, E> delegate;
  private DelegateMap delegateMap = new DelegateMap(DelegateMap.IDENTITY);
  private E editor;
  private List<EditorError> errors;
  private T object;

  public void edit(T object) {
    checkEditor();
    this.object = object;
    delegate = createDelegate();
    delegate.initialize("", object, editor, delegateMap);
    delegateMap.put(object, delegate);
  }

  public T flush() {
    checkDelegate();
    errors = new ArrayList<EditorError>();
    delegate.flush(errors);
    return object;
  }

  public List<EditorError> getErrors() {
    return errors;
  }

  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  public void initialize(E editor) {
    this.editor = editor;
  }

  public boolean setConstraintViolations(
      final Iterable<ConstraintViolation<?>> violations) {
    checkDelegate();
    SimpleViolation.pushViolations(
        SimpleViolation.iterableFromConstrantViolations(violations),
        delegateMap);

    // Flush the errors, which will take care of co-editor chains.
    errors = new ArrayList<EditorError>();
    delegate.flushErrors(errors);
    return hasErrors();
  }

  protected abstract SimpleBeanEditorDelegate<T, E> createDelegate();

  /**
   * Visible for testing.
   */
  DelegateMap getDelegateMap() {
    return delegateMap;
  }

  private void checkDelegate() {
    if (delegate == null) {
      throw new IllegalStateException("Must call edit() first");
    }
  }

  private void checkEditor() {
    if (editor == null) {
      throw new IllegalStateException("Must call initialize() first");
    }
  }

}
