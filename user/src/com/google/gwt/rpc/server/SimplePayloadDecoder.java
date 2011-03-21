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
package com.google.gwt.rpc.server;

import static com.google.gwt.rpc.client.impl.SimplePayloadSink.ARRAY_TYPE;
import static com.google.gwt.rpc.client.impl.SimplePayloadSink.BACKREF_TYPE;
import static com.google.gwt.rpc.client.impl.SimplePayloadSink.BOOLEAN_TYPE;
import static com.google.gwt.rpc.client.impl.SimplePayloadSink.BYTE_TYPE;
import static com.google.gwt.rpc.client.impl.SimplePayloadSink.CHAR_TYPE;
import static com.google.gwt.rpc.client.impl.SimplePayloadSink.DOUBLE_TYPE;
import static com.google.gwt.rpc.client.impl.SimplePayloadSink.ENUM_TYPE;
import static com.google.gwt.rpc.client.impl.SimplePayloadSink.FLOAT_TYPE;
import static com.google.gwt.rpc.client.impl.SimplePayloadSink.INT_TYPE;
import static com.google.gwt.rpc.client.impl.SimplePayloadSink.INVOKE_TYPE;
import static com.google.gwt.rpc.client.impl.SimplePayloadSink.LONG_TYPE;
import static com.google.gwt.rpc.client.impl.SimplePayloadSink.NL_CHAR;
import static com.google.gwt.rpc.client.impl.SimplePayloadSink.OBJECT_TYPE;
import static com.google.gwt.rpc.client.impl.SimplePayloadSink.RETURN_TYPE;
import static com.google.gwt.rpc.client.impl.SimplePayloadSink.RPC_SEPARATOR_CHAR;
import static com.google.gwt.rpc.client.impl.SimplePayloadSink.SHORT_TYPE;
import static com.google.gwt.rpc.client.impl.SimplePayloadSink.STRING_TYPE;
import static com.google.gwt.rpc.client.impl.SimplePayloadSink.THROW_TYPE;
import static com.google.gwt.rpc.client.impl.SimplePayloadSink.VOID_TYPE;

import com.google.gwt.rpc.client.ast.ArrayValueCommand;
import com.google.gwt.rpc.client.ast.BooleanValueCommand;
import com.google.gwt.rpc.client.ast.ByteValueCommand;
import com.google.gwt.rpc.client.ast.CharValueCommand;
import com.google.gwt.rpc.client.ast.DoubleValueCommand;
import com.google.gwt.rpc.client.ast.EnumValueCommand;
import com.google.gwt.rpc.client.ast.FloatValueCommand;
import com.google.gwt.rpc.client.ast.HasSetters;
import com.google.gwt.rpc.client.ast.IdentityValueCommand;
import com.google.gwt.rpc.client.ast.InstantiateCommand;
import com.google.gwt.rpc.client.ast.IntValueCommand;
import com.google.gwt.rpc.client.ast.InvokeCustomFieldSerializerCommand;
import com.google.gwt.rpc.client.ast.LongValueCommand;
import com.google.gwt.rpc.client.ast.NullValueCommand;
import com.google.gwt.rpc.client.ast.ReturnCommand;
import com.google.gwt.rpc.client.ast.RpcCommand;
import com.google.gwt.rpc.client.ast.ScalarValueCommand;
import com.google.gwt.rpc.client.ast.ShortValueCommand;
import com.google.gwt.rpc.client.ast.StringValueCommand;
import com.google.gwt.rpc.client.ast.ValueCommand;
import com.google.gwt.user.server.rpc.impl.SerializabilityUtil;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Decodes the simple payload.
 */
public class SimplePayloadDecoder {
  private static final String OBFUSCATED_CLASS_PREFIX = "Class$ ";
  private static final Map<String, Class<?>> PRIMITIVE_TYPES = new HashMap<String, Class<?>>();

