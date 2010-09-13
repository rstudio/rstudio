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
import com.google.gwt.requestfactory.shared.Property;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.RequestObject;
import com.google.gwt.requestfactory.shared.SyncResult;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * <p> <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * Abstract implementation of {@link RequestObject}. Each request stores a
 * {@link DeltaValueStoreJsonImpl}.
 * 
 * @param <T> return type
 * @param <R> type of this request object
 */
public abstract class AbstractRequest<T, R extends AbstractRequest<T, R>>
    implements RequestObject<T> {

  protected final RequestFactoryJsonImpl requestFactory;
  protected DeltaValueStoreJsonImpl deltaValueStore;
  protected Receiver<T> receiver;

  private final Set<String> propertyRefs = new HashSet<String>();

  public AbstractRequest(RequestFactoryJsonImpl requestFactory) {
    this.requestFactory = requestFactory;
    ValueStoreJsonImpl valueStore = requestFactory.getValueStore();
    this.deltaValueStore = new DeltaValueStoreJsonImpl(valueStore,
        requestFactory);
  }

  public void clearUsed() {
    deltaValueStore.clearUsed();
  }

  @SuppressWarnings("unchecked")
  public <P extends EntityProxy> P edit(P record) {
    P returnRecordImpl = (P) ((ProxyImpl) record).getSchema().create(
        ((ProxyImpl) record).asJso(), ((ProxyImpl) record).isFuture());
    ((ProxyImpl) returnRecordImpl).setDeltaValueStore(deltaValueStore);
    return returnRecordImpl;
  }

  public void fire(Receiver<T> receiver) {
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

  public void handleResponseText(String responseText) {
    JsonResults results = JsonResults.fromResults(responseText);
    if (results.getException() != null) {
      throw new RuntimeException(results.getException());
    }
    processRelated(results.getRelated());

    // handle violations
    JsArray<DeltaValueStoreJsonImpl.ReturnRecord> violationsArray = results.getViolations();
    Set<SyncResult> syncResults = new HashSet<SyncResult>();
    if (violationsArray != null) {
      int length = violationsArray.length();
      for (int i = 0; i < length; i++) {
        ReturnRecord violationRecord = violationsArray.get(i);
        Long id = null;
        if (violationRecord.hasFutureId()) {
          id = Long.valueOf(violationRecord.getFutureId());
        } else {
          id = violationRecord.getId();
        }
        final EntityProxyIdImpl key = new EntityProxyIdImpl(id,
            requestFactory.getSchema(violationRecord.getSchema()),
            violationRecord.hasFutureId(), null);
        ProxyJsoImpl copy = ProxyJsoImpl.create(id, 1, key.schema,
            requestFactory);
        assert violationRecord.hasViolations();
        HashMap<String, String> violations = new HashMap<String, String>();
        violationRecord.fillViolations(violations);
        syncResults.add(DeltaValueStoreJsonImpl.makeSyncResult(copy,
            violations, id));
      }
      /*
       * TODO (amitmanjhi): call onViolations once the Receiver interface has
       * been updated. remove null checks from all implementations once Receiver
       * has the onViolations method.
       */
      handleResult(null, syncResults);
    } else {
      handleResult(results.getResult(),
          deltaValueStore.commit(results.getSideEffects()));
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

  public void reset() {
    ValueStoreJsonImpl valueStore = requestFactory.getValueStore();
    deltaValueStore = new DeltaValueStoreJsonImpl(valueStore, requestFactory);
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

  /**
   * Subclasses must override to return {@code this}, to allow builder-style
   * methods to do the same.
   */
  protected abstract R getThis();

  protected abstract void handleResult(Object result,
      Set<SyncResult> syncResults);

  protected native void processRelated(JavaScriptObject related) /*-{
    for(var recordKey in related) {
      // Workaround for __gwt_ObjectId appearing in Chrome dev mode.
      if (!related.hasOwnProperty(recordKey)) continue;
      var schemaAndId = recordKey.split(/-/, 2);
      var jso = related[recordKey];
      this.@com.google.gwt.requestfactory.client.impl.AbstractRequest::pushToValueStore(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(schemaAndId[0], jso);
    }
  }-*/;
}