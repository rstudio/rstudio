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
package com.google.web.bindery.requestfactory.shared;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * A simple proxy used for testing. Has an int field and date field. Add other
 * data types as their support gets built in.
 */
public interface BaseFooProxy extends EntityProxy {

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

  Integer getIntId();

  Long getLongField();

  String getNullField();

  List<Integer> getNumberListField();

  List<SimpleBarProxy> getOneToManyField();

  Set<SimpleBarProxy> getOneToManySetField();

  Boolean getOtherBoolField();

  String getPassword();

  Integer getPleaseCrash();

  List<SimpleFooProxy> getSelfOneToManyField();

  Short getShortField();

  SimpleValueProxy getSimpleValue();

  List<SimpleValueProxy> getSimpleValues();

  boolean getUnpersisted();

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

  void setEnumField(SimpleEnum value);

  void setFloatField(Float f);

  void setIntId(Integer intId);

  void setLongField(Long longField);

  void setNullField(String nullField);

  void setNumberListField(List<Integer> field);

  void setOneToManyField(List<SimpleBarProxy> field);

  void setOneToManySetField(Set<SimpleBarProxy> field);

  void setOtherBoolField(Boolean boolField);

  void setPassword(String password);

  void setPleaseCrash(Integer dummy);

  void setSelfOneToManyField(List<SimpleFooProxy> field);

  void setShortField(Short s);

  void setSimpleValue(SimpleValueProxy value);

  void setSimpleValues(List<SimpleValueProxy> value);

  void setUnpersisted(boolean unpersisted);

  void setUserName(String userName);
}
