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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * JSON object.
 */
public class JsonObject implements JsonValue, Iterable<Pair<String, JsonValue>> {
  private static class Iter implements Iterator<Pair<String, JsonValue>> {
    private final Iterator<Map.Entry<String, JsonValue>> iter;

    Iter(Iterator<Map.Entry<String, JsonValue>> iter) {
      this.iter = iter;
    }

    public boolean hasNext() {
      return iter.hasNext();
    }

    public Pair<String, JsonValue> next() {
      final Map.Entry<String, JsonValue> entry = iter.next();
      return new Pair<String, JsonValue>(entry.getKey(), entry.getValue());
    }

    public void remove() {
      iter.remove();
    }
  }

  public static JsonObject create() {
    return new JsonObject();
  }

  /**
   * Creates a {@link JsonObject} from a valid JSON string. This routine expects
   * the first non-whitespace character to be '{', then reads a single encoded
   * object from the {@link Reader} and leaves the reader positioned after the
   * last '}' character.
   *
   * @param reader {@link Reader} positioned to contain a JSON object encoded as
   *          a string.
   * @return a valid {@link JsonObject} on success, throws exception on failure.
   * @throws JsonException The input string is not in valid JSON format.
   * @throws IOException An IO error was encountered before all of the string
   *           could be read.
   */
  public static JsonObject parse(Reader reader) throws JsonException,
      IOException {
    return JsonObject.parse(new Tokenizer(reader));
  }

  static JsonObject parse(Tokenizer tokenizer) throws IOException,
      JsonException {
    final JsonObject object = new JsonObject();
    int c = tokenizer.nextNonWhitespace();
    if (c != '{') {
      throw new JsonException("Payload does not begin with '{'.  Got " + c + "("
          + Character.valueOf((char) c) + ")");
    }

    while (true) {
      c = tokenizer.nextNonWhitespace();
      switch (c) {
        case '}':
          // We're done.
          return object;
        case '"':
          tokenizer.back(c);
          // Ready to start a key.
          final String key = tokenizer.nextString();
          if (tokenizer.nextNonWhitespace() != ':') {
            throw new JsonException("Invalid object: expecting \":\"");
          }
          // TODO(knorton): Make sure this key is not already set.
          object.put(key, tokenizer.nextValue());
          switch (tokenizer.nextNonWhitespace()) {
            case ',':
              break;
            case '}':
              return object;
            default:
              throw new JsonException("Invalid object: expecting } or ,");
          }
          break;
        case ',':
          break;
        default:
          throw new JsonException("Invalid object: ");
      }
    }
  }

  private final Map<String, JsonValue> properties = new HashMap<String, JsonValue>();

  public JsonObject() {
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
    return this;
  }

  public JsonString asString() {
    return null;
  }

  public JsonObject copyDeeply() {
    final JsonObject copy = new JsonObject();
    for (Map.Entry<String, JsonValue> entry : properties.entrySet()) {
      final JsonValue value = entry.getValue();
      copy.properties.put(entry.getKey(), value == null ? null
          : value.copyDeeply());
    }
    return copy;
  }

  public JsonValue get(String key) {
    final JsonValue value = properties.get(key);
    return (value == null) ? JsonValue.NULL : value;
  }

  public boolean isArray() {
    return false;
  }

  public boolean isBoolean() {
    return false;
  }

  public boolean isEmpty() {
    return properties.isEmpty();
  }

  public boolean isNumber() {
    return false;
  }

  public boolean isObject() {
    return true;
  }

  public boolean isString() {
    return false;
  }

  public Iterator<Pair<String, JsonValue>> iterator() {
    return new Iter(properties.entrySet().iterator());
  }

  public void put(String key, boolean val) {
    put(key, JsonBoolean.create(val));
  }

  public void put(String key, double val) {
    put(key, JsonNumber.create(val));
  }

  public void put(String key, JsonValue val) {
    properties.put(key, val);
  }

  public void put(String key, long val) {
    put(key, JsonNumber.create(val));
  }

  public void put(String key, String val) {
    put(key, JsonString.create(val));
  }

  public void write(Writer writer) throws IOException {
    boolean first = true;
    writer.write('{');
    for (Map.Entry<String, JsonValue> e : properties.entrySet()) {
      if (!first) {
        writer.append(',');
      } else {
        first = false;
      }
      JsonString.write(e.getKey(), writer);
      writer.append(':');
      e.getValue().write(writer);
    }
    writer.write('}');
  }
}
