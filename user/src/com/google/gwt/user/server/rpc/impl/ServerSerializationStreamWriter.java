/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.user.server.rpc.impl;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.client.rpc.impl.AbstractSerializationStreamWriter;
import com.google.gwt.user.server.rpc.SerializationPolicy;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;

/**
 * For internal use only. Used for server call serialization. This class is
 * carefully matched with the client-side version.
 */
public final class ServerSerializationStreamWriter extends
    AbstractSerializationStreamWriter {

  private static final char NON_BREAKING_HYPHEN = '\u2011';

  /**
   * Number of escaped JS Chars.
   */
  private static final int NUMBER_OF_JS_ESCAPED_CHARS = 128;

  /**
   * A list of any characters that need escaping when printing a JavaScript
   * string literal. Contains a 0 if the character does not need escaping,
   * otherwise contains the character to escape with.
   */
  private static final char[] JS_CHARS_ESCAPED = new char[NUMBER_OF_JS_ESCAPED_CHARS];

  /**
   * This defines the character used by JavaScript to mark the start of an
   * escape sequence.
   */
  private static final char JS_ESCAPE_CHAR = '\\';

  /**
   * This defines the character used to enclose JavaScript strings.
   */
  private static final char JS_QUOTE_CHAR = '\"';

  /**
   * Index into this array using a nibble, 4 bits, to get the corresponding
   * hexa-decimal character representation.
   */
  private static final char NIBBLE_TO_HEX_CHAR[] = {
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',
      'E', 'F'};

  static {
    /*
     * NOTE: The JS VM in IE6 & IE7 do not interpret \v correctly. They convert
     * JavaScript Vertical Tab character '\v' into 'v'. As such, we do not use
     * the short form of the unicode escape here.
     */
    JS_CHARS_ESCAPED['\u0000'] = '0';
    JS_CHARS_ESCAPED['\b'] = 'b';
    JS_CHARS_ESCAPED['\t'] = 't';
    JS_CHARS_ESCAPED['\n'] = 'n';
    JS_CHARS_ESCAPED['\f'] = 'f';
    JS_CHARS_ESCAPED['\r'] = 'r';
    JS_CHARS_ESCAPED[JS_ESCAPE_CHAR] = JS_ESCAPE_CHAR;
    JS_CHARS_ESCAPED[JS_QUOTE_CHAR] = JS_QUOTE_CHAR;
  }

  /**
   * This method takes a string and outputs a JavaScript string literal. The
   * data is surrounded with quotes, and any contained characters that need to
   * be escaped are mapped onto their escape sequence.
   * 
   * Assumptions: We are targeting a version of JavaScript that that is later
   * than 1.3 that supports unicode strings.
   */
  private static String escapeString(String toEscape) {
    // make output big enough to escape every character (plus the quotes)
    char[] input = toEscape.toCharArray();
    CharVector charVector = new CharVector(input.length * 2 + 2, input.length);

    charVector.add(JS_QUOTE_CHAR);

    for (int i = 0, n = input.length; i < n; ++i) {
      char c = input[i];
      if (c < NUMBER_OF_JS_ESCAPED_CHARS && JS_CHARS_ESCAPED[c] != 0) {
        charVector.add(JS_ESCAPE_CHAR);
        charVector.add(JS_CHARS_ESCAPED[c]);
      } else if (needsUnicodeEscape(c)) {
        charVector.add(JS_ESCAPE_CHAR);
        unicodeEscape(c, charVector);
      } else {
        charVector.add(c);
      }
    }

    charVector.add(JS_QUOTE_CHAR);
    return String.valueOf(charVector.asArray(), 0, charVector.getSize());
  }

  /**
   * Returns <code>true</code> if the character requires the \\uXXXX unicode
   * character escape sequence. This is necessary if the raw character could be
   * consumed and/or interpreted as a special character when the JSON encoded
   * response is evaluated. For example, 0x2028 and 0x2029 are alternate line
   * endings for JS per ECMA-232, which are respected by Firefox and Mozilla.
   * 
   * @param ch character to check
   * @return <code>true</code> if the character requires the \\uXXXX unicode
   *         character escape
   * 
   * Notes:
   * <ol>
   * <li> The following cases are a more conservative set of cases which are are
   * in the future proofing space as opposed to the required minimal set. We
   * could remove these and still pass our tests.
   * <ul>
   * <li>UNASSIGNED - 6359</li>
   * <li>NON_SPACING_MARK - 530</li>
   * <li>ENCLOSING_MARK - 10</li>
   * <li>COMBINING_SPACE_MARK - 131</li>
   * <li>SPACE_SEPARATOR - 19</li>
   * <li>CONTROL - 65</li>
   * <li>PRIVATE_USE - 6400</li>
   * <li>DASH_PUNCTUATION - 1</li>
   * <li>Total Characters Escaped: 13515</li>
   * </ul>
   * </li>
   * <li> The following cases are the minimal amount of escaping required to
   * prevent test failure.
   * <ul>
   * <li>LINE_SEPARATOR - 1</li>
   * <li>PARAGRAPH_SEPARATOR - 1</li>
   * <li>FORMAT - 32</li>
   * <li>SURROGATE - 2048</li>
   * <li>Total Characters Escaped: 2082</li>
   * </li>
   * </ul>
   * </li>
   * </ol>
   */
  private static boolean needsUnicodeEscape(char ch) {
    switch (Character.getType(ch)) {
      // Conservative
      case Character.COMBINING_SPACING_MARK:
      case Character.ENCLOSING_MARK:
      case Character.NON_SPACING_MARK:
      case Character.UNASSIGNED:
      case Character.PRIVATE_USE:
      case Character.SPACE_SEPARATOR:
      case Character.CONTROL:

      // Minimal
      case Character.LINE_SEPARATOR:
      case Character.FORMAT:
      case Character.PARAGRAPH_SEPARATOR:
      case Character.SURROGATE:
        return true;

      default:
        if (ch == NON_BREAKING_HYPHEN) {
          // This can be expanded into a break followed by a hyphen
          return true;
        }
        break;
    }

    return false;
  }

  /**
   * Writes either the two or four character escape sequence for a character.
   * 
   * 
   * @param ch character to unicode escape
   * @param charVector char vector to receive the unicode escaped representation
   */
  private static void unicodeEscape(char ch, CharVector charVector) {
    if (ch < 256) {
      charVector.add('x');
      charVector.add(NIBBLE_TO_HEX_CHAR[(ch >> 4) & 0x0F]);
      charVector.add(NIBBLE_TO_HEX_CHAR[ch & 0x0F]);
    } else {
      charVector.add('u');
      charVector.add(NIBBLE_TO_HEX_CHAR[(ch >> 12) & 0x0F]);
      charVector.add(NIBBLE_TO_HEX_CHAR[(ch >> 8) & 0x0F]);
      charVector.add(NIBBLE_TO_HEX_CHAR[(ch >> 4) & 0x0F]);
      charVector.add(NIBBLE_TO_HEX_CHAR[ch & 0x0F]);
    }
  }

  private int objectCount;

  private IdentityHashMap objectMap = new IdentityHashMap();

  private HashMap stringMap = new HashMap();

  private ArrayList stringTable = new ArrayList();

  private ArrayList tokenList = new ArrayList();

  private int tokenListCharCount;

  private final SerializationPolicy serializationPolicy;

  public ServerSerializationStreamWriter(
      SerializationPolicy serializationPolicy) {
    this.serializationPolicy = serializationPolicy;
  }

  public void prepareToWrite() {
    objectCount = 0;
    objectMap.clear();
    tokenList.clear();
    tokenListCharCount = 0;
    stringMap.clear();
    stringTable.clear();
  }

  public void serializeValue(Object value, Class type)
      throws SerializationException {
    if (type == boolean.class) {
      writeBoolean(((Boolean) value).booleanValue());
    } else if (type == byte.class) {
      writeByte(((Byte) value).byteValue());
    } else if (type == char.class) {
      writeChar(((Character) value).charValue());
    } else if (type == double.class) {
      writeDouble(((Double) value).doubleValue());
    } else if (type == float.class) {
      writeFloat(((Float) value).floatValue());
    } else if (type == int.class) {
      writeInt(((Integer) value).intValue());
    } else if (type == long.class) {
      writeLong(((Long) value).longValue());
    } else if (type == short.class) {
      writeShort(((Short) value).shortValue());
    } else if (type == String.class) {
      writeString((String) value);
    } else {
      writeObject(value);
    }
  }

  /**
   * Build an array of JavaScript string literals that can be decoded by the
   * client via the eval function.
   * 
   * NOTE: We build the array in reverse so the client can simply use the pop
   * function to remove the next item from the list.
   */
  public String toString() {
    // Build a JavaScript string (with escaping, of course).
    // We take a guess at how big to make to buffer to avoid numerous resizes.
    //
    int capacityGuess = 2 * tokenListCharCount + 2 * tokenList.size();
    StringBuffer buffer = new StringBuffer(capacityGuess);
    buffer.append("[");
    writePayload(buffer);
    writeStringTable(buffer);
    writeHeader(buffer);
    buffer.append("]");
    return buffer.toString();
  }

  protected int addString(String string) {
    if (string == null) {
      return 0;
    }
    Integer o = (Integer) stringMap.get(string);
    if (o != null) {
      return o.intValue();
    }
    stringTable.add(string);
    // index is 1-based
    int index = stringTable.size();
    stringMap.put(string, new Integer(index));
    return index;
  }

  protected void append(String token) {
    tokenList.add(token);
    if (token != null) {
      tokenListCharCount += token.length();
    }
  }

  protected int getIndexForObject(Object instance) {
    Integer o = (Integer) objectMap.get(instance);
    if (o != null) {
      return o.intValue();
    }
    return -1;
  }

  protected String getObjectTypeSignature(Object instance) {
    if (shouldEnforceTypeVersioning()) {
      return SerializabilityUtil.encodeSerializedInstanceReference(instance.getClass());
    } else {
      return SerializabilityUtil.getSerializedTypeName(instance.getClass());
    }
  }

  protected void saveIndexForObject(Object instance) {
    objectMap.put(instance, new Integer(objectCount++));
  }

  protected void serialize(Object instance, String typeSignature)
      throws SerializationException {
    assert (instance != null);

    Class clazz = instance.getClass();
    serializationPolicy.validateSerialize(clazz);

    serializeImpl(instance, clazz);
  }

  private void serializeClass(Object instance, Class instanceClass)
      throws SerializationException {
    assert (instance != null);

    Field[] declFields = instanceClass.getDeclaredFields();
    Field[] serializableFields = SerializabilityUtil.applyFieldSerializationPolicy(declFields);
    for (int index = 0; index < serializableFields.length; ++index) {
      Field declField = serializableFields[index];
      assert (declField != null);

      boolean isAccessible = declField.isAccessible();
      boolean needsAccessOverride = !isAccessible
          && !Modifier.isPublic(declField.getModifiers());
      if (needsAccessOverride) {
        // Override the access restrictions
        declField.setAccessible(true);
      }

      Object value;
      try {
        value = declField.get(instance);
        serializeValue(value, declField.getType());

      } catch (IllegalArgumentException e) {
        throw new SerializationException(e);

      } catch (IllegalAccessException e) {
        throw new SerializationException(e);
      }

      if (needsAccessOverride) {
        // Restore the access restrictions
        declField.setAccessible(isAccessible);
      }
    }

    Class superClass = instanceClass.getSuperclass();
    if (serializationPolicy.shouldSerializeFields(superClass)) {
      serializeImpl(instance, superClass);
    }
  }

  private void serializeImpl(Object instance, Class instanceClass)
      throws SerializationException {
    assert (instance != null);

    Class customSerializer = SerializabilityUtil.hasCustomFieldSerializer(instanceClass);
    if (customSerializer != null) {
      serializeWithCustomSerializer(customSerializer, instance, instanceClass);
    } else {
      // Arrays are serialized using custom serializers so we should never get
      // here for array types.
      //
      assert (!instanceClass.isArray());
      serializeClass(instance, instanceClass);
    }
  }

  private void serializeWithCustomSerializer(Class customSerializer,
      Object instance, Class instanceClass) throws SerializationException {

    Method serialize;
    try {
      if (instanceClass.isArray()) {
        Class componentType = instanceClass.getComponentType();
        if (!componentType.isPrimitive()) {
          instanceClass = Class.forName("[Ljava.lang.Object;");
        }
      }

      serialize = customSerializer.getMethod("serialize", new Class[] {
          SerializationStreamWriter.class, instanceClass});

      serialize.invoke(null, new Object[] {this, instance});

    } catch (SecurityException e) {
      throw new SerializationException(e);

    } catch (NoSuchMethodException e) {
      throw new SerializationException(e);

    } catch (IllegalArgumentException e) {
      throw new SerializationException(e);

    } catch (IllegalAccessException e) {
      throw new SerializationException(e);

    } catch (InvocationTargetException e) {
      throw new SerializationException(e);

    } catch (ClassNotFoundException e) {
      throw new SerializationException(e);
    }
  }

  /**
   * Notice that the field are written in reverse order that the client can just
   * pop items out of the stream.
   */
  private void writeHeader(StringBuffer buffer) {
    buffer.append(",");
    buffer.append(getFlags());
    buffer.append(",");
    buffer.append(getVersion());
  }

  private void writePayload(StringBuffer buffer) {
    for (int i = tokenList.size() - 1; i >= 0; --i) {
      String token = (String) tokenList.get(i);
      buffer.append(token);
      if (i > 0) {
        buffer.append(",");
      }
    }
  }

  private void writeStringTable(StringBuffer buffer) {
    if (tokenList.size() > 0) {
      buffer.append(",");
    }
    buffer.append("[");
    for (int i = 0, c = stringTable.size(); i < c; ++i) {
      if (i > 0) {
        buffer.append(",");
      }
      buffer.append(escapeString((String) stringTable.get(i)));
    }
    buffer.append("]");
  }
}
