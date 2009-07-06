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

import com.google.gwt.rpc.client.ast.CommandSink;
import com.google.gwt.rpc.client.ast.ValueCommand;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

/**
 * Contains base methods for implementing a SerializationStreamWriter.
 */
public abstract class CommandSerializationStreamWriterBase implements
    SerializationStreamWriter {

  private final CommandSink commandSink;

  protected CommandSerializationStreamWriterBase(CommandSink sink) {
    this.commandSink = sink;
  }

  public void writeBoolean(boolean value) throws SerializationException {
    commandSink.accept(makeValue(boolean.class, value));
  }

  public void writeByte(byte value) throws SerializationException {
    commandSink.accept(makeValue(byte.class, value));
  }

  public void writeChar(char value) throws SerializationException {
    commandSink.accept(makeValue(char.class, value));
  }

  public void writeDouble(double value) throws SerializationException {
    commandSink.accept(makeValue(double.class, value));
  }

  public void writeFloat(float value) throws SerializationException {
    commandSink.accept(makeValue(float.class, value));
  }

  public void writeInt(int value) throws SerializationException {
    commandSink.accept(makeValue(int.class, value));
  }

  public void writeLong(long value) throws SerializationException {
    commandSink.accept(makeValue(long.class, value));
  }

  public void writeObject(Object instance) throws SerializationException {
    commandSink.accept(makeValue(instance != null ? instance.getClass()
        : void.class, instance));
  }

  public void writeShort(short value) throws SerializationException {
    commandSink.accept(makeValue(short.class, value));
  }

  public void writeString(String value) throws SerializationException {
    commandSink.accept(makeValue(String.class, value));
  }

  protected final CommandSink getCommandSink() {
    return commandSink;
  }

  protected abstract ValueCommand makeValue(Class<?> type, Object value)
      throws SerializationException;

}