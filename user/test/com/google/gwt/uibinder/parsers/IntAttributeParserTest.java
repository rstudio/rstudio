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

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.uibinder.rebind.MortalLogger;

import junit.framework.TestCase;

/**
 * Test parsing integer attributes.
 */
public class IntAttributeParserTest extends TestCase {
  private IntAttributeParser parser;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    parser = new IntAttributeParser();
  }

  public void testGood() throws UnableToCompleteException {
    assertEquals("1234", parser.parse("1234", MortalLogger.NULL));
    assertEquals("-4321", parser.parse("-4321", MortalLogger.NULL));
  }

  public void testBad() {
    try {
      parser.parse("fnord", MortalLogger.NULL);
      fail("Expected UnableToCompleteException");
    } catch (UnableToCompleteException e) {
      /* pass */
    }
  }

  public void testFieldRef() throws UnableToCompleteException {
    assertEquals("foo.bar().baz()", parser.parse("{foo.bar.baz}",
        MortalLogger.NULL));
  }
}
