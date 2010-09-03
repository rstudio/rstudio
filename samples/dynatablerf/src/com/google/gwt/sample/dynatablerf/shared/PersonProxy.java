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
package com.google.gwt.sample.dynatablerf.shared;

import com.google.gwt.requestfactory.shared.ProxyFor;
import com.google.gwt.requestfactory.shared.Property;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.sample.dynatablerf.domain.Person;

/**
 * Person DTO.
 */
@ProxyFor(Person.class)
public interface PersonProxy extends EntityProxy {
  /*
   * These property objects will soon no longer be necessary (and will no longer
   * be public api).
   */
  Property<String> name = new Property<String>("name", "Name", String.class);
  Property<String> description = new Property<String>("description",
      "Description", String.class);
  Property<String> schedule = new Property<String>("schedule", "Schedule",
      String.class);
  Property<String> note = new Property<String>("note", "Note", String.class);
  Property<AddressProxy> address = new Property<AddressProxy>("address",
      "Name", AddressProxy.class);

  AddressProxy getAddress();

  String getDescription();

  String getName();

  String getNote();

  String getSchedule();

  void setDescription(String description);

  void setName(String name);

  void setNote(String note);

}
