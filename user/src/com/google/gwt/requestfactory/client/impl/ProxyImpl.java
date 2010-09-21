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

import com.google.gwt.requestfactory.shared.EntityProxy;

import com.google.gwt.requestfactory.shared.EntityProxyId;
import com.google.gwt.requestfactory.shared.impl.Property;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * Base class for implementations of {@link EntityProxy}. It wraps a
 * {@link ProxyJsoImpl} that does all the actual work. This class has little
 * reason to exist except to allow client code to make instanceof checks, and to
 * work around issue 4859 (JSOs cannot have abstract superclasses). If the issue
 * is fixed it might be worth abandoning the instanceof capability, needs
 * thinking.
 */
public class ProxyImpl implements EntityProxy {

  static final Property<String> id = new Property<String>("id", String.class);
  static final Property<Integer> version = new Property<Integer>("version", Integer.class);

  protected static String getWireFormatId(String id, boolean isFuture,
      ProxySchema<?> schema) {
    return id + "-" + (isFuture ? "IS" : "NO") + "-" + schema.getToken();
  }

  private final ProxyJsoImpl jso;
  private final boolean isFuture;

  private DeltaValueStoreJsonImpl deltaValueStore;
  
  /**
   * For use by generated subclasses only. Other code should use
   * {@link ProxySchema#create(ProxyJsoImpl, boolean)}, typically:
   * <code><pre>proxyJsoImpl.getSchema().create(proxyJsoImpl.getSchema(), isFuture);
   * </pre></code>
   */
  protected ProxyImpl(ProxyJsoImpl jso, boolean isFuture) {
    /*
     * A funny place for these asserts, but it's proved hard to control in the
     * JSO itself
     */
    assert jso.getRequestFactory() != null;
    assert jso.getSchema() != null;
    this.jso = jso;
    this.isFuture = isFuture;
    deltaValueStore = null;
  }

  public ProxyJsoImpl asJso() {
    return jso;
  }

  /**
   * Get this proxy's value for the given property. Behavior is undefined if
   * the proxy has no such property, or if the property has never been set. It
   * is unusual to call this method directly. Rather it is expected to be called
   * by bean-style getter methods provided by implementing classes.
   * 
   * @param <V> the type of the property's value
   * @param property the property to fetch
   * @return the value
   */
  public <V> V get(Property<V> property) {
    // javac 1.6.0_20 on mac has problems without the explicit parameterization
    return jso.<V> get(property);
  }

  public <V> V get(String propertyName, Class<?> propertyType) {
    // javac 1.6.0_20 on mac has problems without the explicit parameterization
    return jso.<V> get(propertyName, propertyType);
  }
  
  public String getId() {
    return jso.getId();
  }

  public ProxySchema<?> getSchema() {
    return jso.getSchema();
  }

  public Integer getVersion() {
    return jso.getVersion();
  }

  public String getWireFormatId() {
    return getWireFormatId(jso.getId(), isFuture, jso.getSchema());
  }

  public boolean isChanged() {
    if (deltaValueStore == null) {
      return false;
    }
    return deltaValueStore.isChanged();
  }

  public boolean isFuture() {
    return isFuture;
  }

  public <V> void set(Property<V> property, ProxyImpl record, V value) {
    if (deltaValueStore == null) {
      throw new UnsupportedOperationException(
          "Setter methods can't be called before calling edit()");
    }
    deltaValueStore.set(property, record, value);
  }

  public EntityProxyId stableId() {
    if (!isFuture) {
      return new EntityProxyIdImpl(
          getId(),
          getSchema(),
          false,
          jso.getRequestFactory().datastoreToFutureMap.get(getId(), getSchema()));
    }
    return new EntityProxyIdImpl(getId(), getSchema(), isFuture, null);
  }

  protected ValueStoreJsonImpl getValueStore() {
    return jso.getRequestFactory().getValueStore();
  }

  void setDeltaValueStore(DeltaValueStoreJsonImpl deltaValueStore) {
    this.deltaValueStore = deltaValueStore;
  }
}
