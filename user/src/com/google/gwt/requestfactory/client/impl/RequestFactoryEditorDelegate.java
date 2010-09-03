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
import com.google.gwt.editor.client.impl.AbstractEditorDelegate;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.requestfactory.shared.EntityProxy;
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
public abstract class RequestFactoryEditorDelegate<P extends EntityProxy, E extends Editor<P>>
    extends AbstractEditorDelegate<P, E> implements EditorDelegate<P> {

  protected RequestFactory factory;
  protected RequestObject<?> request;

  @Override
  public P ensureMutable(P object) {
    if (request == null) {
      throw new IllegalStateException("No RequestObject");
    }
    return request.edit(object);
  }

  public void initialize(EventBus eventBus, RequestFactory factory,
      String pathSoFar, P object, E editor, RequestObject<?> editRequest) {
    this.factory = factory;
    this.request = editRequest;
    super.initialize(eventBus, pathSoFar, object, editor);
  }

  @Override
  public HandlerRegistration subscribe() {
    // Eventually will make use of pushValues()
    GWT.log("subscribe() is currently unimplemented pending RequestFactory changes");
    return new HandlerRegistration() {
      public void removeHandler() {
      }
    };
  }

  @Override
  public Set<ConstraintViolation<P>> validate(P object) {
    GWT.log("validate() is currently unimplemented");
    return Collections.emptySet();
  }
}
