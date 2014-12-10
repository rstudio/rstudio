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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JPermutationDependentValue;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Set;

/**
 * Records all live rebinds.
 */
public class RecordRebinds {

  private class RebindVisitor extends JVisitor {
    @Override
    public void endVisit(JPermutationDependentValue x, Context ctx) {
      if (x.isTypeRebind()) {
        liveRebindRequests.add(x.getRequestedValue());
      }
    }
  }

  public static Set<String> exec(JProgram program) {
    Event recordRebindsEvent = SpeedTracerLogger.start(CompilerEventType.RECORD_REBINDS);
    Set<String> liveRebindRequests = Sets.newHashSet();
    new RecordRebinds(program, liveRebindRequests).execImpl();
    recordRebindsEvent.end();
    return liveRebindRequests;
  }

  private final Set<String> liveRebindRequests;
  private final JProgram program;

  private RecordRebinds(JProgram program, Set<String> liveRebindRequests) {
    this.program = program;
    this.liveRebindRequests = liveRebindRequests;
  }

  private void execImpl() {
    RebindVisitor rebinder = new RebindVisitor();
    rebinder.accept(program);
  }

}
