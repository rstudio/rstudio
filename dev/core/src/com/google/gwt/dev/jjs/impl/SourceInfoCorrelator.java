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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.dev.jjs.Correlation.Literal;
import com.google.gwt.dev.jjs.CorrelationFactory;
import com.google.gwt.dev.jjs.CorrelationFactory.RealCorrelationFactory;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.SourceInfoCorrelation;
import com.google.gwt.dev.jjs.SourceOrigin;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JStringLiteral;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.js.JsniMethodBody;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsNode;
import com.google.gwt.dev.js.ast.JsStringLiteral;
import com.google.gwt.dev.js.ast.JsSuperVisitor;

import java.util.Stack;

/**
 * Fix up SOYC parents and add correlations.
 */
public class SourceInfoCorrelator {

  private static class SourceInfoVisitor extends JVisitor {

    /**
     * Fix up SOYC for JSNI methods.
     */
    private class SourceInfoJsVisitor extends JsSuperVisitor {

      @Override
      public void endVisit(JsNode x, JsContext ctx) {
        SourceInfo popped = parents.pop();
        assert popped == x.getSourceInfo();
      }

      @Override
      public void endVisit(JsStringLiteral x, JsContext ctx) {
        x.getSourceInfo().addCorrelation(factory.by(Literal.STRING));
        super.endVisit(x, ctx);
      }

      @Override
      public boolean visit(JsNode x, JsContext ctx) {
        SourceInfo info = x.getSourceInfo();
        info = pushAndConvert(info);
        x.setSourceInfo(info);
        return true;
      }
    }

    private CorrelationFactory factory = RealCorrelationFactory.INSTANCE;
    private SourceInfoJsVisitor jsVisitor = new SourceInfoJsVisitor();
    private Stack<SourceInfoCorrelation> parents = new Stack<SourceInfoCorrelation>();

    @Override
    public void endVisit(JClassLiteral x, Context ctx) {
      x.getSourceInfo().addCorrelation(factory.by(Literal.CLASS));
      super.endVisit(x, ctx);
    }

    @Override
    public void endVisit(JDeclaredType x, Context ctx) {
      x.getSourceInfo().addCorrelation(factory.by(x));
      super.endVisit(x, ctx);
    }

    @Override
    public void endVisit(JField x, Context ctx) {
      x.getSourceInfo().addCorrelation(factory.by(x));
      super.endVisit(x, ctx);
    }

    @Override
    public void endVisit(JMethod x, Context ctx) {
      x.getSourceInfo().addCorrelation(factory.by(x));
      super.endVisit(x, ctx);
    }

    @Override
    public void endVisit(JNode x, Context ctx) {
      SourceInfo popped = parents.pop();
      assert popped == x.getSourceInfo();
    }

    @Override
    public void endVisit(JProgram x, Context ctx) {
      SourceInfo popped = parents.pop();
      assert popped == null;
    }

    @Override
    public void endVisit(JsniMethodBody x, Context ctx) {
      jsVisitor.accept(x.getFunc());
      super.endVisit(x, ctx);
    }

    @Override
    public void endVisit(JStringLiteral x, Context ctx) {
      x.getSourceInfo().addCorrelation(factory.by(Literal.STRING));
      super.endVisit(x, ctx);
    }

    @Override
    public void endVisit(JFieldRef x, Context ctx) {
      x.getSourceInfo().addCorrelation(factory.by(x.getField()));
      super.endVisit(x, ctx);
    }

    @Override
    public boolean visit(JNode x, Context ctx) {
      SourceInfo info = x.getSourceInfo();
      info = pushAndConvert(info);
      x.setSourceInfo(info);
      return true;
    }

    @Override
    public boolean visit(JProgram x, Context ctx) {
      // Types have no source parent.
      parents.push(null);
      return true;
    }

    protected SourceInfo pushAndConvert(SourceInfo info) {
      if (info instanceof SourceOrigin) {
        SourceOrigin origin = (SourceOrigin) info;
        SourceInfoCorrelation parent = parents.peek();
        if (parent != null) {
          info = new SourceInfoCorrelation(parent, origin);
        } else {
          info = new SourceInfoCorrelation(origin);
        }
      }
      parents.push((SourceInfoCorrelation) info);
      return info;
    }
  }

  public static void exec(JProgram program) {
    new SourceInfoVisitor().accept(program);
  }
}