  static {
    // Obfuscated when class metadata is disabled
    PRIMITIVE_TYPES.put(OBFUSCATED_CLASS_PREFIX + BOOLEAN_TYPE, boolean.class);
    PRIMITIVE_TYPES.put(OBFUSCATED_CLASS_PREFIX + BYTE_TYPE, byte.class);
    PRIMITIVE_TYPES.put(OBFUSCATED_CLASS_PREFIX + CHAR_TYPE, char.class);
    PRIMITIVE_TYPES.put(OBFUSCATED_CLASS_PREFIX + DOUBLE_TYPE, double.class);
    PRIMITIVE_TYPES.put(OBFUSCATED_CLASS_PREFIX + FLOAT_TYPE, float.class);
    PRIMITIVE_TYPES.put(OBFUSCATED_CLASS_PREFIX + INT_TYPE, int.class);
    PRIMITIVE_TYPES.put(OBFUSCATED_CLASS_PREFIX + LONG_TYPE, long.class);
    PRIMITIVE_TYPES.put(OBFUSCATED_CLASS_PREFIX + SHORT_TYPE, short.class);
    PRIMITIVE_TYPES.put(OBFUSCATED_CLASS_PREFIX + VOID_TYPE, void.class);

    // Regular
    PRIMITIVE_TYPES.put(boolean.class.getName(), boolean.class);
    PRIMITIVE_TYPES.put(byte.class.getName(), byte.class);
    PRIMITIVE_TYPES.put(char.class.getName(), char.class);
    PRIMITIVE_TYPES.put(double.class.getName(), double.class);
    PRIMITIVE_TYPES.put(float.class.getName(), float.class);
    PRIMITIVE_TYPES.put(int.class.getName(), int.class);
    PRIMITIVE_TYPES.put(long.class.getName(), long.class);
    PRIMITIVE_TYPES.put(short.class.getName(), short.class);
    PRIMITIVE_TYPES.put(void.class.getName(), void.class);
  }

  private final Map<Integer, ValueCommand> backRefs = new HashMap<Integer, ValueCommand>();
  private final Map<String, Class<?>> classCache = new HashMap<String, Class<?>>(
      PRIMITIVE_TYPES);
  private final ClientOracle clientOracle;
  private final Stack<RpcCommand> commands = new Stack<RpcCommand>();
  private int idx;
  private final CharSequence payload;
  private ReturnCommand toReturn;
  private ValueCommand toThrow;

  /**
   * Construct a new SimplePayloadDecoder. This will consume the entire payload
   * which will be made available through {@link #getValues}. If the payload
   * stream contains an embedded exception, processing will end early and the
   * Throwable will be available via {@link #getThrownValue()}.
   * 
   * @throws ClassNotFoundException
   */
  public SimplePayloadDecoder(ClientOracle clientOracle, CharSequence payload)
      throws ClassNotFoundException {
    this.clientOracle = clientOracle;
    this.payload = payload;
    while (toReturn == null && idx < payload.length()) {
      decodeCommand();

      // We hit an error in the stream; stop now
      if (toThrow != null) {
        return;
      }
    }
  }

  /**
   * Returns the thrown value, if any.
   */
  public ValueCommand getThrownValue() {
    return toThrow;
  }

  /**
   * Returns the values encoded in the payload.
   */
  public List<ValueCommand> getValues() {
    return toReturn == null ? Collections.<ValueCommand> emptyList()
        : toReturn.getValues();
  }

