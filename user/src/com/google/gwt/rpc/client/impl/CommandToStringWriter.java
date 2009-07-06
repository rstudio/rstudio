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
import com.google.gwt.rpc.client.ast.ReturnCommand;
import com.google.gwt.rpc.client.ast.RpcCommand;
import com.google.gwt.rpc.client.ast.ValueCommand;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

/**
 * Existing code assumes that a SerializationStreamWriter will export its
 * payload via the toString() method. This uses an internal CommandSink and
 * creates a string payload on-demand.
 */
public class CommandToStringWriter implements SerializationStreamWriter {

  private static class ToStringCommandSink extends CommandSink {
    private final ReturnCommand retCommand = new ReturnCommand();

    @Override
    public void accept(RpcCommand command) throws SerializationException {
      retCommand.addValue((ValueCommand) command);
    }

    @Override
    public void finish() throws SerializationException {
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      SimplePayloadSink sink = new SimplePayloadSink(sb);
      try {
        sink.accept(retCommand);
        sink.finish();
      } catch (SerializationException e) {
        throw new RuntimeException("Unable to create payload", e);
      }
      return sb.toString();
    }
  }

  private final ToStringCommandSink commandSink = new ToStringCommandSink();
  private final SerializationStreamWriter writer;

  public CommandToStringWriter(TypeOverrides overrides) {
    writer = ClientWriterFactory.createWriter(overrides, commandSink);
  }

  @Override
  public String toString() {
    return commandSink.toString();
  }

  public void writeBoolean(boolean value) throws SerializationException {
    writer.writeBoolean(value);
  }

  public void writeByte(byte value) throws SerializationException {
    writer.writeByte(value);
  }

  public void writeChar(char value) throws SerializationException {
    writer.writeChar(value);
  }

  public void writeDouble(double value) throws SerializationException {
    writer.writeDouble(value);
  }

  public void writeFloat(float value) throws SerializationException {
    writer.writeFloat(value);
  }

  public void writeInt(int value) throws SerializationException {
    writer.writeInt(value);
  }

  public void writeLong(long value) throws SerializationException {
    writer.writeLong(value);
  }

  public void writeObject(Object value) throws SerializationException {
    writer.writeObject(value);
  }

  public void writeShort(short value) throws SerializationException {
    writer.writeShort(value);
  }

  public void writeString(String value) throws SerializationException {
    writer.writeString(value);
  }
}
