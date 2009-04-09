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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.jdt.RebindPermutationOracle;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JGwtCreate;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;

import java.util.ArrayList;
import java.util.List;

/**
 * Replaces any "GWT.create()" calls with a special node.
 */
public class ReplaceRebinds {

  private class RebindVisitor extends JModVisitor {

    private final JMethod rebindCreateMethod;

    public RebindVisitor(JMethod rebindCreateMethod) {
      this.rebindCreateMethod = rebindCreateMethod;
    }

    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      JMethod method = x.getTarget();
      if (method == rebindCreateMethod) {
        assert (x.getArgs().size() == 1);
        JExpression arg = x.getArgs().get(0);
        assert (arg instanceof JClassLiteral);
        JClassLiteral classLiteral = (JClassLiteral) arg;
        JReferenceType sourceType = (JReferenceType) classLiteral.getRefType();
        List<JClassType> allRebindResults = getAllPossibleRebindResults(sourceType);
        JGwtCreate gwtCreate = new JGwtCreate(x.getSourceInfo(), sourceType,
            allRebindResults, program.getTypeJavaLangObject());
        if (allRebindResults.size() == 1) {
          // Just replace with the instantiation expression.
          ctx.replaceMe(gwtCreate.getInstantiationExpressions().get(0));
        } else {
          ctx.replaceMe(gwtCreate);
        }
      }
    }
  }

  public static boolean exec(TreeLogger logger, JProgram program,
      RebindPermutationOracle rpo) {
    return new ReplaceRebinds(logger, program, rpo).execImpl();
  }

  private final TreeLogger logger;
  private final JProgram program;
  private final RebindPermutationOracle rpo;

  private ReplaceRebinds(TreeLogger logger, JProgram program,
      RebindPermutationOracle rpo) {
    this.logger = logger;
    this.program = program;
    this.rpo = rpo;
  }

  protected List<JClassType> getAllPossibleRebindResults(JReferenceType type) {
    // Rebinds are always on a source type name.
    String reqType = type.getName().replace('$', '.');
    String[] answers;
    try {
      answers = rpo.getAllPossibleRebindAnswers(logger, reqType);
    } catch (UnableToCompleteException e) {
      // Should never happen.
      throw new InternalCompilerException(
          "Unexpected failure to get possible rebind answers for '" + reqType
              + "'");
    }
    List<JClassType> rebindAnswers = new ArrayList<JClassType>();
    for (String answer : answers) {
      JReferenceType result = program.getFromTypeMap(answer);
      assert (result != null);
      rebindAnswers.add((JClassType) result);
    }
    assert rebindAnswers.size() > 0;
    return rebindAnswers;
  }

  private boolean execImpl() {
    RebindVisitor rebinder = new RebindVisitor(
        program.getIndexedMethod("GWT.create"));
    rebinder.accept(program);
    return rebinder.didChange();
  }

}
