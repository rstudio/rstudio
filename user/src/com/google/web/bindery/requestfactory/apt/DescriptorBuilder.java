/*
 * Copyright 2011 Google Inc.
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
package com.google.web.bindery.requestfactory.apt;

import com.google.gwt.core.ext.typeinfo.JniConstants;
import com.google.gwt.dev.util.Name.BinaryName;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.SimpleTypeVisitor6;

/**
 * Builds descriptors from TypeMirrors for both simple types and methods. Used
 * by {@link DeobfuscatorBuilder} to construct client-to-server method mappings.
 */
class DescriptorBuilder extends SimpleTypeVisitor6<String, State> {

  /**
   * Arrays aren't actually used anywhere in RequestFactory, but it's trivial to
   * implement and might be useful later on.
   */
  @Override
  public String visitArray(ArrayType x, State state) {
    return "[" + x.getComponentType().accept(this, state);
  }

  @Override
  public String visitDeclared(DeclaredType x, State state) {
    return "L"
        + BinaryName.toInternalName(state.elements.getBinaryName((TypeElement) x.asElement())
            .toString()) + ";";
  }

  /**
   * Only generates the method descriptor, which does not include the method's
   * name.
   */
  @Override
  public String visitExecutable(ExecutableType x, State state) {
    StringBuilder sb = new StringBuilder();
    sb.append("(");
    for (TypeMirror param : x.getParameterTypes()) {
      sb.append(param.accept(this, state));
    }
    sb.append(")");
    sb.append(x.getReturnType().accept(this, state));
    return sb.toString();
  }

  @Override
  public String visitNoType(NoType x, State state) {
    if (x.getKind().equals(TypeKind.VOID)) {
      return "V";
    }
    // The mythical NONE or PACKAGE type
    return super.visitNoType(x, state);
  }

  @Override
  public String visitPrimitive(PrimitiveType x, State state) {
    switch (x.getKind()) {
      case BOOLEAN:
        return String.valueOf(JniConstants.DESC_BOOLEAN);
      case BYTE:
        return String.valueOf(JniConstants.DESC_BYTE);
      case CHAR:
        return String.valueOf(JniConstants.DESC_CHAR);
      case DOUBLE:
        return String.valueOf(JniConstants.DESC_DOUBLE);
      case FLOAT:
        return String.valueOf(JniConstants.DESC_FLOAT);
      case INT:
        return String.valueOf(JniConstants.DESC_INT);
      case LONG:
        return String.valueOf(JniConstants.DESC_LONG);
      case SHORT:
        return String.valueOf(JniConstants.DESC_SHORT);
    }
    return super.visitPrimitive(x, state);
  }

  @Override
  public String visitTypeVariable(TypeVariable x, State state) {
    return state.types.erasure(x).accept(this, state);
  }

  @Override
  public String visitWildcard(WildcardType x, State state) {
    return state.types.erasure(x).accept(this, state);
  }

  @Override
  protected String defaultAction(TypeMirror x, State state) {
    throw new RuntimeException("Unhandled type: " + x.toString());
  }
}
