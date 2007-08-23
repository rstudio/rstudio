/*
 * Copyright 2007 Google Inc.
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

import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JType;

import org.eclipse.jdt.internal.compiler.lookup.ArrayBinding;
import org.eclipse.jdt.internal.compiler.lookup.BaseTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.ParameterizedFieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.ParameterizedMethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ParameterizedTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeVariableBinding;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Contains the list of the top-level and array types.
 */
public class TypeMap {

  /**
   * Maps Eclipse AST nodes to our JNodes.
   */
  private final Map/* <Binding, JNode> */crossRefMap = new IdentityHashMap();

  /**
   * Centralizes creation and singleton management.
   */
  private final JProgram program;

  public TypeMap(JProgram program) {
    this.program = program;
  }

  public JNode get(Binding binding) {
    if (binding instanceof TypeVariableBinding) {
      TypeVariableBinding tvb = (TypeVariableBinding) binding;
      return get(tvb.erasure());
    } else if (binding instanceof ParameterizedTypeBinding) {
      ParameterizedTypeBinding ptb = (ParameterizedTypeBinding) binding;
      return get(ptb.erasure());
    } else if (binding instanceof ParameterizedMethodBinding) {
      ParameterizedMethodBinding pmb = (ParameterizedMethodBinding) binding;
      return get(pmb.original());
    } else if (binding instanceof ParameterizedFieldBinding) {
      ParameterizedFieldBinding pfb = (ParameterizedFieldBinding) binding;
      return get(pfb.original());
    }
    JNode result = internalGet(binding);
    if (result == null) {
      InternalCompilerException ice = new InternalCompilerException(
          "Failed to get JNode");
      ice.addNode(binding.getClass().getName(), binding.toString(), null);
      throw ice;
    }
    return result;
  }

  public JProgram getProgram() {
    return program;
  }

  public void put(Binding binding, JNode to) {
    if (binding == null) {
      throw new InternalCompilerException("Trying to put null into typeMap.");
    }

    Object old = crossRefMap.put(binding, to);
    assert (old == null);
  }

  public JNode tryGet(Binding binding) {
    return internalGet(binding);
  }

  private JNode internalGet(Binding binding) {
    JNode cached = (JNode) crossRefMap.get(binding);
    if (cached != null) {
      // Already seen this one.
      return cached;
    } else if (binding instanceof BaseTypeBinding) {
      BaseTypeBinding baseTypeBinding = (BaseTypeBinding) binding;
      // see org.eclipse.jdt.internal.compiler.lookup.TypeIds constants
      switch (baseTypeBinding.id) {
        case BaseTypeBinding.T_undefined:
          return null;
        case BaseTypeBinding.T_JavaLangObject:
          // here for consistency, should already be cached
          return program.getTypeJavaLangObject();
        case BaseTypeBinding.T_char:
          return program.getTypePrimitiveChar();
        case BaseTypeBinding.T_byte:
          return program.getTypePrimitiveByte();
        case BaseTypeBinding.T_short:
          return program.getTypePrimitiveShort();
        case BaseTypeBinding.T_boolean:
          return program.getTypePrimitiveBoolean();
        case BaseTypeBinding.T_void:
          return program.getTypeVoid();
        case BaseTypeBinding.T_long:
          return program.getTypePrimitiveLong();
        case BaseTypeBinding.T_double:
          return program.getTypePrimitiveDouble();
        case BaseTypeBinding.T_float:
          return program.getTypePrimitiveFloat();
        case BaseTypeBinding.T_int:
          return program.getTypePrimitiveInt();
        case BaseTypeBinding.T_JavaLangString:
          // here for consistency, should already be cached
          return program.getTypeJavaLangString();
        case BaseTypeBinding.T_null:
          return program.getTypeNull();
        default:
          return null;
      }
    } else if (binding instanceof ArrayBinding) {
      ArrayBinding arrayBinding = (ArrayBinding) binding;

      // Compute the JType for the leaf type
      JType leafType = (JType) get(arrayBinding.leafComponentType);

      // Don't create a new JArrayType; use TypeMap to get the singleton
      // instance
      JArrayType arrayType = program.getTypeArray(leafType,
          arrayBinding.dimensions);

      return arrayType;
    } else {
      return null;
    }
  }

}
