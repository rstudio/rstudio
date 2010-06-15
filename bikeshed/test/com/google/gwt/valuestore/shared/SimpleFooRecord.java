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
package com.google.gwt.valuestore.shared;

import java.util.Date;

/**
 * A simple entity used for testing. Has an int field and date field. Add other
 * data types as their support gets built in.
 */
public interface SimpleFooRecord extends Record {

  String TOKEN = "SimpleFooRecord";

  Property<String> userName = new Property<String>("userName", "User Name",
      String.class);
  Property<String> password = new Property<String>("password", "Password",
      String.class);
  Property<Integer> intId = new Property<Integer>("intId", Integer.class);
  Property<Date> created = new Property<Date>("created", Date.class);

  Date getCreated();

  Integer getIntId();

  String getPassword();

  String getUserName();
}