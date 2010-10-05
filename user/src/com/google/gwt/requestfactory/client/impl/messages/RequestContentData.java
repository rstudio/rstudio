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
package com.google.gwt.requestfactory.client.impl.messages;

import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.requestfactory.shared.WriteOperation;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Accumulates data to place in the contentData section of a client request.
 */
public class RequestContentData {
  /**
   * Create a JSON map ouf of keys and JSON expressions. The keys are quoted,
   * however, the expressions are not.
   */
  public static String flattenKeysToExpressions(
      Map<String, String> keysToExpressions) {
    if (keysToExpressions.isEmpty()) {
      return "{}";
    }

    StringBuilder flattenedProperties = new StringBuilder();
    for (Map.Entry<String, String> entry : keysToExpressions.entrySet()) {
      flattenedProperties.append(",").append(
          JsonUtils.escapeValue(entry.getKey())).append(":").append(
          entry.getValue());
    }
    capMap(flattenedProperties);
    return flattenedProperties.toString();
  }

  /**
   * Appends a flattened map object to a larger map of keys to lists.
   */
  private static void addToMap(Map<WriteOperation, StringBuilder> map,
      WriteOperation op, String typeToken, Map<String, String> toFlatten) {
    // { "id": 1, "foo": "bar", "baz": [1,2,3] }
    String flattenedProperties = flattenKeysToExpressions(toFlatten);

    StringBuilder sb = map.get(op);
    if (sb == null) {
      sb = new StringBuilder();
      map.put(op, sb);
    }

    // {"com.google.Foo" : { id:1, foo:"bar"}}
    sb.append(",").append("{\"").append(typeToken).append("\":").append(
        flattenedProperties.toString()).append("}");
  }

  /**
   * For simplicity, most lists are generated with a leading comma. This method
   * makes the StringBuilder a properly terminated.
   */
  private static void capList(StringBuilder properties) {
    properties.deleteCharAt(0).insert(0, "[").append("]");
  }

  private static void capMap(StringBuilder sb) {
    sb.deleteCharAt(0).insert(0, "{").append("}");
  }

  private final Map<WriteOperation, StringBuilder> payloads = new EnumMap<WriteOperation, StringBuilder>(
      WriteOperation.class);

  public void addPersist(String typeToken, Map<String, String> encoded) {
    addToMap(payloads, WriteOperation.PERSIST, typeToken, encoded);
  }

  public void addUpdate(String typeToken, Map<String, String> encoded) {
    addToMap(payloads, WriteOperation.UPDATE, typeToken, encoded);
  }

  public String toJson() {
    Map<String, String> toReturn = new LinkedHashMap<String, String>();
    addToReturn(toReturn, WriteOperation.PERSIST);
    addToReturn(toReturn, WriteOperation.UPDATE);
    return flattenKeysToExpressions(toReturn);
  }

  /**
   * For debugging use only.
   */
  @Override
  public String toString() {
    return toJson();
  }

  private void addToReturn(Map<String, String> toReturn, WriteOperation op) {
    StringBuilder sb = payloads.get(op);
    if (sb != null) {
      capList(sb);
      toReturn.put(op.getUnObfuscatedEnumName(), sb.toString());
    }
  }
}
