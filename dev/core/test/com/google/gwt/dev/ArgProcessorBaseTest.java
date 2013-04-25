/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.dev;

import com.google.gwt.dev.util.arg.ArgHandlerDraftCompile;
import com.google.gwt.dev.util.arg.ArgHandlerOptimize;
import com.google.gwt.dev.util.arg.OptionAggressivelyOptimize;
import com.google.gwt.dev.util.arg.OptionOptimize;

/**
 * Test for {@link ArgProcessorBase}.
 */
public class ArgProcessorBaseTest extends ArgProcessorTestBase {

  private static class OptimizationOptions implements OptionOptimize, OptionAggressivelyOptimize {

    private boolean aggressivelyOptimize;
    private int level;

    @Override
    public int getOptimizationLevel() {
      return level;
    }

    @Override
    public boolean isAggressivelyOptimize() {
      return aggressivelyOptimize;
    }

    @Override
    public void setAggressivelyOptimize(boolean aggressivelyOptimize) {
      this.aggressivelyOptimize = aggressivelyOptimize;
    }

    @Override
    public void setOptimizationLevel(int level) {
      this.level = level;
    }
  }

  private static class OptimizeArgProcessor extends ArgProcessorBase {
    public OptimizeArgProcessor(OptimizationOptions option) {
      registerHandler(new ArgHandlerDraftCompile(option));
      registerHandler(new ArgHandlerOptimize(option));
    }

    @Override
    protected String getName() {
      return this.getClass().getSimpleName();
    }
  }

  private final OptimizeArgProcessor argProcessor;
  private final OptimizationOptions options = new OptimizationOptions();

  public ArgProcessorBaseTest() {
    argProcessor = new OptimizeArgProcessor(options);
  }

  public void testOptionOrderIsPrecedenceArgs() {
    assertProcessSuccess(argProcessor);
    assertEquals(0, options.getOptimizationLevel());

    assertProcessSuccess(argProcessor, "-optimize", "5");
    assertEquals(5, options.getOptimizationLevel());

    assertProcessSuccess(argProcessor, "-optimize", "5", "-draftCompile");
    assertEquals(0, options.getOptimizationLevel());

    assertProcessSuccess(argProcessor, "-optimize", "5", "-draftCompile", "-optimize", "9");
    assertEquals(9, options.getOptimizationLevel());
  }
}
