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
import com.google.gwt.requestfactory.shared.impl.CollectionProperty;
import com.google.gwt.requestfactory.shared.impl.HasWireFormatId;
import com.google.gwt.requestfactory.shared.impl.Property;

import java.util.Collection;

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
public class ProxyImpl implements EntityProxy, HasWireFormatId {

  protected static String wireFormatId(String id, boolean isFuture,
      ProxySchema<?> schema) {
    return id + "@" + (isFuture ? "IS" : "NO") + "@" + schema.getToken();
  }

  private final ProxyJsoImpl jso;
  private final boolean isFuture;
  private DeltaValueStoreJsonImpl deltaValueStore;
  private AbstractRequest<?, ?> request;

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
  }

  public ProxyJsoImpl asJso() {
    return jso;
  }

  public String encodedId() {
    return jso.encodedId();
  }

  /**
   * A ProxyImpl is equal to another ProxyImpl if they have equal EntityProxyIds
   * and they are from the same Request object.
   */
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ProxyImpl)) {
      return false;
    }
    ProxyImpl other = (ProxyImpl) o;
    return stableId().equals(other.stableId())
        && (request == other.request || request != null
            && request.equals(other.request));
  }

  /**
   * Get this proxy's value for the given property. Behavior is undefined if the
   * proxy has no such property, or if the property has never been set. It is
   * unusual to call this method directly. Rather it is expected to be called by
   * bean-style getter methods provided by implementing classes.
   * <p>
   * If the ProxyImpl is mutable, any EntityProxies reachable from the return
   * value will have already been made mutable.
   * 
   * @param <V> the type of the property's value
   * @param property the property to fetch
   * @return the value
   */
  @SuppressWarnings("unchecked")
  public <L extends Collection<V>, V> V get(Property<V> property) {
    // Read through to the DeltaValueStore to see if the entity has been mutated
    V toReturn = null;
    if (deltaValueStore != null
        && deltaValueStore.isPropertySet(property, this)) {
      toReturn = deltaValueStore.get(property, this);
    } else {
      if (property instanceof CollectionProperty) {
        // Possibly create a new JsoCollection if one does not exist
        toReturn = (V) jso.getCollection((CollectionProperty) property);
      } else {
        // Return a scalar property from the backing object
        toReturn = jso.<L, V> get(property);
      }
    }
    // Ensure mutability
    if (toReturn instanceof JsoCollection) {
      ((JsoCollection) toReturn).setDependencies(property, this);
    } else if (mutable()) {
      toReturn = request.ensureMutable(toReturn);
    }
    return toReturn;
  }

  public <V> V get(String propertyName, Class<?> propertyType) {
    // javac 1.6.0_20 on mac has problems without the explicit parameterization
    return jso.<V> get(propertyName, propertyType);
  }

  public boolean hasChanged() {
    if (deltaValueStore == null) {
      return false;
    }
    return deltaValueStore.isChanged();
  }

  @Override
  public int hashCode() {
    return stableId().hashCode() * 13
        + (request == null ? 0 : request.hashCode()) * 5;
  }

  public boolean mutable() {
    return deltaValueStore != null;
  }

  public AbstractRequest<?, ?> request() {
    return request;
  }

  public ProxySchema<?> schema() {
    return jso.getSchema();
  }

  public <V> void set(Property<V> property, ProxyImpl record, V value) {
    if (deltaValueStore == null) {
      throw new UnsupportedOperationException(
          "Setter methods can't be called before calling edit()");
    }
    deltaValueStore.set(property, record, value);
  }

  /**
   * Allow the generated subclass to return the specific type its public
   * interface probably demands.
   */
  @SuppressWarnings(value = {"unchecked", "rawtypes"})
  public EntityProxyId stableId() {
    if (isFuture) {
      return new EntityProxyIdImpl(encodedId(), schema(), isFuture, null);
    }
    return new EntityProxyIdImpl<ProxyImpl>(encodedId(), schema(), false,
        jso.getRequestFactory().datastoreToFutureMap.get(encodedId(), schema()));
  }

  public boolean unpersisted() {
    return isFuture;
  }

  public Integer version() {
    return jso.version();
  }

  public String wireFormatId() {
    return wireFormatId(jso.encodedId(), isFuture, jso.getSchema());
  }

  protected ValueStoreJsonImpl valueStore() {
    return jso.getRequestFactory().getValueStore();
  }

  void putDeltaValueStore(DeltaValueStoreJsonImpl deltaValueStore,
      AbstractRequest<?, ?> request) {
    this.deltaValueStore = deltaValueStore;
    this.request = request;
  }
}
