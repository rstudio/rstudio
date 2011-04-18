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
package com.google.web.bindery.requestfactory.shared.messages;

import com.google.web.bindery.autobean.shared.AutoBean.PropertyName;
import com.google.web.bindery.autobean.shared.Splittable;

import java.util.List;
import java.util.Set;

/**
 * Describes a method invocation.
 */
public interface InvocationMessage {
  String OPERATIONS = "O";
  String PARAMETERS = "P";
  String PROPERTY_REFS = "R";

  @PropertyName(OPERATIONS)
  String getOperation();

  @PropertyName(PARAMETERS)
  List<Splittable> getParameters();

  @PropertyName(PROPERTY_REFS)
  Set<String> getPropertyRefs();

  @PropertyName(OPERATIONS)
  void setOperation(String value);

  @PropertyName(PARAMETERS)
  void setParameters(List<Splittable> value);

  @PropertyName(PROPERTY_REFS)
  void setPropertyRefs(Set<String> value);
}
