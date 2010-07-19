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
package com.google.gwt.dev.json;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * A JSON Array.
 */
public class JsonArray implements JsonValue {
  public static JsonArray create() {
    return new JsonArray();
  }

  public static JsonArray parse(Reader reader) throws IOException,
      JsonException {
    final JsonArray arr = new Tokenizer(reader).nextValue().asArray();
    if (arr == null) {
      throw new JsonException("Object is not a JSON array.");
    }
    return arr;
  }

  static JsonArray parse(Tokenizer tokenizer) throws IOException, JsonException {
    final JsonArray array = new JsonArray();
    int c = tokenizer.nextNonWhitespace();
    assert c == '[';
    while (true) {
      c = tokenizer.nextNonWhitespace();
      switch (c) {
        case ']':
          return array;
        default:
          tokenizer.back(c);
          array.add(tokenizer.nextValue());
          final int d = tokenizer.nextNonWhitespace();
          switch (d) {
            case ']':
              return array;
            case ',':
              break;
            default:
              throw new JsonException("Invalid array: expected , or ]");
          }
      }
    }
  }

  private final List<JsonValue> values = new ArrayList<JsonValue>();

  public JsonArray() {
  }

  public void add(boolean value) {
    add(JsonBoolean.create(value));
  }

  public void add(double value) {
    add(JsonNumber.create(value));
  }

  public void add(JsonValue value) {
    values.add(value);
  }

  public void add(long value) {
    add(JsonNumber.create(value));
  }

  public void add(String value) {
    add(JsonString.create(value));
  }

  public JsonArray asArray() {
    return this;
  }

  public JsonBoolean asBoolean() {
    return null;
  }

  public JsonNumber asNumber() {
    return null;
  }

  public JsonObject asObject() {
    return null;
  }

  public JsonString asString() {
    return null;
  }

  public JsonArray copyDeeply() {
    final JsonArray copy = new JsonArray();
    for (JsonValue value : values) {
      copy.values.add(value == null ? null : value.copyDeeply());
    }
    return copy;
  }

  public JsonValue get(int index) {
    final JsonValue value = values.get(index);
    return (value == null) ? JsonValue.NULL : value;
  }

  public int getLength() {
    return values.size();
  }

  public boolean isArray() {
    return true;
  }

  public boolean isBoolean() {
    return false;
  }

  public boolean isNumber() {
    return false;
  }

  public boolean isObject() {
    return false;
  }

  public boolean isString() {
    return false;
  }

  public void write(Writer writer) throws IOException {
    writer.write('[');
    for (int i = 0, n = values.size(); i < n; ++i) {
      if (i != 0) {
        writer.write(',');
      }
      values.get(i).write(writer);
    }
    writer.write(']');
  }
}
