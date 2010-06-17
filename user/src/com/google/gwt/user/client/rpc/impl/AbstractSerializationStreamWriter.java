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
package com.google.gwt.user.client.rpc.impl;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for the client and server serialization streams. This class
 * handles the basic serialization and deserialization formatting for primitive
 * types since these are common between the client and the server. It also
 * handles Object- and String-tracking for building graph references.
 */
public abstract class AbstractSerializationStreamWriter extends
    AbstractSerializationStream implements SerializationStreamWriter {

  private static final double TWO_PWR_16_DBL = 0x10000;
  private static final double TWO_PWR_32_DBL = TWO_PWR_16_DBL * TWO_PWR_16_DBL;

  /**
   * Return a pair of doubles { low, high } that add up to the given number,
   * such that "low" is always between 0 and 2^32-1 inclusive and "high" is
   * always between -2^63 and 2^63-2^32 inclusive and is a multiple of 2^32.
   */
  public static double[] getAsDoubleArray(long value) {
    int lowBits = (int) (value & 0xffffffff);
    int highBits = (int) (value >> 32);
    return makeLongComponents(lowBits, highBits);
  }
  
  // Equivalent to getAsDoubleArray((long) highBits << 32 | lowBits);
  protected static double[] makeLongComponents(int lowBits, int highBits) {
    double high = highBits * TWO_PWR_32_DBL;
    double low = lowBits;
    if (lowBits < 0) {
      low += TWO_PWR_32_DBL;
    }
    return new double[] {low, high};
  }

  private int objectCount;

  private Map<Object, Integer> objectMap = new IdentityHashMap<Object, Integer>();

  private Map<String, Integer> stringMap = new HashMap<String, Integer>();

  private List<String> stringTable = new ArrayList<String>();

  public void prepareToWrite() {
    objectCount = 0;
    objectMap.clear();
    stringMap.clear();
    stringTable.clear();
  }

  @Override
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
    writeDouble(fieldValue);
  }

  public void writeInt(int fieldValue) {
    append(String.valueOf(fieldValue));
  }
  
  public abstract void writeLong(long value);

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
  protected int addString(String string) {
    if (string == null) {
      return 0;
    }
    Integer o = stringMap.get(string);
    if (o != null) {
      return o;
    }
    stringTable.add(string);
    // index is 1-based
    int index = stringTable.size();
    stringMap.put(string, index);
    return index;
  }

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
  protected int getIndexForObject(Object instance) {
    return objectMap.containsKey(instance) ? objectMap.get(instance) : -1;
  }

  /**
   * Compute and return the type signature for an object.
   * 
   * @param instance the instance to inspect
   * @return the type signature of the instance
   */
  protected abstract String getObjectTypeSignature(Object instance)
      throws SerializationException;

  /**
   * Gets the string table.
   */
  protected List<String> getStringTable() {
    return stringTable;
  }

  /**
   * Remember this object as having been seen before.
   * 
   * @param instance the object to remember
   */
  protected void saveIndexForObject(Object instance) {
    objectMap.put(instance, objectCount++);
  }

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
