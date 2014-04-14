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
package com.google.gwt.dev;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.util.UnitTestTreeLogger;

import junit.framework.TestCase;

import org.xml.sax.SAXParseException;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Test for {@link ServletValidator}.
 */
public class ServletValidatorTest extends TestCase {

  public void testBadUrl() throws Exception {
    UnitTestTreeLogger.Builder builder = new UnitTestTreeLogger.Builder();
    builder.setLowestLogLevel(TreeLogger.WARN);
    URL url = new File("nonexistent.web.xml").toURI().toURL();
    builder.expectWarn("Unable to process '" + url.toExternalForm()
        + "' for servlet validation", IOException.class);
    UnitTestTreeLogger logger = builder.createLogger();
    ServletValidator validator = ServletValidator.create(logger, url);
    assertNull(validator);
    logger.assertCorrectLogEntries();
  }

  public void testBadWebXml() throws Exception {
    UnitTestTreeLogger.Builder builder = new UnitTestTreeLogger.Builder();
    builder.setLowestLogLevel(TreeLogger.WARN);
    URL url = this.getClass().getResource("invalid.web.xml");
    assertNotNull(url);
    builder.expectWarn("Unable to process '" + url.toExternalForm()
        + "' for servlet validation", SAXParseException.class);
    UnitTestTreeLogger logger = builder.createLogger();
    ServletValidator validator = ServletValidator.create(logger, url);
    assertNull(validator);
    logger.assertCorrectLogEntries();
  }

  public void testGoodWebXml() throws Exception {
    createValidator();
  }

  public void testNoMappings() throws Exception {
    ServletValidator validator = createValidator();
    assertTrue(validator.containsServletClass("org.test.NoMappings"));
    assertFalse(validator.containsServletMapping("org.test.NoMappings",
        "/no/mapping"));

    UnitTestTreeLogger.Builder builder = new UnitTestTreeLogger.Builder();
    builder.setLowestLogLevel(TreeLogger.WARN);
    builder.expectWarn(ServletValidator.generateMissingMappingMessage(
        "org.test.NoMappings", "/no/mapping", "noMappings"), null);

    UnitTestTreeLogger logger = builder.createLogger();
    validator.validate(logger, "org.test.NoMappings", "/no/mapping");
    logger.assertCorrectLogEntries();
  }

  public void testNoServlet() throws Exception {
    ServletValidator validator = createValidator();
    assertFalse(validator.containsServletClass("not.defined.Class"));

    UnitTestTreeLogger.Builder builder = new UnitTestTreeLogger.Builder();
    builder.setLowestLogLevel(TreeLogger.WARN);
    builder.expectWarn(ServletValidator.generateMissingServletMessage(
        "not.defined.Class", "/no/mapping"), null);

    UnitTestTreeLogger logger = builder.createLogger();
    validator.validate(logger, "not.defined.Class", "/no/mapping");
    logger.assertCorrectLogEntries();
  }

  public void testOneMapping() throws Exception {
    ServletValidator validator = createValidator();
    assertTrue(validator.containsServletClass("org.test.OneMapping"));
    assertTrue(validator.containsServletMapping("org.test.OneMapping",
        "/one/mapping"));
    assertFalse(validator.containsServletMapping("org.test.OneMapping",
        "/no/mapping"));

    UnitTestTreeLogger.Builder builder = new UnitTestTreeLogger.Builder();
    builder.setLowestLogLevel(TreeLogger.WARN);

    UnitTestTreeLogger logger = builder.createLogger();
    validator.validate(logger, "org.test.OneMapping", "/one/mapping");
    logger.assertCorrectLogEntries();
  }

  public void testSuggestServletName() throws Exception {
    assertEquals("a", ServletValidator.suggestServletName("A"));
    assertEquals("className", ServletValidator.suggestServletName("ClassName"));
    assertEquals("className",
        ServletValidator.suggestServletName("a.b.ClassName"));
  }

  public void testTwoMappings() throws Exception {
    ServletValidator validator = createValidator();
    assertTrue(validator.containsServletClass("org.test.TwoMappings"));
    assertTrue(validator.containsServletMapping("org.test.TwoMappings",
        "/two/mappings1"));
    assertTrue(validator.containsServletMapping("org.test.TwoMappings",
        "/two/mappings2"));
    assertFalse(validator.containsServletMapping("org.test.TwoMappings",
        "/no/mapping"));

    UnitTestTreeLogger.Builder builder = new UnitTestTreeLogger.Builder();
    builder.setLowestLogLevel(TreeLogger.WARN);

    UnitTestTreeLogger logger = builder.createLogger();
    validator.validate(logger, "org.test.TwoMappings", "/two/mappings1");
    validator.validate(logger, "org.test.TwoMappings", "/two/mappings2");
    logger.assertCorrectLogEntries();
  }

  private ServletValidator createValidator() {
    UnitTestTreeLogger.Builder builder = new UnitTestTreeLogger.Builder();
    builder.setLowestLogLevel(TreeLogger.WARN);
    UnitTestTreeLogger logger = builder.createLogger();
    URL url = this.getClass().getResource("valid.web.xml");
    assertNotNull(url);
    ServletValidator validator = ServletValidator.create(logger, url);
    assertNotNull(validator);
    logger.assertCorrectLogEntries();
    return validator;
  }
}
