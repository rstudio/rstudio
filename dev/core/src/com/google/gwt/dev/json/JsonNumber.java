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
 * JSON Number.
 */
public abstract class JsonNumber implements JsonValue {
  private static class JsonDecimal extends JsonNumber {
    private final double value;

    public JsonDecimal(double value) {
      this.value = value;
    }

    public JsonDecimal copyDeeply() {
      return new JsonDecimal(value);
    }

    @Override
    public double getDecimal() {
      return value;
    }

    @Override
    public long getInteger() {
      return (long) value;
    }

    public void write(Writer writer) throws IOException {
      writer.write(Double.toString(value));
    }
  }

  private static class JsonInteger extends JsonNumber {
    private final long value;

    public JsonInteger(long value) {
      this.value = value;
    }

    public JsonInteger copyDeeply() {
      return new JsonInteger(value);
    }

    @Override
    public double getDecimal() {
      return value;
    }

    @Override
    public long getInteger() {
      return value;
    }

    public void write(Writer writer) throws IOException {
      writer.write(Long.toString(value));
    }
  }

  public static JsonNumber create(double value) {
    return new JsonDecimal(value);
  }

  public static JsonNumber create(int value) {
    return new JsonInteger(value);
  }

  public static JsonNumber create(long value) {
    return new JsonInteger(value);
  }

  private JsonNumber() {
  }

  public JsonArray asArray() {
    return null;
  }

  public JsonBoolean asBoolean() {
    return null;
  }

  public JsonNumber asNumber() {
    return this;
  }

  public JsonObject asObject() {
    return null;
  }

  public JsonString asString() {
    return null;
  }

  public abstract double getDecimal();

  public abstract long getInteger();

  public boolean isArray() {
    return false;
  }

  public boolean isBoolean() {
    return false;
  }

  public boolean isNumber() {
    return true;
  }

  public boolean isObject() {
    return false;
  }

  public boolean isString() {
    return false;
  }
}
