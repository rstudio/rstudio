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
package com.google.gwt.requestfactory.server;

import junit.framework.TestCase;

/**
 * Tests for {@link com.google.gwt.requestfactory.server.RequestProperty} .
 */
public class RequestPropertyTest extends TestCase {

  public void testParseMultipleSelector() {
    RequestProperty prop = RequestProperty.parse("supervisor.name, supervisor.age");
    RequestProperty sup = prop.getProperty("supervisor");
    assertNotNull(sup);
    RequestProperty name = sup.getProperty("name");
    assertNotNull(name);
    RequestProperty age = sup.getProperty("age");
    assertNotNull(name);
  }
  public void testParseSingleSelector() {
    RequestProperty prop = RequestProperty.parse("supervisor.name");
    RequestProperty sup = prop.getProperty("supervisor");
    assertNotNull(sup);
    RequestProperty name = sup.getProperty("name");
    assertNotNull(name);
  }
}