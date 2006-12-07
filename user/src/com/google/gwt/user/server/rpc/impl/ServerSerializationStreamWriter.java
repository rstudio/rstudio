/*
 * Copyright 2006 Google Inc.
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

  static {
    JS_CHARS_ESCAPED['\u0000'] = '0';
    JS_CHARS_ESCAPED['\b'] = 'b';
    JS_CHARS_ESCAPED['\t'] = 't';
    JS_CHARS_ESCAPED['\n'] = 'n';
    // JavaScript Vertical Tab character '\v'
    JS_CHARS_ESCAPED['\u000b'] = 'v';
    JS_CHARS_ESCAPED['\f'] = 'f';
    JS_CHARS_ESCAPED['\r'] = 'r';
    JS_CHARS_ESCAPED[JS_ESCAPE_CHAR] = JS_ESCAPE_CHAR;
    JS_CHARS_ESCAPED[JS_QUOTE_CHAR] = JS_QUOTE_CHAR;
  }

  /**
   * This method takes a string and outputs a JavaScript string literal. The
   * data is surrounded with quotes, and any contained characters that need to
   * be escaped are mapped them onto their escape sequence.
   * 
   * Assumptions: We are targetting a version of JavaScript that that is later
   * than 1.3 that supports unicode strings. Therefore there is no need to
   * escape unicode characters.
   */
  private static String escapeString(String toEscape) {
    // make output big enough to escape every character (plus the quotes)
    char[] input = toEscape.toCharArray();
    char[] output = new char[input.length * 2 + 2];

    int j = 0;
    output[j] = JS_QUOTE_CHAR;

    for (int i = 0, n = input.length; i < n; ++i) {
      char c = input[i];
      if (c < NUMBER_OF_JS_ESCAPED_CHARS && JS_CHARS_ESCAPED[c] != 0) {
        output[++j] = JS_ESCAPE_CHAR;
        output[++j] = JS_CHARS_ESCAPED[c];
      } else {
        output[++j] = c;
      }
    }
    output[++j] = JS_QUOTE_CHAR;
    return String.valueOf(output, 0, ++j);
  }

  private int objectCount;

  private IdentityHashMap objectMap = new IdentityHashMap();

  private ServerSerializableTypeOracle serializableTypeOracle;

  private HashMap stringMap = new HashMap();

  private ArrayList stringTable = new ArrayList();

  private ArrayList tokenList = new ArrayList();

  private int tokenListCharCount;

  public ServerSerializationStreamWriter(
      ServerSerializableTypeOracle serializableTypeOracle) {
    this.serializableTypeOracle = serializableTypeOracle;
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
      return serializableTypeOracle.encodeSerializedInstanceReference(instance.getClass());
    } else {
      return serializableTypeOracle.getSerializedTypeName(instance.getClass());
    }
  }

  protected void saveIndexForObject(Object instance) {
    objectMap.put(instance, new Integer(objectCount++));
  }

  protected void serialize(Object instance, String typeSignature)
      throws SerializationException {
    serializeImpl(instance, instance.getClass());
  }

  private void serializeClass(Object instance, Class instanceClass)
      throws SerializationException {
    assert (instance != null);

    Field[] declFields = instanceClass.getDeclaredFields();
    Field[] serializableFields = serializableTypeOracle.applyFieldSerializationPolicy(declFields);
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
    if (superClass != null && serializableTypeOracle.isSerializable(superClass)) {
      serializeImpl(instance, superClass);
    }
  }

  private void serializeImpl(Object instance, Class instanceClass)
      throws SerializationException {

    assert (instance != null);

    Class customSerializer = serializableTypeOracle.hasCustomFieldSerializer(instanceClass);
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
    buffer.append(SERIALIZATION_STREAM_VERSION);
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
