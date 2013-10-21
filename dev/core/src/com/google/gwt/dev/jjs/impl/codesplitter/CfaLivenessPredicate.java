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
package com.google.gwt.dev.jjs.impl.codesplitter;

import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.impl.ControlFlowAnalyzer;

/**
 * A {@link LivenessPredicate} that bases liveness on a single
 * {@link com.google.gwt.dev.jjs.impl.ControlFlowAnalyzer}.
 */
public class CfaLivenessPredicate implements LivenessPredicate {
  private final ControlFlowAnalyzer cfa;

  public CfaLivenessPredicate(ControlFlowAnalyzer cfa) {
    this.cfa = cfa;
  }

  @Override
  public boolean isLive(JDeclaredType type) {
    return cfa.getInstantiatedTypes().contains(type);
  }

  @Override
  public boolean isLive(JField field) {
    return cfa.getLiveFieldsAndMethods().contains(field)
        || cfa.getFieldsWritten().contains(field);
  }

  @Override
  public boolean isLive(JMethod method) {
    return cfa.getLiveFieldsAndMethods().contains(method);
  }

  @Override
  public boolean isLive(String string) {
    return cfa.getLiveStrings().contains(string);
  }

  @Override
  public boolean miscellaneousStatementsAreLive() {
    return true;
  }
}
