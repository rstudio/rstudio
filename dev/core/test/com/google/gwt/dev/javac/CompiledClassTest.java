/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.dev.javac;

import com.google.gwt.dev.util.Util;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Test for {@link CompiledClass}.
 */
public class CompiledClassTest extends TestCase {
  static byte[] dummyByteCode = {
    (byte) 0xDE, (byte) 0xAD, (byte)0xBE, (byte)0xEF
  };

  public void testCompiledClassSerialization() throws Exception {
    CompiledClass writeObject = new CompiledClass(dummyByteCode, null, false,
        "com/example/DeadBeef", "com.example.DeadBeef");
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    Util.writeObjectToStream(outputStream, writeObject);
    ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
    CompiledClass readObject = Util.readStreamAsObject(inputStream, CompiledClass.class);
    assertEquals(4, readObject.getBytes().length);
    byte[] readBytes = readObject.getBytes();
    for (int i = 0; i < 4; ++i) {
      assertEquals(dummyByteCode[i], readBytes[i]);
    }
  }
}
