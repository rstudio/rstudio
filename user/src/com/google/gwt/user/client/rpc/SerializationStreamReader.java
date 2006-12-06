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
package com.google.gwt.user.client.rpc;

/**
 * An interface for reading values from a stream.
 */
public interface SerializationStreamReader {

  boolean readBoolean() throws SerializationException;

  byte readByte() throws SerializationException;

  char readChar() throws SerializationException;

  double readDouble() throws SerializationException;

  float readFloat() throws SerializationException;

  int readInt() throws SerializationException;

  long readLong() throws SerializationException;

  Object readObject() throws SerializationException;

  short readShort() throws SerializationException;

  String readString() throws SerializationException;

}
