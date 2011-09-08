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

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * Represents an address.
 */
public class Address {
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
  @Pattern(regexp = "\\d{5}(-\\d{4})?")
  private String zip;

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

  public String getState() {
    return state;
  }

  public String getStreet() {
    return street;
  }

  public String getZip() {
    return zip;
  }

  public Address makeCopy() {
    return new Address(this);
  }

  public void setCity(String city) {
    this.city = city;
  }

  public void setState(String state) {
    this.state = state;
  }

  public void setStreet(String street) {
    this.street = street;
  }

  public void setZip(String zip) {
    this.zip = zip;
  }
}
