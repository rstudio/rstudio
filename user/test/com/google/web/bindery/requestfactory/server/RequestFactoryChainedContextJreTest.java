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
package com.google.web.bindery.requestfactory.server;

import com.google.web.bindery.requestfactory.gwt.client.RequestFactoryChainedContextTest;
import com.google.web.bindery.requestfactory.shared.SimpleRequestFactory;

/**
 * Runs the RequestFactoryChainedContext tests in-process.
 */
public class RequestFactoryChainedContextJreTest extends RequestFactoryChainedContextTest {

  @Override
  public String getModuleName() {
    return null;
  }

  @Override
  protected Factory createChainedFactory() {
    return RequestFactoryJreTest.createInProcess(Factory.class);
  }

  @Override
  protected SimpleRequestFactory createFactory() {
    return RequestFactoryJreTest.createInProcess(SimpleRequestFactory.class);
  }
}
