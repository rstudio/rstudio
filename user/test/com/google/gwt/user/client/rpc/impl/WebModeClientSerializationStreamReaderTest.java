/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.gwt.user.client.rpc.impl;

import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.user.client.rpc.RpcTestBase;
import com.google.gwt.user.client.rpc.SerializationException;

/**
 * Tests the super sourced version for {@link ClientSerializationStreamReader}. For devmode version
 * see {@link ClientSerializationStreamReaderTest}
 */
@DoNotRunWith(Platform.Devel)
public class WebModeClientSerializationStreamReaderTest extends RpcTestBase {

  public void testParsingVersion8() throws SerializationException {

    ClientSerializationStreamReader reader = new ClientSerializationStreamReader(null);

    String encoded = "["
        + "\"NaN\"," // a stringified double
        + "\"Infinity\"," // a stringified double
        + "\"-Infinity\"," // a stringified double
        + "[]," // stringTable
        + "0," // flags
        + "8" // version
        + "]";

    assertEquals(8, readVersion(encoded));

    reader.prepareToRead(encoded);

    assertEquals(8, reader.getVersion());

    assertTrue(Double.isInfinite(reader.readDouble()));
    assertTrue(Double.isInfinite(reader.readDouble()));
    assertTrue(Double.isNaN(reader.readDouble()));
  }

  public void testParsingVersion7() throws SerializationException {

    ClientSerializationStreamReader reader = new ClientSerializationStreamReader(null);

    String encoded = "["
        + "NaN," // a double
        + "Infinity," // a double
        + "-Infinity," // a double
        + "[]," // stringTable
        + "0," // flags
        + "7" // version
        + "]";

    assertEquals(7, readVersion(encoded));

    reader.prepareToRead(encoded);

    assertEquals(7, reader.getVersion());

    assertTrue(Double.isInfinite(reader.readDouble()));
    assertTrue(Double.isInfinite(reader.readDouble()));
    assertTrue(Double.isNaN(reader.readDouble()));
  }

  /**
   * Tests edge-case where version is moved to single item in concatenated array.
   *
   * See https://github.com/gwtproject/gwt/issues/9536
   */
  public void testParsingVersion7ArrayConcats() throws SerializationException {
    ClientSerializationStreamReader reader = new ClientSerializationStreamReader(null);

    String encoded = "[42,[],0].concat([7])";

    assertEquals(7, readVersion(encoded));

    reader.prepareToRead(encoded);

    assertEquals(7, reader.getVersion());

    assertEquals(42, reader.readInt());
  }

  private native int readVersion(String encoded)/*-{
    return @com.google.gwt.user.client.rpc.impl.ClientSerializationStreamReader::readVersion(Ljava/lang/String;)(encoded);
  }-*/;
}
