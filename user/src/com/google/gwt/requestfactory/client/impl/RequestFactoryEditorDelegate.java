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

import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.impl.AbstractEditorDelegate;
import com.google.gwt.editor.client.impl.DelegateMap;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.EntityProxyChange;
import com.google.gwt.requestfactory.shared.EntityProxyId;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.Request;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.requestfactory.shared.WriteOperation;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for generated EditorDelegates using a RequestFactory as the
 * backend.
 * 
 * @param <P> the type of Proxy
 * @param <E> the type of Editor
 */
public abstract class RequestFactoryEditorDelegate<P, E extends Editor<P>>
    extends AbstractEditorDelegate<P, E> {

  private class SubscriptionHandler implements
      EntityProxyChange.Handler<EntityProxy> {

    public void onProxyChange(EntityProxyChange<EntityProxy> event) {
      if (event.getWriteOperation().equals(WriteOperation.UPDATE)
          && event.getProxyId().equals(
              ((EntityProxy) getObject()).stableId())) {
        List<String> paths = new ArrayList<String>();
        traverse(paths);
        @SuppressWarnings("rawtypes")
        EntityProxyId id = event.getProxyId();
        doFind(paths, id);
      }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void doFind(List<String> paths, EntityProxyId id) {
      factory.find(id).with(paths.toArray(new String[paths.size()])).fire(
          new SubscriptionReceiver());
    }
  }

  private class SubscriptionReceiver extends Receiver<EntityProxy> {
    @Override
    public void onSuccess(EntityProxy response) {
      @SuppressWarnings("unchecked")
      P cast = (P) response;
      refresh(cast);
    }
  }

  protected EventBus eventBus;
  protected RequestFactory factory;
  protected Request<?> request;

  @Override
  public P ensureMutable(P object) {
    if (request == null) {
      throw new IllegalStateException("No Request");
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
      Request<?> editRequest) {
    this.eventBus = eventBus;
    this.factory = factory;
    this.request = editRequest;
    super.initialize(pathSoFar, object, editor, delegateMap);
  }

  @Override
  public HandlerRegistration subscribe() {
    if (!(getObject() instanceof EntityProxy)) {
      /*
       * This is kind of weird. The user is asking for notifications on a
       * String, which means there's a HasEditorDelegate<String> in play and not
       * the usual LeafValueEditor<String>.
       */
      return null;
    }

    // Can't just use getObject().getClass() because it's not the proxy type
    EntityProxyId stableId = ((EntityProxy) getObject()).stableId();
    @SuppressWarnings("unchecked")
    Class<EntityProxy> clazz = (Class<EntityProxy>) stableId.getProxyClass();
    HandlerRegistration toReturn = EntityProxyChange.<EntityProxy> registerForProxyType(
        eventBus, clazz, new SubscriptionHandler());
    return toReturn;
  }

  @Override
  protected <R, S extends Editor<R>> void initializeSubDelegate(
      AbstractEditorDelegate<R, S> subDelegate, String path, R object,
      S subEditor, DelegateMap delegateMap) {
    ((RequestFactoryEditorDelegate<R, S>) subDelegate).initialize(eventBus,
        factory, path, object, subEditor, delegateMap, request);
  }

  @Override
  protected boolean shouldFlush() {
    return request != null;
  }
}
