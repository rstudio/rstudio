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
import com.google.gwt.requestfactory.client.impl.DeltaValueStoreJsonImpl.ReturnRecord;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.EntityProxyId;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.Request;
import com.google.gwt.requestfactory.shared.ServerFailure;
import com.google.gwt.requestfactory.shared.Violation;
import com.google.gwt.requestfactory.shared.impl.Property;
import com.google.gwt.requestfactory.shared.impl.RequestData;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * Abstract implementation of {@link Request}. Each request stores a
 * {@link DeltaValueStoreJsonImpl}.
 * 
 * @param <T> return type
 * @param <R> type of this request object
 */
public abstract class AbstractRequest<T, R extends AbstractRequest<T, R>>
    implements Request<T> {

  protected final RequestFactoryJsonImpl requestFactory;
  protected DeltaValueStoreJsonImpl deltaValueStore;
  private Receiver<? super T> receiver;

  private final Set<String> propertyRefs = new HashSet<String>();

  public AbstractRequest(RequestFactoryJsonImpl requestFactory) {
    this.requestFactory = requestFactory;
    ValueStoreJsonImpl valueStore = requestFactory.getValueStore();
    this.deltaValueStore = new DeltaValueStoreJsonImpl(valueStore,
        requestFactory);
  }

  @SuppressWarnings("unchecked")
  public <P extends EntityProxy> P edit(P record) {
    P returnRecordImpl = (P) ((ProxyImpl) record).schema().create(
        ((ProxyImpl) record).asJso(), ((ProxyImpl) record).unpersisted());
    ((ProxyImpl) returnRecordImpl).putDeltaValueStore(deltaValueStore);
    return returnRecordImpl;
  }

  public void fire(Receiver<? super T> receiver) {
    assert null != receiver : "receiver cannot be null";
    this.receiver = receiver;
    requestFactory.fire(this);
  }
  
  /**
   * @deprecated use {@link #with(String...)} instead.
   * @param properties
   */
  @Deprecated
  public R forProperties(Collection<Property<?>> properties) {
    for (Property<?> p : properties) {
      with(p.getName());
    }
    return getThis();
  }
  
  /**
   * @return the properties
   */
  public Set<String> getPropertyRefs() {
    return Collections.unmodifiableSet(propertyRefs);
  }

  public abstract RequestData getRequestData();

  public void handleResponseText(String responseText) {
    JsonResults results = JsonResults.fromResults(responseText);
    JsonServerException cause = results.getException();
    if (cause != null) {
      fail(new ServerFailure(
          cause.getMessage(), cause.getType(), cause.getTrace()));
      return;
    }
    processRelated(results.getRelated());

    // handle violations
    JsArray<DeltaValueStoreJsonImpl.ReturnRecord> violationsArray = results.getViolations();
    if (violationsArray != null) {
      processViolations(violationsArray);
    } else {
      deltaValueStore.commit(results.getSideEffects());
      handleResult(results.getResult());
    }
  }

  public boolean isChanged() {
    return deltaValueStore.isChanged();
  }

  /*
   * used from the JSNI method processRelated.
   */
  public void pushToValueStore(String schemaToken, JavaScriptObject jso) {
    requestFactory.getValueStore().putInValueStore(
        ProxyJsoImpl.create(jso, requestFactory.getSchema(schemaToken),
            requestFactory));
  }

  public R with(String... propertyRef) {
    for (String ref : propertyRef) {
      propertyRefs.add(ref);
    }
    return getThis();
  }

  protected String asString(Object jso) {
    return String.valueOf(jso);
  }

  protected void fail(ServerFailure failure) {
    deltaValueStore.reuse();
    receiver.onFailure(failure);
  }

  /**
   * Subclasses must override to return {@code this}, to allow builder-style
   * methods to do the same.
   */
  protected abstract R getThis();

  /**
   * Process the response and call {@link #succeed(Object) or
   * #fail(com.google.gwt.requestfactory.shared.ServerFailure).
   */
  protected abstract void handleResult(Object result);

  protected native void processRelated(JavaScriptObject related) /*-{
    for(var recordKey in related) {
      // Workaround for __gwt_ObjectId appearing in Chrome dev mode.
      if (!related.hasOwnProperty(recordKey)) continue;
      var schemaAndId = recordKey.split(/@/, 3);
      var jso = related[recordKey];
      this.@com.google.gwt.requestfactory.client.impl.AbstractRequest::pushToValueStore(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(schemaAndId[2], jso);
    }
  }-*/;

  protected void succeed(T t) {
    receiver.onSuccess(t);
  }

  private void processViolations(
      JsArray<DeltaValueStoreJsonImpl.ReturnRecord> violationsArray) {
    int length = violationsArray.length();
    Set<Violation> errors = new HashSet<Violation>(length);

    for (int i = 0; i < length; i++) {
      ReturnRecord violationRecord = violationsArray.get(i);
      String id = null;
      if (violationRecord.hasFutureId()) {
        id = violationRecord.getFutureId();
      } else {
        id = violationRecord.getEncodedId();
      }
      final EntityProxyIdImpl key = new EntityProxyIdImpl(id,
          requestFactory.getSchema(violationRecord.getSchema()),
          violationRecord.hasFutureId(), null);
      assert violationRecord.hasViolations();

      HashMap<String, String> violations = new HashMap<String, String>();
      violationRecord.fillViolations(violations);

      for (Map.Entry<String, String> entry : violations.entrySet()) {
        final String path = entry.getKey();
        final String message = entry.getValue();
        errors.add(new Violation() {
          public String getMessage() {
            return message;
          }

          public String getPath() {
            return path;
          }

          public EntityProxyId getProxyId() {
            return key;
          }
        });
      }
    }

    deltaValueStore.reuse();
    receiver.onViolation(errors);
  }
}
