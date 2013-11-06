/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.core.ext.test;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;

import junit.framework.TestCase;

/**
 * Tests the Generator base class.
 */
public class GeneratorTest extends TestCase {

  private class SimpleGenerator extends Generator {

    @Override
    public String generate(TreeLogger logger, GeneratorContext context, String typeName)
        throws UnableToCompleteException {
      return null;
    }
  }

  public void testDefaultPropertyValueStability() {
    SimpleGenerator simpleGenerator = new SimpleGenerator();
    // Defaults to the worst case of claiming that generator output content is unstable and will
    // change as property values change.
    assertTrue(simpleGenerator.contentDependsOnProperties());
  }

  public void testDefaultRelevantPropertyNames() {
    SimpleGenerator simpleGenerator = new SimpleGenerator();
    // Defaults to the worst case of claiming that generator output content is affected by all
    // properties (that is the meaning of returning null as opposed to a specific list of property
    // names).
    assertNull(simpleGenerator.getAccessedPropertyNames());
  }

  public void testDefaultTypeStability() {
    SimpleGenerator simpleGenerator = new SimpleGenerator();
    // Defaults to the worst case of claiming that generator output content is unstable and will
    // change as the list of available types changes.
    assertTrue(simpleGenerator.contentDependsOnTypes());
  }
}