  private void decodeCommand() throws ClassNotFoundException {
    char command = next();
    if (command == NL_CHAR) {
      // Pretty mode payload
      command = next();
    }
    String token = token();
    switch (command) {
      case BOOLEAN_TYPE: {
        push(new BooleanValueCommand(token.equals("1")));
        break;
      }
      case BYTE_TYPE: {
        push(new ByteValueCommand(Byte.valueOf(token)));
        break;
      }
      case CHAR_TYPE: {
        push(new CharValueCommand(Character.valueOf((char) Integer.valueOf(
            token).intValue())));
        break;
      }
      case DOUBLE_TYPE: {
        push(new DoubleValueCommand(Double.valueOf(token)));
        break;
      }
      case FLOAT_TYPE: {
        push(new FloatValueCommand(Float.valueOf(token)));
        break;
      }
      case INT_TYPE: {
        push(new IntValueCommand(Integer.valueOf(token)));
        break;
      }
      case LONG_TYPE: {
        push(new LongValueCommand(Long.valueOf(token)));
        break;
      }
      case VOID_TYPE: {
        push(NullValueCommand.INSTANCE);
        break;
      }
      case SHORT_TYPE: {
        push(new ShortValueCommand(Short.valueOf(token)));
        break;
      }
      case STRING_TYPE: {
        // "4~abcd
        int length = Integer.valueOf(token);
        String value = next(length);
        if (next() != RPC_SEPARATOR_CHAR) {
          throw new RuntimeException("Overran string");
        }
        push(new StringValueCommand(value));
        break;
      }
      case ENUM_TYPE: {
        // ETypeSeedName~IOrdinal~
        EnumValueCommand x = new EnumValueCommand();
        push(x);
        
        // use ordinal (and not name), since name might have been obfuscated
        int ordinal = readCommand(IntValueCommand.class).getValue();

        @SuppressWarnings("rawtypes")
        Class<? extends Enum> clazz = findClass(token).asSubclass(Enum.class);
        
        /*
         * TODO: Note this approach could be prone to subtle corruption or
         * an ArrayOutOfBoundsException if the client and server have drifted.
         */
        Enum<?> enumConstants[] = clazz.getEnumConstants();
        x.setValue(enumConstants[ordinal]);
        break;
      }
      case ARRAY_TYPE: {
        // Encoded as (leafType, dimensions, length, .... )
        Class<?> leaf = findClass(token);

        Integer numDims = readCommand(IntValueCommand.class).getValue();
        Class<?> clazz;
        if (numDims > 1) {
          int[] dims = new int[numDims - 1];
          clazz = Array.newInstance(leaf, dims).getClass();
        } else {
          clazz = leaf;
        }

        ArrayValueCommand x = new ArrayValueCommand(clazz);
        push(x);
        int length = readCommand(IntValueCommand.class).getValue();
        for (int i = 0; i < length; i++) {
          x.add(readCommand(ValueCommand.class));
        }
        break;
      }
      case OBJECT_TYPE: {
        // @TypeSeedName~3~... N-many setters ...
        Class<?> clazz = findClass(token);
        InstantiateCommand x = new InstantiateCommand(clazz);
        push(x);
        readSetters(clazz, x);
        break;
      }
      case INVOKE_TYPE: {
        // !TypeSeedName~Number of objects written by CFS~...CFS objects...~
        // Number of extra fields~...N-many setters...
        Class<?> clazz = findClass(token);
        Class<?> serializerClass = null;

        // The custom serializer type might be for a supertype
        Class<?> manualType = clazz;
        while (manualType != null) {
          serializerClass = SerializabilityUtil.hasCustomFieldSerializer(manualType);
          if (serializerClass != null) {
            break;
          }
          manualType = manualType.getSuperclass();
        }

        InvokeCustomFieldSerializerCommand x = new InvokeCustomFieldSerializerCommand(
            clazz, serializerClass, manualType);
        push(x);

        readFields(x);
        readSetters(clazz, x);
        break;
      }
      case RETURN_TYPE: {
        // R4~...values...
        toReturn = new ReturnCommand();
        int toRead = Integer.valueOf(token);
        for (int i = 0; i < toRead; i++) {
          toReturn.addValue(readCommand(ValueCommand.class));
        }
        break;
      }
      case THROW_TYPE: {
        // T...value...
        toThrow = readCommand(ValueCommand.class);
        break;
      }
      case BACKREF_TYPE: {
        // @backrefNumber~
        ValueCommand x = backRefs.get(Integer.valueOf(token));
        assert x != null : "Could not find backref";
        commands.push(x);
        break;
      }
      case RPC_SEPARATOR_CHAR: {
        /*
         * Not strictly necessary, but it makes an off-by-one easier to
         * distinguish.
         */
        throw new RuntimeException("Segmentation overrun at " + idx);
      }
      default:
        throw new RuntimeException("Unknown command " + command);
    }
  }

  /**
   * Uses the ClientOracle to decode a type name.
   */
  private Class<?> findClass(String token) throws ClassNotFoundException {
    /*
     * NB: This is the only method in SimplePayloadDecoder which would require
     * any real adaptation to be made to run in Production Mode.
     */

    Class<?> clazz = classCache.get(token);
    if (clazz != null) {
      return clazz;
    }

    String className = clientOracle.getTypeName(token);
    if (className == null) {
      // Probably a regular class name
      className = token;
    }

    if (className.contains("[]")) {
      // Array types are annoying to construct
      int firstIndex = -1;
      int j = -1;
      int dims = 0;
      while ((j = className.indexOf("[", j + 1)) != -1) {
        if (dims++ == 0) {
          firstIndex = j;
        }
      }
      Class<?> componentType = findClass(className.substring(0, firstIndex));
      assert componentType != null : "Could not determine component type with "
          + className.substring(0, firstIndex);
      clazz = Array.newInstance(componentType, new int[dims]).getClass();
    } else {
      // Ensure that we use the bridge classloader in CCL
      ClassLoader myCCL = getClass().getClassLoader();
      clazz = Class.forName(className, false, myCCL);
    }
    classCache.put(token, clazz);
    return clazz;
  }

