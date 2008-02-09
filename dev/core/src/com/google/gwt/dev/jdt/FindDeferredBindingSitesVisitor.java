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
package com.google.gwt.dev.jdt;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ast.ClassLiteralAccess;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblem;
import org.eclipse.jdt.internal.compiler.problem.ProblemSeverities;
import org.eclipse.jdt.internal.compiler.util.Util;

import java.util.Map;

/**
 * Walks a
 * {@link org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration} to
 * find <code>GWT.create()</code> class so that we can eagerly complain about
 * deferred binding problems.
 */
public class FindDeferredBindingSitesVisitor extends ASTVisitor {

  /**
   * Information about the site at which a rebind request was found, used to
   * report problems.
   */
  public static class DeferredBindingSite {
    public final MessageSend messageSend;

    public final Scope scope;

    public DeferredBindingSite(MessageSend messageSend, Scope scope) {
      this.messageSend = messageSend;
      this.scope = scope;
    }
  }

  public static final String REBIND_MAGIC_CLASS = "com.google.gwt.core.client.GWT";
  public static final String REBIND_MAGIC_METHOD = "create";

  public static void reportRebindProblem(DeferredBindingSite site,
      String message) {
    MessageSend messageSend = site.messageSend;
    Scope scope = site.scope;
    CompilationResult compResult = scope.compilationUnitScope().referenceContext().compilationResult();
    int[] lineEnds = compResult.getLineSeparatorPositions();
    int startLine = Util.getLineNumber(messageSend.sourceStart(), lineEnds, 0,
        lineEnds.length - 1);
    int startColumn = Util.searchColumnNumber(lineEnds, startLine,
        messageSend.sourceStart());
    DefaultProblem problem = new DefaultProblem(compResult.fileName, message,
        IProblem.ExternalProblemNotFixable, null, ProblemSeverities.Error,
        messageSend.sourceStart, messageSend.sourceEnd, startLine, startColumn);
    compResult.record(problem, scope.referenceContext());
  }

  private final Map results;

  public FindDeferredBindingSitesVisitor(Map requestedTypes) {
    this.results = requestedTypes;
  }

  public void endVisit(MessageSend messageSend, BlockScope scope) {
    if (messageSend.binding == null) {
      // Some sort of problem.
      //
      return;
    }

    String methodName = String.valueOf(messageSend.selector);
    if (!methodName.equals(REBIND_MAGIC_METHOD)) {
      // Not the create() method.
      //
      return;
    }

    char[][] targetClass = messageSend.binding.declaringClass.compoundName;
    String targetClassName = CharOperation.toString(targetClass);
    if (!targetClassName.equals(REBIND_MAGIC_CLASS)) {
      // Not being called on the Rebind class.
      return;
    }

    DeferredBindingSite site = new DeferredBindingSite(messageSend, scope);

    Expression[] args = messageSend.arguments;
    if (args.length != 1) {
      reportRebindProblem(site, "GWT.create() should take exactly one argument");
      return;
    }

    Expression arg = args[0];
    if (!(arg instanceof ClassLiteralAccess)) {
      reportRebindProblem(site,
          "Only class literals may be used as arguments to GWT.create()");
      return;
    }

    ClassLiteralAccess cla = (ClassLiteralAccess) arg;
    String typeName = String.valueOf(cla.targetType.readableName());
    if (!results.containsKey(typeName)) {
      results.put(typeName, site);
    }
  }
}
