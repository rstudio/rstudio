/*
 * Copyright 2012 Google Inc.
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

import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JDeclarationStatement;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JThisRef;
import com.google.gwt.dev.jjs.ast.JValueLiteral;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.js.JsniMethodBody;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.thirdparty.guava.common.base.Charsets;
import com.google.gwt.thirdparty.guava.common.collect.HashMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;
import com.google.gwt.thirdparty.guava.common.collect.Sets;
import com.google.gwt.thirdparty.guava.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * Build up a collection of all instrumentable lines, useful for generating
 * coverage reports.
 */
public class BaselineCoverageGatherer {
  public static Multimap<String, Integer> exec(JProgram jProgram) {
    return new BaselineCoverageGatherer(jProgram, getCoveredSourceFiles()).execImpl();
  }

  private static Set<String> getCoveredSourceFiles() {
    String filename = System.getProperty("gwt.coverage");
    File instrumentationFile = new File(filename);
    try {
      return Sets.newHashSet(Files.readLines(instrumentationFile, Charsets.UTF_8));
    } catch (IOException e) {
      throw new InternalCompilerException("Could not open " + filename, e);
    }
  }

  private Multimap<String, Integer> instrumentableLines = HashMultimap.create();
  private Set<String> instrumentedFiles;
  private JProgram jProgram;

  private BaselineCoverageGatherer(JProgram jProgram, Set<String> instrumentedFiles) {
    this.jProgram = jProgram;
    this.instrumentedFiles = instrumentedFiles;
  }

  private void cover(SourceInfo info) {
    if (instrumentedFiles.contains(info.getFileName())) {
      instrumentableLines.put(info.getFileName(), info.getStartLine());
    }
  }

  private Multimap<String, Integer> execImpl() {
    /**
     * Figure out which lines are executable. This is mostly straightforward
     * except that we have to avoid some synthetic nodes introduced earlier,
     * otherwise e.g. class declarations will be visited.
     */
    new JVisitor() {
      @Override public void endVisit(JMethodCall x, Context ctx) {
        // this is a bit of a hack. The compiler inserts no-arg super calls, but
        // there isn't really a way to detect that they're synthetic, and the
        // strategy below of comparing source info with that of the enclosing type
        // doesn't work because the enclosing type is set to be that of the superclass.
        if (x.getTarget().isSynthetic() || x.toSource().equals("super()")) {
          return;
        }
        endVisit((JExpression) x, ctx);
      }

      @Override public void endVisit(JThisRef x, Context ctx) {
        if (x.getSourceInfo().equals(x.getClassType().getSourceInfo())) {
          return;
        }
        endVisit((JExpression) x, ctx);
      }

      @Override public void endVisit(JClassLiteral x, Context ctx) {
        if (x.getSourceInfo().equals(x.getRefType().getSourceInfo())) {
          return;
        }
        endVisit((JExpression) x, ctx);
      }

      @Override public void endVisit(JExpression x, Context ctx) {
        cover(x.getSourceInfo());
      }

      @Override public void endVisit(JsniMethodBody x, Context ctx) {
        new CoverageVisitor(instrumentedFiles) {
          @Override public void endVisit(JsExpression x, JsContext ctx) {
            cover(x.getSourceInfo());
          }
        }.accept(x.getFunc());
      }

      // don't instrument fields whose initializers are literals, because (1) CoverageVisitor
      // doesn't visit literals because it can introduce syntax errors in some cases, and (2) it's
      // consistent with other coverage tools, e.g. Emma.
      @Override public boolean visit(JDeclarationStatement x, Context ctx) {
        return !(x.getInitializer() instanceof JValueLiteral &&
            x.getVariableRef().getTarget() instanceof JField);
      }

      // don't instrument method call arguments; we can get weird coverage results when a call is
      // spread over several lines
      @Override public boolean visit(JMethodCall x, Context ctx) {
        return false;
      }
    }.accept(jProgram);
    return instrumentableLines;
  }
}