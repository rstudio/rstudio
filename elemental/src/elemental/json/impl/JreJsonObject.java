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
package elemental.json.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonBoolean;
import elemental.json.JsonFactory;
import elemental.json.JsonNumber;
import elemental.json.JsonObject;
import elemental.json.JsonString;
import elemental.json.JsonType;
import elemental.json.JsonValue;

/**
 * Server-side implementation of JsonObject.
 */
public class JreJsonObject extends JreJsonValue implements JsonObject {

  private static final long serialVersionUID = 1L;

  private static List<String> stringifyOrder(String[] keys) {
    List<String> toReturn = new ArrayList<String>();
    List<String> nonNumeric = new ArrayList<String>();
    for (String key : keys) {
      if (key.matches("\\d+")) {
        toReturn.add(key);
      } else {
        nonNumeric.add(key);
      }
    }
    Collections.sort(toReturn);
    toReturn.addAll(nonNumeric);
    return toReturn;
  }

  private transient JsonFactory factory;
  private transient Map<String, JsonValue> map = new LinkedHashMap<String, JsonValue>();

  public JreJsonObject(JsonFactory factory) {
    this.factory = factory;
  }

  @Override
  public boolean asBoolean() {
    return true;
  }

  @Override
  public double asNumber() {
    return Double.NaN;
  }

  @Override
  public String asString() {
    return "[object Object]";
  }

  public JsonValue get(String key) {
    return map.get(key);
  }

  public JsonArray getArray(String key) {
    return (JsonArray) get(key);
  }


  public boolean getBoolean(String key) {
    return ((JsonBoolean) get(key)).getBoolean();
  }

  public double getNumber(String key) {
    return ((JsonNumber) get(key)).getNumber();
  }

  public JsonObject getObject(String key) {
    return (JsonObject) get(key);
  }

  public Object getObject() {
    Map<String, Object> obj = new HashMap<String, Object>();
    for (Map.Entry<String, JsonValue> e : map.entrySet()) {
      obj.put(e.getKey(), ((JreJsonValue) e.getValue()).getObject());
    }
    return obj;
  }


  public String getString(String key) {
    return ((JsonString) get(key)).getString();
  }

  public JsonType getType() {
    return JsonType.OBJECT;
  }

  @Override
  public boolean hasKey(String key) {
    return map.containsKey(key);
  }

  @Override
  public boolean jsEquals(JsonValue value) {
    return getObject().equals(((JreJsonValue) value).getObject());
  }

  public String[] keys() {
    return map.keySet().toArray(new String[map.size()]);
  }

  public void put(String key, JsonValue value) {
    if (value == null) {
      value = factory.createNull();
    }
    map.put(key, value);
  }

  public void put(String key, String value) {
    put(key, factory.create(value));
  }

  public void put(String key, double value) {
    put(key, factory.create(value));
  }

  public void put(String key, boolean bool) {
    put(key, factory.create(bool));
  }

  @Override
  public void remove(String key) {
    map.remove(key);
  }

  public void set(String key, JsonValue value) {
      put(key, value);
  }

  public String toJson() {
    return JsonUtil.stringify(this);
  }

  public String toString() {
    return toJson();
  }

  @Override
  public void traverse(JsonVisitor visitor, JsonContext ctx) {
    if (visitor.visit(this, ctx)) {
      JsonObjectContext objCtx = new JsonObjectContext(this);
      for (String key : stringifyOrder(keys())) {
        objCtx.setCurrentKey(key);
        if (visitor.visitKey(objCtx.getCurrentKey(), objCtx)) {
          visitor.accept(get(key), objCtx);
          objCtx.setFirst(false);
        }
      }
    }
    visitor.endVisit(this, ctx);
  }

  private void readObject(ObjectInputStream stream)
          throws IOException, ClassNotFoundException {
    JreJsonObject instance = parseJson(stream);
    this.factory = Json.instance();
    this.map = instance.map;
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.writeObject(toJson());
  }

}
