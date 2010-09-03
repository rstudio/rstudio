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

  Property<String> userName = new Property<String>("userName", "User Name",
      String.class);
  Property<String> password = new Property<String>("password", "Password",
      String.class);

  Property<Character> charField = new Property<Character>("charField", Character.class);
  
  Property<Long> longField = new Property<Long>("longField", Long.class);
  Property<BigDecimal> bigDecimalField = new Property<BigDecimal>("bigDecimalField", BigDecimal.class);
  Property<BigInteger> bigIntField = new Property<BigInteger>("bigIntField", BigInteger.class);
  
  Property<Integer> intId = new Property<Integer>("intId", Integer.class);
  Property<Short> shortField = new Property<Short>("shortField", Short.class);
  Property<Byte> byteField = new Property<Byte>("byteField", Byte.class);
  
  Property<Date> created = new Property<Date>("created", Date.class);

  Property<Double> doubleField = new Property<Double>("doubleField", Double.class);
  Property<Float> floatField = new Property<Float>("floatField", Float.class);
  
  Property<SimpleEnum> enumField =
      new EnumProperty<SimpleEnum>("enumField",
          SimpleEnum.class, SimpleEnum.values());

  Property<Boolean> boolField = new Property<Boolean>("boolField", Boolean.class);
  Property<Boolean> otherBoolField = new Property<Boolean>("otherBoolField", Boolean.class);
  
  Property<SimpleBarProxy> barField = new Property<SimpleBarProxy>("barField",
      SimpleBarProxy.class);

  Property<SimpleFooProxy> fooField = new Property<SimpleFooProxy>("fooField",
      SimpleFooProxy.class);

  SimpleBarProxy getBarField();
  
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
  
  Boolean getOtherBoolField();
  
  String getPassword();

  Short getShortField();
  
  String getUserName();
  
  void setBarField(SimpleBarProxy barField);

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

  void setOtherBoolField(Boolean boolField);

  void setPassword(String password);

  void setShortField(Short s);

  void setUserName(String userName);
}
