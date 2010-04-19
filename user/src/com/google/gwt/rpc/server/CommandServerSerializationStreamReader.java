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
import com.google.gwt.rpc.client.ast.BooleanValueCommand;
import com.google.gwt.rpc.client.ast.ByteValueCommand;
import com.google.gwt.rpc.client.ast.CharValueCommand;
import com.google.gwt.rpc.client.ast.DoubleValueCommand;
import com.google.gwt.rpc.client.ast.EnumValueCommand;
import com.google.gwt.rpc.client.ast.FloatValueCommand;
import com.google.gwt.rpc.client.ast.IdentityValueCommand;
import com.google.gwt.rpc.client.ast.InstantiateCommand;
import com.google.gwt.rpc.client.ast.IntValueCommand;
import com.google.gwt.rpc.client.ast.InvokeCustomFieldSerializerCommand;
import com.google.gwt.rpc.client.ast.LongValueCommand;
import com.google.gwt.rpc.client.ast.NullValueCommand;
import com.google.gwt.rpc.client.ast.RpcCommandVisitor;
import com.google.gwt.rpc.client.ast.ScalarValueCommand;
import com.google.gwt.rpc.client.ast.SetCommand;
import com.google.gwt.rpc.client.ast.ShortValueCommand;
import com.google.gwt.rpc.client.ast.StringValueCommand;
import com.google.gwt.rpc.client.ast.ValueCommand;
import com.google.gwt.rpc.server.CommandSerializationUtil.Accessor;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * This class will use ValueCommands to reconstitute objects.
 */
