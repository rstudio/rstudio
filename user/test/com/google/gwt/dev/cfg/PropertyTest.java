/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.dev.cfg;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import junit.framework.TestCase;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

/**
 * Checks the behaviors of ModuleDefLoader and Properties.
 */
public class PropertyTest extends TestCase {

  private static TreeLogger getRootLogger() {
    PrintWriterTreeLogger logger = new PrintWriterTreeLogger(new PrintWriter(
        System.err, true));
    logger.setMaxDetail(TreeLogger.ERROR);
    return logger;
  }

  private final ModuleDef moduleDef;

  public PropertyTest() throws UnableToCompleteException {
    // Module has the same name as this class.
    String moduleName = getClass().getCanonicalName();
    moduleDef = ModuleDefLoader.loadFromClassPath(getRootLogger(), moduleName);
  }

  public void testModule() {
    Properties p = moduleDef.getProperties();

    {
      BindingProperty restricted = (BindingProperty) p.find("restricted");
      assertNotNull(restricted);
      assertEquals(3,
          restricted.getAllowedValues(restricted.getRootCondition()).length);
      assertEquals(
          Arrays.asList("a", "b", "c"),
          Arrays.asList(restricted.getAllowedValues(restricted.getRootCondition())));
      assertTrue(restricted.isDefinedValue("d"));
      assertFalse(restricted.isAllowedValue("d"));
    }

    {
      BindingProperty restricted1s = (BindingProperty) p.find("restricted1s");
      assertNotNull(restricted1s);
      assertTrue(restricted1s.isAllowedValue("a"));
      assertEquals(1,
          restricted1s.getAllowedValues(restricted1s.getRootCondition()).length);
      assertEquals("a",
          restricted1s.getAllowedValues(restricted1s.getRootCondition())[0]);
    }

    {
      BindingProperty conditional = (BindingProperty) p.find("conditional");
      assertNotNull(conditional);
      assertFalse(conditional.isDerived());

      Set<String> required = conditional.getRequiredProperties();
      assertEquals(required.size(), 1);
      assertTrue(required.contains("restricted"));

      assertEquals(3, conditional.getConditionalValues().size());
      Iterator<Condition> it = conditional.getConditionalValues().keySet().iterator();

      assertEquals(Arrays.asList("a", "b", "c"),
          Arrays.asList(conditional.getAllowedValues(it.next())));
      assertEquals(Arrays.asList("a", "b"),
          Arrays.asList(conditional.getAllowedValues(it.next())));
      assertEquals(Arrays.asList("c"),
          Arrays.asList(conditional.getAllowedValues(it.next())));
    }

    {
      Property configProperty = p.find("configProperty");
      assertEquals("Hello World!",
          ((ConfigurationProperty) configProperty).getValue());
    }

    {
      Property configRedefined = p.find("configRedefined");
      assertEquals("bar", ((ConfigurationProperty) configRedefined).getValue());
    }

    {
      BindingProperty derived = (BindingProperty) p.find("derived");
      assertNotNull(derived);
      assertTrue(derived.isDerived());

      Set<String> required = derived.getRequiredProperties();
      assertEquals(required.size(), 1);
      assertTrue(required.contains("restricted"));

      assertEquals(3, derived.getConditionalValues().size());
    }

    {
      BindingProperty reset = (BindingProperty) p.find("reset");
      assertNotNull(reset);
      assertTrue(reset.isDerived());

      Set<String> required = reset.getRequiredProperties();
      assertEquals(0, required.size());

      assertEquals(1, reset.getConditionalValues().size());
      assertSame(reset.getRootCondition(),
          reset.getConditionalValues().keySet().iterator().next());
    }
  }

  public void testModuleInvalidOverride() {
    String moduleName = getClass().getCanonicalName();

    for (String name : Arrays.asList("A", "B", "C", "D")) {
      try {
        ModuleDefLoader.loadFromClassPath(TreeLogger.NULL, moduleName + "Bad"
            + name);
        fail("Test " + name + " should have thrown UnableToCompleteException");
      } catch (UnableToCompleteException e) {
        // OK
      }
    }
  }

  public void testProperty() {
    Properties properties = new Properties();

    assertNull(properties.find("deferred"));
    BindingProperty d = properties.createBinding("deferred");
    assertSame(d, properties.createBinding("deferred"));
    try {
      properties.createConfiguration("deferred", false);
      fail("Should have thrown an IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // OK
    }

    assertNull(properties.find("config"));
    ConfigurationProperty c = properties.createConfiguration("config", false);
    assertSame(c, properties.createConfiguration("config", false));
    try {
      properties.createBinding("config");
      fail("Should have thrown an IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // OK
    }
  }
}
