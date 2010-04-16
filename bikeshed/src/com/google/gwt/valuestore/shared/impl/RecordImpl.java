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
package com.google.gwt.valuestore.shared.impl;

import com.google.gwt.valuestore.shared.Property;
import com.google.gwt.valuestore.shared.PropertyReference;
import com.google.gwt.valuestore.shared.Record;

/**
 * Base class for implementations of {@link Record}. It wraps a
 * {@link RecordJsoImpl} that does all the actual work. This class has little
 * reason to exist except to allow client code to make instanceof checks, and to
 * work around issue 4859 (JSOs cannot have abstract superclasses). If the issue
 * is fixed it might be worth abandoning the instanceof capability, needs
 * thinking.
 */
public class RecordImpl implements Record {
  private final RecordJsoImpl jso;

  protected RecordImpl(RecordJsoImpl record) {
    this.jso = record;
  }

  public RecordJsoImpl asJso() {
    return jso;
  }

  public <V> V get(Property<V> property) {
    return jso.get(property);
  }

  public String getId() {
    return jso.getId();
  }

  public <V> PropertyReference<V> getRef(Property<V> property) {
    return jso.getRef(property);
  }

  public RecordSchema<?> getSchema() {
    return jso.getSchema();
  }

  public Integer getVersion() {
    return jso.getVersion();
  }
}
