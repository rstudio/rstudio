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

import com.google.gwt.core.ext.TreeLogger.HelpInfo;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblem;
import org.eclipse.jdt.internal.compiler.problem.ProblemSeverities;
import org.eclipse.jdt.internal.compiler.util.Util;

/**
 * A problem specific to compiling for GWT.
 */
public class GWTProblem extends DefaultProblem {

  static void recordInCud(ASTNode node, CompilationUnitDeclaration cud,
      String message, HelpInfo helpInfo) {
    CompilationResult compResult = cud.compilationResult();
    int[] lineEnds = compResult.getLineSeparatorPositions();
    int startLine = Util.getLineNumber(node.sourceStart(), lineEnds, 0,
        lineEnds.length - 1);
    int startColumn = Util.searchColumnNumber(lineEnds, startLine,
        node.sourceStart());
    DefaultProblem problem = new GWTProblem(compResult.fileName, message,
        node.sourceStart(), node.sourceEnd(), startLine, startColumn, helpInfo);
    compResult.record(problem, cud);
  }

  private HelpInfo helpInfo;

  public GWTProblem(char[] originatingFileName, String message,
      int startPosition, int endPosition, int line, int column,
      HelpInfo helpInfo) {
    super(originatingFileName, message, IProblem.ExternalProblemNotFixable,
        null, ProblemSeverities.Error, startPosition, endPosition, line, column);
    this.helpInfo = helpInfo;
  }

  public HelpInfo getHelpInfo() {
    return helpInfo;
  }

}
