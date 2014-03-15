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
import com.google.web.bindery.requestfactory.shared.WriteOperation;

import java.util.Map;

/**
 * Represents an operation to be carried out on a single entity on the server.
 */
public interface OperationMessage extends IdMessage, VersionedMessage {
  String OPERATION = "O";
  String PROPERTY_MAP = "P";

  @PropertyName(OPERATION)
  WriteOperation getOperation();

  @PropertyName(PROPERTY_MAP)
  Map<String, Splittable> getPropertyMap();

  @PropertyName(OPERATION)
  void setOperation(WriteOperation value);

  @PropertyName(PROPERTY_MAP)
  void setPropertyMap(Map<String, Splittable> map);
}
