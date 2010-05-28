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
package com.google.gwt.sample.expenses.gwt.scaffold.place;

/**
 * A place in the app focused on the {@link Values} of a particular type of
 * {@link com.google.gwt.valuestore.shared.Record Record}.
 */
public abstract class ScaffoldRecordPlace extends ScaffoldPlace {

  /**
   * The things you do with a record, each of which is a different bookmarkable
   * location in the scaffold app.
   */
  public enum Operation {
    EDIT, DETAILS
  }
  private final String id;

  private final Operation operation;

  /**
   * @param record
   */
  public ScaffoldRecordPlace(String id, Operation operation) {
    assert null != id;
    assert null != operation;

    this.id = id;
    this.operation = operation;
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
    ScaffoldRecordPlace other = (ScaffoldRecordPlace) obj;

    if (!id.equals(other.id)) {
      return false;
    }

    if (!operation.equals(other.operation)) {
      return false;
    }

    return true;
  }

  /**
   * @return the id for this record
   */
  public String getId() {
    return id;
  }

  /**
   * @return what to do with the record here
   */
  public Operation getOperation() {
    return operation;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + id.hashCode();
    result = prime * result + operation.hashCode();
    return result;
  }
}
