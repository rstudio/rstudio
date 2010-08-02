/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.sample.dynatablerf.domain;

/**
 * Hold relevant data for Person.
 */
public abstract class Person {
  private static Long serial = 1L;

  private String description = "DESC";

  private String name;
  
  private final Long id;

  public Person() {
    id = serial++;
  }

  public String getDescription() {
    return description;
  }
  
  public Long getId() {
    return id;
  }
  
  public String getName() {
    return name;
  }

  public String getSchedule() {
    return getSchedule(new boolean[] {true, true, true, true, true, true, true});
  }

  public abstract String getSchedule(boolean[] daysFilter);

  public Integer getVersion() {
    return 1;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setName(String name) {
    this.name = name;
  }
}
