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

import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;

/**
 * Collects method parameter names.
 */
public class MethodParamCollector {

  private static class Visitor extends MethodVisitor {

    private final MethodArgNamesLookup methodArgs;

    public Visitor(MethodArgNamesLookup methodArgs) {
      this.methodArgs = methodArgs;
    }

    /**
     * Collect information on methods with at least one argument that will be
     * visible in the TypeOracle.
     */
    @Override
    protected boolean interestingMethod(AbstractMethodDeclaration method) {
      if (method.arguments == null || method.arguments.length == 0) {
        return false;
      }
      MethodBinding binding = method.binding;
      if (binding == null || binding.declaringClass.isLocalType()) {
        return false;
      }
      return true;
    }

    @Override
    protected void processMethod(TypeDeclaration typeDecl,
        AbstractMethodDeclaration method, String enclosingType) {
      methodArgs.store(enclosingType, method);
    }
  }

  /**
   * Returns an unmodifiable MethodArgNamesLookup containing the method argument
   * names for the supplied compilation unit.
   *
   * @return MethodArgNamesLookup instance
   */
  public static MethodArgNamesLookup collect(CompilationUnitDeclaration cud, String sourceMapPath) {
    MethodArgNamesLookup methodArgs = new MethodArgNamesLookup();
    new Visitor(methodArgs).collect(cud, sourceMapPath);
    methodArgs.freeze();
    return methodArgs;
  }
}
