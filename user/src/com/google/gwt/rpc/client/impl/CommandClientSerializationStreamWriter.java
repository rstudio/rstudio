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
package com.google.gwt.rpc.client.impl;

import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.rpc.client.ast.ArrayValueCommand;
import com.google.gwt.rpc.client.ast.BooleanValueCommand;
import com.google.gwt.rpc.client.ast.CharValueCommand;
import com.google.gwt.rpc.client.ast.CommandSink;
import com.google.gwt.rpc.client.ast.DoubleValueCommand;
import com.google.gwt.rpc.client.ast.EnumValueCommand;
import com.google.gwt.rpc.client.ast.HasSetters;
import com.google.gwt.rpc.client.ast.IdentityValueCommand;
import com.google.gwt.rpc.client.ast.InstantiateCommand;
import com.google.gwt.rpc.client.ast.InvokeCustomFieldSerializerCommand;
import com.google.gwt.rpc.client.ast.LongValueCommand;
import com.google.gwt.rpc.client.ast.NullValueCommand;
import com.google.gwt.rpc.client.ast.StringValueCommand;
import com.google.gwt.rpc.client.ast.ValueCommand;
import com.google.gwt.rpc.client.impl.TypeOverrides.SerializeFunction;
import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.SerializationException;

import java.io.Serializable;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Provides a facade around serialization logic in client code.
 */