  /**
   * Reads the next character in the input, possibly evaluating escape
   * sequences.
   */
  private char next() {
    char c = payload.charAt(idx++);

    if (c == '\\') {
      switch (payload.charAt(idx++)) {
        case '0':
          c = '\0';
          break;
        case '!':
          // Compatibility since we're using the legacy escaping code
          c = '|';
          break;
        case 'b':
          c = '\b';
          break;
        case 't':
          c = '\t';
          break;
        case 'n':
          c = '\n';
          break;
        case 'f':
          c = '\f';
          break;
        case 'r':
          c = '\r';
          break;
        case '\\':
          c = '\\';
          break;
        case '"':
          c = '"';
          break;
        case 'u':
          c = (char) Integer.parseInt(
              payload.subSequence(idx, idx += 4).toString(), 16);
          break;
        case 'x':
          c = (char) Integer.parseInt(
              payload.subSequence(idx, idx += 2).toString(), 16);
          break;
        default:
          throw new RuntimeException("Unhandled escape " + payload.charAt(idx));
      }
    }
    return c;
  }

  /**
   * Reads <code>count</code> many characters and returns them as a string.
   */
  private String next(int count) {
    StringBuilder sb = new StringBuilder();
    while (count-- > 0) {
      sb.append(next());
    }
    return sb.toString();
  }

  /**
   * Retains the object value and establishes a backreference.
   */
  private void push(IdentityValueCommand x) {
    commands.push(x);
    backRefs.put(backRefs.size(), x);
  }

  /**
   * Retains the scalar value, but does not establish a backreference.
   */
  private void push(ScalarValueCommand x) {
    commands.push(x);
  }

  /**
   * Retains the string value and establishes a backreference.
   */
  private void push(StringValueCommand x) {
    commands.push(x);
    backRefs.put(backRefs.size(), x);
  }

  /**
   * Read one command from the stream.
   * 
   * @param <T> the expected type of RpcCommand to read
   * @param clazz the expected type of RpcCommand to read
   * @throws ClassCastException if a command was successfully read, but could
   *           not be assigned to <code>clazz</code>
   */
  private <T extends RpcCommand> T readCommand(Class<T> clazz)
      throws ClassNotFoundException {
    decodeCommand();
    RpcCommand value = commands.pop();
    assert clazz.isInstance(value) : "Cannot assign a "
        + value.getClass().getName() + " to " + clazz.getName();
    return clazz.cast(value);
  }

  /**
   * Format is (int, value...).
   */
  private void readFields(InvokeCustomFieldSerializerCommand x)
      throws ClassNotFoundException {
    int length = readCommand(IntValueCommand.class).getValue();
    for (int i = 0; i < length; i++) {
      x.addValue(readCommand(ValueCommand.class));
    }
  }

  /**
   * Format is (fieldDeclClassName, fieldId, value). fieldDeclClassName may be
   * null.
   */
  private void readSetter(Class<?> clazz, HasSetters x)
      throws ClassNotFoundException {
    // Only used by Development Mode to handle shadowing
    if (!clientOracle.isScript()) {
      String fieldDeclClassName = readCommand(StringValueCommand.class).getValue();
      if (fieldDeclClassName != null) {
        clazz = findClass(fieldDeclClassName);
      }
    }
    String fieldId = readCommand(StringValueCommand.class).getValue();

    Pair<Class<?>, String> data = clientOracle.getFieldName(clazz, fieldId);
    Class<?> fieldDeclClass = data.getA();
    String fieldName = data.getB();
    ValueCommand value = readCommand(ValueCommand.class);
    x.set(fieldDeclClass, fieldName, value);
  }

  /**
   * Format is (int, setter...).
   */
  private void readSetters(Class<?> clazz, HasSetters x)
      throws ClassNotFoundException {
    int length = readCommand(IntValueCommand.class).getValue();
    for (int i = 0; i < length; i++) {
      readSetter(clazz, x);
    }
  }

  /**
   * Read through the next separator character.
   */
  private String token() {
    StringBuilder sb = new StringBuilder();
    char n = next();
    while (n != RPC_SEPARATOR_CHAR) {
      sb.append(n);
      n = next();
    }
    return sb.toString();
  }
}
