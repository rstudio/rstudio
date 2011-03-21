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

import com.google.gwt.editor.client.CompositeEditor;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorContext;
import com.google.gwt.editor.client.EditorDelegate;
import com.google.gwt.editor.client.EditorVisitor;
import com.google.gwt.editor.client.HasEditorDelegate;
import com.google.gwt.editor.client.HasEditorErrors;
import com.google.gwt.editor.client.LeafValueEditor;
import com.google.gwt.editor.client.ValueAwareEditor;

/**
 * Base implementation of EditorContext.
 * 
 * @param <T> the type of data being edited
 */
public abstract class AbstractEditorContext<T> implements EditorContext<T> {
  private final String path;
  private final CompositeEditor<?, ?, ?> compositeEditor;
  private AbstractEditorDelegate<T, ?> delegate;
  private final Editor<T> editor;
  private final HasEditorDelegate<T> hasEditorDelegate;
  private final HasEditorErrors<T> hasEditorErrors;
  private boolean isHalted;
  private final LeafValueEditor<T> leafValueEditor;
  private final ValueAwareEditor<T> valueAwareEditor;

  public AbstractEditorContext(Editor<T> editor, String path) {
    this.editor = editor;
    this.path = path;
    /*
     * TODO(bobv): Determine if pre-casting is better than demand-casting or
     * generating the asFoo methods.
     */
    compositeEditor = editor instanceof CompositeEditor<?, ?, ?>
        ? (CompositeEditor<?, ?, ?>) editor : null;
    hasEditorDelegate = editor instanceof HasEditorDelegate<?>
        ? (HasEditorDelegate<T>) editor : null;
    hasEditorErrors = editor instanceof HasEditorErrors<?>
        ? (HasEditorErrors<T>) editor : null;
    leafValueEditor = editor instanceof LeafValueEditor<?>
        ? (LeafValueEditor<T>) editor : null;
    valueAwareEditor = editor instanceof ValueAwareEditor<?>
        ? (ValueAwareEditor<T>) editor : null;
  }

  @SuppressWarnings("unchecked")
  public CompositeEditor<T, ?, ?> asCompositeEditor() {
    return (CompositeEditor<T, ?, ?>) compositeEditor;
  }

  public HasEditorDelegate<T> asHasEditorDelegate() {
    return hasEditorDelegate;
  }

  public HasEditorErrors<T> asHasEditorErrors() {
    return hasEditorErrors;
  }

  public LeafValueEditor<T> asLeafValueEditor() {
    return leafValueEditor;
  }

  public ValueAwareEditor<T> asValueAwareEditor() {
    return valueAwareEditor;
  }

  public abstract boolean canSetInModel();

  public abstract T checkAssignment(Object value);

  @SuppressWarnings(value = {"rawtypes", "unchecked"})
  public void doTraverseSyntheticCompositeEditor(EditorVisitor visitor) {
    Editor<?> sample = this.asCompositeEditor().createEditorForTraversal();
    AbstractEditorDelegate subDelegate = delegate.createComposedDelegate();
    delegate.addSubDelegate(subDelegate, path, sample);
    delegate.getEditorChain().traverse(visitor, subDelegate);
  }

  public String getAbsolutePath() {
    // Not delegate.getPath() since delegate might be null for a leaf editor
    return path;
  }

  public abstract Class<T> getEditedType();

  public Editor<T> getEditor() {
    return editor;
  }

  public EditorDelegate<T> getEditorDelegate() {
    return delegate;
  }

  public abstract T getFromModel();

  public void halt() {
    isHalted = true;
  }

  public boolean isHalted() {
    return isHalted;
  }

  public void setEditorDelegate(AbstractEditorDelegate<T, ?> delegate) {
    this.delegate = delegate;
  }

  public abstract void setInModel(T data);

  public void traverse(EditorVisitor visitor, AbstractEditorDelegate<?, ?> next) {
    if (visitor.visit(this) && next != null) {
      next.accept(visitor);
    }
    visitor.endVisit(this);
  }

  public void traverseSyntheticCompositeEditor(EditorVisitor visitor) {
    if (asCompositeEditor() == null) {
      throw new IllegalStateException();
    }
    doTraverseSyntheticCompositeEditor(visitor);
  }
}
