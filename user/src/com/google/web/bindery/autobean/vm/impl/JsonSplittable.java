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
package com.google.web.bindery.autobean.vm.impl;

import com.google.gwt.core.client.impl.WeakMapping;
import com.google.web.bindery.autobean.shared.Splittable;
import com.google.web.bindery.autobean.shared.impl.HasSplittable;
import com.google.web.bindery.autobean.shared.impl.StringQuoter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Uses the org.json packages to slice and dice request payloads.
 */
public class JsonSplittable implements Splittable, HasSplittable {

  public static JsonSplittable create() {
    return new JsonSplittable(new JSONObject());
  }

  public static Splittable create(String payload) {
    try {
      switch (payload.charAt(0)) {
        case '{':
          return new JsonSplittable(new JSONObject(payload));
        case '[':
          return new JsonSplittable(new JSONArray(payload));
        case '"':
          return new JsonSplittable(new JSONArray("[" + payload + "]").getString(0));
        case '-':
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
          return new JsonSplittable(Double.parseDouble(payload));
        case 't':
        case 'f':
          return new JsonSplittable(Boolean.parseBoolean(payload));
        case 'n':
          return null;
        default:
          throw new RuntimeException("Could not parse payload: payload[0] = " + payload.charAt(0));
      }
    } catch (JSONException e) {
      throw new RuntimeException("Could not parse payload", e);
    }
  }

  public static Splittable createIndexed() {
    return new JsonSplittable(new JSONArray());
  }

  public static Splittable createNull() {
    return new JsonSplittable();
  }

  /**
   * Private equivalent of org.json.JSONObject.getNames(JSONObject) since that
   * method is not available in Android 2.2. Used to represent a null value.
   */
  private static String[] getNames(JSONObject json) {
    int length = json.length();
    if (length == 0) {
      return null;
    }
    String[] names = new String[length];
    Iterator<?> i = json.keys();
    int j = 0;
    while (i.hasNext()) {
      names[j++] = (String) i.next();
    }
    return names;
  }

  private JSONArray array;
  private Boolean bool;
  /**
   * Used to represent a null value.
   */
  private boolean isNull;
  private Double number;
  private JSONObject obj;
  private String string;
  private final Map<String, Object> reified = new HashMap<String, Object>();

  /**
   * Constructor for a null value.
   */
  private JsonSplittable() {
    isNull = true;
  }

  private JsonSplittable(boolean value) {
    this.bool = value;
  }

  private JsonSplittable(double value) {
    this.number = value;
  }

  private JsonSplittable(JSONArray array) {
    this.array = array;
  }

  private JsonSplittable(JSONObject obj) {
    this.obj = obj;
  }

  private JsonSplittable(String string) {
    this.array = null;
    this.obj = null;
    this.string = string;
  }

  public boolean asBoolean() {
    return bool;
  }

  public double asNumber() {
    return number;
  }

  public void assign(Splittable parent, int index) {
    try {
      ((JsonSplittable) parent).array.put(index, value());
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  public void assign(Splittable parent, String propertyName) {
    try {
      ((JsonSplittable) parent).obj.put(propertyName, value());
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  public String asString() {
    return string;
  }

  public Splittable deepCopy() {
    return create(getPayload());
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
    if (isNull) {
      return "null";
    }
    if (obj != null) {
      return obj.toString();
    }
    if (array != null) {
      return array.toString();
    }
    if (string != null) {
      return StringQuoter.quote(string);
    }
    if (number != null) {
      return String.valueOf(number);
    }
    if (bool != null) {
      return String.valueOf(bool);
    }
    throw new RuntimeException("No data in this JsonSplittable");
  }

  public List<String> getPropertyKeys() {
    String[] names = getNames(obj);
    if (names == null) {
      return Collections.emptyList();
    } else {
      return Collections.unmodifiableList(Arrays.asList(names));
    }
  }

  public Object getReified(String key) {
    return reified.get(key);
  }

  public Splittable getSplittable() {
    return this;
  }

  public boolean isBoolean() {
    return bool != null;
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

  public boolean isNumber() {
    return number != null;
  }

  public boolean isReified(String key) {
    return reified.containsKey(key);
  }

  public boolean isString() {
    return string != null;
  }

  public boolean isUndefined(String key) {
    return !obj.has(key);
  }

  public void setReified(String key, Object object) {
    reified.put(key, object);
  }

  public void setSize(int size) {
    // This is terrible, but there's no API support for resizing or splicing
    JSONArray newArray = new JSONArray();
    for (int i = 0; i < size; i++) {
      try {
        newArray.put(i, array.get(i));
      } catch (JSONException e) {
        throw new RuntimeException(e);
      }
    }
    array = newArray;
  }

  public int size() {
    return array.length();
  }

  private synchronized JsonSplittable makeSplittable(Object object) {
    if (JSONObject.NULL.equals(object)) {
      return null;
    }
    /*
     * Maintain a 1:1 mapping between object instances and JsonSplittables.
     * Doing this with a WeakHashMap doesn't work on Android, since its org.json
     * arrays appear to have value-based equality.
     */
    JsonSplittable seen = (JsonSplittable) WeakMapping.get(object, JsonSplittable.class.getName());
    if (seen == null) {
      if (object instanceof JSONObject) {
        seen = new JsonSplittable((JSONObject) object);
        WeakMapping.setWeak(object, JsonSplittable.class.getName(), seen);
      } else if (object instanceof JSONArray) {
        seen = new JsonSplittable((JSONArray) object);
        WeakMapping.setWeak(object, JsonSplittable.class.getName(), seen);
      } else if (object instanceof String) {
        seen = new JsonSplittable(object.toString());
      } else if (object instanceof Number) {
        seen = new JsonSplittable(((Number) object).doubleValue());
      } else if (object instanceof Boolean) {
        seen = new JsonSplittable((Boolean) object);
      } else {
        throw new RuntimeException("Unhandled type " + object.getClass());
      }
    }
    return seen;
  }

  private Object value() {
    if (isNull) {
      return null;
    }
    if (obj != null) {
      return obj;
    }
    if (array != null) {
      return array;
    }
    if (string != null) {
      return string;
    }
    if (number != null) {
      return number;
    }
    if (bool != null) {
      return bool;
    }
    throw new RuntimeException("No data");
  }
}
