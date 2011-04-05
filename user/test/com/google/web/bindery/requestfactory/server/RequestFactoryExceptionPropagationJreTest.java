/*
 * Copyright 2011 Google Inc.
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

import com.google.gwt.core.client.GWT;
import com.google.web.bindery.requestfactory.gwt.client.RequestFactoryExceptionPropagationTest;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.SimpleRequestFactory;

/**
 * JRE version of {@link RequestFactoryExceptionPropagationTest}.
 */
public class RequestFactoryExceptionPropagationJreTest extends
    RequestFactoryExceptionPropagationTest {
  @Override
  public String getModuleName() {
    return null;
  }

  @Override
  protected SimpleRequestFactory createFactory() {
    return RequestFactoryJreTest.createInProcess(SimpleRequestFactory.class);
  }
  
  @Override
  protected void fireContextAndCatch(RequestContext context,
      Receiver<Void> receiver, GWT.UncaughtExceptionHandler exceptionHandler) {
    try {
      if (receiver == null) {
        context.fire();
      } else {
        context.fire(receiver);
      }
    } catch (Throwable e) {
      exceptionHandler.onUncaughtException(e);
    }
  }
}
