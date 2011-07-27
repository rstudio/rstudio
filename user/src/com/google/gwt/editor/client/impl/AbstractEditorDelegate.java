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

import com.google.gwt.editor.client.CompositeEditor;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorDelegate;
import com.google.gwt.editor.client.EditorError;
import com.google.gwt.editor.client.EditorVisitor;
import com.google.gwt.event.shared.HandlerRegistration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A base implementation of EditorDelegate for use by generated types.
 * 
 * @param <T> the type of object being edited
 * @param <E> the type of editor
 */
public abstract class AbstractEditorDelegate<T, E extends Editor<T>> implements
    EditorDelegate<T> {

  /**
   * The machinery for attaching and detaching editors from the hierarchy via a
   * {@link CompositeEditor}. An instance of a Chain is only created when
   * necessary for a given hierarchy type.
   * 
   * @param <R> the component element type
   * @param <S> the component editor type
   */
  protected class Chain<R, S extends Editor<R>> implements
      CompositeEditor.EditorChain<R, S> {
    private final CompositeEditor<T, R, S> composedEditor;
    private final Class<R> composedElementType;
    private final Map<S, AbstractEditorDelegate<R, S>> map = new LinkedHashMap<S, AbstractEditorDelegate<R, S>>();

    /**
     * Constructed via
     * {@link AbstractEditorDelegate#createChain(CompositeEditor)}.
     */
    Chain(CompositeEditor<T, R, S> composedEditor, Class<R> composedElementType) {
      this.composedEditor = composedEditor;
      this.composedElementType = composedElementType;
    }

    public void accept(EditorVisitor visitor) {
      for (AbstractEditorDelegate<R, S> delegate : map.values()) {
        traverse(visitor, delegate);
      }
    }

    public void attach(R object, S subEditor) {
      AbstractEditorDelegate<R, S> subDelegate = map.get(subEditor);

      String subPath = path + composedEditor.getPathElement(subEditor);

      if (subDelegate == null) {
        @SuppressWarnings("unchecked")
        AbstractEditorDelegate<R, S> temp = (AbstractEditorDelegate<R, S>) createComposedDelegate();
        subDelegate = temp;
        map.put(subEditor, subDelegate);
        addSubDelegate(subDelegate, subPath, subEditor);
      } else {
        subDelegate.path = subPath;
      }
      subDelegate.setObject(ensureMutable(object));
      traverse(createInitializerVisitor(), subDelegate);
    }

    public void detach(S subEditor) {
      map.remove(subEditor);
    }

    public R getValue(S subEditor) {
      AbstractEditorDelegate<R, S> subDelegate = map.get(subEditor);
      if (subDelegate == null) {
        return null;
      }
      return subDelegate.getObject();
    }

    void traverse(EditorVisitor visitor, AbstractEditorDelegate<R, S> delegate) {
      R object = delegate.getObject();
      new RootEditorContext<R>(delegate, composedElementType, object).traverse(
          visitor, delegate);
    }
  }

  protected static String appendPath(String prefix, String path) {
    if ("".equals(prefix)) {
      return path;
    } else {
      return prefix + "." + path;
    }
  }

  private boolean dirty;
  private Chain<?, ?> editorChain;
  private List<EditorError> errors;
  private String path;

  public abstract void accept(EditorVisitor visitor);

  public abstract T getObject();

  public String getPath() {
    return path;
  }

  /**
   * Just returns the last value passed to {@link #setDirty(boolean)}.
   */
  public boolean isDirty() {
    return dirty;
  }

  public void recordError(String message, Object value, Object userData) {
    EditorError error = new SimpleError(this, message, value, userData);
    errors.add(error);
  }

  public void recordError(String message, Object value, Object userData,
      String extraPath, Editor<?> leafEditor) {
    EditorError error = new SimpleError(this, message, value, userData,
        extraPath, leafEditor);
    errors.add(error);
  }

  public void setDirty(boolean dirty) {
    this.dirty = dirty;
  }

  public abstract HandlerRegistration subscribe();

  /**
   * Initialize a sub-delegate whenever one is added to the editor hierarchy.
   */
  protected <R, S extends Editor<R>> void addSubDelegate(
      AbstractEditorDelegate<R, S> subDelegate, String path, S subEditor) {
    subDelegate.initialize(path, subEditor);
  }

  protected String appendPath(String path) {
    if (path.length() == 0) {
      return this.path;
    }
    return appendPath(this.path, path);
  }

  protected <R, S extends Editor<R>> void createChain(
      Class<R> composedElementType) {
    @SuppressWarnings("unchecked")
    CompositeEditor<T, R, S> editor = (CompositeEditor<T, R, S>) getEditor();
    editorChain = new Chain<R, S>(editor, composedElementType);
  }

  /**
   * Only implemented by delegates for a {@link CompositeEditor}.
   */
  protected AbstractEditorDelegate<?, ?> createComposedDelegate() {
    throw new IllegalStateException();
  }

  protected EditorVisitor createInitializerVisitor() {
    return new Initializer();
  }

  protected <Q> Q ensureMutable(Q object) {
    return object;
  }

  protected abstract E getEditor();

  protected Chain<?, ?> getEditorChain() {
    return editorChain;
  }

  protected List<EditorError> getErrors() {
    return errors;
  }

  protected void initialize(String pathSoFar, E editor) {
    this.path = pathSoFar;
    setEditor(editor);
    errors = new ArrayList<EditorError>();
    initializeSubDelegates();
  }

  protected abstract void initializeSubDelegates();

  protected abstract void setEditor(E editor);

  protected abstract void setObject(T object);

  /**
   * Indicates whether or not calls to {@link #flush} are expected as part of
   * normal operation.
   */
  protected boolean shouldFlush() {
    return true;
  }
}