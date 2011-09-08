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

import static com.google.gwt.sample.dynatablerf.shared.DynaTableRequestFactory.SchoolCalendarRequest.ALL_DAYS;

import com.google.gwt.sample.dynatablerf.server.SchoolCalendarService;

import java.util.List;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Hold relevant data for Person.
 */
public class Person {
  /**
   * New instances could be created on the client, but it's a better demo to
   * send back a Person with a bunch of dummy data.
   */
  public static Person createPerson() {
    return SchoolCalendarService.createPerson();
  }

  /**
   * The RequestFactory requires a static finder method for each proxied type.
   * Soon it should allow you to customize how instances are found.
   */
  public static Person findPerson(String id) {
    /*
     * TODO At the moment requestFactory requires a finder method per type It
     * should get more flexible soon.
     */
    return SchoolCalendarService.findPerson(id);
  }

  @NotNull
  private Address address = new Address();

  @NotNull
  private Schedule classSchedule = new Schedule();

  @NotNull
  private String description = "DESC";

  private Person mentor;

  @NotNull
  @Size(min = 2, message = "Persons aren't just characters")
  private String name;

  // May be null if the person is newly-created
  private String id;

  @NotNull
  @DecimalMin("0")
  private Integer version = 0;

  private String note;

  private List<Boolean> daysFilters = ALL_DAYS;

  public Person() {
  }

  protected Person(Person copyFrom) {
    copyFrom(copyFrom);
  }

  public void copyFrom(Person copyFrom) {
    address.copyFrom(copyFrom.address);
    classSchedule = copyFrom.classSchedule;
    description = copyFrom.description;
    name = copyFrom.name;
    mentor = copyFrom.mentor;
    id = copyFrom.id;
    version = copyFrom.version;
    note = copyFrom.note;
  }

  public Address getAddress() {
    return address;
  }

  public Schedule getClassSchedule() {
    return classSchedule;
  }

  public String getDescription() {
    return description;
  }

  /**
   * The RequestFactory requires a Long id property for each proxied type.
   * <p>
   * The requirement for some kind of id object with proper hash / equals
   * semantics is not going away, but it should become possible to use types
   * other than Long, and properties other than "id".
   */
  public String getId() {
    return id;
  }

  public Person getMentor() {
    return mentor;
  }

  public String getName() {
    return name;
  }

  public String getNote() {
    return note;
  }
  
  public String getScheduleDescription() {
    return getScheduleWithFilter(daysFilters);
  }

  public String getScheduleWithFilter(List<Boolean> daysFilter) {
    return classSchedule.getDescription(daysFilter);
  }

  /**
   * The RequestFactory requires an Integer version property for each proxied
   * type, but makes no good use of it. This requirement will be removed soon.
   */
  public Integer getVersion() {
    return version;
  }

  public Person makeCopy() {
    return new Person(this);
  }

  public void persist() {
    SchoolCalendarService.persist(this);
  }

  public void setAddress(Address address) {
    this.address = address;
  }
  
  public void setClassSchedule(Schedule schedule) {
    this.classSchedule = schedule;
  }

  public void setDaysFilter(List<Boolean> daysFilter) {
    assert daysFilter.size() == this.daysFilters.size();
    this.daysFilters = daysFilter;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setMentor(Person mentor) {
    this.mentor = mentor;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setNote(String note) {
    this.note = note;
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
