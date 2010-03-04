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

import com.google.gwt.requestfactory.shared.Entity;
import com.google.gwt.requestfactory.shared.Slot;
import com.google.gwt.valuestore.shared.Property;

/**
 * The employee proxy object, would be auto-generated.
 *
 */
public class Employee implements Entity<Employee> {
  
   public static final Property<Employee, String> DISPLAY_NAME = new Property<Employee, String>(
      Employee.class, String.class, "DISPLAY_NAME");
  public static final Property<Employee, Employee> SUPERVISOR = new Property<Employee, Employee>(
      Employee.class, Employee.class, "SUPERVISOR");

  public static final Property<Employee, String> USER_NAME = new Property<Employee, String>(
      Employee.class, String.class, "USER_NAME");
  
  private final String id;
  private final Integer version;

  Employee(String id, Integer version) {
    this.id = id;
    this.version = version;
  }
  
  public String getId() {
    return id;
  }

  public Integer getVersion() {
    return version;
  }

  public <V> Slot<Employee, V> slot(Property<Employee, V> property) {
    return new Slot<Employee, V>(this, property);
  }
}
