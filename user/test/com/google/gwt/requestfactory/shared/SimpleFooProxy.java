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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

/**
 * A simple entity used for testing. Has an int field and date field. Add other
 * data types as their support gets built in.
 */
@ProxyFor(SimpleFoo.class)
public interface SimpleFooProxy extends EntityProxy {
  SimpleBarProxy getBarField();
  
  SimpleBarProxy getBarNullField();
  
  BigDecimal getBigDecimalField();
  
  BigInteger getBigIntField();
  
  Boolean getBoolField();
  
  Byte getByteField();
  
  Character getCharField();
  
  Date getCreated();
  
  Double getDoubleField();
  
  SimpleEnum getEnumField();
  
  Float getFloatField();
  
  SimpleFooProxy getFooField();
  
  Integer getIntId();
  
  Long getLongField();
  
  String getNullField();
  
  Boolean getOtherBoolField();
  
  String getPassword();

  Short getShortField();
  
  String getUserName();
  
  void setBarField(SimpleBarProxy barField);

  void setBarNullField(SimpleBarProxy barNullField);

  void setBigDecimalField(BigDecimal d);

  void setBigIntField(BigInteger i);

  void setBoolField(Boolean boolField);

  void setByteField(Byte b);

  void setCharField(Character c);

  void setCreated(Date created);

  void setDoubleField(Double d);

  void setFloatField(Float f);
  
  void setFooField(SimpleFooProxy fooField);

  void setIntId(Integer intId);

  void setLongField(Long longField);

  void setNullField(String nullField);

  void setOtherBoolField(Boolean boolField);

  void setPassword(String password);

  void setShortField(Short s);

  void setUserName(String userName);
}
