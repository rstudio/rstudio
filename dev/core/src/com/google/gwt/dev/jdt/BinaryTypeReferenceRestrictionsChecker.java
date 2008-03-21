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

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.MarkerAnnotation;
import org.eclipse.jdt.internal.compiler.ast.NormalAnnotation;
import org.eclipse.jdt.internal.compiler.ast.SingleMemberAnnotation;
import org.eclipse.jdt.internal.compiler.lookup.BinaryTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblem;
import org.eclipse.jdt.internal.compiler.problem.ProblemSeverities;
import org.eclipse.jdt.internal.compiler.util.Util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Check a {@link CompilationUnitDeclaration} for references to binary types
 * outside the context of an annotation.
 */
class BinaryTypeReferenceRestrictionsChecker {
  /**
   * Records the location from which a {@link BinaryTypeBinding} is referenced.
   */
  static class BinaryTypeReferenceSite {
    private final Expression expression;
    private final BinaryTypeBinding binaryTypeBinding;

    BinaryTypeReferenceSite(Expression expression,
        BinaryTypeBinding binaryTypeBinding) {
      this.expression = expression;
      this.binaryTypeBinding = binaryTypeBinding;
    }

    public BinaryTypeBinding getBinaryTypeBinding() {
      return binaryTypeBinding;
    }

    public Expression getExpression() {
      return expression;
    }
  }

  /**
   * Visits a {@link CompilationUnitDeclaration} and records all expressions
   * which use a {@link BinaryTypeBinding} that are not part of an annotation
   * context.
   */
  static class BinaryTypeReferenceVisitor extends TypeRefVisitor {
    private final List<BinaryTypeReferenceSite> binaryTypeReferenceSites;

    public BinaryTypeReferenceVisitor(
        List<BinaryTypeReferenceSite> binaryTypeReferenceSites) {
      this.binaryTypeReferenceSites = binaryTypeReferenceSites;
    }

    @Override
    public boolean visit(MarkerAnnotation annotation, BlockScope scope) {
      // Ignore annotations
      return false;
    }

    @Override
    public boolean visit(NormalAnnotation annotation, BlockScope scope) {
      // Ignore annotations
      return false;
    }

    @Override
    public boolean visit(SingleMemberAnnotation annotation, BlockScope scope) {
      // Ignore annotations
      return false;
    }

    @Override
    protected void onBinaryTypeRef(BinaryTypeBinding binding,
        CompilationUnitDeclaration unitOfReferrer, Expression expression) {
      binaryTypeReferenceSites.add(new BinaryTypeReferenceSite(expression,
          binding));
    }

    @Override
    protected void onTypeRef(SourceTypeBinding referencedType,
        CompilationUnitDeclaration unitOfReferrer) {
      // do nothing
    }
  }

  /**
   * Scans a {@link CompilationUnitDeclaration} for expressions that use
   * {@link BinaryTypeBinding}s outside the context of an annotation. An error
   * is reported against the {@link CompilationUnitDeclaration} for the first
   * instance of each unique {@link BinaryTypeBinding}.
   */
  public static void check(CompilationUnitDeclaration cud) {
    List<BinaryTypeReferenceSite> binaryTypeReferenceSites = findInvalidBinaryTypeReferenceSites(cud);
    Set<BinaryTypeBinding> invalidBindaryTypeBindings = new HashSet<BinaryTypeBinding>();

    for (BinaryTypeReferenceSite binaryTypeReferenceSite : binaryTypeReferenceSites) {
      BinaryTypeBinding binaryTypeBinding = binaryTypeReferenceSite.getBinaryTypeBinding();
      if (invalidBindaryTypeBindings.contains(binaryTypeBinding)) {
        continue;
      }
      invalidBindaryTypeBindings.add(binaryTypeBinding);

      String qualifiedTypeName = binaryTypeBinding.debugName();
      String error = formatBinaryTypeRefErrorMessage(qualifiedTypeName);

      recordError(cud, binaryTypeReferenceSite.getExpression(), error);
    }
  }

  static List<BinaryTypeReferenceSite> findInvalidBinaryTypeReferenceSites(
      CompilationUnitDeclaration cud) {
    List<BinaryTypeReferenceSite> binaryTypeReferenceSites = new ArrayList<BinaryTypeReferenceSite>();
    BinaryTypeReferenceVisitor binaryTypeReferenceVisitor = new BinaryTypeReferenceVisitor(
        binaryTypeReferenceSites);
    cud.traverse(binaryTypeReferenceVisitor, cud.scope);
    return binaryTypeReferenceSites;
  }

  static String formatBinaryTypeRefErrorMessage(String qualifiedTypeName) {
    return "No source code is available for type " + qualifiedTypeName
        + "; did you forget to inherit a required module?";
  }

  static void recordError(CompilationUnitDeclaration cud, ASTNode node,
      String error) {
    CompilationResult compResult = cud.compilationResult();
    int[] lineEnds = compResult.getLineSeparatorPositions();
    int startLine = Util.getLineNumber(node.sourceStart(), lineEnds, 0,
        lineEnds.length - 1);
    int startColumn = Util.searchColumnNumber(lineEnds, startLine,
        node.sourceStart());
    DefaultProblem problem = new DefaultProblem(compResult.fileName, error,
        IProblem.ExternalProblemNotFixable, null, ProblemSeverities.Error,
        node.sourceStart(), node.sourceEnd(), startLine, startColumn);
    compResult.record(problem, cud);
  }

  private BinaryTypeReferenceRestrictionsChecker() {
    // Not instantiable
  }
}
