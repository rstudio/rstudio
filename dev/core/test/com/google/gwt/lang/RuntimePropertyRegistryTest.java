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
package com.google.gwt.lang;

import com.google.gwt.lang.RuntimePropertyRegistry.PropertyValueProvider;

import junit.framework.TestCase;

/**
 * Tests for the RuntimePropertyRegistry.
 */
public class RuntimePropertyRegistryTest extends TestCase {

  private static class SimplePropertyValueProvider extends PropertyValueProvider {

    private int accessCount;
    private String name;
    private String value;

    public SimplePropertyValueProvider(String name, String value) {
      this.name = name;
      this.value = value;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String getValue() {
      accessCount++;
      return value;
    }
  }

  public void testRegisterAndGet() {
    // Sets up.
    SimplePropertyValueProvider propertyValueProvider =
        new SimplePropertyValueProvider("user.agent", "webkit");
    RuntimePropertyRegistry.registerPropertyValueProvider(propertyValueProvider);

    // Verifies lookup and caching.
    assertEquals("webkit", RuntimePropertyRegistry.getPropertyValue("user.agent"));
    assertEquals("webkit", RuntimePropertyRegistry.getPropertyValue("user.agent"));
    assertEquals("webkit", RuntimePropertyRegistry.getPropertyValue("user.agent"));
    assertEquals(1, propertyValueProvider.accessCount);
  }
}
