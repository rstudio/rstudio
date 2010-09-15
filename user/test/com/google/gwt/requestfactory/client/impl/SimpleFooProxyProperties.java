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
package com.google.gwt.requestfactory.client.impl;

import com.google.gwt.requestfactory.shared.SimpleBarProxy;
import com.google.gwt.requestfactory.shared.SimpleEnum;
import com.google.gwt.requestfactory.shared.SimpleFooProxy;
import com.google.gwt.requestfactory.shared.impl.EnumProperty;
import com.google.gwt.requestfactory.shared.impl.Property;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

class SimpleFooProxyProperties {
  static final Property<Long> id = new Property<Long>("id", "Id",
      Long.class);
  static final Property<String> version = new Property<String>("version", "Version",
      String.class);

  static final Property<String> userName = new Property<String>("userName", "User Name",
      String.class);
  static final Property<String> password = new Property<String>("password", "Password",
      String.class);

  static final Property<Character> charField = new Property<Character>("charField",
      Character.class);

  static final Property<Long> longField = new Property<Long>("longField", Long.class);
  static final Property<BigDecimal> bigDecimalField = new Property<BigDecimal>(
      "bigDecimalField", BigDecimal.class);
  static final Property<BigInteger> bigIntField = new Property<BigInteger>("bigIntField",
      BigInteger.class);

  static final Property<Integer> intId = new Property<Integer>("intId", Integer.class);
  static final Property<Short> shortField = new Property<Short>("shortField", Short.class);
  static final Property<Byte> byteField = new Property<Byte>("byteField", Byte.class);

  static final Property<Date> created = new Property<Date>("created", Date.class);

  static final Property<Double> doubleField = new Property<Double>("doubleField",
      Double.class);
  static final Property<Float> floatField = new Property<Float>("floatField", Float.class);

  static final Property<SimpleEnum> enumField = new EnumProperty<SimpleEnum>("enumField",
      SimpleEnum.class, SimpleEnum.values());

  static final Property<Boolean> boolField = new Property<Boolean>("boolField",
      Boolean.class);
  static final Property<Boolean> otherBoolField = new Property<Boolean>("otherBoolField",
      Boolean.class);

  static final Property<SimpleBarProxy> barField = new Property<SimpleBarProxy>(
      "barField", SimpleBarProxy.class);

  static final Property<SimpleFooProxy> fooField = new Property<SimpleFooProxy>(
      "fooField", SimpleFooProxy.class);
}