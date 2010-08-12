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

import com.google.gwt.sample.dynatablerf.server.SchoolCalendarService;

/**
 * Hold relevant data for Person.
 */
public abstract class Person {
  /**
   * The {@link RequestFactory} requires a static finder method for each proxied type.
   * Soon it should allow you to customize how instances are found.
   */
  public static Person findPerson(Long id) {
    return SchoolCalendarService.findPerson(id);
  }

  private String description = "DESC";

  private String name;

  private Long id;

  private Integer version = 0;

  public Person() {
  }

  public String getDescription() {
    return description;
  }

  /**
   * The {@link RequestFactory} requires a Long id property for each proxied type.
   * <p>
   * The requirement for some kind of id object with proper hash / equals
   * semantics is not going away, but it should become possible to use types
   * other than Long, and properties other than "id".
   */
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

  /**
   * The {@link RequestFactory} requires an Integer version property for each proxied
   * type, but makes no good use of it. This requirement will be removed soon.
   */
  public Integer getVersion() {
    return version;
  }

  /**
   * When this was written the {@link RequestFactory} required a persist method per type. 
   * That requirement should be relaxed very soon (and may well have been already
   * if we forget to update this comment).
   */
  public void persist() {
    SchoolCalendarService.persist(this);
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  @Override
  public String toString() {
    return "Person [description=" + description + ", id=" + id + ", name="
        + name + ", version=" + version + "]";
  }
}
