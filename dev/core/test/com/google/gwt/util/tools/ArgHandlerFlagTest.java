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
package com.google.gwt.util.tools;

import junit.framework.TestCase;

/**
 * Test for ArgHandlerFlag.
 */
public class ArgHandlerFlagTest extends TestCase {

  private class MockArgHandlerFlag extends ArgHandlerFlag {

    private boolean value;

    public MockArgHandlerFlag() {
      addTagValue("-XdisableSand", false);
    }

    @Override
    public String getLabel() {
      return "floorSanding";
    }

    @Override
    public String getPurposeSnippet() {
      return null;
    }

    @Override
    public boolean isExperimental() {
      return true;
    }

    @Override
    public boolean setFlag(boolean value) {
      this.value = value;
      return true;
    }

    @Override
    public boolean getDefaultValue() {
      return value;
    }
  }

  private MockArgHandlerFlag argHandlerFlag;

  @Override
  protected void setUp() throws Exception {
    argHandlerFlag = new MockArgHandlerFlag();
  }

  public void testGetTag() {
    assertEquals("-XfloorSanding", argHandlerFlag.getTag());
  }

  public void testGetTags() {
    assertEquals("-XfloorSanding", argHandlerFlag.getTags()[0]);
    assertEquals("-XnofloorSanding", argHandlerFlag.getTags()[1]);
    assertEquals("-XdisableSand", argHandlerFlag.getTags()[2]);
  }

  public void testGetValueByTag() {
    assertEquals(true, argHandlerFlag.getValueByTag("-XfloorSanding"));
    assertEquals(false, argHandlerFlag.getValueByTag("-XnofloorSanding"));
    assertEquals(false, argHandlerFlag.getValueByTag("-XdisableSand"));
  }

  public void testHandle() {
    argHandlerFlag.handle(new String[] {"-XfloorSanding"}, 0);
    assertEquals(true, argHandlerFlag.value);

    argHandlerFlag.handle(new String[] {"-XnofloorSanding"}, 0);
    assertEquals(false, argHandlerFlag.value);

    argHandlerFlag.handle(new String[] {"-XdisableSand"}, 0);
    assertEquals(false, argHandlerFlag.value);
  }
}
