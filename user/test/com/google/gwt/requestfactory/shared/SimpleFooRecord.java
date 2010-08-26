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
package com.google.gwt.requestfactory.shared;

import com.google.gwt.requestfactory.server.SimpleFoo;

import java.util.Date;

/**
 * A simple entity used for testing. Has an int field and date field. Add other
 * data types as their support gets built in.
 */
@DataTransferObject(SimpleFoo.class)
public interface SimpleFooRecord extends Record {

  Property<String> userName = new Property<String>("userName", "User Name",
      String.class);
  Property<String> password = new Property<String>("password", "Password",
      String.class);
  Property<Boolean> boolField = new Property<Boolean>("boolField", Boolean.class);
  Property<Integer> intId = new Property<Integer>("intId", Integer.class);
  Property<Date> created = new Property<Date>("created", Date.class);
  Property<Long> longField = new Property<Long>("longField", Long.class);
  Property<com.google.gwt.requestfactory.shared.SimpleEnum> enumField =
      new EnumProperty<com.google.gwt.requestfactory.shared.SimpleEnum>("enumField",
          com.google.gwt.requestfactory.shared.SimpleEnum.class, SimpleEnum.values());

  Property<SimpleBarRecord> barField = new Property<SimpleBarRecord>("barField",
      SimpleBarRecord.class);

  Property<SimpleFooRecord> fooField = new Property<SimpleFooRecord>("fooField",
      SimpleFooRecord.class);

  SimpleBarRecord getBarField();

  Boolean getBoolField();
  
  Date getCreated();

  SimpleEnum getEnumField();

  SimpleFooRecord getFooField();

  Integer getIntId();

  Long getLongField();

  String getPassword();

  String getUserName();

  void setBarField(SimpleBarRecord barField);

  void setBoolField(Boolean boolField);

  void setCreated(Date created);

  void setFooField(SimpleFooRecord fooField);

  void setIntId(Integer intId);

  void setLongField(Long longField);

  void setPassword(String password);

  void setUserName(String userName);
}
