/*
 * Copyright 2008 Google Inc.
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

package com.google.gwt.i18n.client;

import com.google.gwt.i18n.client.LocalizableResource.DefaultLocale;
import com.google.gwt.i18n.client.LocalizableResource.Generate;
import com.google.gwt.i18n.client.LocalizableResource.GenerateKeys;

import java.util.Map;

/**
 * Test of Constants generation using annotations.
 */
@DefaultLocale("en-US")
@GenerateKeys("com.google.gwt.i18n.server.keygen.MethodNameKeyGenerator") // default
@Generate(format = "com.google.gwt.i18n.server.PropertyCatalogFactory")
public interface TestAnnotatedConstants extends Constants {

  @DefaultIntValue(14)
  int fourteen();
  
  @DefaultBooleanValue(false)
  boolean isFalse();
  
  @DefaultBooleanValue(true)
  boolean isTrue();
  
  @Key(" properties key ")
  @DefaultStringValue("Key with whitespace")
  String propertiesKey();
  
  @DefaultStringValue("Properties value #s need quoting!")
  String propertiesQuoting();
  
  @DefaultStringValue("   Check that leading spaces are quoted and trailing ones aren't   ")
  String propertiesSpaces();
  
  @DefaultStringArrayValue("String array with one string")
  String[] singleString();
  
  @DefaultStringMapValue({"key1", "value1", "key2", "value2"})
  Map<String, String> stringMap();

  @SuppressWarnings({"rawtypes"}) // intentional test of raw Map type
  @DefaultStringMapValue({"key1", "value1", "key2", "value2"})
  Map rawMap();

  @DefaultStringValue("Test me")
  String testMe();
  
  @DefaultFloatValue(13.7f)
  float thirteenPointSeven();
  
  @DefaultDoubleValue(3.14)
  double threePointOneFour();
  
  @DefaultStringArrayValue({"One", "Two", "Three,Comma"})
  String[] threeStrings();
  
  @DefaultStringValue("Once more, with meaning")
  @Meaning("Mangled quote")
  String withMeaning();
}