public class CommandClientSerializationStreamWriter extends
    CommandSerializationStreamWriterBase {

  private static Object anObject = new Object[] {};

  static {
    // Don't need to explicitly filter $H
    anObject.hashCode();
  }

  private final Map<Object, IdentityValueCommand> identityMap;
  private final TypeOverrides serializer;

  public CommandClientSerializationStreamWriter(TypeOverrides serializer,
      CommandSink sink) {
    this(serializer, sink, new IdentityHashMap<Object, IdentityValueCommand>());
  }

  private CommandClientSerializationStreamWriter(TypeOverrides serializer,
      CommandSink sink, Map<Object, IdentityValueCommand> identityMap) {
    super(sink);
    this.serializer = serializer;
    this.identityMap = identityMap;
  }

  /**
   * Type is passed in to handle primitive types.
   */
  @Override
  protected ValueCommand makeValue(Class<?> type, Object value)
      throws SerializationException {
    SerializeFunction customSerializer;
    ValueCommand toReturn;

    if (value == null) {
      toReturn = NullValueCommand.INSTANCE;
    } else if (type.isPrimitive()) {
      if (type == boolean.class) {
        toReturn = new BooleanValueCommand((Boolean) value);
      } else if (type == void.class) {
        toReturn = NullValueCommand.INSTANCE;
      } else if (type == long.class) {
        toReturn = new LongValueCommand((Long) value);
      } else if (type == char.class) {
        toReturn = new CharValueCommand((Character) value);
      } else {
        assert value instanceof Number : "Expecting Number; had "
            + value.getClass().getName();
        toReturn = new DoubleValueCommand(((Number) value).doubleValue());
      }

    } else if ((toReturn = identityMap.get(value)) != null) {
      // Fall through

    } else if (type == String.class) {
      toReturn = new StringValueCommand((String) value);

    } else if (type.isArray()) {
      ArrayValueCommand array = new ArrayValueCommand(type.getComponentType());
      identityMap.put(value, array);
      extractData(array, value);
      toReturn = array;

    } else if (value instanceof Enum<?>) {
      EnumValueCommand e = new EnumValueCommand();
      e.setValue((Enum<?>) value);
      toReturn = e;

    } else if ((customSerializer = serializer.getOverride(type.getName())) != null) {
      toReturn = invokeCustomSerializer(customSerializer, type, value);

    } else {
      toReturn = makeObject(type, value);
    }

    return toReturn;
  }

  private native void extractData(ArrayValueCommand x, Object obj) /*-{
    for (var i = 0, j = obj.length; i < j; i++) {
      var value = this.@com.google.gwt.rpc.client.impl.CommandClientSerializationStreamWriter::makeValue(Ljava/lang/Object;)(obj[i]);
      x.@com.google.gwt.rpc.client.ast.ArrayValueCommand::add(Lcom/google/gwt/rpc/client/ast/ValueCommand;)(value);
    }
  }-*/;

  private native void extractData(HasSetters x, Object obj) /*-{
    for (var key in obj) {
      // Ignore common properties
      if (key in @com.google.gwt.rpc.client.impl.CommandClientSerializationStreamWriter::anObject) {
        continue;
      }
      this.@com.google.gwt.rpc.client.impl.CommandClientSerializationStreamWriter::extractField(Lcom/google/gwt/rpc/client/ast/HasSetters;Ljava/lang/Object;Ljava/lang/String;)(x,obj,key);
    }
  }-*/;

  private native void extractField(HasSetters x, Object obj, String key) /*-{
    var command = this.@com.google.gwt.rpc.client.impl.CommandClientSerializationStreamWriter::makeValue(Ljava/lang/Object;)(obj[key]);

    // makeValue may return undefined
    command && x.@com.google.gwt.rpc.client.ast.HasSetters::set(Ljava/lang/Class;Ljava/lang/String;Lcom/google/gwt/rpc/client/ast/ValueCommand;)(null, key, command);
  }-*/;

  private ValueCommand invokeCustomSerializer(
      SerializeFunction serializeFunction, Class<?> type, Object value) {
    InvokeCustomFieldSerializerCommand command = new InvokeCustomFieldSerializerCommand(
        type, null, null);
    identityMap.put(value, command);

    /*
     * Pass the current identityMap into the new writer to allow circular
     * references through the graph emitted by the CFS.
     */
    CommandClientSerializationStreamWriter subWriter = new CommandClientSerializationStreamWriter(
        serializer, new HasValuesCommandSink(command), identityMap);

    serializeFunction.serialize(subWriter, value);
    if (serializer.hasExtraFields(type.getName())) {
      for (String extraField : serializer.getExtraFields(type.getName())) {
        if (extraField != null) {
          // Sometimes fields might be pruned
          extractField(command, value, extraField);
        }
      }
    }
    return command;
  }

  private ValueCommand makeObject(Class<?> clazz, Object value)
      throws SerializationException {
    if (!(value instanceof Serializable || value instanceof IsSerializable)) {
      throw new SerializationException(clazz.getName()
          + " is not a Serializable type");
    }
    InstantiateCommand x = new InstantiateCommand(clazz);
    identityMap.put(value, x);

    if (serializer.hasExtraFields(clazz.getName())) {
      // Objects with transient fields or non-trivial semantics
      for (String fieldName : serializer.getExtraFields(clazz.getName())) {
        extractField(x, value, fieldName);
      }
    } else {
      // Just a for-in loop
      extractData(x, value);
    }
    return x;
  }

  @UnsafeNativeLong
  private native ValueCommand makeValue(Object value) /*-{
    var type;
    if (value) {
      // Maybe turn objects into primitives
      value.valueOf && (value = value.valueOf());

      // See if the value is our web-mode representation of a long
      if (value.hasOwnProperty('l') && value.hasOwnProperty('m') && value.hasOwnProperty('h')) {
        type = 'long';
      }
    }
    type || (type = typeof value);

    switch (type) {
      case 'boolean':
        return @com.google.gwt.rpc.client.ast.BooleanValueCommand::new(Z)(value);

      case 'number':
        return @com.google.gwt.rpc.client.ast.DoubleValueCommand::new(D)(value);

      case 'string':
        return @com.google.gwt.rpc.client.ast.StringValueCommand::new(Ljava/lang/String;)(value);

      case 'long':
        return @com.google.gwt.rpc.client.ast.LongValueCommand::new(J)(value);

      case 'function':
        // Not serializable
        break;

      case 'object':
        // typeof null == 'object'
        if (!value) {
          return @com.google.gwt.rpc.client.ast.NullValueCommand::INSTANCE;
        }

        if (!value.@java.lang.Object::typeMarker) {
          // Not a Java object
          break;
        }

        return this.@com.google.gwt.rpc.client.impl.CommandClientSerializationStreamWriter::makeValue(Ljava/lang/Class;Ljava/lang/Object;)(value.@java.lang.Object::getClass()(), value);

      case 'undefined':
        // typeof undefined == 'undefined', but we treat it as null
        return @com.google.gwt.rpc.client.ast.NullValueCommand::INSTANCE;

      default:
        throw @java.lang.RuntimeException::new(Ljava/lang/String;)('Unknown type ' + type);
    }

    // Intentionally return undefined
  }-*/;
}
