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
package com.google.gwt.junit.client;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.JUnitBridge;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.JUnitShell.Strategy;
import com.google.gwt.junit.client.impl.JUnitResult;

import junit.framework.TestCase;

/**
 * Tests that we can run a test in dev mode even when the selection script is
 * from a compile. Note that this is the VM-only version of the class; there is
 * a translatable version for the client side.
 */
@DoNotRunWith(Platform.Prod)
public class DevModeOnCompiledScriptTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.junit.DevModeOnCompiledScriptTest";
  }

  /**
   * GWT-unfriendly, forcing us to use a translatable class.
   */
  @Override
  public Strategy getStrategy() {
    final Strategy impl = super.getStrategy();
    return new Strategy() {
      public String getModuleInherit() {
        return impl.getModuleInherit();
      }

      public String getSyntheticModuleExtension() {
        return impl.getSyntheticModuleExtension();
      }

      public void processModule(ModuleDef module) {
        impl.processModule(module);
        try {
          JUnitBridge.compileForWebMode(module);
        } catch (UnableToCompleteException e) {
          throw new RuntimeException("Failed to manually compile test module",
              e);
        }
      }

      public void processResult(TestCase testCase, JUnitResult result) {
        impl.processResult(testCase, result);
      }
    };
  }

  public void testSomethingTrivial() {
    assertTrue(true);
  }
}
