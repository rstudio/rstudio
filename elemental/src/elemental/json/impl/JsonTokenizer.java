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

import elemental.json.JsonArray;
import elemental.json.JsonException;
import elemental.json.JsonFactory;
import elemental.json.JsonNumber;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Implementation of parsing a JSON string into instances of {@link
 * com.google.gwt.dev.json.JsonValue}.
 */
class JsonTokenizer {

  private static final int INVALID_CHAR = -1;

  private static final String STOPCHARS = ",:]}/\\\"[{;=#";

  private JsonFactory jsonFactory;

  private boolean lenient = true;

  private int pushBackBuffer = INVALID_CHAR;

  private final String json;
  private int position = 0;

  JsonTokenizer(JreJsonFactory serverJsonFactory, String json) {
    this.jsonFactory = serverJsonFactory;
    this.json = json;
  }

  void back(char c) {
    assert pushBackBuffer == INVALID_CHAR;
    pushBackBuffer = c;
  }

  void back(int c) {
    back((char) c);
  }

  int next() {
    if (pushBackBuffer != INVALID_CHAR) {
      final int c = pushBackBuffer;
      pushBackBuffer = INVALID_CHAR;
      return c;
    }

    return position < json.length() ? json.charAt(position++) : INVALID_CHAR;
  }

  String next(int n) throws JsonException {
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
    while ((pos < n) && ((len = read(buffer, pos, n - pos)) != -1)) {
      pos += len;
    }

    if (pos < n) {
      throw new JsonException("TODO"/* TODO(knorton): Add message. */);
    }

    return String.valueOf(buffer);
  }

  int nextNonWhitespace() {
    while (true) {
      final int c = next();
      if (!Character.isSpace((char) c)) {
        return c;
      }
    }
  }

  String nextString(int startChar) throws JsonException {
    final StringBuffer buffer = new StringBuffer();
    int c = next();
    assert c == '"' || (lenient && c == '\'');
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
          if (c == startChar) {
            return buffer.toString();
          }
          buffer.append((char) c);
      }
    }
  }

  String nextUntilOneOf(String chars)  {
    final StringBuffer buffer = new StringBuffer();
    int c = next();
    while (c != INVALID_CHAR) {
      if (Character.isSpace((char) c) || chars.indexOf((char) c) >= 0) {
        back(c);
        break;
      }
      buffer.append((char) c);
      c = next();
    }
    return buffer.toString();
  }

  <T extends JsonValue> T nextValue() throws JsonException {
    final int c = nextNonWhitespace();
    back(c);
    switch (c) {
      case '"':
      case '\'':
        return (T) jsonFactory.create(nextString(c));
      case '{':
        return (T) parseObject();
      case '[':
        return (T) parseArray();
      default:
        return (T) getValueForLiteral(nextUntilOneOf(STOPCHARS));
    }
  }

  JsonArray parseArray() throws JsonException {
     final JsonArray array = jsonFactory.createArray();
     int c = nextNonWhitespace();
     assert c == '[';
     while (true) {
       c = nextNonWhitespace();
       switch (c) {
         case ']':
           return array;
         default:
           back(c);
           array.set(array.length(), nextValue());
           final int d = nextNonWhitespace();
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

  JsonObject parseObject() throws JsonException {
    final JsonObject object = jsonFactory.createObject();
    int c = nextNonWhitespace();
    if (c != '{') {
      throw new JsonException(
          "Payload does not begin with '{'.  Got " + c + "("
              + Character.valueOf((char) c) + ")");
    }

    while (true) {
      c = nextNonWhitespace();
      switch (c) {
        case '}':
          // We're done.
          return object;
        case '"':
        case '\'':
          back(c);
          // Ready to start a key.
          final String key = nextString(c);
          if (nextNonWhitespace() != ':') {
            throw new JsonException(
                "Invalid object: expecting \":\"");
          }
          // TODO(knorton): Make sure this key is not already set.
          object.put(key, nextValue());
          switch (nextNonWhitespace()) {
            case ',':
              break;
            case '}':
              return object;
            default:
              throw new JsonException(
                  "Invalid object: expecting } or ,");
          }
          break;
        case ',':
          break;
        default:
          if (lenient && (Character.isDigit((char) c) || Character.isLetterOrDigit((char) c)))
        {
          StringBuffer keyBuffer = new StringBuffer();
          keyBuffer.append(c);
          while (true) {
            c = next();
            if (Character.isDigit((char) c) || Character.isLetterOrDigit((char) c)) {
              keyBuffer.append(c);
            } else {
              back(c);
              break;
            }
          }
          if (nextNonWhitespace() != ':') {
            throw new JsonException(
                "Invalid object: expecting \":\"");
          }
          // TODO(knorton): Make sure this key is not already set.
          object.put(keyBuffer.toString(), nextValue());
          switch (nextNonWhitespace()) {
            case ',':
              break;
            case '}':
              return object;
            default:
              throw new JsonException(
                  "Invalid object: expecting } or ,");
          }

        }else{
          throw new JsonException("Invalid object: ");
        }
      }
    }
  }

  private JsonNumber getNumberForLiteral(String literal)
      throws JsonException {
    try {
      return jsonFactory.create(Double.parseDouble(literal));
    } catch (NumberFormatException e) {
      throw new JsonException("Invalid number literal: " + literal);
    }
  }

  private JsonValue getValueForLiteral(String literal) throws JsonException {
    if ("".equals(literal)) {
      throw new JsonException("Missing value");
    }

    if ("null".equals(literal) || "undefined".equals(literal)) {
      return jsonFactory.createNull();
    }

    if ("true".equals(literal)) {
      return jsonFactory.create(true);
    }

    if ("false".equals(literal)) {
      return jsonFactory.create(false);
    }

    final char c = literal.charAt(0);
    if (c == '-' || Character.isDigit(c)) {
      return getNumberForLiteral(literal);
    }

    throw new JsonException("Invalid literal: \"" + literal + "\"");
  }

  private int read(char[] buffer, int pos, int len) {
    int maxLen = Math.min(json.length() - position, len);
    String src = json.substring(position, position + maxLen);
    char result[] = src.toCharArray();
    System.arraycopy(result, 0, buffer, pos, maxLen);
    position += maxLen;
    return maxLen;
  }
}
