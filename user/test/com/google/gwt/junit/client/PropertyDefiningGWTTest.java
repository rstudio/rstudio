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
package com.google.gwt.junit.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.junit.client.WithProperties.Property;

import java.util.Date;

/**
 * A {@link GWTTestCase} that defines module properties.
 */
public class PropertyDefiningGWTTest extends GWTTestCase {
  /**
   * Base interface used for testing.
   */
  public static interface TestInterface {
    String value();
  }

  /**
   * Implementation used in one deferred binding.
   */
  public static class TestImplOne implements TestInterface {
    public String value() {
      return "one";
    }
  }

  /**
   * Implementation used in the other deferred binding.
   */
  public static class TestImplTwo implements TestInterface {
    public String value() {
      return "two";
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.junit.JUnitTestWithProperties";
  }

  @WithProperties({
    @Property(name = "locale", value = "en_US"),
    @Property(name = "my.property", value = "one")
  })
  public void testInUSLocaleAndPropertyOne() {
    assertEquals("June", DateTimeFormat.getFormat("MMMM").format(new Date(99, 5, 13)));
    assertEquals("one", GWT.<TestInterface> create(TestInterface.class).value());
  }

  @WithProperties({
    @Property(name = "locale", value = "de_CH"),
    @Property(name = "my.property", value = "two")
  })
  public void testInSwissLocaleAndPropertyTwo() {
    assertEquals("Juni", DateTimeFormat.getFormat("MMMM").format(new Date(99, 5, 13)));
    assertEquals("two", GWT.<TestInterface> create(TestInterface.class).value());
  }
}
