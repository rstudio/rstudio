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
import com.google.gwt.editor.client.HasEditorDelegate;
import com.google.gwt.editor.client.HasEditorErrors;
import com.google.gwt.editor.client.LeafValueEditor;
import com.google.gwt.editor.client.ValueAwareEditor;
import com.google.gwt.event.shared.HandlerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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

  private class Chain<R, S extends Editor<R>> implements
      CompositeEditor.EditorChain<R, S> {
    private Map<S, AbstractEditorDelegate<R, S>> map = new LinkedHashMap<S, AbstractEditorDelegate<R, S>>();

    public void attach(R object, S subEditor) {
      AbstractEditorDelegate<R, S> subDelegate = createComposedDelegate();
      map.put(subEditor, subDelegate);
      @SuppressWarnings("unchecked")
      Editor<Object> temp = (Editor<Object>) subEditor;
      initializeSubDelegate(subDelegate, path
          + composedEditor.getPathElement(temp), object, subEditor);
    }

    public void detach(S subEditor) {
      map.remove(subEditor).flush(errors);
    }

    public R getValue(S subEditor) {
      AbstractEditorDelegate<R, S> subDelegate = map.get(subEditor);
      subDelegate.flush(errors);
      return subDelegate.getObject();
    }

    void collectErrors() {
      for (AbstractEditorDelegate<?, ?> delegate : map.values()) {
        errors.addAll(delegate.errors);
        delegate.errors.clear();
      }
    }
  }

  protected static String appendPath(String prefix, String path) {
    if ("".equals(prefix)) {
      return path;
    } else {
      return prefix + "." + path;
    }
  }

  protected CompositeEditor<T, Object, Editor<Object>> composedEditor;
  protected Chain<Object, Editor<Object>> editorChain;
  protected List<EditorError> errors = new ArrayList<EditorError>();
  protected HasEditorErrors<T> hasEditorErrors;
  protected LeafValueEditor<T> leafValueEditor;
  protected String path;
  /**
   * This field avoids needing to repeatedly cast {@link #editor}.
   */
  protected ValueAwareEditor<T> valueAwareEditor;

  public AbstractEditorDelegate() {
    super();
  }

  public abstract T ensureMutable(T object);

  public void flush(List<EditorError> errorAccumulator) {
    try {
      if (valueAwareEditor != null) {
        valueAwareEditor.flush();
      }

      if (leafValueEditor != null) {
        // See comment in initialize about LeafValueEditors
        setObject(leafValueEditor.getValue());
        return;
      }

      if (getObject() == null) {
        return;
      }
      setObject(ensureMutable(getObject()));
      flushSubEditors(errors);

      if (editorChain != null) {
        editorChain.collectErrors();
      }
    } finally {
      if (hasEditorErrors != null) {
        // Allow higher-priority co-editor to spy on errors
        for (Iterator<EditorError> it = errorAccumulator.iterator(); it.hasNext();) {
          EditorError error = it.next();
          if (error.getAbsolutePath().startsWith(getPath())) {
            errors.add(error);
            it.remove();
          }
        }
        // Include the trailing dot
        int length = getPath().length();
        int pathPrefixLength = length == 0 ? 0 : (length + 1);
        for (EditorError error : errors) {
          ((SimpleError) error).setPathPrefixLength(pathPrefixLength);
        }
        // Give all of the errors to the handler and use a new local accumulator
        hasEditorErrors.showErrors(Collections.unmodifiableList(errors));

        for (EditorError error : errors) {
          if (!error.isConsumed()) {
            errorAccumulator.add(error);
          }
        }

        // Reset local error list
        errors = new ArrayList<EditorError>();
      } else {
        errorAccumulator.addAll(errors);
        errors.clear();
      }
    }
  }

  public abstract T getObject();

  public String getPath() {
    return path;
  }

  public void recordError(String message, Object value, Object userData) {
    EditorError error = new SimpleError(this, message, value, userData);
    errors.add(error);
  }

  public abstract HandlerRegistration subscribe();

  protected String appendPath(String path) {
    return appendPath(this.path, path);
  }

  protected abstract void attachSubEditors();

  /**
   * Only implemented by delegates for a {@link CompositeEditor}.
   */
  protected <C, D extends Editor<C>> AbstractEditorDelegate<C, D> createComposedDelegate() {
    throw new IllegalStateException();
  }

  protected abstract void flushSubEditors(List<EditorError> errorAccumulator);

  protected abstract E getEditor();

  protected void initialize(String pathSoFar, T object, E editor) {
    this.path = pathSoFar;
    setEditor(editor);
    setObject(object);

    // Set up pre-casted fields to access the editor
    if (editor instanceof HasEditorErrors<?>) {
      hasEditorErrors = (HasEditorErrors<T>) editor;
    }
    if (editor instanceof LeafValueEditor<?>) {
      leafValueEditor = (LeafValueEditor<T>) editor;
    }
    if (editor instanceof HasEditorDelegate<?>) {
      ((HasEditorDelegate<T>) editor).setDelegate(this);
    }
    if (editor instanceof ValueAwareEditor<?>) {
      valueAwareEditor = (ValueAwareEditor<T>) editor;
      if (editor instanceof CompositeEditor<?, ?, ?>) {
        @SuppressWarnings("unchecked")
        CompositeEditor<T, Object, Editor<Object>> temp = (CompositeEditor<T, Object, Editor<Object>>) editor;
        composedEditor = temp;
        editorChain = new Chain<Object, Editor<Object>>();
        composedEditor.setEditorChain(editorChain);
      }
    }

    /*
     * Unusual case: The user may have installed an editor subtype that adds the
     * LeafValueEditor interface into a plain Editor field. If this has
     * happened, only set the value and don't descend into any sub-Editors.
     */
    if (leafValueEditor != null) {
      leafValueEditor.setValue(object);
      return;
    }

    if (valueAwareEditor != null) {
      valueAwareEditor.setValue(object);
    }
    if (object != null) {
      attachSubEditors();
    }
  }

  /**
   * Initialize a sub-delegate returned from {@link #createComposedDelegate()}.
   */
  protected abstract <R, S extends Editor<R>> void initializeSubDelegate(
      AbstractEditorDelegate<R, S> subDelegate, String path, R object,
      S subEditor);

  protected abstract void setEditor(E editor);

  protected abstract void setObject(T object);
}