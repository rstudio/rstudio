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

/**
 * Used as a base type for messages that are about a particular id.
 */
public interface IdMessage {
  /**
   * Describes the longevity of the id.
   */
  public enum Strength {
    /**
     * The id is indefinitely persistent and can be freely reused by the client
     * and the server.
     */
    @PropertyName("0")
    PERSISTED,

    /**
     * The id is managed by the client and is generally unknown to the server.
     */
    @PropertyName("1")
    EPHEMERAL,

    /**
     * The id not not managed by the client or server and is valid only for the
     * duration of a single request or response.
     */
    @PropertyName("2")
    SYNTHETIC;
  }

  String CLIENT_ID = "C";
  String SERVER_ID = "S";
  String TYPE_TOKEN = "T";
  String STRENGTH = "R";
  String SYNTHETIC_ID = "Y";

  @PropertyName(CLIENT_ID)
  int getClientId();

  @PropertyName(SERVER_ID)
  String getServerId();

  @PropertyName(STRENGTH)
  Strength getStrength();

  @PropertyName(SYNTHETIC_ID)
  int getSyntheticId();

  @PropertyName(TYPE_TOKEN)
  String getTypeToken();

  @PropertyName(CLIENT_ID)
  void setClientId(int value);

  @PropertyName(SERVER_ID)
  void setServerId(String value);

  @PropertyName(STRENGTH)
  void setStrength(Strength value);

  @PropertyName(SYNTHETIC_ID)
  void setSyntheticId(int value);

  @PropertyName(TYPE_TOKEN)
  void setTypeToken(String value);
}
