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
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.QualifiedAllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the removal of GwtIncompatible annotated classes and members.
 */
public class GwtIncompatiblePreprocessor {
  /**
   * Checks whether GwtIncompatible is in the array of {@code Annotation}.
   *
   * @param annotations an (possible null) array of {@code Annotation}
   * @return {@code true} if there is an annotation of class {@code *.GwtIncompatible} in
   *         array. {@code false} otherwise.
   */
  private static boolean hasGwtIncompatibleAnnotation(Annotation[] annotations) {
    if (annotations == null) {
      return false;
    }
    for (Annotation ann : annotations) {
      String typeName = new String(ann.type.getLastToken());
      if (typeName.equals("GwtIncompatible")) {
        return true;
      }
    }
    return false;
  }

  /**
   * Process all members of a type to remove any @GwtIncompatible one.
   */
  private static void processMembers(TypeDeclaration tyDecl) {
    processTypes(tyDecl.memberTypes);
    processMethods(tyDecl);
    processFields(tyDecl);
  }

  /**
   * Remove @GwtIncompatible classes, members and recursively remove @GwtIncompatible
   * inner classes. A dummy empty stub is retained for @GwtIncompatible classes.
   */
  private static void processTypes(TypeDeclaration[] types) {
    if (types == null) {
      return;
    }
    for (TypeDeclaration tyDecl : types) {
      if (!hasGwtIncompatibleAnnotation(tyDecl.annotations)) {
        processMembers(tyDecl);
      } else {
        // Leave the empty class.
        stripAllMembers(tyDecl);
      }
    }
  }

  /**
   * Modifies the methods array of type {@code tyDecl} to remove any GwtIncompatible methods.
   */
  private static void processMethods(TypeDeclaration tyDecl) {
    if (tyDecl.methods == null) {
      return;
    }

    List<AbstractMethodDeclaration> newMethods = new ArrayList<AbstractMethodDeclaration>();
    for (AbstractMethodDeclaration methodDecl : tyDecl.methods) {
      if (!hasGwtIncompatibleAnnotation(methodDecl.annotations)) {
        newMethods.add(methodDecl);
      }
    }

    if (newMethods.size() != tyDecl.methods.length) {
      tyDecl.methods = newMethods.toArray(new AbstractMethodDeclaration[newMethods.size()]);
    }
  }

  /**
   * Modifies the fields array of type {@code tyDecl} to remove any GwtIncompatible fields.
   */
  private static void processFields(TypeDeclaration tyDecl) {
    if (tyDecl.fields == null) {
      return;
    }

    List<FieldDeclaration> newFields = new ArrayList<FieldDeclaration>();
    for (FieldDeclaration fieldDecl : tyDecl.fields) {
      if (!hasGwtIncompatibleAnnotation(fieldDecl.annotations)) {
        newFields.add(fieldDecl);
      }
    }

    if (newFields.size() != tyDecl.fields.length) {
      tyDecl.fields = newFields.toArray(new FieldDeclaration[newFields.size()]);
    }
  }

  /**
   * Process inner classes, methods and fields from all anonymous inner classes.
   *
   * <p>Anonymous inner classes are represented inside expressions. Traverse the JDT AST removing
   * anonymous inner classes in one go.
   */
  private static void processAllAnonymousInnerClasses(CompilationUnitDeclaration cud) {
    ASTVisitor visitor = new ASTVisitor() {
      // Anonymous types are represented within the AST expression that creates the it.
      @Override
      public void endVisit(QualifiedAllocationExpression qualifiedAllocationExpression,
          BlockScope scope) {
        if (qualifiedAllocationExpression.anonymousType != null) {
          processMembers(qualifiedAllocationExpression.anonymousType);
        }
      }
    };
    cud.traverse(visitor, cud.scope);
  }

  /**
   * Removes all members of a class to leave it as an empty stub.
   */
  private static void stripAllMembers(TypeDeclaration tyDecl) {
    tyDecl.superclass = null;
    tyDecl.superInterfaces = new TypeReference[0];
    tyDecl.annotations = new Annotation[0];
    tyDecl.methods = new AbstractMethodDeclaration[0];
    tyDecl.memberTypes = new TypeDeclaration[0];
    tyDecl.fields = new FieldDeclaration[0];
    if (TypeDeclaration.kind(tyDecl.modifiers) != TypeDeclaration.INTERFACE_DECL) {
      // Create a default constructor so that the class is proper.
      ConstructorDeclaration constructor = tyDecl.createDefaultConstructor(true, true);
      // Mark only constructor as private so that it can not be instantiated.
      constructor.modifiers = ClassFileConstants.AccPrivate;
      // Clear a bit that is used for marking the constructor as default as it makes JDT
      // assume that the constructor is public.
      constructor.bits &= ~ASTNode.IsDefaultConstructor;
      // Mark the class as final so that it can not be extended.
      tyDecl.modifiers |= ClassFileConstants.AccFinal;
      tyDecl.modifiers &= ~ClassFileConstants.AccAbstract;
    }
  }

  /**
   * Preprocess the compilation unit to remove @GwtIncompatible classes and members.
   */
  public static void preproccess(CompilationUnitDeclaration cud) {
    processTypes(cud.types);
    processAllAnonymousInnerClasses(cud);
  }
}
