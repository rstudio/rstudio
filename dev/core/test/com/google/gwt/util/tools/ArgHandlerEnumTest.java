/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.util.tools;

import junit.framework.TestCase;

/**
 * Test for ArgHandlerEnum.
 */
public class ArgHandlerEnumTest extends TestCase {

  enum SomeFlags { THIS_FLAG, THAT_FLAG, THAT_OTHER_FLAG, ANOTHER_FLAG }

  static public SomeFlags optionValue = null;

  private abstract class MockArgHandlerEnumBase extends ArgHandlerEnum<SomeFlags> {

    public MockArgHandlerEnumBase() {
      super(SomeFlags.class);
    }

    public MockArgHandlerEnumBase(SomeFlags defaultValue,
        boolean allowAbbraviation) {
      super(SomeFlags.class, defaultValue, allowAbbraviation);
    }

    @Override
    public String getPurpose() {
      return getPurposeString("Set flag:");
    }

    @Override
    public String getTag() {
      return "-Xflag";
    }

    @Override
    public boolean isExperimental() {
      return true;
    }

    @Override
    public void setValue(SomeFlags value) {
      optionValue = value;
    }
  }

  public void testHandle() {
    ArgHandler handler = new MockArgHandlerEnumBase() { };
    optionValue = null;
    int consuemdArguments = handler.handle(new String[] {"-Xflag", "THIS_FLAG"}, 0);
    assertEquals(1, consuemdArguments);
    assertEquals(SomeFlags.THIS_FLAG, optionValue);

    consuemdArguments = handler.handle(new String[] {"-Xflag", "THAT_FLAG"}, 0);
    assertEquals(1, consuemdArguments);
    assertEquals(SomeFlags.THAT_FLAG, optionValue);

    consuemdArguments = handler.handle(new String[] {"-Xflag", "ThAt_OtHeR_fLaG"}, 0);
    assertEquals(1, consuemdArguments);
    assertEquals(SomeFlags.THAT_OTHER_FLAG, optionValue);

    consuemdArguments = handler.handle(new String[] {"-Xflag", "THAT_FLA"}, 0);
    assertEquals(-1, consuemdArguments);
    assertEquals(SomeFlags.THAT_OTHER_FLAG, optionValue);
  }

  public void testHandle_default() {
    ArgHandler handler = new MockArgHandlerEnumBase(SomeFlags.THAT_OTHER_FLAG, false) { };
    optionValue = null;
    int consuemdArguments = handler.handle(handler.getDefaultArgs(), 0);
    assertEquals(1, consuemdArguments);
    assertEquals(SomeFlags.THAT_OTHER_FLAG, optionValue);
  }

  public void testHandle_Abbreviations() {
    ArgHandler handler = new MockArgHandlerEnumBase(null, true) { };
    optionValue = null;
    int consuemdArguments =  handler.handle(new String[] {"-Xflag", "THIS"}, 0);
    assertEquals(1, consuemdArguments);
    assertEquals(SomeFlags.THIS_FLAG, optionValue);

    consuemdArguments =  handler.handle(new String[] {"-Xflag", "THAT"}, 0);
    assertEquals(-1, consuemdArguments);
    assertEquals(SomeFlags.THIS_FLAG, optionValue);

    consuemdArguments =  handler.handle(new String[] {"-Xflag", "THAT_O"}, 0);
    assertEquals(1, consuemdArguments);
    assertEquals(SomeFlags.THAT_OTHER_FLAG, optionValue);

    consuemdArguments =  handler.handle(new String[] {"-Xflag", "ThAt_f"}, 0);
    assertEquals(1, consuemdArguments);
    assertEquals(SomeFlags.THAT_FLAG, optionValue);

    consuemdArguments = handler.handle(new String[] {"-Xflag", "AN"}, 0);
    assertEquals(-1, consuemdArguments);
    assertEquals(SomeFlags.THAT_FLAG, optionValue);

    consuemdArguments = handler.handle(new String[] {"-Xflag", "ANO"}, 0);
    assertEquals(1, consuemdArguments);
    assertEquals(SomeFlags.ANOTHER_FLAG, optionValue);
  }

  public void testGetDefaultTags() {
    ArgHandler handler = new MockArgHandlerEnumBase() { };
    assertNull(handler.getDefaultArgs());

    handler = new MockArgHandlerEnumBase(SomeFlags.THAT_FLAG, false) { };
    assertContentsEquals(new String[]{"-Xflag", "THAT_FLAG"}, handler.getDefaultArgs());
  }

  private <T> void assertContentsEquals(T[] expected, T[] actual) {
    assertEquals("Different sizes", expected.length, actual.length);
    for (int i = 0; i < expected.length; i++) {
      assertEquals(expected[i], actual[i]);
    }
  }
}
