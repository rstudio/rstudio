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
import java.io.Writer;

/**
 * JSON String.
 */
public class JsonString implements JsonValue {
  public static JsonString create(String value) {
    return new JsonString(value);
  }

  static void write(String data, Writer writer) throws IOException {
    if (data == null) {
      writer.append("null");
      return;
    }

    writer.append('"');
    for (int i = 0, n = data.length(); i < n; ++i) {
      final char c = data.charAt(i);
      switch (c) {
        case '\\':
        case '"':
          writer.append('\\').append(c);
          break;
        case '\b':
          writer.append("\\b");
          break;
        case '\t':
          writer.append("\\t");
          break;
        case '\n':
          writer.append("\\n");
          break;
        case '\f':
          writer.append("\\f");
          break;
        case '\r':
          writer.append("\\r");
          break;
        default:
          // TODO(knorton): The json.org code encodes ranges of characters in
          // the form u####. Given that JSON is supposed to be UTF-8, I don't
          // understand why you would want to do that.
          writer.append(c);
      }
    }
    writer.append('"');
  }

  private final String value;

  private JsonString(String value) {
    this.value = value;
  }

  public JsonArray asArray() {
    return null;
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
    return this;
  }

  public JsonString copyDeeply() {
    return new JsonString(value);
  }

  public String getString() {
    return value;
  }

  public boolean isArray() {
    return false;
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
    return true;
  }

  public void write(Writer writer) throws IOException {
    write(value, writer);
  }
}
