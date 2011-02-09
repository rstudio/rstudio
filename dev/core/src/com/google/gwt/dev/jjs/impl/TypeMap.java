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

import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.ast.JConstructor;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.util.collect.IdentityHashSet;

import org.eclipse.jdt.internal.compiler.lookup.ArrayBinding;
import org.eclipse.jdt.internal.compiler.lookup.BaseTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.BinaryTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ParameterizedFieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.ParameterizedMethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ParameterizedTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;
import org.eclipse.jdt.internal.compiler.lookup.TypeVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.WildcardBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains the list of the top-level and array types.
 */
public class TypeMap {

  /**
   * Maps Eclipse AST nodes to our JNodes.
   */
  private final Map<Binding, JNode> crossRefMap = new IdentityHashMap<Binding, JNode>();

  private final Map<String, JDeclaredType> externalTypesByName = new HashMap<String, JDeclaredType>();

  /**
   * Centralizes creation and singleton management.
   */
  private final JProgram program;

  public TypeMap(JProgram program) {
    this.program = program;
  }

  public JNode get(Binding binding) {
    return get(binding, true);
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

    if (to instanceof JDeclaredType) {
      JDeclaredType type = (JDeclaredType) to;
      if (type.isExternal()) {
        externalTypesByName.put(type.getName(), type);
      }
    }
  }

  public JNode tryGet(Binding binding) {
    return get(binding, false);
  }

  private boolean equals(MethodBinding binding, JMethod method) {
    if (!(method instanceof JConstructor && binding.isConstructor()) &&
        !method.getName().equals(String.valueOf(binding.constantPoolName()))) {
      return false;
    }

    List<JType> paramTypes = method.getOriginalParamTypes();
    TypeBinding[] bindingParams = binding.parameters;

    if (paramTypes.size() != bindingParams.length) {
      return false;
    }

    for (int i = 0; i < bindingParams.length; ++i) {
      TypeBinding bindingParam = bindingParams[i];
      if (paramTypes.get(i) != get(bindingParam)) {
        return false;
      }
    }

    return method.getType() == get(binding.returnType);
  }

  private JNode get(Binding binding, boolean failOnNull) {
    if (binding instanceof TypeVariableBinding) {
      TypeVariableBinding tvb = (TypeVariableBinding) binding;
      return get(tvb.erasure(), failOnNull);
    } else if (binding instanceof ParameterizedTypeBinding) {
      ParameterizedTypeBinding ptb = (ParameterizedTypeBinding) binding;
      return get(ptb.erasure(), failOnNull);
    } else if (binding instanceof ParameterizedMethodBinding) {
      ParameterizedMethodBinding pmb = (ParameterizedMethodBinding) binding;
      return get(pmb.original(), failOnNull);
    } else if (binding instanceof ParameterizedFieldBinding) {
      ParameterizedFieldBinding pfb = (ParameterizedFieldBinding) binding;
      return get(pfb.original(), failOnNull);
    } else if (binding instanceof WildcardBinding) {
      WildcardBinding wcb = (WildcardBinding) binding;
      return get(wcb.erasure(), failOnNull);
    }
    JNode result = internalGet(binding, failOnNull);
    if (result == null && failOnNull) {
      InternalCompilerException ice = new InternalCompilerException(
          "Failed to get JNode");
      ice.addNode(binding.getClass().getName(), binding.toString(), null);
      throw ice;
    }
    return result;
  }

  private JField getFieldForBinding(JDeclaredType type, FieldBinding binding) {
    for (JField field : type.getFields()) {
      if (field.getName().equals(String.valueOf(binding.name))) {
        return field;
      }
    }

    return null;
  }

  private JMethod getMethodForBinding(JDeclaredType type, MethodBinding binding) {
    for (JMethod method : type.getMethods()) {
      if (equals(binding, method)) {
        return method;
      }
    }

    return null;
  }

