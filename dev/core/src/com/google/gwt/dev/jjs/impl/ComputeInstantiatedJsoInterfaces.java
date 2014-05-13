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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JCastOperation;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JNullType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVisitor;

import java.util.HashSet;
import java.util.Set;

/**
 * Record all Cast operations on JSOs as instantiations. This must run before
 * {@link com.google.gwt.dev.jjs.impl.ImplementCastsAndTypeChecks}.
 */
public class ComputeInstantiatedJsoInterfaces {
  class InstantiatedJsoInterfacesCollector extends JVisitor {
    public void endVisit(JCastOperation x, Context ctx) {
      JType toType = x.getCastType();

      if (toType instanceof JReferenceType && !(toType instanceof JNullType)) {
        JReferenceType refType = ((JReferenceType) toType).getUnderlyingType();
        if (program.typeOracle.willCrossCastLikeJso(refType) ||
            program.typeOracle.isOrExtendsJsType(toType, true)) {
          instantiateJsoInterface(refType);
        }
      }
    }
  }

  private void instantiateJsoInterface(JReferenceType toType) {
    if (instantiatedJsoTypes.add(toType)) {
      if (program.typeOracle.getSingleJsoImpl(toType) != null) {
        // rescuing an Interface via Cast, we record the JSO implementing it
        instantiateJsoInterface(program.typeOracle.getSingleJsoImpl(toType));
      }
      // if it's a class, and the superType is JSO, rescue it too
      if (toType instanceof JClassType) {
        JClassType superType = ((JClassType) toType).getSuperClass();
        if (superType != null && program.isJavaScriptObject(superType)) {
          instantiateJsoInterface(superType);
        }
      }
      // if we extend another JsType, or Interface with JSO implementation, rescue it
      for (JInterfaceType intf : ((JDeclaredType) toType).getImplements()) {
        if (intf.isJsType() || program.typeOracle.getSingleJsoImpl(intf) != null) {
          instantiateJsoInterface(intf);
        }
      }
    }
  }

  private final JProgram program;
  private final Set<JReferenceType> instantiatedJsoTypes = new HashSet<JReferenceType>();

  public static void exec(JProgram program) {
    new ComputeInstantiatedJsoInterfaces(program).execImpl();
  }

  private ComputeInstantiatedJsoInterfaces(JProgram program) {
    this.program = program;
  }

  private void execImpl() {
    InstantiatedJsoInterfacesCollector replacer = new InstantiatedJsoInterfacesCollector();
    replacer.accept(program);
    program.typeOracle.setInstantiatedJsoTypesViaCast(instantiatedJsoTypes);
  }
}
