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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.requestfactory.shared.EntityProxy;

import java.util.Collection;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * Abstract implementation of
 * {@link com.google.gwt.requestfactory.shared.RequestObject
 * RequestFactory.RequestObject} for requests that return Collection of
 * {@link com.google.gwt.requestfactory.shared.EntityProxy}. This class is
 * meant to be subclassed by specific implementations of Collection subtypes.
 *
 * @param <C> the type of collection
 * @param <T> the type of entities returned
 * @param <R> this request type
 */
public abstract class //
    AbstractJsonProxyCollectionRequest<C extends Collection<T>, T extends EntityProxy,
    R extends AbstractJsonProxyCollectionRequest<C, T, R>>
    extends AbstractRequest<C, R>  {
  protected final ProxySchema<?> schema;

  public AbstractJsonProxyCollectionRequest(ProxySchema<?> schema,
      RequestFactoryJsonImpl requestService) {
    super(requestService);
    this.schema = schema;
  }

  @Override
  public void handleResult(Object jsoResult) {
    @SuppressWarnings("unchecked")
    JsArray<JavaScriptObject> rawJsos = (JsArray<JavaScriptObject>) jsoResult;

    JsArray<ProxyJsoImpl> proxyJsos = ProxyJsoImpl.create(rawJsos, schema, requestFactory);
    requestFactory.getValueStore().setRecords(proxyJsos);

    /*
     * TODO would it be a win if we come up with a List that does the
     * schema.create() call on demand during get() and iteration?
     */
    C proxies = createCollection();
    for (int i = 0; i < proxyJsos.length(); i++) {
      ProxyJsoImpl jso = proxyJsos.get(i);

      /*
       * schema really should be ProxySchema<? extends T>, and then this cast
       * wouldn't be necessary. But that tickles a bug in javac:
       * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6437894
       */
      @SuppressWarnings("unchecked")
      T proxy = (T) schema.create(jso);
      proxies.add(proxy);
    }
    succeed(proxies);
  }

  /**
   * Creates empty mutable collection of type C.
   * @return a new Collection, such as ArrayList<T> or HashSet<T>
   */
  protected abstract C createCollection();
}
