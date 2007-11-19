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
package com.google.gwt.user.client.rpc;

/**
 * An interface for writing values into a stream.
 */
public interface SerializationStreamWriter {
  /**
   * Serializes the contents of this stream into a string.
   * 
   * @return a string that is the serialization of the contents of this stream
   */
  String toString();

  void writeBoolean(boolean value) throws SerializationException;

  void writeByte(byte value) throws SerializationException;

  void writeChar(char value) throws SerializationException;

  void writeDouble(double value) throws SerializationException;

  void writeFloat(float value) throws SerializationException;

  void writeInt(int value) throws SerializationException;

  void writeLong(long value) throws SerializationException;

  void writeObject(Object value) throws SerializationException;

  void writeShort(short value) throws SerializationException;

  void writeString(String value) throws SerializationException;
}
