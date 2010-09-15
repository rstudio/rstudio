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
import com.google.gwt.editor.client.impl.AbstractEditorDelegate;
import com.google.gwt.editor.client.impl.DelegateMap;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.requestfactory.shared.RequestObject;

/**
 * Base class for generated EditorDelegates using a RequestFactory as the
 * backend.
 * 
 * @param <P> the type of Proxy
 * @param <E> the type of Editor
 */
public abstract class RequestFactoryEditorDelegate<P, E extends Editor<P>>
    extends AbstractEditorDelegate<P, E> {

  protected EventBus eventBus;
  protected RequestFactory factory;
  protected RequestObject<?> request;

  @Override
  public P ensureMutable(P object) {
    if (request == null) {
      throw new IllegalStateException("No RequestObject");
    } else if (object instanceof EntityProxy) {
      @SuppressWarnings("unchecked")
      P toReturn = (P) request.edit(((EntityProxy) object));
      return toReturn;
    } else {
      // Likely a value object
      return object;
    }
  }

  public void initialize(EventBus eventBus, RequestFactory factory,
      String pathSoFar, P object, E editor, DelegateMap delegateMap,
      RequestObject<?> editRequest) {
    this.eventBus = eventBus;
    this.factory = factory;
    this.request = editRequest;
    super.initialize(pathSoFar, object, editor, delegateMap);
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
  protected <R, S extends Editor<R>> void initializeSubDelegate(
      AbstractEditorDelegate<R, S> subDelegate, String path, R object,
      S subEditor, DelegateMap delegateMap) {
    ((RequestFactoryEditorDelegate<R, S>) subDelegate).initialize(eventBus,
        factory, path, object, subEditor, delegateMap, request);
  }
}
