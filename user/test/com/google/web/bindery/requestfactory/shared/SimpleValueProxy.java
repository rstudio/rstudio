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

import com.google.web.bindery.requestfactory.server.SimpleValue;

import java.util.Date;
import java.util.List;

/**
 * A proxy for a non-addressable value object.
 */
@ProxyFor(value = SimpleValue.class)
public interface SimpleValueProxy extends ValueProxy {
  Date getDate();

  int getNumber();

  String getShouldBeNull();

  SimpleFooProxy getSimpleFoo();

  List<SimpleValueProxy> getSimpleValue();

  String getString();

  void setDate(Date value);

  void setNumber(int value);

  void setShouldBeNull(String value);

  void setSimpleFoo(SimpleFooProxy value);

  void setSimpleValue(List<SimpleValueProxy> value);

  void setString(String value);
}
