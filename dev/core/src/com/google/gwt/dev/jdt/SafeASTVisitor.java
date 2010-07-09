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
package com.google.gwt.dev.jdt;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;

/**
 * Avoids visiting invalid local types due to compile errors or unreachability.
 */
public class SafeASTVisitor extends ASTVisitor {

  @Override
  public final void endVisit(TypeDeclaration typeDecl, BlockScope scope) {
    if (typeDecl.binding == null || typeDecl.binding.constantPoolName() == null) {
      /*
       * Weird case: if JDT determines that this local class is totally
       * uninstantiable, it won't bother allocating a local name.
       */
      return;
    }
    endVisitValid(typeDecl, scope);
  }

  public void endVisitValid(TypeDeclaration typeDecl, BlockScope scope) {
    super.endVisit(typeDecl, scope);
  }

  @Override
  public final boolean visit(TypeDeclaration typeDecl, BlockScope scope) {
    if (typeDecl.binding == null || typeDecl.binding.constantPoolName() == null) {
      /*
       * Weird case: if JDT determines that this local class is totally
       * uninstantiable, it won't bother allocating a local name.
       */
      return false;
    }
    return visitValid(typeDecl, scope);
  }

  public boolean visitValid(TypeDeclaration typeDecl, BlockScope scope) {
    return super.visit(typeDecl, scope);
  }
}
