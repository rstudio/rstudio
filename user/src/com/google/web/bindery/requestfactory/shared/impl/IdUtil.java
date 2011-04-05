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
package com.google.web.bindery.requestfactory.shared.impl;

/**
 * Common functions for slicing and dicing EntityProxy ids.
 */
class IdUtil {
  private static final String ANY_SEPARATOR_PATTERN = "@[012]@";
  private static final String EPHEMERAL_SEPARATOR = "@1@";
  private static final String TOKEN_SEPARATOR = "@0@";
  private static final String SYNTHETIC_SEPARATOR = "@2@";

  private static final int ID_TOKEN_INDEX = 0;
  private static final int TYPE_TOKEN_INDEX = 1;

  public static String ephemeralId(int clientId, String typeToken) {
    return clientId + EPHEMERAL_SEPARATOR + typeToken;
  }

  public static int getClientId(String encodedId) {
    return Integer.valueOf(asEphemeral(encodedId)[ID_TOKEN_INDEX]);
  }

  public static String getServerId(String encodedId) {
    return asPersisted(encodedId)[ID_TOKEN_INDEX];
  }

  public static int getSyntheticId(String encodedId) {
    return Integer.valueOf(asSynthetic(encodedId)[ID_TOKEN_INDEX]);
  }

  public static String getTypeToken(String encodedId) {
    String[] split = asAny(encodedId);
    if (split.length == 2) {
      return split[TYPE_TOKEN_INDEX];
    }
    return null;
  }

  public static boolean isEphemeral(String encodedId) {
    return encodedId.contains(EPHEMERAL_SEPARATOR);
  }

  public static boolean isPersisted(String encodedId) {
    return encodedId.contains(TOKEN_SEPARATOR);
  }

  public static boolean isSynthetic(String encodedId) {
    return encodedId.contains(SYNTHETIC_SEPARATOR);
  }

  public static String persistedId(String serverId, String typeToken) {
    return serverId + TOKEN_SEPARATOR + typeToken;
  }

  public static String syntheticId(int syntheticId, String historyToken) {
    return syntheticId + SYNTHETIC_SEPARATOR + historyToken;
  }

  private static String[] asAny(String encodedId) {
    return encodedId.split(ANY_SEPARATOR_PATTERN);
  }

  private static String[] asEphemeral(String encodedId) {
    return encodedId.split(EPHEMERAL_SEPARATOR);
  }

  private static String[] asPersisted(String encodedId) {
    return encodedId.split(TOKEN_SEPARATOR);
  }

  private static String[] asSynthetic(String encodedId) {
    return encodedId.split(SYNTHETIC_SEPARATOR);
  }

  /**
   * Utility class.
   */
  private IdUtil() {
  }
}
