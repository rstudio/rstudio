/*
 * Copyright 2012 Google Inc.
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

import junit.framework.TestCase;

/**
 * Tests {@link ClientSerializationStreamReader}.
 */
public class ClientSerializationStreamReaderTest extends TestCase {

  public void testRead() throws SerializationException {
    ClientSerializationStreamReader reader = new ClientSerializationStreamReader(null);

    reader.prepareToRead("["
        + "3.5,"  // a double
        + "1,"  // String table index: "one"
        + "3,"  // String table index: "three"
        + "2,"  // String table index: "two"
        + "[\"one\",\"two\",\"three\"],"
        + "0,"  // flags
        + AbstractSerializationStream.SERIALIZATION_STREAM_VERSION  // version
        + "]");

    assertEquals("two", reader.readString());
    assertEquals("three", reader.readString());
    assertEquals("one", reader.readString());
    assertEquals(3.5, reader.readDouble());
  }

  public void testRead_stringConcats() throws SerializationException {
    ClientSerializationStreamReader reader = new ClientSerializationStreamReader(null);

    reader.prepareToRead("["
        + "1,"  // String table index: "onetwothree"
        + "[\"one\"+\"two\"+\"three\"],"
        + "0,"  // flags
        + AbstractSerializationStream.SERIALIZATION_STREAM_VERSION  // version
        + "]");

    assertEquals("onetwothree", reader.readString());
  }

  public void testRead_arrayConcats() throws SerializationException {
    ClientSerializationStreamReader reader = new ClientSerializationStreamReader(null);

    reader.prepareToRead("["
        + "1,"  // String table index: "one"
        + "2"  // String table index: "two"
        + "].concat(["
        + "[\"one\"].concat([\"two\"]),"
        + "0,"  // flags
        + AbstractSerializationStream.SERIALIZATION_STREAM_VERSION  // version
        + "])");

    assertEquals("two", reader.readString());
    assertEquals("one", reader.readString());
  }

  /*
   * Note: this test verifies a issue with the Rhino parser that limits the size of a single string
   * node to 64KB. If this test starts failing, then the Rhino parser may have been fixed to support
   * larger strings and the string concat workaround could be removed.
   */
  public void testRead_stringOver64KB() {
    ClientSerializationStreamReader reader = new ClientSerializationStreamReader(null);

    int stringLength = 0xFFFF;
    StringBuilder builder = new StringBuilder(stringLength);
    for (int i = 0; i < stringLength; i++) {
      builder.append('y');
    }

    // Push the string size over 64KB.
    builder.append('z');

    try {
      reader.prepareToRead("["
          + "1,"  // String table index
          + "[\"" + builder.toString() + "\"],"
          + "0,"  // flags
          + AbstractSerializationStream.SERIALIZATION_STREAM_VERSION  // version
          + "]");
      fail("Expected SerializationException");
    } catch (SerializationException e) {
      // Expected.
    }
  }

  public void testRead_stringOver64KB_concat() throws SerializationException {
    ClientSerializationStreamReader reader = new ClientSerializationStreamReader(null);

    // First node is maximum allowed 64KB.
    int node1Length = 0xFFFF;
    StringBuilder node1Builder = new StringBuilder(node1Length);
    for (int i = 0; i < node1Length; i++) {
      node1Builder.append('y');
    }

    int node2Length = 0xFF;
    StringBuilder node2Builder = new StringBuilder(0xFF);
    for (int i = 0; i < node2Length; i++) {
      node2Builder.append('z');
    }

    reader.prepareToRead("["
        + "1,"  // String table index
        + "[\"" + node1Builder.toString() + "\"+\"" + node2Builder.toString() + "\"],"
        + "0,"  // flags
        + AbstractSerializationStream.SERIALIZATION_STREAM_VERSION  // version
        + "]");

    assertEquals(node1Builder.toString() + node2Builder.toString(), reader.readString());
  }
}

