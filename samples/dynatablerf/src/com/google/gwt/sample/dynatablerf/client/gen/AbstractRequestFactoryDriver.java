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
package com.google.gwt.sample.dynatablerf.client.gen;

import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.ValueAwareEditor;
import com.google.gwt.editor.client.impl.RequestFactoryEditorDriverImpl;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.requestfactory.shared.Record;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.requestfactory.shared.RequestObject;

import java.util.Set;

import javax.validation.ConstraintViolation;

/**
 * Base class for generated EditorDelegates using a RequestFactory as the
 * backend.
 * 
 * @param <F> the RequestFactory type
 * @param <R> the type of Record
 */
public abstract class AbstractRequestFactoryDriver<F extends RequestFactory, R extends Record>
    implements RequestFactoryEditorDriverImpl<F, R> {

  protected ValueAwareEditor<R> valueAwareEditor;
  protected EventBus eventBus;
  protected F factory;
  protected R object;
  protected RequestObject<Object> request;
  protected Editor<? super R> editor;
  private String path;

  public R ensureMutable(R object) {
    return request.edit(object);
  }

  public void flush() {
    if (valueAwareEditor != null) {
      valueAwareEditor.flush();
    }
    object = ensureMutable(object);
  }

  public String getPath() {
    return path;
  }

  @SuppressWarnings("unchecked")
  public void initialize(EventBus eventBus, F factory, String pathSoFar,
      R object, Editor<R> editor, RequestObject<?> editRequest) {
    this.eventBus = eventBus;
    this.factory = factory;
    this.path = pathSoFar;
    this.editor = editor;
    this.object = object;
    this.request = (RequestObject<Object>) editRequest;
    if (editor instanceof ValueAwareEditor) {
      this.valueAwareEditor = (ValueAwareEditor<R>) editor;
      this.valueAwareEditor.setDelegate(this);
      this.valueAwareEditor.setValue(object);
    }
  }

  public abstract HandlerRegistration subscribe();

  public abstract Set<ConstraintViolation<R>> validate(R object);

  protected String appendPath(String path) {
    if ("".equals(this.path)) {
      return path;
    } else {
      return this.path + "." + path;
    }
  }
}
