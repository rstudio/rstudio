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
 */package org.hibernate.jsr303.tck.util;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.validation.rebind.BeanHelper;

/**
 * Abstract TestCase for TCK tests that are expected to fail to compile.
 */
public abstract class TckCompileTestCase extends GWTTestCase {

  public TckCompileTestCase() {
    super();
  }

  @Override
  public final String getModuleName() {
    return null; // Run as JRE tests
  }

  @Override
  protected void gwtSetUp() throws Exception {
    super.gwtSetUp();
    BeanHelper.clearBeanHelpersForTests();
  }

  @Override
  protected void gwtTearDown() throws Exception {
   BeanHelper.clearBeanHelpersForTests();
    super.gwtTearDown();
  }

}