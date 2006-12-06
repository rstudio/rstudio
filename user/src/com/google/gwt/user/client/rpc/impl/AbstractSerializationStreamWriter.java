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
package com.google.gwt.user.client.rpc.impl;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

/**
 * Base class for the client and server serialization streams. This class
 * handles the basic serialization and desirialization formatting for primitive
 * types since these are common between the client and the server.
 */
public abstract class AbstractSerializationStreamWriter extends
    AbstractSerializationStream implements SerializationStreamWriter {

  public abstract String toString();

  public void writeBoolean(boolean fieldValue) {
    append(fieldValue ? "1" : "0");
  }

  public void writeByte(byte fieldValue) {
    append(String.valueOf(fieldValue));
  }

  public void writeChar(char ch) {
    // just use an int, it's more foolproof
    append(String.valueOf((int) ch));
  }

  public void writeDouble(double fieldValue) {
    append(String.valueOf(fieldValue));
  }

  public void writeFloat(float fieldValue) {
    append(String.valueOf(fieldValue));
  }

  public void writeInt(int fieldValue) {
    append(String.valueOf(fieldValue));
  }

  public void writeLong(long fieldValue) {
    append(String.valueOf(fieldValue));
  }

  public void writeObject(Object instance) throws SerializationException {
    if (instance == null) {
      // write a null string
      writeString(null);
      return;
    }

    int objIndex = getIndexForObject(instance);
    if (objIndex >= 0) {
      // We've already encoded this object, make a backref
      // Transform 0-based to negative 1-based
      writeInt(-(objIndex + 1));
      return;
    }

    saveIndexForObject(instance);

    // Serialize the type signature
    String typeSignature = getObjectTypeSignature(instance);
    writeString(typeSignature);
    // Now serialize the rest of the object
    serialize(instance, typeSignature);
  }

  public void writeShort(short value) {
    append(String.valueOf(value));
  }

  public void writeString(String value) {
    writeInt(addString(value));
  }

  /**
   * Add a string to the string table and return its index.
   * 
   * @param string the string to add
   * @return the index to the string
   */
  protected abstract int addString(String string);

  /**
   * Append a token to the underlying output buffer.
   * 
   * @param token the token to append
   */
  protected abstract void append(String token);

  /**
   * Get the index for an object that may have previously been saved via
   * {@link #saveIndexForObject(Object)}.
   * 
   * @param instance the object to save
   * @return the index associated with this object, or -1 if this object hasn't
   *         been seen before
   */
  protected abstract int getIndexForObject(Object instance);

  /**
   * Compute and return the type signature for an object.
   * 
   * @param instance the instance to inspect
   * @return the type signature of the instance
   */
  protected abstract String getObjectTypeSignature(Object instance);

  /**
   * Remember this object as having been seen before.
   * 
   * @param instance the object to remember
   */
  protected abstract void saveIndexForObject(Object instance);

  /**
   * Serialize an object into the stream.
   * 
   * @param instance the object to serialize
   * @param typeSignature the type signature of the object
   * @throws SerializationException
   */
  protected abstract void serialize(Object instance, String typeSignature)
      throws SerializationException;

}
