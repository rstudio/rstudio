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

import com.google.gwt.requestfactory.shared.DeltaValueStore;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.RequestObject;
import com.google.gwt.valuestore.shared.Property;
import com.google.gwt.valuestore.shared.Record;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * Abstract implementation of {@link RequestObject}. Each request
 * stores a {@link DeltaValueStore}.
 * 
 * @param <T> return type
 * @param <R> type of this request object
 */
public abstract class AbstractRequest<T, R extends AbstractRequest<T, R>>
    implements RequestObject<T> {

  protected final RequestFactoryJsonImpl requestFactory;
  protected DeltaValueStore deltaValueStore;
  protected Receiver<T> receiver;

  private final Set<Property<?>> properties = new HashSet<Property<?>>();

  public AbstractRequest(RequestFactoryJsonImpl requestFactory) {
    this.requestFactory = requestFactory;
    ValueStoreJsonImpl valueStore = requestFactory.getValueStore();
    this.deltaValueStore = new DeltaValueStoreJsonImpl(valueStore, valueStore.map);
  }

  public void fire(Receiver<T> receiver) {
    // TODO: do something with deltaValueStore.
    assert null != receiver : "receiver cannot be null";
    this.receiver = receiver;
    requestFactory.fire(this);
  }

  public R forProperties(Collection<Property<?>> properties) {
    this.properties.addAll(properties);
    return getThis();
  }

  public R forProperty(Property<?> property) {
    this.properties.add(property);
    return getThis();
  }

  public DeltaValueStore getDeltaValueStore() {
    return deltaValueStore;
  }

  /**
   * @return the properties
   */
  public Set<Property<?>> getProperties() {
    return Collections.unmodifiableSet(properties);
  }

  public void reset() {
    ValueStoreJsonImpl valueStore = requestFactory.getValueStore();
    deltaValueStore = new DeltaValueStoreJsonImpl(valueStore, valueStore.map);
  }

  public <V> void set(Property<V> property, Record record, V value) {
    deltaValueStore.set(property, record, value);
  }

  /**
   * Subclasses must override to return {@code this}, to allow builder-style
   * methods to do the same.
   */
  protected abstract R getThis();

}