  /**
   * Returns a list of JNodes that have the same name as the JDT Binding.
   * This method is only used during debugging sessions from the interactive
   * expression evaluator.
   */
  @SuppressWarnings("unused")
  private List<JNode> haveSameName(Binding binding) {
    IdentityHashSet<JNode> nodes = new IdentityHashSet<JNode>();
    for (Binding b : crossRefMap.keySet()) {
      if (String.valueOf(b.readableName()).equals(String.valueOf(binding.readableName()))) {
        nodes.add(crossRefMap.get(b));
      }
    }
    return new ArrayList<JNode>(nodes);
  }
  
  private JNode internalGet(Binding binding, boolean failOnNull) {
    JNode cached = crossRefMap.get(binding);
    if (cached != null) {
      return cached;
    } else if (binding instanceof BaseTypeBinding) {
      BaseTypeBinding baseTypeBinding = (BaseTypeBinding) binding;
      // see org.eclipse.jdt.internal.compiler.lookup.TypeIds constants
      switch (baseTypeBinding.id) {
        case TypeIds.T_JavaLangObject:
          // here for consistency, should already be cached
          return program.getTypeJavaLangObject();
        case TypeIds.T_char:
          return program.getTypePrimitiveChar();
        case TypeIds.T_byte:
          return program.getTypePrimitiveByte();
        case TypeIds.T_short:
          return program.getTypePrimitiveShort();
        case TypeIds.T_boolean:
          return program.getTypePrimitiveBoolean();
        case TypeIds.T_void:
          return program.getTypeVoid();
        case TypeIds.T_long:
          return program.getTypePrimitiveLong();
        case TypeIds.T_double:
          return program.getTypePrimitiveDouble();
        case TypeIds.T_float:
          return program.getTypePrimitiveFloat();
        case TypeIds.T_int:
          return program.getTypePrimitiveInt();
        case TypeIds.T_JavaLangString:
          // here for consistency, should already be cached
          return program.getTypeJavaLangString();
        case TypeIds.T_null:
          return program.getTypeNull();
        case TypeIds.T_undefined:
        default:
          return null;
      }
    } else if (binding instanceof ArrayBinding) {
      ArrayBinding arrayBinding = (ArrayBinding) binding;
      JType elementType = (JType) get(arrayBinding.elementsType(), failOnNull);
      if (elementType == null) {
        return null;
      }
      return program.getTypeArray(elementType);
    } else if (binding instanceof BinaryTypeBinding) {
      BinaryTypeBinding binaryBinding = (BinaryTypeBinding) binding;
      String name = BuildTypeMap.dotify(binaryBinding.compoundName);

      // There may be many BinaryTypeBindings for a single binary type
      JDeclaredType type = externalTypesByName.get(name);
      if (type != null) {
        put(binding, type);
      }
      return type;
    } else if (binding instanceof MethodBinding) {
      MethodBinding b = (MethodBinding) binding;
      JMethod cachedMethod = (JMethod) crossRefMap.get(b);
      if (cachedMethod == null) {
        JDeclaredType type = (JDeclaredType) get(b.declaringClass, failOnNull);
        if (type == null) {
          return type;
        }
        cachedMethod = getMethodForBinding(type, b);
        if (cachedMethod != null) {
          put(b, cachedMethod);
        }
      } else {
        // Happens sometimes when looking up the type to resolve the binding
        // causes us to also resolve the binding.
      }

      return cachedMethod;
    } else if (binding instanceof FieldBinding) {
      FieldBinding b = (FieldBinding) binding;
      JField cachedField = (JField) crossRefMap.get(b);

      if (cachedField == null) {
        JDeclaredType type = (JDeclaredType) get(b.declaringClass, failOnNull);
        if (type == null) {
          return null;
        }
        cachedField = getFieldForBinding(type, b);
        if (cachedField != null) {
          put(b, cachedField);
        }
      } else {
        // Happens sometimes when looking up the type to resolve the binding
        // causes us to also resolve the binding.
      }

      return cachedField;
    }

    return null;
  }
}