/*
 * Copyright 2009 Google Inc.
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

/**
 * Implementation of parsing a JSON string into instances of {@link JsonValue}.
 */
class Tokenizer {
  private static final int INVALID_CHAR = -1;
  private static final String STOPCHARS = ",:]}/\\\"[{;=#";

  private static JsonNumber getNumberForLiteral(String literal)
      throws JsonException {
    try {
      // The .2 is not a good value, we would need 0.2
      if (literal.indexOf('.') > 0 || literal.indexOf('e') > 0
          || literal.indexOf('E') > 0) {
        return JsonNumber.create(Double.parseDouble(literal));
      }
      return JsonNumber.create(Long.parseLong(literal));
    } catch (NumberFormatException e) {
      throw new JsonException("Invalid number literal: " + literal);
    }
  }

  private static JsonValue getValueForLiteral(String literal)
      throws JsonException {
    if ("".equals(literal)) {
      throw new JsonException("Missing value");
    }

    if ("null".equals(literal)) {
      return JsonValue.NULL;
    }

    if ("true".equals(literal)) {
      return JsonBoolean.create(true);
    }

    if ("false".equals(literal)) {
      return JsonBoolean.create(false);
    }

    final char c = literal.charAt(0);
    if (c == '-' || Character.isDigit(c)) {
      return getNumberForLiteral(literal);
    }

    throw new JsonException("Invalid literal: \"" + literal + "\"");
  }

  private int pushBackBuffer = INVALID_CHAR;

  private final Reader reader;

  Tokenizer(Reader reader) {
    this.reader = reader;
  }

  void back(char c) {
    assert pushBackBuffer == INVALID_CHAR;
    pushBackBuffer = c;
  }

  void back(int c) {
    back((char) c);
  }

  int next() throws IOException {
    if (pushBackBuffer != INVALID_CHAR) {
      final int c = pushBackBuffer;
      pushBackBuffer = INVALID_CHAR;
      return c;
    }

    return reader.read();
  }

  String next(int n) throws IOException, JsonException {
    if (n == 0) {
      return "";
    }

    char[] buffer = new char[n];
    int pos = 0;

    if (pushBackBuffer != INVALID_CHAR) {
      buffer[0] = (char) pushBackBuffer;
      pos = 1;
      pushBackBuffer = INVALID_CHAR;
    }

    int len;
    while ((pos < n) && ((len = reader.read(buffer, pos, n - pos)) != -1)) {
      pos += len;
    }

    if (pos < n) {
      throw new JsonException(/* TODO(knorton): Add message. */);
    }

    return String.valueOf(buffer);
  }

  int nextNonWhitespace() throws IOException {
    while (true) {
      final int c = next();
      if (!Character.isWhitespace(c)) {
        return c;
      }
    }
  }

  String nextString() throws IOException, JsonException {
    final StringBuffer buffer = new StringBuffer();
    int c = next();
    assert c == '"';
    while (true) {
      c = next();
      switch (c) {
        case '\r':
        case '\n':
          throw new JsonException("");
        case '\\':
          c = next();
          switch (c) {
            case 'b':
              buffer.append('\b');
              break;
            case 't':
              buffer.append('\t');
              break;
            case 'n':
              buffer.append('\n');
              break;
            case 'f':
              buffer.append('\f');
              break;
            case 'r':
              buffer.append('\r');
              break;
            // TODO(knorton): I'm not sure I should even support this escaping
            // mode since JSON is always UTF-8.
            case 'u':
              buffer.append((char) Integer.parseInt(next(4), 16));
              break;
            default:
              buffer.append((char) c);
          }
          break;
        default:
          if (c == '"') {
            return buffer.toString();
          }
          buffer.append((char) c);
      }
    }
  }

  String nextUntilOneOf(String chars) throws IOException {
    final StringBuffer buffer = new StringBuffer();
    int c = next();
    while (c != INVALID_CHAR) {
      if (Character.isWhitespace(c) || chars.indexOf((char) c) >= 0) {
        back(c);
        break;
      }
      buffer.append((char) c);
      c = next();
    }
    return buffer.toString();
  }

  JsonValue nextValue() throws IOException, JsonException {
    final int c = nextNonWhitespace();
    back(c);
    switch (c) {
      case '"':
        return JsonString.create(nextString());
      case '{':
        return JsonObject.parse(this);
      case '[':
        return JsonArray.parse(this);
      default:
        return getValueForLiteral(nextUntilOneOf(STOPCHARS));
    }
  }
}
