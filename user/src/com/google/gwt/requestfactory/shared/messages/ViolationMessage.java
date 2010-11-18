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
package com.google.gwt.requestfactory.shared.messages;

import com.google.gwt.autobean.shared.AutoBean.PropertyName;

/**
 * Represents a ConstraintViolation.
 */
public interface ViolationMessage extends IdMessage {
  String MESSAGE = "M";
  String PATH = "P";

  @PropertyName(MESSAGE)
  String getMessage();

  @PropertyName(PATH)
  String getPath();

  @PropertyName(MESSAGE)
  void setMessage(String value);

  @PropertyName(PATH)
  void setPath(String value);
}
