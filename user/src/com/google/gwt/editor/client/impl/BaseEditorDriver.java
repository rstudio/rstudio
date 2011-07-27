/*
 * Copyright 2011 Google Inc.
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

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorDriver;
import com.google.gwt.editor.client.EditorError;
import com.google.gwt.editor.client.EditorVisitor;
import com.google.gwt.editor.client.LeafValueEditor;
import com.google.gwt.editor.client.impl.DelegateMap.KeyMethod;
import com.google.gwt.editor.client.testing.EditorHierarchyPrinter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.validation.ConstraintViolation;

/**
 * Contains common code shared between the SimpleBeanEditorDriver and
 * RequestFactoryEditorDriver.
 * 
 * @param <T> the type of data being edited
 * @param <E> the type of editor
 */
public abstract class BaseEditorDriver<T, E extends Editor<T>> {
  private AbstractEditorDelegate<T, E> delegate;
  /**
   * Used for {@link #isDirty()} computations.
   */
  private Map<LeafValueEditor<?>, Object> leafValueMap;
  private E editor;
  private List<EditorError> errors;
  private T object;

  public abstract void accept(EditorVisitor visitor);

  public List<EditorError> getErrors() {
    return errors;
  }

  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  public boolean isDirty() {
    DirtCollector c = new DirtCollector();
    accept(c);
    return c.isDirty() || !leafValueMap.equals(c.getLeafValues());
  }

  public boolean setConstraintViolations(final Iterable<ConstraintViolation<?>> violations) {
    return doSetViolations(SimpleViolation.iterableFromConstrantViolations(violations));
  }

  @Override
  public String toString() {
    if (GWT.isProdMode()) {
      return super.toString();
    } else {
      return editor == null ? "Uninitialized" : EditorHierarchyPrinter.toString(asEditorDriver());
    }
  }

  protected void configureDelegate(AbstractEditorDelegate<T, E> rootDelegate) {
    rootDelegate.initialize("", getEditor());
  }

  protected abstract AbstractEditorDelegate<T, E> createDelegate();

  protected EditorVisitor createInitializerVisitor() {
    return new Initializer();
  }

  protected void doEdit(T object) {
    checkEditor();
    object = delegate.ensureMutable(object);
    this.object = object;
    delegate.setObject(object);
    accept(createInitializerVisitor());
    DirtCollector c = new DirtCollector();
    accept(c);
    leafValueMap = c.getLeafValues();
  }

  protected void doFlush() {
    checkObject();
    errors = new ArrayList<EditorError>();
    accept(new Flusher());
    accept(new ErrorCollector(errors));
  }

  protected void doInitialize(E editor) {
    this.editor = editor;
    delegate = createDelegate();
    configureDelegate(delegate);
  }

  protected boolean doSetViolations(Iterable<SimpleViolation> violations) {
    checkObject();
    SimpleViolation.pushViolations(violations, asEditorDriver(), getViolationKeyMethod());

    // Collect the errors, which will take care of co-editor chains.
    errors = new ArrayList<EditorError>();
    accept(new ErrorCollector(errors));
    return hasErrors();
  }

  protected AbstractEditorDelegate<T, E> getDelegate() {
    return delegate;
  }

  protected E getEditor() {
    return editor;
  }

  protected T getObject() {
    return object;
  }

  protected KeyMethod getViolationKeyMethod() {
    return DelegateMap.IDENTITY;
  }

  /**
   * This cast avoids the need to add another parameterization to
   * BaseEditorDriver since the class cannot be declared to extend an unbound
   * interface.
   */
  private EditorDriver<?> asEditorDriver() {
    return (EditorDriver<?>) this;
  }

  private void checkEditor() {
    if (editor == null) {
      throw new IllegalStateException("Must call initialize() first");
    }
  }

  private void checkObject() {
    if (object == null) {
      throw new IllegalStateException("Must call edit() first");
    }
  }
}
