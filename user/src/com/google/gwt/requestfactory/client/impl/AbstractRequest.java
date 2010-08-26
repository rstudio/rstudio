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
import com.google.gwt.requestfactory.shared.Property;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.Record;
import com.google.gwt.requestfactory.shared.RequestObject;
import com.google.gwt.requestfactory.shared.SyncResult;

import java.util.Collection;
import java.util.Collections;
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

  protected String asString(Object jso) {
    return String.valueOf(jso);
  }

  public void clearUsed() {
    deltaValueStore.clearUsed();
  }

  @SuppressWarnings("unchecked")
  public <P extends Record> P edit(P record) {
    P returnRecordImpl = (P) ((RecordImpl) record).getSchema().create(
        ((RecordImpl) record).asJso(), ((RecordImpl) record).isFuture());
    ((RecordImpl) returnRecordImpl).setDeltaValueStore(deltaValueStore);
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
    RecordJsoImpl.JsonResults results = RecordJsoImpl.fromResults(responseText);
    processRelated(results.getRelated());
    handleResult(results.getResult(),
        deltaValueStore.commit(results.getSideEffects()));
  }
  
  public boolean isChanged() {
    return deltaValueStore.isChanged();
  }

  public void reset() {
    ValueStoreJsonImpl valueStore = requestFactory.getValueStore();
    deltaValueStore = new DeltaValueStoreJsonImpl(valueStore, requestFactory);
  }

  public void setSchemaAndRecord(String schemaToken, RecordJsoImpl jso) {
    jso.setSchema(requestFactory.getSchema(schemaToken));
    requestFactory.getValueStore().setRecord(jso, requestFactory);
  }

  public R with(String... propertyRef) {
    for (String ref : propertyRef) {
      propertyRefs.add(ref);
    }
    return getThis();
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
      this.@com.google.gwt.requestfactory.client.impl.AbstractRequest::setSchemaAndRecord(Ljava/lang/String;Lcom/google/gwt/requestfactory/client/impl/RecordJsoImpl;)(schemaAndId[0], jso);
    }
  }-*/;
}