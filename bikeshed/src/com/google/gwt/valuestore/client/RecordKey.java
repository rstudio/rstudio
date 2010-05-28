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
package com.google.gwt.valuestore.client;

import com.google.gwt.valuestore.shared.impl.RecordImpl;
import com.google.gwt.valuestore.shared.impl.RecordJsoImpl;
import com.google.gwt.valuestore.shared.impl.RecordSchema;

/**
 * The key used to store {@link com.google.gwt.valuestore.shared.Record Record}s
 * in {@link com.google.gwt.valuestore.shared.ValueStore ValueStore}.
 */
class RecordKey {
  final RecordSchema<?> schema;
  final Object id;

  RecordKey(RecordImpl record) {
    this(record.getId(), record.getSchema());
  }

  RecordKey(RecordJsoImpl record) {
    this(record.getId(), record.getSchema());
  }

  protected RecordKey(Object id, RecordSchema<?> schema) {
    assert id != null;
    assert schema != null;

    this.id = id;
    this.schema = schema;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    RecordKey other = (RecordKey) obj;
    if (!id.equals(other.id)) {
      return false;
    }
    if (!schema.equals(other.schema)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    result = prime * result + ((schema == null) ? 0 : schema.hashCode());
    return result;
  }

  @Override
  public String toString() {
    return "[RecordKey schema: " + schema.getClass().getName() + " id: " + id
        + "]";
  }
}
