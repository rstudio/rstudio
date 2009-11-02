package com.google.gwt.uibinder.parsers;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.uibinder.rebind.DummyMortalLogger;

import junit.framework.TestCase;

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
