/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.sample.i18n.client;

import com.google.gwt.i18n.client.Constants;

import java.util.Map;

/**
 * Internationalized constants used to demonstrate {@link Constants}.
 */
public interface ConstantsExampleConstants extends Constants {

  @DefaultStringMapValue({"black", "Black", "blue", "Blue", "green", "Green", "grey", "Grey",
      "lightGrey", "Light Grey", "red", "Red", "white", "White", "yellow", "Yellow"})
  Map<String, String> colorMap();

  @DefaultStringValue("Favorite color")
  String favoriteColor();

  @DefaultStringValue("First Name")
  String firstName();

  @DefaultStringValue("Last Name")
  String lastName();
}
