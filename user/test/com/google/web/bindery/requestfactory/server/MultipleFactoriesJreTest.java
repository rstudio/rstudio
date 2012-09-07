/*
 * Copyright 2012 Google Inc.
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
package com.google.web.bindery.requestfactory.server;

import com.google.web.bindery.requestfactory.shared.MultipleFactoriesTest;

/**
 * A JRE version of {@link MultipleFactoriesTest}.
 */
public class MultipleFactoriesJreTest extends MultipleFactoriesTest {

  @Override
  public String getModuleName() {
    return null;
  }

  @Override
  protected Factory1 createFactory1() {
    return RequestFactoryJreTest.createInProcess(Factory1.class);
  }

  @Override
  protected Factory2 createFactory2() {
    return RequestFactoryJreTest.createInProcess(Factory2.class);
  }
}
