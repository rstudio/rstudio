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

package com.google.gwt.user.rebind.rpc;

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.ConfigurationProperty;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.SelectionProperty;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.TypeOracleTestingUtils;
import com.google.gwt.dev.javac.testing.impl.JavaResourceBase;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.javac.testing.impl.StaticJavaResource;
import com.google.gwt.dev.resource.Resource;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tests {@link BlacklistTypeFilter}.
 */
public class BlacklistTypeFilterTest extends TestCase {
  private static class MockConfigurationProperty implements
      ConfigurationProperty {
    String name;
    List<String> values = new ArrayList<String>();

    public MockConfigurationProperty(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public List<String> getValues() {
      return values;
    }
  }

  private class MockPropertyOracle implements PropertyOracle {
    public ConfigurationProperty getConfigurationProperty(String propertyName)
        throws BadPropertyValueException {
      if (propertyName.equals(propRpcBlacklist.getName())) {
        return propRpcBlacklist;
      }
      throw new BadPropertyValueException("No property named " + propertyName);
    }

    @Deprecated
    public String getPropertyValue(TreeLogger logger, String propertyName)
        throws BadPropertyValueException {
      return getConfigurationProperty(propertyName).getValues().get(0);
    }

    @Deprecated
    public String[] getPropertyValueSet(TreeLogger logger, String propertyName)
        throws BadPropertyValueException {
      return getConfigurationProperty(propertyName).getValues().toArray(
          new String[0]);
    }

    public SelectionProperty getSelectionProperty(TreeLogger logger,
        String propertyName) throws BadPropertyValueException {
      throw new BadPropertyValueException("No property named " + propertyName);
    }
  }

  private MockConfigurationProperty propRpcBlacklist =
    new MockConfigurationProperty("rpc.blacklist");

  public void testBasics() throws UnableToCompleteException, NotFoundException {
    Set<Resource> resources = new HashSet<Resource>();
    for (MockJavaResource resource : JavaResourceBase.getStandardResources()) {
      resources.add(resource);
    }
    resources.add(makeClass("Type1"));
    resources.add(makeClass("Type2"));
    resources.add(makeClass("Type3"));
    resources.add(makeClass("Type4"));
    resources.add(makeClass("Type5"));

    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(TreeLogger.NULL,
        resources);

    propRpcBlacklist.values.add("Type1");
    propRpcBlacklist.values.add("+Type2");
    propRpcBlacklist.values.add("-Type3");
    propRpcBlacklist.values.add("+Type4");
    propRpcBlacklist.values.add("-Type4");

    BlacklistTypeFilter filter = new BlacklistTypeFilter(TreeLogger.NULL,
        new MockPropertyOracle());
    assertFalse(filter.isAllowed(to.getType("Type1")));
    assertTrue(filter.isAllowed(to.getType("Type2")));
    assertFalse(filter.isAllowed(to.getType("Type3")));
    assertFalse(filter.isAllowed(to.getType("Type4")));
    assertTrue(filter.isAllowed(to.getType("Type5")));
  }

  private StaticJavaResource makeClass(String className) {
    StringBuilder code = new StringBuilder();
    code.append("public class " + className + "{ }\n");
    StaticJavaResource e = new StaticJavaResource(className, code);
    return e;
  }
}
