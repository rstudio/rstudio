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
import com.google.gwt.valuestore.shared.Record;
import com.google.gwt.valuestore.shared.RecordChangedEvent;
import com.google.gwt.valuestore.shared.SimpleFooRecord;
import com.google.gwt.valuestore.shared.WriteOperation;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * The actual implementation of {@link SimpleFooRecord}, which is normally
 * generated.
 * 
 * TODO: Use the generator here.
 */
public class SimpleFooRecordImpl extends RecordImpl implements SimpleFooRecord {

  /**
   * The Schema class.
   */
  public static class MySchema extends RecordSchema<SimpleFooRecordImpl> {
    private final Set<Property<?>> allProperties;
    {
      Set<Property<?>> set = new HashSet<Property<?>>();
      set.addAll(super.allProperties());
      set.add(userName);
      set.add(password);
      set.add(intId);
      set.add(created);
      allProperties = Collections.unmodifiableSet(set);
    }

    public Set<Property<?>> allProperties() {
      return allProperties;
    }

    @Override
    public SimpleFooRecordImpl create(RecordJsoImpl jso) {
      return new SimpleFooRecordImpl(jso);
    }

    @Override
    public RecordChangedEvent<?, ?> createChangeEvent(Record record,
        WriteOperation writeOperation) {
      // ignore
      return null;
    }

    public String getToken() {
      return SimpleFooRecord.TOKEN; // special field
    }
  }

  public static final RecordSchema<SimpleFooRecordImpl> SCHEMA = new MySchema();

  private SimpleFooRecordImpl(RecordJsoImpl jso) {
    super(jso);
  }

  public java.util.Date getCreated() {
    return get(created);
  }

  public java.lang.Integer getIntId() {
    return get(intId);
  }

  public java.lang.String getPassword() {
    return get(password);
  }

  public java.lang.String getUserName() {
    return get(userName);
  }

}
