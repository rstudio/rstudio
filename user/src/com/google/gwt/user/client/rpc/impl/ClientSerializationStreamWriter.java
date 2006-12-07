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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.rpc.SerializationException;

import java.util.ArrayList;

/**
 * For internal use only. Used for server call serialization.
 */
public final class ClientSerializationStreamWriter extends
    AbstractSerializationStreamWriter {

  private static void append(StringBuffer sb, String token) {
    assert (token != null);
    sb.append(token);
    sb.append('\uffff');
  }

  private static native JavaScriptObject createJavaScriptObject() /*-{
    return {};
  }-*/;

  JavaScriptObject objectMap;

  JavaScriptObject stringMap;

  private StringBuffer encodeBuffer;

  private int objectCount;

  private Serializer serializer;

  private ArrayList stringTable = new ArrayList();

  public ClientSerializationStreamWriter(Serializer serializer) {
    this.serializer = serializer;
  }

  /**
   * Call this method before attempting to append any tokens. This method
   * implementation <b>must</b> be called by any overridden version.
   */
  public void prepareToWrite() {
    objectCount = 0;
    objectMap = createJavaScriptObject();
    stringMap = createJavaScriptObject();
    stringTable.clear();
    encodeBuffer = new StringBuffer();
  }

  public String toString() {
    StringBuffer buffer = new StringBuffer();
    writeHeader(buffer);
    writeStringTable(buffer);
    writePayload(buffer);
    return buffer.toString();
  }

  protected int addString(String string) {
    if (string == null) {
      return 0;
    }

    int index = getIntForString(string);
    if (index > 0) {
      return index;
    }
    stringTable.add(string);
    // index is 1-based (that's why we're taking the size AFTER add)
    index = stringTable.size();
    setIntForString(string, index);
    return index;
  }

  /**
   * Appends a token to the end of the buffer.
   */
  protected void append(String token) {
    append(encodeBuffer, token);
  }

  protected int getIndexForObject(Object instance) {
    return getIntForInt(System.identityHashCode(instance));
  }

  protected String getObjectTypeSignature(Object o) {
    String typeName = GWT.getTypeName(o);
    String serializationSignature = serializer.getSerializationSignature(typeName);
    if (serializationSignature != null) {
      typeName += "/" + serializationSignature;
    }
    return typeName;
  }

  protected void saveIndexForObject(Object instance) {
    setIntForInt(System.identityHashCode(instance), objectCount++);
  }

  protected void serialize(Object instance, String typeSignature)
      throws SerializationException {
    serializer.serialize(this, instance, typeSignature);
  }

  private native int getIntForInt(int key) /*-{
    var result = this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamWriter::objectMap[key];
    return (result == null) ? -1 : result;
  }-*/;

  private native int getIntForString(String key) /*-{
    var result = this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamWriter::stringMap[key];
    return (result == null) ? 0 : result;
  }-*/;

  private native void setIntForInt(int key, int value) /*-{
    this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamWriter::objectMap[key] = value;
  }-*/;

  private native void setIntForString(String key, int value) /*-{
    this.@com.google.gwt.user.client.rpc.impl.ClientSerializationStreamWriter::stringMap[key] = value;
  }-*/;

  private void writeHeader(StringBuffer buffer) {
    append(buffer, String.valueOf(SERIALIZATION_STREAM_VERSION));
    append(buffer, String.valueOf(getFlags()));
  }

  private void writePayload(StringBuffer buffer) {
    buffer.append(encodeBuffer.toString());
  }

  private StringBuffer writeStringTable(StringBuffer buffer) {
    int stringTableSize = stringTable.size();
    append(buffer, String.valueOf(stringTableSize));
    for (int i = 0; i < stringTableSize; ++i) {
      append(buffer, (String) stringTable.get(i));
    }
    return buffer;
  }

}
