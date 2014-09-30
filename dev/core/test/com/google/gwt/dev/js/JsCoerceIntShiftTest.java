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
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.shell.FailErrorLogger;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Test for {@link JsCoerceIntShift}.
 */
public class JsCoerceIntShiftTest extends OptimizerTestBase {

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

    @Override
    public ConfigurationProperty getConfigurationProperty(String propertyName)
        throws BadPropertyValueException {
      throw new BadPropertyValueException("no config properties");
    }

    @Override
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
  private PropertyOracle noAgentOracle = new MockOracle(null);

  private TreeLogger logger = new FailErrorLogger();

  public void testNonSafari() throws Exception {
    assertNotRewritten(firefoxOracle);
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
    return optimize(js, new JsCoerceIntShiftProxy(oracles).getClass());
  }

  // JsCoerceIntShift does not have a one parameter exec function. Test infrastructure
  // call exec(JsProgram) reflectively.
  private static class JsCoerceIntShiftProxy {
    static PropertyOracle[] oracles;
    JsCoerceIntShiftProxy(PropertyOracle[] oracles) {
      JsCoerceIntShiftProxy.oracles = oracles;
    }
    static public void exec(JsProgram program) {
      JsCoerceIntShift.exec(program, TreeLogger.NULL, oracles);
    }
  }
}