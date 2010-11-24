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
package com.google.gwt.autobean.server.impl;

import com.google.gwt.autobean.shared.Splittable;
import com.google.gwt.autobean.shared.impl.StringQuoter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Uses the org.json packages to slice and dice request payloads.
 */
public class JsonSplittable implements Splittable {
  public static Splittable create(String payload) {
    try {
      switch (payload.charAt(0)) {
        case '{':
          return new JsonSplittable(new JSONObject(payload));
        case '[':
          return new JsonSplittable(new JSONArray(payload));
        case '"':
          return new JsonSplittable(
              new JSONArray("[" + payload + "]").getString(0));
        case '0':
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
        case '8':
        case '9':
          return new JsonSplittable(payload);
        default:
          throw new RuntimeException("Could not parse payload: payload[0] = "
              + payload.charAt(0));
      }
    } catch (JSONException e) {
      throw new RuntimeException("Could not parse payload", e);
    }
  }

  private final JSONArray array;
  private final JSONObject obj;
  private final String string;

  private JsonSplittable(JSONArray array) {
    this.array = array;
    this.obj = null;
    this.string = null;
  }

  private JsonSplittable(JSONObject obj) {
    this.array = null;
    this.obj = obj;
    this.string = null;
  }

  private JsonSplittable(String string) {
    this.array = null;
    this.obj = null;
    this.string = string;
  }

  public String asString() {
    return string;
  }

  public Splittable get(int index) {
    try {
      return makeSplittable(array.get(index));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  public Splittable get(String key) {
    try {
      return makeSplittable(obj.get(key));
    } catch (JSONException e) {
      throw new RuntimeException(key, e);
    }
  }

  public String getPayload() {
    if (obj != null) {
      return obj.toString();
    }
    if (array != null) {
      return array.toString();
    }
    if (string != null) {
      return StringQuoter.quote(string);
    }
    throw new RuntimeException("No data in this JsonSplittable");
  }

  public List<String> getPropertyKeys() {
    String[] names = JSONObject.getNames(obj);
    if (names == null) {
      return Collections.emptyList();
    } else {
      return Collections.unmodifiableList(Arrays.asList(names));
    }
  }

  public boolean isIndexed() {
    return array != null;
  }

  public boolean isKeyed() {
    return obj != null;
  }

  public boolean isNull(int index) {
    return array.isNull(index);
  }

  public boolean isNull(String key) {
    // Treat undefined and null as the same
    return !obj.has(key) || obj.isNull(key);
  }

  public boolean isString() {
    return string != null;
  }

  public int size() {
    return array.length();
  }

  /**
   * For debugging use only.
   */
  @Override
  public String toString() {
    if (obj != null) {
      return obj.toString();
    } else if (array != null) {
      return array.toString();
    } else if (string != null) {
      return string;
    }
    return "<Uninitialized>";
  }

  private JsonSplittable makeSplittable(Object object) {
    if (object instanceof JSONObject) {
      return new JsonSplittable((JSONObject) object);
    }
    if (object instanceof JSONArray) {
      return new JsonSplittable((JSONArray) object);
    }
    return new JsonSplittable(object.toString());
  }
}