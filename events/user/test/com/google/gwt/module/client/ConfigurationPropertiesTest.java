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
package com.google.gwt.module.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Ensures that Generators are able to access module configuration properties.
 */
public class ConfigurationPropertiesTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.module.ConfigurationProperties";
  }

  /**
   * Marker interface for deferred binding to run
   * {@link com.google.gwt.module.rebind.ConfigurationPropertiesGenerator}.
   */
  public interface TestHook {
    String getConfigProperty();
  }

  public void testConfigProperty() {
    assertEquals("Hello World!",
        GWT.<TestHook> create(TestHook.class).getConfigProperty());
  }
}
