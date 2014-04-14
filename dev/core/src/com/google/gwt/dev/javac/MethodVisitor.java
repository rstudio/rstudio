/*
 * Copyright 2010 Google Inc.
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

import com.google.gwt.dev.jdt.SafeASTVisitor;
import com.google.gwt.dev.util.Name.InternalName;

import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;

/**
 * Base class of things that walk methods in a CUD and collect things about
 * interesting methods.
 */
public abstract class MethodVisitor {

  /**
   * Gets a unique name for this method, including its signature.
   */
  protected static String getMemberSignature(AbstractMethodDeclaration method) {
    String name = String.valueOf(method.selector);
    StringBuilder sb = new StringBuilder();
    sb.append(name);
    sb.append("(");
    if (method.arguments != null) {
      for (Argument param : method.arguments) {
        sb.append(param.binding.type.signature());
      }
    }
    sb.append(")");
    return sb.toString();
  }

  /**
   * Collect data about interesting methods in one compilation unit.
   */
  public void collect(final CompilationUnitDeclaration cud, String sourceMapPath) {
    cud.traverse(new SafeASTVisitor() {
      @Override
      public void endVisit(TypeDeclaration type, ClassScope scope) {
        collectMethods(type);
      }

      @Override
      public void endVisit(TypeDeclaration type, CompilationUnitScope scope) {
        collectMethods(type);
      }

      @Override
      public void endVisitValid(TypeDeclaration type, BlockScope scope) {
        collectMethods(type);
      }
    }, cud.scope);
  }

  /**
   * Provided by a subclass to return true if this method should be processed.
   * This is separate since some extra work is performed in order to call
   * {@link #processMethod}.
   *
   * @param method
   * @return true if processMethod should be called on this method
   */
  protected abstract boolean interestingMethod(AbstractMethodDeclaration method);

  /**
   * Provided by a subclass to process a method definition. Methods which have
   * no name are not passed to this method, even if {@link #interestingMethod}
   * returns true.
   *
   * @param typeDecl
   * @param method
   * @param enclosingType
   * @param loc
   */
  protected abstract void processMethod(TypeDeclaration typeDecl,
      AbstractMethodDeclaration method, String enclosingType);

  /**
   * Collect data about interesting methods on a particular type in a
   * compilation unit.
   *
   * @param cud
   * @param typeDecl
   */
  private void collectMethods(TypeDeclaration typeDecl) {
    AbstractMethodDeclaration[] methods = typeDecl.methods;
    if (methods == null) {
      return;
    }

    // Lazy initialize these when an interesting method is actually hit.
    String enclosingType = null;
    boolean lazyInitialized = false;

    for (AbstractMethodDeclaration method : methods) {
      if (!interestingMethod(method)) {
        continue;
      }

      if (!lazyInitialized) {
        enclosingType = InternalName.toBinaryName(String.valueOf(typeDecl.binding.constantPoolName()));
        lazyInitialized = true;
      }
      processMethod(typeDecl, method, enclosingType);
    }
  }
}
