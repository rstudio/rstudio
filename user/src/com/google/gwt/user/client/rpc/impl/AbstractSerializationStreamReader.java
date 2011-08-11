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
package com.google.gwt.user.client.rpc.impl;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;

import java.util.ArrayList;

/**
 * Base class for the client and server serialization streams. This class
 * handles the basic serialization and deserialization formatting for primitive
 * types since these are common between the client and the server.
 */
public abstract class AbstractSerializationStreamReader extends
    AbstractSerializationStream implements SerializationStreamReader {
  
  private static final double TWO_PWR_15_DBL = 0x8000;
  private static final double TWO_PWR_16_DBL = 0x10000;
  private static final double TWO_PWR_22_DBL = 0x400000;
  private static final double TWO_PWR_31_DBL = TWO_PWR_16_DBL * TWO_PWR_15_DBL;
  private static final double TWO_PWR_32_DBL = TWO_PWR_16_DBL * TWO_PWR_16_DBL;
  private static final double TWO_PWR_44_DBL = TWO_PWR_22_DBL * TWO_PWR_22_DBL;
  private static final double TWO_PWR_63_DBL = TWO_PWR_32_DBL * TWO_PWR_31_DBL;

  /**
   * Return a long from a pair of doubles { low, high } such that the
   * actual value is equal to high + low.
   */
  public static long fromDoubles(double lowDouble, double highDouble) {
    long high = fromDouble(highDouble);
    long low = fromDouble(lowDouble);
    return high + low;
  }

  private static long fromDouble(double value) {
    if (Double.isNaN(value)) {
      return 0L;
    }
    if (value < -TWO_PWR_63_DBL) {
      return Long.MIN_VALUE;
    }
    if (value >= TWO_PWR_63_DBL) {
      return Long.MAX_VALUE;
    }
  
    boolean negative = false;
    if (value < 0) {
      negative = true;
      value = -value;
    }
    int a2 = 0;
    if (value >= TWO_PWR_44_DBL) {
      a2 = (int) (value / TWO_PWR_44_DBL);
      value -= a2 * TWO_PWR_44_DBL;
    }
    int a1 = 0;
    if (value >= TWO_PWR_22_DBL) {
      a1 = (int) (value / TWO_PWR_22_DBL);
      value -= a1 * TWO_PWR_22_DBL;
    }
    int a0 = (int) value;
    
    long result = ((long) a2 << 44) | ((long) a1 << 22) | a0;
    if (negative) {
      result = -result;
    }
    return result;
  }

  private ArrayList<Object> seenArray = new ArrayList<Object>();

 /**
  * Prepare to read the stream.
  *
  * @param encoded unused true if the stream is encoded
  */
  public void prepareToRead(String encoded) throws SerializationException {
    seenArray.clear();

    // Read the stream version number
    //
    setVersion(readInt());

    // Read the flags from the stream
    //
    setFlags(readInt());
  }

  public final Object readObject() throws SerializationException {
    int token = readInt();

    if (token < 0) {
      // Negative means a previous object
      // Transform negative 1-based to 0-based.
      return seenArray.get(-(token + 1));
    }

    // Positive means a new object
    String typeSignature = getString(token);
    if (typeSignature == null) {
      // a null string means a null instance
      return null;
    }

    return deserialize(typeSignature);
  }

  /**
   * Deserialize an object with the given type signature.
   * 
   * @param typeSignature the type signature to deserialize
   * @return the deserialized object
   * @throws SerializationException
   */
  protected abstract Object deserialize(String typeSignature)
      throws SerializationException;

  /**
   * Get the previously seen object at the given index which must be 1-based.
   * 
   * @param index a 1-based index into the seen objects
   * 
   * @return the object stored in the seen array at index - 1
   */
  protected final Object getDecodedObject(int index) {
    // index is 1-based
    return seenArray.get(index - 1);
  }

  /**
   * Gets a string out of the string table.
   * 
   * @param index the index of the string to get
   * @return the string
   */
  protected abstract String getString(int index);

  /**
   * Set an object in the seen list.
   * 
   * @param index a 1-based index into the seen objects
   * @param o the object to remember
   */

  protected final void rememberDecodedObject(int index, Object o) {
    // index is 1-based
    seenArray.set(index - 1, o);
  }

  /**
   * Reserve an entry for an object in the seen list.
   * 
   * @return the index to be used in future for the object
   */
  protected final int reserveDecodedObjectIndex() {
    seenArray.add(null);

    // index is 1-based
    return seenArray.size();
  }
}
