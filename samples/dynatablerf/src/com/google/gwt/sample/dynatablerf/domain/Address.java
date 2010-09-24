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
package com.google.gwt.sample.dynatablerf.domain;

import com.google.gwt.sample.dynatablerf.server.SchoolCalendarService;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Represents an address.
 */
public class Address {
  /**
   * The RequestFactory requires a static finder method for each proxied type.
   * Soon it should allow you to customize how instances are found.
   */
  public static Address findAddress(String id) {
    return SchoolCalendarService.findPerson(id).getAddress();
  }

  @NotNull
  @Size(min = 1)
  private String city;

  // May be null if Address is newly-created
  private String id;

  @NotNull
  @Size(min = 1)
  private String state;

  @NotNull
  @Size(min = 1)
  private String street;

  @NotNull
  @DecimalMin("0")
  private Integer version = 0;

  @NotNull
  @DecimalMin("10000")
  private Integer zip;

  public Address() {
  }

  private Address(Address copyFrom) {
    copyFrom(copyFrom);
  }

  public void copyFrom(Address copyFrom) {
    city = copyFrom.city;
    id = copyFrom.id;
    state = copyFrom.state;
    street = copyFrom.street;
    version = copyFrom.version;
    zip = copyFrom.zip;
  }

  public String getCity() {
    return city;
  }

  public String getId() {
    return id;
  }

  public String getState() {
    return state;
  }

  public String getStreet() {
    return street;
  }

  public Integer getVersion() {
    return version;
  }

  public Integer getZip() {
    return zip;
  }

  public Address makeCopy() {
    return new Address(this);
  }

  /**
   * When this was written the RequestFactory required a persist method per
   * type. That requirement should be relaxed very soon (and may well have been
   * already if we forget to update this comment).
   */
  public void persist() {
    SchoolCalendarService.persist(this);
  }

  public void setCity(String city) {
    this.city = city;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setState(String state) {
    this.state = state;
  }

  public void setStreet(String street) {
    this.street = street;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public void setZip(Integer zip) {
    this.zip = zip;
  }
}
