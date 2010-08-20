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

import com.google.gwt.valuestore.shared.Property;
import com.google.gwt.valuestore.shared.Record;
import com.google.gwt.valuestore.shared.RecordChangedEvent;
import com.google.gwt.valuestore.shared.WriteOperation;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Just a dummy class for testing purposes.
 */
public class SimpleBazRecordImpl extends RecordImpl {

  /**
   * The Schema class.
   */
  public static class MySchema extends RecordSchema<SimpleBazRecordImpl> {
    
    private final Set<Property<?>> allProperties;
    {
      Set<Property<?>> set = new HashSet<Property<?>>();
      set.addAll(super.allProperties());
      allProperties = Collections.unmodifiableSet(set);
    }
    
    public MySchema() {
      super(SimpleBazRecordImpl.class.getName());
    }

    @Override
    public Set<Property<?>> allProperties() {
      return allProperties;
    }

    @Override
    public SimpleBazRecordImpl create(RecordJsoImpl jso, boolean isFuture) {
      return new SimpleBazRecordImpl(jso, isFuture);
    }

    @Override
    public RecordChangedEvent<?, ?> createChangeEvent(Record record,
        WriteOperation writeOperation) {
      // ignore
      return null;
    }

    @Override
    public Class<? extends Record> getProxyClass() {
      // ignore
      return null;
    }
  }

  public static final RecordSchema<SimpleBazRecordImpl> SCHEMA = new MySchema();

  private SimpleBazRecordImpl(RecordJsoImpl jso, boolean isFuture) {
    super(jso, isFuture);
  }
}
