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
import com.google.gwt.user.client.rpc.SerializationStreamReader;

import java.util.ArrayList;

/**
 * Base class for the client and server serialization streams. This class
 * handles the basic serialization and desirialization formatting for primitive
 * types since these are common between the client and the server.
 */
public abstract class AbstractSerializationStreamReader extends
    AbstractSerializationStream implements SerializationStreamReader {

  private ArrayList seenArray = new ArrayList();

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
   * Gets a string out of the string table.
   * 
   * @param index the index of the string to get
   * @return the string
   */
  protected abstract String getString(int index);

  protected final void rememberDecodedObject(Object o) {
    seenArray.add(o);
  }

}
