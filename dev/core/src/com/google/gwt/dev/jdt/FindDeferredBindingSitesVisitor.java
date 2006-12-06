/*
 * Copyright 2006 Google Inc.
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
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ast.ClassLiteralAccess;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;

import java.util.Set;

/**
 * Walks a
 * {@link org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration} to
 * find <code>GWT.create()</code> class so that we can eagerly complain about
 * deferred binding problems.
 */
public class FindDeferredBindingSitesVisitor extends ASTVisitor {

  public static final String REBIND_MAGIC_CLASS = "com.google.gwt.core.client.GWT";
  public static final String REBIND_MAGIC_METHOD = "create";

  public FindDeferredBindingSitesVisitor(Set results) {
    this.results = results;
  }

  public void endVisit(MessageSend messageSend, BlockScope scope) {
    final ProblemReporter problemReporter = scope.problemReporter();

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

    Expression[] args = messageSend.arguments;
    if (args.length != 1) {
      problemReporter.abortDueToInternalError(
        "GWT.create() should take exactly one argument", messageSend);
      return;
    }

    Expression arg = args[0];
    if (!(arg instanceof ClassLiteralAccess)) {
      problemReporter.abortDueToInternalError(
        "Only class literals may be used as arguments to GWT.create()",
        messageSend);
      return;
    }

    ClassLiteralAccess cla = (ClassLiteralAccess) arg;
    String typeName = String.valueOf(cla.targetType.readableName());
    results.add(typeName);
  }

  private final Set results;
}
