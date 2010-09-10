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
import com.google.gwt.editor.client.LeafValueEditor;
import com.google.gwt.editor.client.ValueAwareEditor;
import com.google.gwt.event.shared.HandlerRegistration;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintViolation;

/**
 * A skeleton for an EditorDelegate.
 * 
 * @param <T> the type of object being edited
 * @param <E> the type of editor
 */
public abstract class AbstractEditorDelegate<T, E extends Editor<T>> implements
    EditorDelegate<T> {

  private class Chain<R, S extends Editor<R>> implements
      CompositeEditor.EditorChain<R, S> {
    private Map<S, AbstractEditorDelegate<R, S>> map = new IdentityHashMap<S, AbstractEditorDelegate<R, S>>();

    public void attach(R object, S subEditor) {
      AbstractEditorDelegate<R, S> subDelegate = createComposedDelegate();
      map.put(subEditor, subDelegate);
      @SuppressWarnings("unchecked")
      Editor<Object> temp = (Editor<Object>) subEditor;
      subDelegate.initialize(path + composedEditor.getPathElement(temp),
          object, subEditor);
    }

    public void detach(S subEditor) {
      map.remove(subEditor).flush();
    }

    public R getValue(S subEditor) {
      AbstractEditorDelegate<R, S> subDelegate = map.get(subEditor);
      subDelegate.flush();
      return subDelegate.getObject();
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
  protected LeafValueEditor<T> leafValueEditor;
  /**
   * This field avoids needing to repeatedly cast {@link #editor}.
   */
  protected ValueAwareEditor<T> valueAwareEditor;
  protected String path;

  public AbstractEditorDelegate() {
    super();
  }

  public abstract T ensureMutable(T object);

  public void flush() {
    if (valueAwareEditor != null) {
      valueAwareEditor.flush();
    }

    // See comment in initialize about LeafValueEditors
    if (leafValueEditor != null) {
      setObject(leafValueEditor.getValue());
      return;
    }

    if (getObject() == null) {
      return;
    }
    setObject(ensureMutable(getObject()));
    flushSubEditors();
    flushValues();
  }

  public abstract T getObject();

  public String getPath() {
    return path;
  }

  public abstract HandlerRegistration subscribe();

  public abstract Set<ConstraintViolation<T>> validate(T object);

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

  protected abstract void flushSubEditors();

  protected abstract void flushValues();

  protected void initialize(String pathSoFar, T object, E editor) {
    this.path = pathSoFar;
    setEditor(editor);
    setObject(object);

    // Set up pre-casted fields to access the editor
    if (editor instanceof LeafValueEditor<?>) {
      leafValueEditor = (LeafValueEditor<T>) editor;
    }
    if (editor instanceof ValueAwareEditor<?>) {
      valueAwareEditor = (ValueAwareEditor<T>) editor;
      valueAwareEditor.setDelegate(this);
      if (editor instanceof CompositeEditor<?, ?, ?>) {
        @SuppressWarnings("unchecked")
        CompositeEditor<T, Object, Editor<Object>> temp = (CompositeEditor<T, Object, Editor<Object>>) editor;
        composedEditor = temp;
        composedEditor.setEditorChain(new Chain<Object, Editor<Object>>());
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
      pushValues();
    }
  }

  protected abstract void pushValues();

  protected abstract void setEditor(E editor);

  protected abstract void setObject(T object);
}