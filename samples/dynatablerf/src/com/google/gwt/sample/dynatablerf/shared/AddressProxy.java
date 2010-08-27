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
package com.google.gwt.sample.dynatablerf.shared;

import com.google.gwt.requestfactory.shared.DataTransferObject;
import com.google.gwt.requestfactory.shared.Property;
import com.google.gwt.requestfactory.shared.Record;
import com.google.gwt.sample.dynatablerf.domain.Address;

/**
 * Represents an Address in the client code.
 */
@DataTransferObject(Address.class)
public interface AddressProxy extends Record {
  /*
   * These property objects will soon no longer be necessary (and will no longer
   * be public api).
   */
  Property<String> city = new Property<String>("city", "City", String.class);
  Property<String> state = new Property<String>("state", "State", String.class);
  Property<String> street = new Property<String>("street", "Street", String.class);
  Property<Integer> zip = new Property<Integer>("zip", "Zip", Integer.class);

  String getCity();

  String getState();

  String getStreet();

  Integer getZip();

  void setCity(String city);

  void setState(String state);

  void setStreet(String street);

  void setZip(Integer zip);
}
