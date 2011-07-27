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
package com.google.web.bindery.requestfactory.gwt.client.impl;

import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorVisitor;
import com.google.gwt.editor.client.impl.AbstractEditorDelegate;
import com.google.gwt.editor.client.impl.Refresher;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.requestfactory.shared.BaseProxy;
import com.google.web.bindery.requestfactory.shared.EntityProxy;
import com.google.web.bindery.requestfactory.shared.EntityProxyChange;
import com.google.web.bindery.requestfactory.shared.EntityProxyId;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.RequestFactory;
import com.google.web.bindery.requestfactory.shared.WriteOperation;
import com.google.web.bindery.requestfactory.shared.impl.AbstractRequestContext;

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
          && event.getProxyId().equals(((EntityProxy) getObject()).stableId())) {
        PathCollector collector = new PathCollector();
        accept(collector);
        EntityProxyId<?> id = event.getProxyId();
        doFind(collector.getPaths(), id);
      }
    }

    @SuppressWarnings(value = {"rawtypes", "unchecked"})
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
      setObject(cast);
      accept(new Refresher());
    }
  }

  protected EventBus eventBus;
  protected RequestFactory factory;
  protected RequestContext request;

  public void setRequestContext(RequestContext request) {
    this.request = request;
  }

  @Override
  public HandlerRegistration subscribe() {
    if (factory == null) {
      /*
       * They called the no-subscriptions version of
       * RequestFactoryEditorDriver#initialize
       */
      return null;
    }

    if (!(getObject() instanceof EntityProxy)) {
      /*
       * This is kind of weird. The user is asking for notifications on a
       * String, which means there's a HasEditorDelegate<String> in play and not
       * the usual LeafValueEditor<String>.
       */
      return null;
    }

    // Can't just use getObject().getClass() because it's not the proxy type
    EntityProxyId<?> stableId = ((EntityProxy) getObject()).stableId();
    @SuppressWarnings("unchecked")
    Class<EntityProxy> clazz = (Class<EntityProxy>) stableId.getProxyClass();

    /*
     * Convert to the old gwt HandlerRegistration type required by the
     * EditorDelegate interface. This can get cleaned up when Editor moves to
     * com.google.web.bindery.
     */
    final com.google.web.bindery.event.shared.HandlerRegistration toReturn =
        EntityProxyChange.<EntityProxy> registerForProxyType(eventBus, clazz,
            new SubscriptionHandler());
    return new HandlerRegistration() {
      public void removeHandler() {
        toReturn.removeHandler();
      }
    };
  }

  @Override
  protected <R, S extends Editor<R>> void addSubDelegate(
      AbstractEditorDelegate<R, S> subDelegate, String path, S subEditor) {
    RequestFactoryEditorDelegate<R, S> d = (RequestFactoryEditorDelegate<R, S>) subDelegate;
    d.initialize(eventBus, factory, path, subEditor);
  }

  @Override
  protected EditorVisitor createInitializerVisitor() {
    return new Initializer(request);
  }

  @Override
  protected <T> T ensureMutable(T object) {
    if (request == null) {
      // Read-only mode
      return object;
    }
    if (object instanceof BaseProxy) {
      @SuppressWarnings("unchecked")
      T toReturn = (T) request.edit((BaseProxy) object);
      return toReturn;
    }
    return object;
  }

  protected void initialize(EventBus eventBus, RequestFactory factory,
      String pathSoFar, E editor) {
    this.eventBus = eventBus;
    this.factory = factory;
    super.initialize(pathSoFar, editor);
  }

  /**
   * Must call four-arg version instead.
   */
  @Override
  protected void initialize(String pathSoFar, E editor) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected boolean shouldFlush() {
    if (request == null) {
      return false;
    }
    if (request instanceof AbstractRequestContext) {
      return !((AbstractRequestContext) request).isLocked();
    }
    return true;
  }
}
