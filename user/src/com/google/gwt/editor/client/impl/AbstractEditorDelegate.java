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
import com.google.gwt.editor.client.EditorDelegate;
import com.google.gwt.editor.client.ValueAwareEditor;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.HandlerRegistration;

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

  protected static String appendPath(String prefix, String path) {
    if ("".equals(prefix)) {
      return path;
    } else {
      return prefix + "." + path;
    }
  }

  protected EventBus eventBus;

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
    if (getObject() == null) {
      return;
    }
    flushSubEditors();
    setObject(ensureMutable(getObject()));
    flushValues();
  }

  public String getPath() {
    return path;
  }

  public abstract HandlerRegistration subscribe();

  public abstract Set<ConstraintViolation<T>> validate(T object);

  protected String appendPath(String path) {
    return appendPath(this.path, path);
  }

  protected abstract void attachSubEditors();

  protected abstract void flushSubEditors();

  protected abstract void flushValues();

  protected abstract T getObject();

  protected void initialize(EventBus eventBus, String pathSoFar, T object,
      E editor) {
    this.eventBus = eventBus;
    this.path = pathSoFar;
    setEditor(editor);
    setObject(object);
    if (editor instanceof ValueAwareEditor<?>) {
      valueAwareEditor = (ValueAwareEditor<T>) editor;
      valueAwareEditor.setDelegate(this);
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