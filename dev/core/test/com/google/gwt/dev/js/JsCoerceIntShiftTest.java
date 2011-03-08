/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.dev.js;

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.ConfigurationProperty;
import com.google.gwt.core.ext.DefaultSelectionProperty;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.SelectionProperty;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.jjs.SourceOrigin;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.dev.shell.FailErrorLogger;
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.dev.util.TextOutput;

import junit.framework.TestCase;

import java.io.StringReader;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Test for {@link JsCoerceIntShift}.
 */
public class JsCoerceIntShiftTest extends TestCase {

  /**
   * Oracle that mocks the user.agent property.
   */
  private static class MockOracle implements PropertyOracle {

    private static SelectionProperty createSelectionProperty(String value) {
      if (value == null) {
        return null;
      } else {
        SortedSet<String> valueSet = new TreeSet<String>();
        valueSet.add(value);
        return new DefaultSelectionProperty(value, value, value, valueSet, 
            null /* fallbackValueMap */);
      }
    }

    private final SelectionProperty userAgent;

    /**
     * @param userAgentName value of user.agent property, if null then the
     *     user.agent property is treated as if it doesn't exist.
     */
    public MockOracle(String userAgentName) {
      userAgent = createSelectionProperty(userAgentName);
    }

    public ConfigurationProperty getConfigurationProperty(String propertyName)
        throws BadPropertyValueException {
      throw new BadPropertyValueException("no config properties");
    }

    @Deprecated
    public String getPropertyValue(TreeLogger logger, String propertyName)
        throws BadPropertyValueException {
      throw new BadPropertyValueException("no deprecated api");
    }

    @Deprecated
    public String[] getPropertyValueSet(TreeLogger logger, String propertyName)
        throws BadPropertyValueException {
      throw new BadPropertyValueException("no deprecated api");
    }

    public SelectionProperty getSelectionProperty(TreeLogger logger,
        String propertyName) throws BadPropertyValueException {
      if (userAgent != null && "user.agent".equals(propertyName)) {
        return userAgent;
      }
      throw new BadPropertyValueException("no property " + propertyName);
    }
  }

  private PropertyOracle safariOracle = new MockOracle("safari");
  private PropertyOracle firefoxOracle = new MockOracle("gecko1_8");
  private PropertyOracle ieOracle = new MockOracle("ie6");
  private PropertyOracle noAgentOracle = new MockOracle(null);

  private TreeLogger logger = new FailErrorLogger();

  public void testNonSafari() throws Exception {
    assertNotRewritten(firefoxOracle);
    assertNotRewritten(ieOracle, firefoxOracle);
  }
  
  public void testNoUserAgent() throws Exception {
    assertRewritten(noAgentOracle);
    assertRewritten(firefoxOracle, noAgentOracle);
  }
  
  public void testSafari() throws Exception {
    assertRewritten(safariOracle);
    assertRewritten(firefoxOracle, safariOracle);
  }

  /**
   * Assert that the provided PropertyOracles do not cause a rewrite of
   * right-shift operations.
   * 
   * @param oracles
   * @throws Exception
   */
  private void assertNotRewritten(PropertyOracle... oracles) throws Exception {
    assertEquals("a<<b;", process("a<<b;", oracles));
    assertEquals("a>>b;", process("a>>b;", oracles));
    assertEquals("a>>>b;", process("a>>>b;", oracles));
    assertEquals("1+1>>2;", process("1+1>>2;", oracles));
  }

  /**
   * Assert that the provided PropertyOracles do cause a rewrite of right-shift
   * operations.
   * 
   * @param oracles
   * @throws Exception
   */
  private void assertRewritten(PropertyOracle... oracles) throws Exception {
    assertEquals("a<<b;", process("a<<b;", oracles));
    assertEquals("~~a>>b;", process("a>>b;", oracles));
    assertEquals("~~a>>>b;", process("a>>>b;", oracles));
    assertEquals("~~(1+1)>>2;", process("1+1>>2;", oracles));
  }

  /**
   * Process a JS program with the {@link JsCoerceIntShift} pass.
   * 
   * @param js the source program
   * @param oracles 
   * @return processed JS
   */
  private String process(String js, PropertyOracle[] oracles)
      throws Exception {
    JsProgram program = new JsProgram();
    List<JsStatement> expected = JsParser.parse(SourceOrigin.UNKNOWN,
        program.getScope(), new StringReader(js));

    program.getGlobalBlock().getStatements().addAll(expected);

    JsCoerceIntShift.exec(program, logger, oracles);

    TextOutput text = new DefaultTextOutput(true);
    JsVisitor generator = new JsSourceGenerationVisitor(text);

    generator.accept(program);
    return text.toString();
  }
}
