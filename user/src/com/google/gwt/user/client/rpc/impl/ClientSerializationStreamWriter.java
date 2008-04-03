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

import java.util.List;

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

  private StringBuffer encodeBuffer;

  private final String moduleBaseURL;

  private final String serializationPolicyStrongName;

  private final Serializer serializer;

  /**
   * Constructs a <code>ClientSerializationStreamWriter</code> that does not
   * use a serialization policy file.
   * 
   * @param serializer the {@link Serializer} to use
   */
  public ClientSerializationStreamWriter(Serializer serializer) {
    this.serializer = serializer;
    this.moduleBaseURL = null;
    this.serializationPolicyStrongName = null;
    // Override the default version if no policy info is given.
    setVersion(SERIALIZATION_STREAM_VERSION_WITHOUT_SERIALIZATION_POLICY);
  }

  /**
   * Constructs a <code>ClientSerializationStreamWriter</code> using the
   * specified module base URL and the serialization policy.
   * 
   * @param serializer the {@link Serializer} to use
   * @param moduleBaseURL the location of the module
   * @param serializationPolicyStrongName the strong name of serialization
   *          policy
   */
  public ClientSerializationStreamWriter(Serializer serializer,
      String moduleBaseURL, String serializationPolicyStrongName) {
    this.serializer = serializer;
    this.moduleBaseURL = moduleBaseURL;
    this.serializationPolicyStrongName = serializationPolicyStrongName;
  }

  /**
   * Call this method before attempting to append any tokens. This method
   * implementation <b>must</b> be called by any overridden version.
   */
  public void prepareToWrite() {
    super.prepareToWrite();
    encodeBuffer = new StringBuffer();

    if (hasSerializationPolicyInfo()) {
      writeString(moduleBaseURL);
      writeString(serializationPolicyStrongName);
    }
  }

  @Override
  public String toString() {
    StringBuffer buffer = new StringBuffer();
    writeHeader(buffer);
    writeStringTable(buffer);
    writePayload(buffer);
    return buffer.toString();
  }

  public void writeLong(long fieldValue) {
    append(Long.toString(fieldValue, 16));
  }

  /**
   * Appends a token to the end of the buffer.
   */
  @Override
  protected void append(String token) {
    append(encodeBuffer, token);
  }

  @Override
  protected String getObjectTypeSignature(Object o) {
    Class<?> clazz = o.getClass();

    if (o instanceof Enum) {
      Enum<?> e = (Enum<?>) o;
      clazz = e.getDeclaringClass();
    }

    String typeName = clazz.getName();

    String serializationSignature = serializer.getSerializationSignature(typeName);
    if (serializationSignature != null) {
      typeName += "/" + serializationSignature;
    }
    return typeName;
  }

  @Override
  protected void serialize(Object instance, String typeSignature)
      throws SerializationException {
    serializer.serialize(this, instance, typeSignature);
  }

  private void writeHeader(StringBuffer buffer) {
    append(buffer, String.valueOf(getVersion()));
    append(buffer, String.valueOf(getFlags()));
  }

  private void writePayload(StringBuffer buffer) {
    buffer.append(encodeBuffer.toString());
  }

  private StringBuffer writeStringTable(StringBuffer buffer) {
    List<String> stringTable = getStringTable();
    append(buffer, String.valueOf(stringTable.size()));
    for (String s : stringTable) {
      append(buffer, s);
    }
    return buffer;
  }

}
