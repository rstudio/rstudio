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

import com.google.gwt.requestfactory.shared.Property;
import com.google.gwt.requestfactory.shared.PropertyReference;
import com.google.gwt.requestfactory.shared.Record;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * Base class for implementations of {@link Record}. It wraps a
 * {@link RecordJsoImpl} that does all the actual work. This class has little
 * reason to exist except to allow client code to make instanceof checks, and to
 * work around issue 4859 (JSOs cannot have abstract superclasses). If the issue
 * is fixed it might be worth abandoning the instanceof capability, needs
 * thinking.
 */
public class RecordImpl implements Record {
  private final RecordJsoImpl jso;
  private final boolean isFuture;
  private DeltaValueStoreJsonImpl deltaValueStore;

  protected RecordImpl(RecordJsoImpl record, boolean isFuture) {
    this.jso = record;
    this.isFuture = isFuture;
    deltaValueStore = null;
  }

  public RecordJsoImpl asJso() {
    return jso;
  }

  public <V> V get(Property<V> property) {
    return jso.get(property);
  }
  
  public Long getId() {
    return jso.getId();
  }

  public <V> PropertyReference<V> getRef(Property<V> property) {
    return jso.getRef(property);
  }

  public RecordSchema<?> getSchema() {
    return jso.getSchema();
  }

  public String getUniqueId() {
    return jso.getId() + "-" + (isFuture ? "IS" : "NO") + "-"
        + getSchema().getToken();
  }

  public Integer getVersion() {
    return jso.getVersion();
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

  public <V> void set(Property<V> property, RecordImpl record, V value) {
    if (deltaValueStore == null) {
      throw new UnsupportedOperationException(
          "Setter methods can't be called before calling edit()");
    }
    deltaValueStore.set(property, record, value);
  }

  /*
   * TODO: this method is public for the time being. Will become
   * package-protected once {@link RecordImpl} moves to the same package as
   * {@link AbstractRequest}.
   */
  public void setDeltaValueStore(DeltaValueStoreJsonImpl deltaValueStore) {
    this.deltaValueStore = deltaValueStore;
  }

  protected ValueStoreJsonImpl getValueStore() {
    return jso.getValueStore();
  }
}
