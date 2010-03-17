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
package com.google.gwt.sample.expenses.shared;

import com.google.gwt.requestfactory.shared.FieldRef;
import com.google.gwt.requestfactory.shared.LongString;
import com.google.gwt.requestfactory.shared.ServerType;
import com.google.gwt.valuestore.shared.Property;

/**
 * "Generated" proxy of {@link com.google.gwt.sample.expenses.server.domain.Employee
 * domain.Employee}.
 */
@ServerType(com.google.gwt.sample.expenses.server.domain.Employee.class)
public class EmployeeRef implements ExpensesEntity<EmployeeRef> {

  @LongString
  public static final Property<EmployeeRef, String> ID = new Property<EmployeeRef, String>(
      EmployeeRef.class, String.class, "ID");

  public static final Property<EmployeeRef, String> DISPLAY_NAME = new Property<EmployeeRef, String>(
      EmployeeRef.class, String.class, "DISPLAY_NAME");
  public static final Property<EmployeeRef, EmployeeRef> SUPERVISOR = new Property<EmployeeRef, EmployeeRef>(
      EmployeeRef.class, EmployeeRef.class, "SUPERVISOR");

  public static final Property<EmployeeRef, String> USER_NAME = new Property<EmployeeRef, String>(
      EmployeeRef.class, String.class, "USER_NAME");

  public static final Property<EmployeeRef, Integer> VERSION = new Property<EmployeeRef, Integer>(
      EmployeeRef.class, Integer.class, "VERSION");

  private final String id;
  private final Integer version;

  public EmployeeRef(String id, Integer version) {
    this.id = id;
    this.version = version;
  }

  public <T> T accept(ExpensesEntityFilter<T> filter) {
    return filter.filter(this);
  }

  public void accept(ExpensesEntityVisitor visitor) {
    visitor.visit(this);
  }

  public <V> FieldRef<EmployeeRef, V> getFieldRef(Property<EmployeeRef, V> property) {
    return new FieldRef<EmployeeRef, V>(this, property);
  }

  public String getId() {
    return id;
  }

  public Integer getVersion() {
    return version;
  }
}
