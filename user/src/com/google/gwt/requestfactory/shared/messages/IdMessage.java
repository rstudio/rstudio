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
 * Used as a base type for messages that are about a particular id.
 */
public interface IdMessage {
  String CLIENT_ID = "C";
  String SERVER_ID = "S";
  String TYPE_TOKEN = "T";

  @PropertyName(CLIENT_ID)
  int getClientId();

  @PropertyName(SERVER_ID)
  String getServerId();

  @PropertyName(TYPE_TOKEN)
  String getTypeToken();

  @PropertyName(CLIENT_ID)
  void setClientId(int value);

  @PropertyName(SERVER_ID)
  void setServerId(String value);

  @PropertyName(TYPE_TOKEN)
  void setTypeToken(String value);
}