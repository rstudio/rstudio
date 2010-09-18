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
import com.google.gwt.requestfactory.shared.ProxyListRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * Abstract implementation of
 * {@link com.google.gwt.requestfactory.shared.RequestObject
 * RequestFactory.RequestObject} for requests that return lists of
 * {@link EntityProxy}.
 * 
 * @param <T> the type of entities returned
 * @param <R> this request type
 */
public abstract class //
AbstractJsonListRequest<T extends EntityProxy, R extends AbstractJsonListRequest<T, R>> //
    extends AbstractRequest<List<T>, R> implements ProxyListRequest<T> {
  protected final ProxySchema<?> schema;

  public AbstractJsonListRequest(ProxySchema<? extends T> schema,
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
    List<T> proxies = new ArrayList<T>();
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
    receiver.onSuccess(proxies);
  }
}
