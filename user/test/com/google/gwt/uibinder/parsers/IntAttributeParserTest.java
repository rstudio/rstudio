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
import com.google.gwt.uibinder.rebind.DummyMortalLogger;

import junit.framework.TestCase;

/**
 * Test parsing integer attributes.
 */
public class IntAttributeParserTest extends TestCase {
  private IntAttributeParser parser;
  private DummyMortalLogger logger;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    parser = new IntAttributeParser();
    logger = new DummyMortalLogger();
  }

  public void testGood() throws UnableToCompleteException {
    assertEquals("1234", parser.parse("1234", logger));
    assertEquals("-4321", parser.parse("-4321", logger));
  }

  public void testBad() {
    try {
      parser.parse("fnord", logger);
      fail("Expected UnableToCompleteException");
    } catch (UnableToCompleteException e) {
      /* pass */
    }
  }

  public void testFieldRef() throws UnableToCompleteException {
    assertEquals("foo.bar().baz()", parser.parse("{foo.bar.baz}", logger));
  }
}
