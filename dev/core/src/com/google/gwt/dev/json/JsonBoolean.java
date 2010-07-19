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
 * JSON boolean.
 */
public class JsonBoolean implements JsonValue {
  public static final JsonBoolean FALSE = new JsonBoolean(false);

  public static final JsonBoolean TRUE = new JsonBoolean(true);

  public static JsonBoolean create(boolean value) {
    return value ? TRUE : FALSE;
  }

  private final boolean value;

  private JsonBoolean(boolean value) {
    this.value = value;
  }

  public JsonArray asArray() {
    return null;
  }

  public JsonBoolean asBoolean() {
    return this;
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

  public JsonBoolean copyDeeply() {
    return this;
  }

  public boolean getBoolean() {
    return value;
  }

  public boolean isArray() {
    return false;
  }

  public boolean isBoolean() {
    return true;
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
    writer.write(Boolean.toString(value));
  }
}
