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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.rpc.SerializationException;

/**
 * For internal use only. Used for server call serialization.
 */
public final class ClientSerializationStreamReader extends
    AbstractSerializationStreamReader {

  private static native JavaScriptObject eval(String encoded) /*-{
    return eval(encoded);
  }-*/;

  private static native int getLength(JavaScriptObject array) /*-{
    return array.length;
  }-*/;

  int index;

  JavaScriptObject results;

  JavaScriptObject stringTable;

  private Serializer serializer;

  public ClientSerializationStreamReader(Serializer serializer) {
    this.serializer = serializer;
  }

  public void prepareToRead(String encoded) throws SerializationException {
    results = eval(encoded);
    index = getLength(results);
    super.prepareToRead(encoded);
    stringTable = readJavaScriptObject();
  }

  public native boolean readBoolean() /*-{
    return !!this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::results[--this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::index];
  }-*/;

  public native byte readByte() /*-{
    return this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::results[--this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::index];
  }-*/;

  public native char readChar() /*-{
    return this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::results[--this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::index];
  }-*/;

  public native double readDouble() /*-{
    return this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::results[--this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::index];
  }-*/;

  public native float readFloat() /*-{
    return this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::results[--this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::index];
  }-*/;

  public native int readInt() /*-{
    return this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::results[--this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::index];
  }-*/;

  public native long readLong() /*-{
    return this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::results[--this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::index];
  }-*/;

  public native short readShort() /*-{
    return this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::results[--this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::index];
  }-*/;

  public String readString() throws SerializationException {
    return getString(readInt());
  }

  protected Object deserialize(String typeSignature)
      throws SerializationException {
    Object instance = serializer.instantiate(this, typeSignature);
    rememberDecodedObject(instance);
    serializer.deserialize(this, instance, typeSignature);
    return instance;
  }

  protected native String getString(int index) /*-{ 
    // index is 1-based
    if (!index) {
      return null;
    }
    return this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::stringTable[index - 1];
  }-*/;

  private native JavaScriptObject readJavaScriptObject() /*-{
    return this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::results[--this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::index];
  }-*/;

}
