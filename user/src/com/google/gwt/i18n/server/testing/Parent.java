/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.i18n.server.testing;

import com.google.gwt.i18n.client.Constants;
import com.google.gwt.i18n.client.LocalizableResource.DefaultLocale;
import com.google.gwt.i18n.client.Messages;

import java.util.Map;

/**
 * Parent interface used for test.
 */
@DefaultLocale("en-US")
public interface Parent extends Constants, Messages {

  @DefaultStringValue("inherited")
  String inheritedConstant();

  @DefaultStringMapValue({"k1", "v1", "k2", "v2"})
  Map<String, String> inheritedMap();

  @DefaultMessage("inherited")
  String inheritedMessage();
}