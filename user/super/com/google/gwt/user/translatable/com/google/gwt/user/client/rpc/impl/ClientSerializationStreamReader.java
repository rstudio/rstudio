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

import com.google.gwt.core.client.GwtScriptOnly;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.SerializationException;

/**
 * The web-mode only version of ClientSerializationStreamReader. Any changes to this class to deal
 * with protocol changes must also be applied to the devmode version of 
 * ClientSerializationStreamReader. 
 */
@GwtScriptOnly
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

  @Override
  public void prepareToRead(String encoded) throws SerializationException {
    results = eval(encoded);
    index = getLength(results);
    super.prepareToRead(encoded);

    if (getVersion() != SERIALIZATION_STREAM_VERSION) {
      throw new IncompatibleRemoteServiceException("Expecting version "
          + SERIALIZATION_STREAM_VERSION + " from server, got " + getVersion()
          + ".");
    }
    
    if (!areFlagsValid()) {
      throw new IncompatibleRemoteServiceException("Got an unknown flag from "
          + "server: " + getFlags());
    }

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
  
  @UnsafeNativeLong
  public native long readLong() /*-{
    var s = this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::results[--this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::index];
    return @com.google.gwt.lang.LongLib::longFromBase64(Ljava/lang/String;)(s);
  }-*/;

  public native short readShort() /*-{
    return this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::results[--this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::index];
  }-*/;

  public String readString() {
    return getString(readInt());
  }

  @Override
  protected Object deserialize(String typeSignature)
      throws SerializationException {
    int id = reserveDecodedObjectIndex();
    Object instance = serializer.instantiate(this, typeSignature);
    rememberDecodedObject(id, instance);
    serializer.deserialize(this, instance, typeSignature);
    return instance;
  }

  @Override
  protected native String getString(int index) /*-{
    // index is 1-based
    return index > 0 ? this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::stringTable[index - 1] : null;
  }-*/;

  private native JavaScriptObject readJavaScriptObject() /*-{
    return this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::results[--this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::index];
  }-*/;

}
