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
package com.google.gwt.dev.js;

/**
 * Tests the JsStaticEval optimizer.
 */
public class JsDuplicateFunctionRemoverTest extends OptimizerTestBase {

  public void testDontRemoveCtors() throws Exception {
    // As fieldref qualifier
    assertEquals("function a(){}\n;function b(){}\nb.prototype={};a();b();",
        optimize("function a(){};function b(){} b.prototype={}; a(); b();"));
    // As parameter
    assertEquals(
        "function defineSeed(a,b){}\n;function a(){}\n;function b(){}\ndefineSeed(a,b);a();b();",
        optimize("function defineSeed(a,b){};function a(){};function b(){} defineSeed(a,b); a(); b();"));
  }

  public void testRemoveDuplicates() throws Exception {
    assertEquals("function a(){}\n;a();a();",
        optimize("function a(){};function b(){} a(); b();"));
  }

  private String optimize(String js) throws Exception {
    return optimize(js, JsSymbolResolver.class,
        JsDuplicateFunctionRemover.class, JsUnusedFunctionRemover.class);
  }
}