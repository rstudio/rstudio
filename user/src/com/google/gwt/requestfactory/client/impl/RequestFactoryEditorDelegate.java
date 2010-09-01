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
package com.google.gwt.requestfactory.client.impl;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorDelegate;
import com.google.gwt.editor.client.ValueAwareEditor;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.requestfactory.shared.Record;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.requestfactory.shared.RequestObject;

import java.util.Collections;
import java.util.Set;

import javax.validation.ConstraintViolation;

/**
 * Base class for generated EditorDelegates using a RequestFactory as the
 * backend.
 * 
 * @param <P> the type of Proxy
 * @param <E> the type of Editor
 */
public abstract class RequestFactoryEditorDelegate<P extends Record, E extends Editor<P>>
    implements EditorDelegate<P> {

  protected static String appendPath(String prefix, String path) {
    if ("".equals(prefix)) {
      return path;
    } else {
      return prefix + "." + path;
    }
  }

  protected EventBus eventBus;
  protected RequestFactory factory;
  protected RequestObject<?> request;
  /**
   * This field avoids needing to repeatedly cast {@link #editor}.
   */
  protected ValueAwareEditor<P> valueAwareEditor;

  private String path;

  public P ensureMutable(P object) {
    if (request == null) {
      throw new IllegalStateException("No RequestObject");
    }
    return request.edit(object);
  }

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

  public void initialize(EventBus eventBus, RequestFactory factory,
      String pathSoFar, P object, E editor, RequestObject<?> editRequest) {
    this.eventBus = eventBus;
    this.factory = factory;
    this.path = pathSoFar;
    setEditor(editor);
    setObject(object);
    this.request = editRequest;
    if (editor instanceof ValueAwareEditor<?>) {
      valueAwareEditor = (ValueAwareEditor<P>) editor;
      valueAwareEditor.setDelegate(this);
      valueAwareEditor.setValue(object);
    }
    if (object != null) {
      attachSubEditors();
      pushValues();
    }
  }

  public HandlerRegistration subscribe() {
    // Eventually will make use of pushValues()
    GWT.log("subscribe() is currently unimplemented pending RequestFactory changes");
    return new HandlerRegistration() {
      public void removeHandler() {
      }
    };
  }

  public Set<ConstraintViolation<P>> validate(P object) {
    GWT.log("validate() is currently unimplemented");
    return Collections.emptySet();
  }

  protected String appendPath(String path) {
    return appendPath(this.path, path);
  }

  protected abstract void attachSubEditors();

  protected abstract void flushSubEditors();

  protected abstract void flushValues();

  protected abstract P getObject();

  protected abstract void pushValues();

  protected abstract void setEditor(E editor);

  protected abstract void setObject(P object);
}
