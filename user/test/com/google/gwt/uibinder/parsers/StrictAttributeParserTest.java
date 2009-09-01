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
package com.google.gwt.uibinder.parsers;

import com.google.gwt.uibinder.parsers.FieldReferenceConverter.IllegalFieldReferenceException;
import com.google.gwt.uibinder.parsers.StrictAttributeParser.FieldReferenceDelegate;

import junit.framework.TestCase;

/**
 * Tests StrictAttributeParser. Actually, tests its static inner class which
 * does all of the actual work, so that we don't have to struggle to mock
 * UiBinderWriter.
 */
public class StrictAttributeParserTest extends TestCase {
  FieldReferenceConverter bp = new FieldReferenceConverter(new FieldReferenceDelegate());

  public void testSimple() {
    String before = "{able.baker.charlie.prawns}";
    String expected = "able.baker().charlie().prawns()";
    assertEquals(expected, bp.convert(before));
  }
  
  public void testNoneShouldFail() {
    String before = "able.baker.charlie.prawns";
    try {
      bp.convert(before);
      fail();
    } catch (IllegalFieldReferenceException e) {
      /* pass */
    }
  }

  public void testTooManyShouldFail() {
    String before = "{able.baker.charlie} {prawns.are.yummy}";
    try {
      bp.convert(before);
      fail();
    } catch (IllegalFieldReferenceException e) {
      /* pass */
    }
  }
  
  public void testMixedShouldFail() {
    String before = "{able.baker.charlie} prawns are still yummy}";
    try {
      bp.convert(before);
      fail();
    } catch (IllegalFieldReferenceException e) {
      /* pass */
    }
  }
}