public class CommandServerSerializationStreamReader implements
    SerializationStreamReader {

  class Visitor extends RpcCommandVisitor {

    @SuppressWarnings("hiding")
    private final Stack<Object> values = new Stack<Object>();

    @Override
    public void endVisit(BooleanValueCommand x, Context ctx) {
      pushScalar(x);
    }

    @Override
    public void endVisit(ByteValueCommand x, Context ctx) {
      pushScalar(x);
    }

    @Override
    public void endVisit(CharValueCommand x, Context ctx) {
      pushScalar(x);
    }

    @Override
    public void endVisit(DoubleValueCommand x, Context ctx) {
      pushScalar(x);
    }

    @Override
    public void endVisit(EnumValueCommand x, Context ctx) {
      push(x, x.getValue());
    }

    @Override
    public void endVisit(FloatValueCommand x, Context ctx) {
      pushScalar(x);
    }

    @Override
    public void endVisit(IntValueCommand x, Context ctx) {
      pushScalar(x);
    }

    @Override
    public void endVisit(LongValueCommand x, Context ctx) {
      pushScalar(x);
    }

    @Override
    public void endVisit(NullValueCommand x, Context ctx) {
      pushScalar(x);
    }

    @Override
    public void endVisit(SetCommand x, Context ctx) {
      Exception ex;
      try {
        Field f = x.getFieldDeclClass().getDeclaredField(x.getField());
        Object value = values.pop();
        Object instance = values.peek();
        assert value == null
            || CommandSerializationUtil.getAccessor(f.getType()).canSet(
                value.getClass()) : "Cannot assign a "
            + value.getClass().getName() + " into " + f.getType().getName();

        CommandSerializationUtil.getAccessor(f.getType()).set(instance, f,
            value);
        return;
      } catch (SecurityException e) {
        ex = e;
      } catch (NoSuchFieldException e) {
        ex = e;
      }
      halt(new SerializationException("Unable to set field value", ex));
    }

    @Override
    public void endVisit(ShortValueCommand x, Context ctx) {
      pushScalar(x);
    }

    @Override
    public void endVisit(StringValueCommand x, Context ctx) {
      pushScalar(x);
    }

    @Override
    public boolean visit(ArrayValueCommand x, Context ctx) {
      if (maybePushBackRef(x)) {
        Object array = Array.newInstance(x.getComponentType(),
            x.getComponentValues().size());
        push(x, array);

        int size = x.getComponentValues().size();
        Accessor a = CommandSerializationUtil.getAccessor(x.getComponentType());
        for (int i = 0; i < size; i++) {
          accept(x.getComponentValues().get(i));
          a.set(array, i, values.pop());
        }
      }

      return false;
    }

    @Override
    public boolean visit(InstantiateCommand x, Context ctx) {
      if (maybePushBackRef(x)) {
        Object instance;
        try {
          instance = CommandSerializationUtil.allocateInstance(x.getTargetClass());
          push(x, instance);
          return true;
        } catch (InstantiationException e) {
          halt(new SerializationException("Unable to create instance", e));
        }
      }

      return false;
    }

    @Override
    public boolean visit(InvokeCustomFieldSerializerCommand x, Context ctx) {
      if (maybePushBackRef(x)) {

        CommandServerSerializationStreamReader subReader = new CommandServerSerializationStreamReader(
            backRefs);
        subReader.prepareToRead(x.getValues());

        Class<?> serializerClass = x.getSerializerClass();
        assert serializerClass != null;

        Method instantiate = null;
        Method deserialize = null;
        for (Method m : serializerClass.getMethods()) {
          if ("instantiate".equals(m.getName())) {
            instantiate = m;
          } else if ("deserialize".equals(m.getName())) {
            deserialize = m;
          }

          if (instantiate != null && deserialize != null) {
            break;
          }
        }

        assert deserialize != null : "No deserialize method in "
            + serializerClass.getName();

        Object instance = null;
        if (instantiate != null) {
          assert Modifier.isStatic(instantiate.getModifiers()) : "instantiate method in "
              + serializerClass.getName() + " must be static";
          try {
            instance = instantiate.invoke(null, subReader);
          } catch (IllegalArgumentException e) {
            halt(new SerializationException("Unable to create instance", e));
          } catch (IllegalAccessException e) {
            halt(new SerializationException("Unable to create instance", e));
          } catch (InvocationTargetException e) {
            halt(new SerializationException("Unable to create instance", e));
          }
        } else {
          try {
            instance = x.getTargetClass().newInstance();
          } catch (InstantiationException e) {
            halt(new SerializationException("Unable to create instance", e));
          } catch (IllegalAccessException e) {
            halt(new SerializationException("Unable to create instance", e));
          }
        }

        assert instance != null : "Did not create instance";
        push(x, instance);

        // Process any additional fields
        accept(x.getSetters());

        try {
          deserialize.invoke(null, subReader, instance);
        } catch (IllegalArgumentException e) {
          halt(new SerializationException("Unable to deserialize instance", e));
        } catch (IllegalAccessException e) {
          halt(new SerializationException("Unable to deserialize instance", e));
        } catch (InvocationTargetException e) {
          halt(new SerializationException("Unable to deserialize instance", e));
        }
      }

      return false;
    }

    /**
     * Returns true if the command must be processed.
     */
    private boolean maybePushBackRef(IdentityValueCommand x) {
      Object instance = backRefs.get(x);
      if (instance == null) {
        return true;
      } else {
        values.push(instance);
        return false;
      }
    }

    private void push(IdentityValueCommand x, Object value) {
      assert !backRefs.containsKey(x) : "Trying to redefine a backref";
      backRefs.put(x, value);
      values.push(value);
    }

    private void pushScalar(ScalarValueCommand x) {
      values.push(x.getValue());
    }
  }

  final Map<IdentityValueCommand, Object> backRefs;
  Iterator<ValueCommand> values;

  public CommandServerSerializationStreamReader() {
    this(new HashMap<IdentityValueCommand, Object>());
  }

  private CommandServerSerializationStreamReader(
      Map<IdentityValueCommand, Object> backRefs) {
    this.backRefs = backRefs;
  }

  public void prepareToRead(List<ValueCommand> commands) {
    values = commands.iterator();
  }

  public boolean readBoolean() throws SerializationException {
    return readNumberCommand(BooleanValueCommand.class).getValue();
  }

  public byte readByte() throws SerializationException {
    return readNumberCommand(ByteValueCommand.class).getValue();
  }

  public char readChar() throws SerializationException {
    return readNumberCommand(CharValueCommand.class).getValue();
  }

  public double readDouble() throws SerializationException {
    return readNumberCommand(DoubleValueCommand.class).getValue();
  }

  public float readFloat() throws SerializationException {
    return readNumberCommand(FloatValueCommand.class).getValue();
  }

  public int readInt() throws SerializationException {
    return readNumberCommand(IntValueCommand.class).getValue();
  }

  public long readLong() throws SerializationException {
    return readNumberCommand(LongValueCommand.class).getValue();
  }

  public Object readObject() throws SerializationException {
    ValueCommand command = readNextCommand(ValueCommand.class);
    Visitor v = new Visitor();
    v.accept(command);
    return v.values.pop();
  }

  public short readShort() throws SerializationException {
    return readNumberCommand(ShortValueCommand.class).getValue();
  }

  public String readString() throws SerializationException {
    return (String) readObject();
  }

  private <T extends ValueCommand> T readNextCommand(Class<T> clazz)
      throws SerializationException {
    if (!values.hasNext()) {
      throw new SerializationException("Reached end of stream");
    }
    ValueCommand next = values.next();
    if (!clazz.isInstance(next)) {
      throw new SerializationException("Cannot assign "
          + next.getClass().getName() + " to " + clazz.getName());
    }
    return clazz.cast(next);
  }

  /**
   * Will perform narrowing conversions from double type to other numeric types.
   */
  private <T extends ValueCommand> T readNumberCommand(Class<T> clazz)
      throws SerializationException {
    if (!values.hasNext()) {
      throw new SerializationException("Reached end of stream");
    }
    ValueCommand next = values.next();

    if (clazz.isInstance(next)) {
      return clazz.cast(next);
    } else if (next instanceof LongValueCommand) {
      if (!clazz.isInstance(next)) {
        throw new SerializationException("Cannot assign "
            + next.getClass().getName() + " to " + clazz.getName());
      }
      return clazz.cast(next);
    } else if (next instanceof DoubleValueCommand) {
      Exception ex;
      try {
        Constructor<T> c = clazz.getConstructor(double.class);
        return c.newInstance(((DoubleValueCommand) next).getValue().doubleValue());
      } catch (SecurityException e) {
        throw new SerializationException("Cannot construct ValueCommand type",
            e);
      } catch (NoSuchMethodException e) {
        throw new SerializationException("Connot initialize a "
            + clazz.getName() + " from a DoubleValueCommand", e);
      } catch (IllegalArgumentException e) {
        ex = e;
      } catch (InstantiationException e) {
        ex = e;
      } catch (IllegalAccessException e) {
        ex = e;
      } catch (InvocationTargetException e) {
        ex = e;
      }
      throw new SerializationException("Cannot create ValueCommand", ex);
    } else {
      throw new SerializationException(
          "Cannot create a numeric ValueCommand from a "
              + next.getClass().getName());
    }
  }
}
