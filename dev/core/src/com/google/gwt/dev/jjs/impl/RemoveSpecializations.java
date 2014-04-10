/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;

/**
 * Remove all method specializations before final pruning pass.
 */
public class RemoveSpecializations extends JModVisitor {

  private JProgram program;

  public RemoveSpecializations(JProgram program) {
    this.program = program;
  }

  @Override
  public boolean visit(JMethod x, Context ctx) {
    x.removeSpecialization();
    return false;
  }

  private static final String NAME = RemoveSpecializations.class.getSimpleName();

  private OptimizerStats execImpl() {
    OptimizerStats stats = new OptimizerStats(NAME);
    accept(program);
    return stats;
  }

  public static OptimizerStats exec(JProgram program) {
    Event optimizeEvent = SpeedTracerLogger
        .start(CompilerEventType.OPTIMIZE, "optimizer", NAME);
    OptimizerStats stats = new RemoveSpecializations(program).execImpl();
    optimizeEvent.end("didChange", "" + stats.didChange());
    return stats;
  }
}
