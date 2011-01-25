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
package com.google.gwt.editor.client;

/**
 * Simple data object used by multiple tests.
 */
public class Person {
  Address address;
  Person manager;
  String name;
  long localTime;

  public Address getAddress() {
    return address;
  }

  public long getLocalTime() {
    return localTime;
  }

  public Person getManager() {
    return manager;
  }

  public String getName() {
    return name;
  }

  public void setAddress(Address address) {
    this.address = address;
  }

  public void setLocalTime(long localTime) {
    this.localTime = localTime;
  }

  public void setManager(Person manager) {
    this.manager = manager;
  }

  public void setName(String name) {
    this.name = name;
  }
}