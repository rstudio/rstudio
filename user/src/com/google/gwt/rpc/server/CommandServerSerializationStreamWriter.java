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

import com.google.gwt.rpc.client.ast.ArrayValueCommand;
import com.google.gwt.rpc.client.ast.CommandSink;
import com.google.gwt.rpc.client.ast.EnumValueCommand;
import com.google.gwt.rpc.client.ast.HasSetters;
import com.google.gwt.rpc.client.ast.IdentityValueCommand;
import com.google.gwt.rpc.client.ast.InstantiateCommand;
import com.google.gwt.rpc.client.ast.InvokeCustomFieldSerializerCommand;
import com.google.gwt.rpc.client.ast.NullValueCommand;
import com.google.gwt.rpc.client.ast.ValueCommand;
import com.google.gwt.rpc.client.impl.CommandSerializationStreamWriterBase;
import com.google.gwt.rpc.client.impl.HasValuesCommandSink;
import com.google.gwt.rpc.server.CommandSerializationUtil.Accessor;
import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.impl.SerializabilityUtil;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * A server-side implementation of SerializationStreamWriter that creates a
 * command stream.
 */
public class CommandServerSerializationStreamWriter extends
    CommandSerializationStreamWriterBase {

  private final ClientOracle clientOracle;
  private final Map<Object, IdentityValueCommand> identityMap;

  public CommandServerSerializationStreamWriter(CommandSink sink) {
    this(new HostedModeClientOracle(), sink);
  }

  public CommandServerSerializationStreamWriter(ClientOracle oracle,
      CommandSink sink) {
    this(oracle, sink, new IdentityHashMap<Object, IdentityValueCommand>());
  }

  private CommandServerSerializationStreamWriter(ClientOracle oracle,
      CommandSink sink, Map<Object, IdentityValueCommand> identityMap) {
    super(sink);
    this.clientOracle = oracle;
    this.identityMap = identityMap;
  }

  /**
   * Type is passed in to handle primitive types.
   */
  @Override
  protected ValueCommand makeValue(Class<?> type, Object value)
      throws SerializationException {
    if (value == null) {
      return NullValueCommand.INSTANCE;
    }

    /*
     * Check accessor map before the identity map because we don't want to
     * recurse on wrapped primitive values.
     */
    Accessor accessor;
    if ((accessor = CommandSerializationUtil.getAccessor(type)).canMakeValueCommand()) {
      return accessor.makeValueCommand(value);

    } else if (identityMap.containsKey(value)) {
      return identityMap.get(value);

    } else if (type.isArray()) {
      return makeArray(type, value);

    } else if (Enum.class.isAssignableFrom(type)) {
      return makeEnum(value);

    } else {
      return makeObject(type, value);
    }
  }

  private ArrayValueCommand makeArray(Class<?> type, Object value)
      throws SerializationException {
    ArrayValueCommand toReturn = new ArrayValueCommand(type.getComponentType());
    identityMap.put(value, toReturn);
    for (int i = 0, j = Array.getLength(value); i < j; i++) {
      Object arrayValue = Array.get(value, i);
      if (arrayValue == null) {
        toReturn.add(NullValueCommand.INSTANCE);
      } else {
        Class<? extends Object> valueType = type.getComponentType().isPrimitive()
            ? type.getComponentType() : arrayValue.getClass();
        toReturn.add(makeValue(valueType, arrayValue));
      }
    }
    return toReturn;
  }

  private ValueCommand makeEnum(Object value) {
    EnumValueCommand toReturn = new EnumValueCommand();
    toReturn.setValue((Enum<?>) value);
    return toReturn;
  }

  /*
   * TODO: Profiling shows that the reflection and conditional logic in this
   * method is a hotspot. This could be remedied by generating synthetic
   * InstantiateCommand types that initialize themselves.
   */
  private IdentityValueCommand makeObject(Class<?> type, Object value)
      throws SerializationException {

    if (type.isAnonymousClass() || type.isLocalClass()) {
      throw new SerializationException(
          "Cannot serialize anonymous or local classes");
    }

    Class<?> manualType = type;
    Class<?> customSerializer;
    do {
      customSerializer = SerializabilityUtil.hasCustomFieldSerializer(manualType);
      if (customSerializer != null) {
        break;
      }
      manualType = manualType.getSuperclass();
    } while (manualType != null);

    IdentityValueCommand ins;
    if (customSerializer != null) {
      ins = serializeWithCustomSerializer(customSerializer, value, type,
          manualType);
    } else {
      ins = new InstantiateCommand(type);
      identityMap.put(value, ins);
    }

    /*
     * If we're looking at a subclass of a manually-serialized type, the
     * subclass must be tagged as serializable in order to qualify for
     * serialization.
     */
    if (type != manualType) {
      if (!Serializable.class.isAssignableFrom(type)
          && !IsSerializable.class.isAssignableFrom(type)) {
        throw new SerializationException(type.getName()
            + " is not a serializable type");
      }
    }

    while (type != manualType) {
      Field[] serializableFields = clientOracle.getOperableFields(type);
      for (Field declField : serializableFields) {
        assert (declField != null);

        Accessor accessor = CommandSerializationUtil.getAccessor(declField.getType());
        ValueCommand valueCommand;
        Object fieldValue = accessor.get(value, declField);
        if (fieldValue == null) {
          valueCommand = NullValueCommand.INSTANCE;
        } else {
          Class<? extends Object> fieldType = declField.getType().isPrimitive()
              ? declField.getType() : fieldValue.getClass();
          valueCommand = makeValue(fieldType, fieldValue);
        }

        ((HasSetters) ins).set(declField.getDeclaringClass(),
            declField.getName(), valueCommand);
      }
      type = type.getSuperclass();
    }
    return ins;
  }

  private InvokeCustomFieldSerializerCommand serializeWithCustomSerializer(
      Class<?> customSerializer, Object instance, Class<?> instanceClass,
      Class<?> manuallySerializedType) throws SerializationException {
    assert !instanceClass.isArray();

    Exception ex;
    try {
      /*
       * NB: Class.getMethod() wants exact formal types. It may be the case that
       * the custom serializer uses looser type bounds in its method
       * declarations.
       */
      for (Method method : customSerializer.getMethods()) {
        if ("serialize".equals(method.getName())) {
          assert Modifier.isStatic(method.getModifiers()) : "serialize method "
              + "in type " + customSerializer.getName() + " must be static";

          final InvokeCustomFieldSerializerCommand toReturn = new InvokeCustomFieldSerializerCommand(
              instanceClass, customSerializer, manuallySerializedType);
          identityMap.put(instance, toReturn);

          /*
           * Pass the current identityMap into the new writer to allow circular
           * references through the graph emitted by the CFS.
           */
          CommandServerSerializationStreamWriter subWriter = new CommandServerSerializationStreamWriter(
              clientOracle, new HasValuesCommandSink(toReturn), identityMap);
          method.invoke(null, subWriter, instance);

          return toReturn;
        }
      }

      throw new NoSuchMethodException(
          "Could not find serialize method in custom serializer "
              + customSerializer.getName());

    } catch (SecurityException e) {
      ex = e;
    } catch (NoSuchMethodException e) {
      ex = e;
    } catch (IllegalArgumentException e) {
      ex = e;
    } catch (IllegalAccessException e) {
      ex = e;
    } catch (InvocationTargetException e) {
      ex = e;
    }

    throw new SerializationException(ex);
  }
}
