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

import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JGwtCreate;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReboundEntryPoint;
import com.google.gwt.dev.jjs.ast.JType;

import java.util.List;
import java.util.Map;

/**
 * Replaces any "GWT.create()" calls with a new expression for the actual result
 * of the deferred binding decision.
 */
public class ResolveRebinds {

  private class RebindVisitor extends JModVisitor {
    @Override
    public void endVisit(JGwtCreate x, Context ctx) {
      JClassType rebindResult = rebind(x.getSourceType());
      List<JClassType> rebindResults = x.getResultTypes();
      for (int i = 0; i < rebindResults.size(); ++i) {
        // Find the matching rebound type.
        if (rebindResult == rebindResults.get(i)) {
          // Replace with the associated instantiation expression.
          ctx.replaceMe(x.getInstantiationExpressions().get(i));
          return;
        }
      }
      throw new InternalCompilerException(
          "No matching rebind result in all rebind results!");
    }

    @Override
    public void endVisit(JReboundEntryPoint x, Context ctx) {
      JClassType rebindResult = rebind(x.getSourceType());
      List<JClassType> rebindResults = x.getResultTypes();
      for (int i = 0; i < rebindResults.size(); ++i) {
        // Find the matching rebound type.
        if (rebindResult == rebindResults.get(i)) {
          // Replace with the associated instantiation expression.
          ctx.replaceMe(x.getEntryCalls().get(i).makeStatement());
          return;
        }
      }
      throw new InternalCompilerException(
          "No matching rebind result in all rebind results!");
    }
  }

  public static boolean exec(JProgram program, Map<String, String> rebindAnswers) {
    return new ResolveRebinds(program, rebindAnswers).execImpl();
  }

  private final JProgram program;
  private final Map<String, String> rebindAnswers;

  private ResolveRebinds(JProgram program, Map<String, String> rebindAnswers) {
    this.program = program;
    this.rebindAnswers = rebindAnswers;
  }

  public JClassType rebind(JType type) {
    // Rebinds are always on a source type name.
    String reqType = type.getName().replace('$', '.');
    String reboundClassName = rebindAnswers.get(reqType);
    if (reboundClassName == null) {
      // The fact that we already compute every rebind permutation before
      // compiling should prevent this case from ever happening in real life.
      //
      throw new InternalCompilerException("Unexpected failure to rebind '"
          + reqType + "'");
    }
    JDeclaredType result = program.getFromTypeMap(reboundClassName);
    assert (result != null);
    return (JClassType) result;
  }

  private boolean execImpl() {
    RebindVisitor rebinder = new RebindVisitor();
    rebinder.accept(program);
    return rebinder.didChange();
  }

}
