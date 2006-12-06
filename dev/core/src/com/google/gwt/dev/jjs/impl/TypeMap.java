// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JType;

import org.eclipse.jdt.internal.compiler.lookup.ArrayBinding;
import org.eclipse.jdt.internal.compiler.lookup.BaseTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.Binding;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Contains the list of the top-level and array types.
 */
public class TypeMap {

  public TypeMap(JProgram program) {
    fProgram = program;
  }

  public JNode get(Binding binding) {
    JNode result = internalGet(binding);
    if (result == null) {
      throw new RuntimeException("Failed to get JNode");
    }
    return result;
  }

  public JProgram getProgram() {
    return fProgram;
  }

  public void put(Binding binding, JNode to) {
    if (binding == null) {
      throw new InternalCompilerException("Trying to put null into typeMap.");
    }
    
    Object old = fCrossRefMap.put(binding, to);
    assert (old == null);
  }

  public JNode tryGet(Binding binding) {
    return internalGet(binding);
  }

  private JNode internalGet(Binding binding) {
    JNode cached = (JNode) fCrossRefMap.get(binding);
    if (cached != null) {
      // Already seen this one.
      return cached;
    } else if (binding instanceof BaseTypeBinding) {
      BaseTypeBinding baseTypeBinding = (BaseTypeBinding) binding;
      switch (baseTypeBinding.id) {
        case BaseTypeBinding.T_void:
          return fProgram.getTypeVoid();
        case BaseTypeBinding.T_boolean:
          return fProgram.getTypePrimitiveBoolean();
        case BaseTypeBinding.T_char:
          return fProgram.getTypePrimitiveChar();
        case BaseTypeBinding.T_byte:
          return fProgram.getTypePrimitiveByte();
        case BaseTypeBinding.T_short:
          return fProgram.getTypePrimitiveShort();
        case BaseTypeBinding.T_int:
          return fProgram.getTypePrimitiveInt();
        case BaseTypeBinding.T_long:
          return fProgram.getTypePrimitiveLong();
        case BaseTypeBinding.T_float:
          return fProgram.getTypePrimitiveFloat();
        case BaseTypeBinding.T_double:
          return fProgram.getTypePrimitiveDouble();
      }
    } else if (binding instanceof ArrayBinding) {
      ArrayBinding arrayBinding = (ArrayBinding) binding;

      // Compute the JType for the leaf type
      JType leafType = (JType) get(arrayBinding.leafComponentType);

      // Don't create a new JArrayType; use TypeMap to get the singleton
      // instance
      JArrayType arrayType = fProgram.getTypeArray(leafType,
        arrayBinding.dimensions);

      return arrayType;
    }
    return null;
  }

  /**
   * Maps Eclipse AST nodes to our JNodes.
   */
  private final Map/* <Binding, JNode> */fCrossRefMap = new IdentityHashMap();

  /**
   * Centralizes creation and singleton management.
   */
  private final JProgram fProgram;

}
