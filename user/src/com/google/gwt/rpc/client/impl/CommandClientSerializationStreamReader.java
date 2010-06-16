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

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;

/**
 * A peer to CommandServerSerializationStreamWriter.
 */
public class CommandClientSerializationStreamReader implements
    SerializationStreamReader {

  /**
   * An identifier in the payload evaluation context that is used to hold
   * backreferences.
   */
  public static final String BACKREF_IDENT = "_";

  private static native JsArray<JavaScriptObject> eval(String payload) /*-{
    return eval(payload);
  }-*/;

  private static native boolean readBoolean0(JavaScriptObject obj, int idx) /*-{
    return !!obj[idx];
  }-*/;

  private static native byte readByte0(JavaScriptObject obj, int idx) /*-{
    return obj[idx];
  }-*/;

  private static native char readChar0(JavaScriptObject obj, int idx) /*-{
    return obj[idx];
  }-*/;

  private static native double readDouble0(JavaScriptObject obj, int idx) /*-{
    return obj[idx];
  }-*/;

  private static native float readFloat0(JavaScriptObject obj, int idx) /*-{
    return obj[idx];
  }-*/;

  private static native int readInt0(JavaScriptObject obj, int idx) /*-{
    return obj[idx];
  }-*/;

  @UnsafeNativeLong
  private static native long readLong0(JavaScriptObject obj, int idx) /*-{
    return obj[idx];
  }-*/;

  private static native Object readObject0(JavaScriptObject obj, int idx) /*-{
    return obj[idx];
  }-*/;

  private static native short readShort0(JavaScriptObject obj, int idx) /*-{
    return obj[idx];
  }-*/;

  private static native String readString0(JavaScriptObject obj, int idx) /*-{
    return obj[idx];
  }-*/;

  // This field may be reset externally
  volatile int idx = 0;
  volatile JsArray<JavaScriptObject> payload;

  public void prepareToRead(String js) throws RemoteException {
    try {
      payload = eval("(function(){var " + BACKREF_IDENT + "={};" + js + "})()");
      assert payload != null : "Payload evaluated to null";
    } catch (JavaScriptException e) {
      throw new IncompatibleRemoteServiceException(
          "Unable to evaluate payload", e);
    } catch (Throwable e) {
      throw new RemoteException("Unable to evaluate payload", e);
    }
  }

  public boolean readBoolean() throws SerializationException {
    assert idx < payload.length() : "Attempting to read beyond end of payload";
    return readBoolean0(payload, idx++);
  }

  public byte readByte() throws SerializationException {
    assert idx < payload.length() : "Attempting to read beyond end of payload";
    return readByte0(payload, idx++);
  }

  public char readChar() throws SerializationException {
    assert idx < payload.length() : "Attempting to read beyond end of payload";
    return readChar0(payload, idx++);
  }

  public double readDouble() throws SerializationException {
    assert idx < payload.length() : "Attempting to read beyond end of payload";
    return readDouble0(payload, idx++);
  }

  public float readFloat() throws SerializationException {
    assert idx < payload.length() : "Attempting to read beyond end of payload";
    return readFloat0(payload, idx++);
  }

  public int readInt() throws SerializationException {
    assert idx < payload.length() : "Attempting to read beyond end of payload";
    return readInt0(payload, idx++);
  }

  public long readLong() throws SerializationException {
    assert idx < payload.length() : "Attempting to read beyond end of payload";
    return readLong0(payload, idx++);
  }

  public Object readObject() throws SerializationException {
    assert idx < payload.length() : "Attempting to read beyond end of payload";
    return readObject0(payload, idx++);
  }

  public short readShort() throws SerializationException {
    assert idx < payload.length() : "Attempting to read beyond end of payload";
    return readShort0(payload, idx++);
  }

  public String readString() throws SerializationException {
    assert idx < payload.length() : "Attempting to read beyond end of payload";
    return readString0(payload, idx++);
  }
}
