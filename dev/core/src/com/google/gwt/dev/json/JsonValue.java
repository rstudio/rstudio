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
 * All specific JSON types in this package implement this interface.
 */
public interface JsonValue {
  /**
   * JSON placeholder for null.
   */
  JsonValue NULL = new JsonValue() {

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
      return null;
    }

    public JsonValue copyDeeply() {
      return this;
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
      return false;
    }

    public void write(Writer writer) throws IOException {
      writer.append("null");
    }
  };

  JsonArray asArray();

  JsonBoolean asBoolean();

  JsonNumber asNumber();

  JsonObject asObject();

  JsonString asString();

  /**
   * Makes a full copy of the JSON data structure.
   */
  JsonValue copyDeeply();

  boolean isArray();

  boolean isBoolean();

  boolean isNumber();

  boolean isObject();

  boolean isString();

  void write(Writer writer) throws IOException;
}
