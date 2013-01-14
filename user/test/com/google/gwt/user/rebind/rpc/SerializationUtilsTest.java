/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.user.rebind.rpc;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.TypeOracleTestingUtils;
import com.google.gwt.dev.javac.testing.impl.StaticJavaResource;

import junit.framework.TestCase;

/**
 * Tests for {@link SerializationUtils}.
 */
public class SerializationUtilsTest extends TestCase {

  public void testGetSerializationSignatureUseEnumConstants() throws Throwable {
    assertEquals("Identical enums have different signature",
        getEnumSerializationSignature("FOO, BAR, BAZ"),
        getEnumSerializationSignature("FOO, BAR, BAZ"));

    assertFalse("Enums w/ renamed constant have same signature",
        getEnumSerializationSignature("FOO, BAR, BAZ").equals(
            getEnumSerializationSignature("FOO, BAZ, BAR")));
    // reordering is equivalent to renaming, but let's test it anyway
    assertFalse("Enums w/ reordered constants have same signature",
        getEnumSerializationSignature("FOO, BAR, BAZ").equals(
            getEnumSerializationSignature("FOO, BAZ, BAR")));

    assertFalse("Enums w/ added constant have same signature",
        getEnumSerializationSignature("FOO, BAR, BAZ").equals(
            getEnumSerializationSignature("FOO, BAR, BAZ, QUUX")));
    assertFalse("Enums w/ removed constant have same signature",
        getEnumSerializationSignature("FOO, BAR, BAZ").equals(
            getEnumSerializationSignature("FOO, BAR")));

    assertEquals("Enums w/ changed implementation have different signature",
        getEnumSerializationSignature("FOO, BAR, BAZ"),
        getEnumSerializationSignature("FOO, BAR { @Override public String toString() { return \"QUUX\"; } }, BAZ"));
  }

  protected String getEnumSerializationSignature(String constants) throws NotFoundException {
    TypeOracle to = TypeOracleTestingUtils.buildStandardTypeOracleWith(TreeLogger.NULL,
        new StaticJavaResource("TestEnum", "public enum TestEnum { " + constants + " }"));
    JClassType enumType = to.getType("TestEnum");
    return SerializationUtils.getSerializationSignature(to, enumType);
  }
}
