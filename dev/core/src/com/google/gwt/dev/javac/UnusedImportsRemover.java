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
package com.google.gwt.dev.javac;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.ArrayQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ArrayTypeReference;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ImportReference;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedSingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ThisReference;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Removes unused imports from CompilationUnitDeclarations.
 *
 * Needed after running GwtIncompatibleRemover to remove imports to GwtIncompatible elements.
 */
public class UnusedImportsRemover {

  private Set<String> usedNames = new HashSet<String>();

  /**
   * Accumulate all names that can be brought in by imports.
   *
   * This is a conservative pass, i.e. it might leave some unused imports.
   */
  private class AccumulateNamesVisitor extends ASTVisitor {
    public void endVisit(
        SingleNameReference singleNameReference,
        BlockScope scope) {
      addName(singleNameReference);
    }

    public void endVisit(
        SingleNameReference singleNameReference,
        ClassScope scope) {
      addName(singleNameReference);
    }

    public void endVisit(
        SingleTypeReference singleTypeReference,
        BlockScope scope) {
      addName(singleTypeReference);
    }

    public void endVisit(
        SingleTypeReference singleTypeReference,
        ClassScope scope) {
      addName(singleTypeReference);
    }

    public void endVisit(MessageSend messageSend, BlockScope scope) {
      if (messageSend.receiver instanceof ThisReference) {
        usedNames.add(new String(messageSend.selector));
       }
    }

    public void endVisit(ArrayTypeReference arrayTypeReference, BlockScope scope) {
      addName(arrayTypeReference);
    }

    public void endVisit(ArrayTypeReference arrayTypeReference, ClassScope scope) {
      addName(arrayTypeReference);
    }

    public void endVisit(
        ArrayQualifiedTypeReference arrayQualifiedTypeReference,
        BlockScope scope) {
      addName(arrayQualifiedTypeReference);
    }

    public void endVisit(
        ArrayQualifiedTypeReference arrayQualifiedTypeReference,
        ClassScope scope) {
      addName(arrayQualifiedTypeReference);
    }

    public void endVisit(ParameterizedQualifiedTypeReference parameterizedQualifiedTypeReference,
        BlockScope scope) {
      addName(parameterizedQualifiedTypeReference);
    }

    public void endVisit(ParameterizedQualifiedTypeReference parameterizedQualifiedTypeReference,
        ClassScope scope) {
      addName(parameterizedQualifiedTypeReference);
    }

    public void endVisit(ParameterizedSingleTypeReference parameterizedSingleTypeReference,
        BlockScope scope) {
      addName(parameterizedSingleTypeReference);
    }

    public void endVisit(ParameterizedSingleTypeReference parameterizedSingleTypeReference,
        ClassScope scope) {
      addName(parameterizedSingleTypeReference);
    }

    public void endVisit(
        QualifiedTypeReference qualifiedTypeReference,
        BlockScope scope) {
      addName(qualifiedTypeReference);
    }

    public void endVisit(
        QualifiedTypeReference qualifiedTypeReference,
        ClassScope scope) {
      addName(qualifiedTypeReference);
    }

    public void endVisit(
        QualifiedNameReference qualifiedNameReference,
        BlockScope scope) {
      addName(qualifiedNameReference);
    }

    public void endVisit(
        QualifiedNameReference qualifiedNameReference,
        ClassScope scope) {
      addName(qualifiedNameReference);
    }

    public void addName(QualifiedNameReference reference) {
      usedNames.add(new String(reference.tokens[0]));
    }

    public void addName(QualifiedTypeReference reference) {
      usedNames.add(new String(reference.tokens[0]));
    }

    public void addName(SingleTypeReference reference) {
      usedNames.add(new String(reference.token));
    }

    public void addName(SingleNameReference reference) {
      usedNames.add(new String(reference.token));
    }
  }

  public static void exec(CompilationUnitDeclaration cud) {
    new UnusedImportsRemover().execImpl(cud);
  }

  void execImpl(CompilationUnitDeclaration cud) {
    if (cud.imports == null) {
      return;
    }

    AccumulateNamesVisitor astVisitor = new AccumulateNamesVisitor();

    if (cud.types != null) {
      for (TypeDeclaration typeDecl : cud.types) {
        typeDecl.traverse(astVisitor, cud.scope);
      }
    }

    // for some reason JDT does not traverse package annotations even if the traversal started at
    // the Compilation unit declaration. Hence we do it manually.
    if (cud.currentPackage != null && cud.currentPackage.annotations != null) {
      for (Annotation annotation : cud.currentPackage.annotations) {
        if (annotation.type instanceof SingleTypeReference) {
          astVisitor.addName((SingleTypeReference) annotation.type);
        } else if (annotation.type instanceof QualifiedTypeReference) {
          astVisitor.addName((QualifiedTypeReference) annotation.type);
        }
      }
    }

    List<ImportReference> newImports = new ArrayList<ImportReference>();
    for (ImportReference importRef : cud.imports) {
      String importName =
          new String(importRef.getImportName()[importRef.getImportName().length - 1]);
      if (importName.equals("*") ||
          // very hacky it seems that this is the only way
          // to notice a import static blah.Blah.*;
          importRef.trailingStarPosition > 0  ||
          usedNames.contains(importName)) {
        // Either a * or a possible reference, so keep it.
        newImports.add(importRef);
      }
    }
    if (newImports.size() != cud.imports.length) {
      cud.imports = newImports.toArray(new ImportReference[newImports.size()]);
    }
  }
}
