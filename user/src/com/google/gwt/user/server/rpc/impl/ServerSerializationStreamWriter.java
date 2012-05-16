/*
 * Copyright 2008 Google Inc.
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

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.impl.AbstractSerializationStreamWriter;
import com.google.gwt.user.server.Base64Utils;
import com.google.gwt.user.server.rpc.SerializationPolicy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

/**
 * For internal use only. Used for server call serialization. This class is
 * carefully matched with the client-side version.
 */
public final class ServerSerializationStreamWriter extends
    AbstractSerializationStreamWriter {

  /**
   * Builds a string that evaluates into an array containing the given elements.
   * This class exists to work around a bug in IE6/7 that limits the size of
   * array literals.
   */
  public static class LengthConstrainedArray {
    public static final int MAXIMUM_ARRAY_LENGTH = 1 << 15;
    private static final String POSTLUDE = "])";
    private static final String PRELUDE = "].concat([";

    private final StringBuffer buffer;
    private int count = 0;
    private boolean needsComma = false;
    private int total = 0;

    public LengthConstrainedArray() {
      buffer = new StringBuffer();
    }

    public LengthConstrainedArray(int capacityGuess) {
      buffer = new StringBuffer(capacityGuess);
    }

    public void addToken(CharSequence token) {
      total++;
      if (count++ == MAXIMUM_ARRAY_LENGTH) {
        if (total == MAXIMUM_ARRAY_LENGTH + 1) {
          buffer.append(PRELUDE);
        } else {
          buffer.append("],[");
        }
        count = 0;
        needsComma = false;
      }

      if (needsComma) {
        buffer.append(",");
      } else {
        needsComma = true;
      }

      buffer.append(token);
    }

    public void addToken(int i) {
      addToken(String.valueOf(i));
    }

    @Override
    public String toString() {
      if (total > MAXIMUM_ARRAY_LENGTH) {
        return "[" + buffer.toString() + POSTLUDE;
      } else {
        return "[" + buffer.toString() + "]";
      }
    }
  }

  /**
   * Enumeration used to provided typed instance writers.
   */
  private enum ValueWriter {
    BOOLEAN {
      @Override
      void write(ServerSerializationStreamWriter stream, Object instance) {
        stream.writeBoolean(((Boolean) instance).booleanValue());
      }
    },
    BYTE {
      @Override
      void write(ServerSerializationStreamWriter stream, Object instance) {
        stream.writeByte(((Byte) instance).byteValue());
      }
    },
    CHAR {
      @Override
      void write(ServerSerializationStreamWriter stream, Object instance) {
        stream.writeChar(((Character) instance).charValue());
      }
    },
    DOUBLE {
      @Override
      void write(ServerSerializationStreamWriter stream, Object instance) {
        stream.writeDouble(((Double) instance).doubleValue());
      }
    },
    FLOAT {
      @Override
      void write(ServerSerializationStreamWriter stream, Object instance) {
        stream.writeFloat(((Float) instance).floatValue());
      }
    },
    INT {
      @Override
      void write(ServerSerializationStreamWriter stream, Object instance) {
        stream.writeInt(((Integer) instance).intValue());
      }
    },
    LONG {
      @Override
      void write(ServerSerializationStreamWriter stream, Object instance) {
        stream.writeLong(((Long) instance).longValue());
      }
    },
    OBJECT {
      @Override
      void write(ServerSerializationStreamWriter stream, Object instance)
          throws SerializationException {
        stream.writeObject(instance);
      }
    },
    SHORT {
      @Override
      void write(ServerSerializationStreamWriter stream, Object instance) {
        stream.writeShort(((Short) instance).shortValue());
      }
    },
    STRING {
      @Override
      void write(ServerSerializationStreamWriter stream, Object instance) {
        stream.writeString((String) instance);
      }
    };

    abstract void write(ServerSerializationStreamWriter stream, Object instance)
        throws SerializationException;
  }

  /**
   * Enumeration used to provided typed vector writers.
   */
  private enum VectorWriter {
    BOOLEAN_VECTOR {
      @Override
      void write(ServerSerializationStreamWriter stream, Object instance) {
        boolean[] vector = (boolean[]) instance;
        stream.writeInt(vector.length);
        for (int i = 0, n = vector.length; i < n; ++i) {
          stream.writeBoolean(vector[i]);
        }
      }
    },
    BYTE_VECTOR {
      @Override
      void write(ServerSerializationStreamWriter stream, Object instance) {
        byte[] vector = (byte[]) instance;
        stream.writeInt(vector.length);
        for (int i = 0, n = vector.length; i < n; ++i) {
          stream.writeByte(vector[i]);
        }
      }
    },
    CHAR_VECTOR {
      @Override
      void write(ServerSerializationStreamWriter stream, Object instance) {
        char[] vector = (char[]) instance;
        stream.writeInt(vector.length);
        for (int i = 0, n = vector.length; i < n; ++i) {
          stream.writeChar(vector[i]);
        }
      }
    },
    DOUBLE_VECTOR {
      @Override
      void write(ServerSerializationStreamWriter stream, Object instance) {
        double[] vector = (double[]) instance;
        stream.writeInt(vector.length);
        for (int i = 0, n = vector.length; i < n; ++i) {
          stream.writeDouble(vector[i]);
        }
      }
    },
    FLOAT_VECTOR {
      @Override
      void write(ServerSerializationStreamWriter stream, Object instance) {
        float[] vector = (float[]) instance;
        stream.writeInt(vector.length);
        for (int i = 0, n = vector.length; i < n; ++i) {
          stream.writeFloat(vector[i]);
        }
      }
    },
    INT_VECTOR {
      @Override
      void write(ServerSerializationStreamWriter stream, Object instance) {
        int[] vector = (int[]) instance;
        stream.writeInt(vector.length);
        for (int i = 0, n = vector.length; i < n; ++i) {
          stream.writeInt(vector[i]);
        }
      }
    },
    LONG_VECTOR {
      @Override
      void write(ServerSerializationStreamWriter stream, Object instance) {
        long[] vector = (long[]) instance;
        stream.writeInt(vector.length);
        for (int i = 0, n = vector.length; i < n; ++i) {
          stream.writeLong(vector[i]);
        }
      }
    },
    OBJECT_VECTOR {
      @Override
      void write(ServerSerializationStreamWriter stream, Object instance)
          throws SerializationException {
        Object[] vector = (Object[]) instance;
        stream.writeInt(vector.length);
        for (int i = 0, n = vector.length; i < n; ++i) {
          stream.writeObject(vector[i]);
        }
      }
    },
    SHORT_VECTOR {
      @Override
      void write(ServerSerializationStreamWriter stream, Object instance) {
        short[] vector = (short[]) instance;
        stream.writeInt(vector.length);
        for (int i = 0, n = vector.length; i < n; ++i) {
          stream.writeShort(vector[i]);
        }
      }
    },
    STRING_VECTOR {
      @Override
      void write(ServerSerializationStreamWriter stream, Object instance) {
        String[] vector = (String[]) instance;
        stream.writeInt(vector.length);
        for (int i = 0, n = vector.length; i < n; ++i) {
          stream.writeString(vector[i]);
        }
      }
    };

    abstract void write(ServerSerializationStreamWriter stream, Object instance)
        throws SerializationException;
  }

  /**
   * Map of {@link Class} objects to {@link ValueWriter}s.
   */
  private static final Map<Class<?>, ValueWriter> CLASS_TO_VALUE_WRITER = new IdentityHashMap<Class<?>, ValueWriter>();

  /**
   * Map of {@link Class} vector objects to {@link VectorWriter}s.
   */
  private static final Map<Class<?>, VectorWriter> CLASS_TO_VECTOR_WRITER = new IdentityHashMap<Class<?>, VectorWriter>();

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

  private static final char NON_BREAKING_HYPHEN = '\u2011';

  /**
   * Maximum length of a string node in RPC responses, not including surrounding
   * quote characters (2 ^ 16 - 1) = 65535.
   * This exists to work around a Rhino parser bug in the hosted mode client
   * that limits string node lengths to 64KB.
   */
  private static final int MAX_STRING_NODE_LENGTH = 0xFFFF;

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

    CLASS_TO_VECTOR_WRITER.put(boolean[].class, VectorWriter.BOOLEAN_VECTOR);
    CLASS_TO_VECTOR_WRITER.put(byte[].class, VectorWriter.BYTE_VECTOR);
    CLASS_TO_VECTOR_WRITER.put(char[].class, VectorWriter.CHAR_VECTOR);
    CLASS_TO_VECTOR_WRITER.put(double[].class, VectorWriter.DOUBLE_VECTOR);
    CLASS_TO_VECTOR_WRITER.put(float[].class, VectorWriter.FLOAT_VECTOR);
    CLASS_TO_VECTOR_WRITER.put(int[].class, VectorWriter.INT_VECTOR);
    CLASS_TO_VECTOR_WRITER.put(long[].class, VectorWriter.LONG_VECTOR);
    CLASS_TO_VECTOR_WRITER.put(Object[].class, VectorWriter.OBJECT_VECTOR);
    CLASS_TO_VECTOR_WRITER.put(short[].class, VectorWriter.SHORT_VECTOR);
    CLASS_TO_VECTOR_WRITER.put(String[].class, VectorWriter.STRING_VECTOR);

    CLASS_TO_VALUE_WRITER.put(boolean.class, ValueWriter.BOOLEAN);
    CLASS_TO_VALUE_WRITER.put(byte.class, ValueWriter.BYTE);
    CLASS_TO_VALUE_WRITER.put(char.class, ValueWriter.CHAR);
    CLASS_TO_VALUE_WRITER.put(double.class, ValueWriter.DOUBLE);
    CLASS_TO_VALUE_WRITER.put(float.class, ValueWriter.FLOAT);
    CLASS_TO_VALUE_WRITER.put(int.class, ValueWriter.INT);
    CLASS_TO_VALUE_WRITER.put(long.class, ValueWriter.LONG);
    CLASS_TO_VALUE_WRITER.put(Object.class, ValueWriter.OBJECT);
    CLASS_TO_VALUE_WRITER.put(short.class, ValueWriter.SHORT);
    CLASS_TO_VALUE_WRITER.put(String.class, ValueWriter.STRING);
  }

  /**
   * This method takes a string and outputs a JavaScript string literal. The
   * data is surrounded with quotes, and any contained characters that need to
   * be escaped are mapped onto their escape sequence.
   * 
   * Assumptions: We are targeting a version of JavaScript that that is later
   * than 1.3 that supports unicode strings.
   */
  public static String escapeString(String toEscape) {
    return escapeString(toEscape, false);
  }

  /**
   * This method takes a string and outputs a JavaScript string literal. The
   * data is surrounded with quotes, and any contained characters that need to
   * be escaped are mapped onto their escape sequence.
   *
   * This splits strings into 64KB chunks to workaround an issue with the hosted mode client where
   * the Rhino parser can't handle string nodes larger than 64KB, e.g. {@code "longstring"} is
   * converted to {@code "long" + "string"}.
   *
   * Assumptions: We are targeting a version of JavaScript that that is later
   * than 1.3 that supports unicode strings.
   */
  public static String escapeStringSplitNodes(String toEscape) {
    return escapeString(toEscape, true);
  }

  private static String escapeString(String toEscape, boolean splitNodes) {
    // Since escaped characters will increase the output size, allocate extra room to start.
    int length = toEscape.length();
    int capacityIncrement = Math.max(length, 16);
    CharVector charVector = new CharVector(capacityIncrement * 2, capacityIncrement);

    charVector.add(JS_QUOTE_CHAR);

    int i = 0;
    while (i < length) {

      // Add one segment at a time, up to maxNodeLength characters. Note this always leave room
      // for at least 6 characters at the end (maximum unicode escaped character size).
      int maxSegmentVectorSize = splitNodes
          ? (charVector.getSize() + MAX_STRING_NODE_LENGTH - 5)
          : Integer.MAX_VALUE;

      while (i < length && charVector.getSize() < maxSegmentVectorSize) {
        char c = toEscape.charAt(i++);
        if (needsUnicodeEscape(c)) {
          unicodeEscape(c, charVector);
        } else {
          charVector.add(c);
        }
      }

      // If there's another segment left, insert a '+' operator.
      if (splitNodes && i < length) {
        charVector.add(JS_QUOTE_CHAR);
        charVector.add('+');
        charVector.add(JS_QUOTE_CHAR);
      }
    }

    charVector.add(JS_QUOTE_CHAR);
    return String.valueOf(charVector.asArray(), 0, charVector.getSize());
  }

  /**
   * Returns the {@link Class} instance to use for serialization. Enumerations
   * are serialized as their declaring class while all others are serialized
   * using their true class instance.
   */
  private static Class<?> getClassForSerialization(Object instance) {
    assert (instance != null);

    if (instance instanceof Enum<?>) {
      Enum<?> e = (Enum<?>) instance;
      return e.getDeclaringClass();
    } else {
      return instance.getClass();
    }
  }

  /**
   * Returns <code>true</code> if the character requires the \\uXXXX unicode
   * character escape sequence. This is necessary if the raw character could be
   * consumed and/or interpreted as a special character when the JSON encoded
   * response is evaluated. For example, 0x2028 and 0x2029 are alternate line
   * endings for JS per ECMA-232, which are respected by Firefox and Mozilla.
   * <p>
   * Notes:
   * <ol>
   * <li>The following cases are a more conservative set of cases which are are
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
   * <li>The following cases are the minimal amount of escaping required to
   * prevent test failure.
   * <ul>
   * <li>LINE_SEPARATOR - 1</li>
   * <li>PARAGRAPH_SEPARATOR - 1</li>
   * <li>FORMAT - 32</li>
   * <li>SURROGATE - 2048</li>
   * <li>Total Characters Escaped: 2082</li></li>
   * </ul> </li>
   * </ol>
   * 
   * @param ch character to check
   * @return <code>true</code> if the character requires the \\uXXXX unicode
   *         character escape
   */
  private static boolean needsUnicodeEscape(char ch) {
    switch (ch) {
      case ' ':
        // ASCII space gets caught in SPACE_SEPARATOR below, but does not
        // need to be escaped
        return false;
      case JS_QUOTE_CHAR:
      case JS_ESCAPE_CHAR:
        // these must be quoted or they will break the protocol
        return true;
      case NON_BREAKING_HYPHEN:
        // This can be expanded into a break followed by a hyphen
        return true;
      default:
        if (ch < ' ') {
          // Chrome 11 mangles control characters
          return true;
        }
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
            break;
        }
        break;
    }
    return false;
  }

  /**
   * Writes a safe escape sequence for a character. Some characters have a short
   * form, such as \n for U+000D, while others are represented as \\xNN or
   * \\uNNNN.
   * 
   * @param ch character to unicode escape
   * @param charVector char vector to receive the unicode escaped representation
   */
  private static void unicodeEscape(char ch, CharVector charVector) {
    charVector.add(JS_ESCAPE_CHAR);
    if (ch < NUMBER_OF_JS_ESCAPED_CHARS && JS_CHARS_ESCAPED[ch] != 0) {
      charVector.add(JS_CHARS_ESCAPED[ch]);
    } else if (ch < 256) {
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

  private final SerializationPolicy serializationPolicy;

  private ArrayList<String> tokenList = new ArrayList<String>();

  private int tokenListCharCount;

  public ServerSerializationStreamWriter(SerializationPolicy serializationPolicy) {
    this.serializationPolicy = serializationPolicy;
  }

  @Override
  public void prepareToWrite() {
    super.prepareToWrite();
    tokenList.clear();
    tokenListCharCount = 0;
  }

  public void serializeValue(Object value, Class<?> type)
      throws SerializationException {
    ValueWriter valueWriter = CLASS_TO_VALUE_WRITER.get(type);
    if (valueWriter != null) {
      valueWriter.write(this, value);
    } else {
      // Arrays of primitive or reference types need to go through writeObject.
      ValueWriter.OBJECT.write(this, value);
    }
  }

  /**
   * Build an array of JavaScript string literals that can be decoded by the
   * client via the eval function.
   * 
   * NOTE: We build the array in reverse so the client can simply use the pop
   * function to remove the next item from the list.
   */
  @Override
  public String toString() {
    // Build a JavaScript string (with escaping, of course).
    // We take a guess at how big to make to buffer to avoid numerous resizes.
    //
    int capacityGuess = 2 * tokenListCharCount + 2 * tokenList.size();
    LengthConstrainedArray stream = new LengthConstrainedArray(capacityGuess);
    writePayload(stream);
    writeStringTable(stream);
    writeHeader(stream);

    return stream.toString();
  }
  
  @Override
  public void writeLong(long value) {
    if (getVersion() == SERIALIZATION_STREAM_MIN_VERSION) {
      // Write longs as a pair of doubles for backwards compatibility
      double[] parts = getAsDoubleArray(value);
      assert parts != null && parts.length == 2;
      writeDouble(parts[0]);
      writeDouble(parts[1]);
    } else {
      StringBuilder sb = new StringBuilder();
      sb.append('\'');
      sb.append(Base64Utils.toBase64(value));
      sb.append('\'');
      append(sb.toString());
    }
  }

  @Override
  protected void append(String token) {
    tokenList.add(token);
    if (token != null) {
      tokenListCharCount += token.length();
    }
  }

  @Override
  protected String getObjectTypeSignature(Object instance)
      throws SerializationException {
    assert (instance != null);

    Class<?> clazz = getClassForSerialization(instance);
    if (hasFlags(FLAG_ELIDE_TYPE_NAMES)) {
      if (serializationPolicy instanceof TypeNameObfuscator) {
        return ((TypeNameObfuscator) serializationPolicy).getTypeIdForClass(clazz);
      }

      throw new SerializationException("The GWT module was compiled with RPC "
          + "type name elision enabled, but "
          + serializationPolicy.getClass().getName() + " does not implement "
          + TypeNameObfuscator.class.getName());
    } else {
      return SerializabilityUtil.encodeSerializedInstanceReference(clazz, serializationPolicy);
    }
  }

  @Override
  protected void serialize(Object instance, String typeSignature)
      throws SerializationException {
    assert (instance != null);

    Class<?> clazz = getClassForSerialization(instance);

    try {
      serializationPolicy.validateSerialize(clazz);
    } catch (SerializationException e) {
      throw new SerializationException(e.getMessage() + ": instance = " + instance);
    }
    serializeImpl(instance, clazz);
  }

  /**
   * Serialize an instance that is an array. Will default to serializing the
   * instance as an Object vector if the instance is not a vector of primitives,
   * Strings or Object.
   * 
   * @param instanceClass
   * @param instance
   * @throws SerializationException
   */
  private void serializeArray(Class<?> instanceClass, Object instance)
      throws SerializationException {
    assert (instanceClass.isArray());

    VectorWriter instanceWriter = CLASS_TO_VECTOR_WRITER.get(instanceClass);
    if (instanceWriter != null) {
      instanceWriter.write(this, instance);
    } else {
      VectorWriter.OBJECT_VECTOR.write(this, instance);
    }
  }

  private void serializeClass(Object instance, Class<?> instanceClass)
      throws SerializationException {
    assert (instance != null);
    Field[] serializableFields = SerializabilityUtil.applyFieldSerializationPolicy(instanceClass);

    /**
     * If clientFieldNames is non-null, identify any additional server-only fields and serialize
     * them separately.  Java serialization is used to construct a byte array, which is encoded
     * as a String and written prior to the rest of the field data.
     */
    Set<String> clientFieldNames = serializationPolicy.getClientFieldNamesForEnhancedClass(instanceClass);
    if (clientFieldNames != null) {
      List<Field> serverFields = new ArrayList<Field>();
      for (Field declField : serializableFields) {
        assert (declField != null);
        
        // Identify server-only fields
        if (!clientFieldNames.contains(declField.getName())) {
          serverFields.add(declField);
          continue;
        }
      }
      
      // Serialize the server-only fields into a byte array and encode as a String
      try {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeInt(serverFields.size());
        for (Field f : serverFields) {
          oos.writeObject(f.getName());
          f.setAccessible(true);
          Object fieldData = f.get(instance);
          oos.writeObject(fieldData);
        }
        oos.close();

        byte[] serializedData = baos.toByteArray();
        String encodedData = Base64Utils.toBase64(serializedData);
        writeString(encodedData);
      } catch (IllegalAccessException e) {
        throw new SerializationException(e);
      } catch (IOException e) {
        throw new SerializationException(e);
      }
    }
    
    // Write the client-visible field data
    for (Field declField : serializableFields) {
      if ((clientFieldNames != null) && !clientFieldNames.contains(declField.getName())) {
        // Skip server-only fields
        continue;
      }

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
    }

    Class<?> superClass = instanceClass.getSuperclass();
    if (serializationPolicy.shouldSerializeFields(superClass)) {
      serializeImpl(instance, superClass);
    }
  }

  private void serializeImpl(Object instance, Class<?> instanceClass)
      throws SerializationException {
    assert (instance != null);

    Class<?> customSerializer = SerializabilityUtil.hasCustomFieldSerializer(instanceClass);
    if (customSerializer != null) {
      // Use custom field serializer
      @SuppressWarnings("unchecked")
      CustomFieldSerializer<Object> customFieldSerializer =
          (CustomFieldSerializer<Object>)
              SerializabilityUtil.loadCustomFieldSerializer(customSerializer);
      if (customFieldSerializer == null) {
        serializeWithCustomSerializer(customSerializer, instance,
            instanceClass);
      } else {
        customFieldSerializer.serializeInstance(this, instance);
      }
    } else if (instanceClass.isArray()) {
      serializeArray(instanceClass, instance);
    } else if (instanceClass.isEnum()) {
      writeInt(((Enum<?>) instance).ordinal());
    } else {
      // Regular class instance
      serializeClass(instance, instanceClass);
    }
  }

  private void serializeWithCustomSerializer(Class<?> customSerializer,
      Object instance, Class<?> instanceClass) throws SerializationException {

    try {
      assert (!instanceClass.isArray());

      for (Method method : customSerializer.getMethods()) {
        if ("serialize".equals(method.getName())) {
          method.invoke(null, this, instance);
          return;
        }
      }
      throw new NoSuchMethodException("serialize");
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
    }
  }

  /**
   * Notice that the field are written in reverse order that the client can just
   * pop items out of the stream.
   */
  private void writeHeader(LengthConstrainedArray stream) {
    stream.addToken(getFlags());
    stream.addToken(getVersion());
  }

  private void writePayload(LengthConstrainedArray stream) {
    ListIterator<String> tokenIterator = tokenList.listIterator(tokenList.size());
    while (tokenIterator.hasPrevious()) {
      stream.addToken(tokenIterator.previous());
    }
  }

  private void writeStringTable(LengthConstrainedArray stream) {
    LengthConstrainedArray tableStream = new LengthConstrainedArray();
    for (String s : getStringTable()) {
      tableStream.addToken(escapeStringSplitNodes(s));
    }
    stream.addToken(tableStream.toString());
  }
}
