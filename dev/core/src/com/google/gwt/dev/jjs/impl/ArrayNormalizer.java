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

import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JAbsentArrayDimension;
import com.google.gwt.dev.jjs.ast.JArrayRef;
import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JCastMap;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JIntLiteral;
import com.google.gwt.dev.jjs.ast.JLiteral;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JNewArray;
import com.google.gwt.dev.jjs.ast.JNullType;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JRuntimeTypeReference;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.js.JsonArray;

import java.util.Collections;

/**
 * Replace array accesses and instantiations with calls to the Array class.
 * Depends on {@link CompoundAssignmentNormalizer} and {@link CastNormalizer}
 * having already run.
 */
public class ArrayNormalizer {

  private class ArrayVisitor extends JModVisitor {

    @Override
    public void endVisit(JBinaryOperation x, Context ctx) {
      if (disableCastChecking || x.getOp() != JBinaryOperator.ASG ||
          !(x.getLhs() instanceof JArrayRef)) {
        return;
      }
      JArrayRef arrayRef = (JArrayRef) x.getLhs();
      JType elementType = arrayRef.getType();
      if (elementType instanceof JNullType) {
        // JNullType will generate a null pointer exception instead,
        return;
      } else if (!(elementType instanceof JReferenceType)) {
        // Primitive array types are statically correct, no need to set check.
        return;
      } else if (elementType.isFinal() &&
          program.typeOracle.canTriviallyCast((JReferenceType) x.getRhs().getType(),
              (JReferenceType) elementType)) {
        // Effectively final element types are statically correct.
        return;
      }

      // replace this assignment with a call to setCheck()
      JMethodCall call = new JMethodCall(x.getSourceInfo(), null, setCheckMethod);
      call.addArgs(arrayRef.getInstance(), arrayRef.getIndexExpr(), x.getRhs());
      ctx.replaceMe(call);
    }

    @Override
    public void endVisit(JNewArray x, Context ctx) {
      JArrayType type = x.getArrayType();

      if (x.initializers != null) {
        processInitializers(x, ctx, type);
      } else {
        int realDims = 0;
        for (JExpression dim : x.dims) {
          if (dim instanceof JAbsentArrayDimension) {
            break;
          }
          ++realDims;
        }
        assert (realDims >= 1);
        if (realDims == 1) {
          processDim(x, ctx, type);
        } else {
          processDims(x, ctx, type, realDims);
        }
      }
    }

    private int getLeafElementClass(JArrayType arrayType) {
      JType elementType = arrayType.getElementType();
      if (elementType instanceof JArrayType) {
        return getLeafElementClass((JArrayType) elementType);
      }

      if (!(elementType instanceof JReferenceType)) {
        return TYPE_JAVA_OBJECT;
      }

      JReferenceType elementRefType = ((JReferenceType) elementType).getUnderlyingType();
      if (elementRefType == program.getTypeJavaLangObject()) {
        return TYPE_JAVA_LANG_OBJECT;
      } else if (program.typeOracle.isEffectivelyJavaScriptObject(elementRefType)) {
        return TYPE_JSO;
      } else if (program.typeOracle.isDualJsoInterface(elementRefType)) {
        return TYPE_JAVA_OBJECT_OR_JSO;
      }
      return TYPE_JAVA_OBJECT;
    }

    private JRuntimeTypeReference getElementRuntimeTypeReference(SourceInfo sourceInfo,
        JArrayType arrayType) {
      JType elementType = arrayType.getElementType();
      if (!(elementType instanceof JReferenceType)) {
        // elementType is a primitive type, store check will be performed statically.
        elementType = JNullType.INSTANCE;
      }

      if (program.typeOracle.isEffectivelyJavaScriptObject(elementType)) {
        /*
         * treat types that are effectively JSO's as JSO's, for the purpose of
         * castability checking
         */
        elementType = program.getJavaScriptObject();
      } else {
        elementType = ((JReferenceType) elementType).getUnderlyingType();
      }
      return new JRuntimeTypeReference(sourceInfo, program.getTypeJavaLangObject(),
          (JReferenceType)  elementType);
    }

    private JExpression getOrCreateCastMap(SourceInfo sourceInfo, JArrayType arrayType) {
      JCastMap castableTypeMap = program.getCastMap(arrayType);
      if (castableTypeMap == null) {
        return new JCastMap(sourceInfo, program.getTypeJavaLangObject(),
            Collections.<JReferenceType>emptyList());
      }
      return castableTypeMap;
    }

    /**
     * @see com.google.gwt.lang.Array regarding initial value types.
     */
    private JIntLiteral getInitValueTypeFor(JType type) {
      if (type instanceof JPrimitiveType) {
        if (type == program.getTypePrimitiveLong()) {
          // The long type, thus 0L.
          return program.getLiteralInt(INIT_TO_ZERO_LONG);
        } else if (type == program.getTypePrimitiveBoolean()) {
          // The boolean type, thus false.
          return program.getLiteralInt(INIT_TO_FALSE);
        } else {
          // A numeric type, thus zero.
          return program.getLiteralInt(INIT_TO_ZERO_INT);
        }
      }
      // An Object type, thus null.
      return program.getLiteralInt(INIT_TO_NULL);
    }

    private void processDim(JNewArray x, Context ctx, JArrayType arrayType) {
      // override the type of the called method with the array's type
      SourceInfo sourceInfo = x.getSourceInfo();
      JMethodCall call = new JMethodCall(sourceInfo, null, initDim, arrayType);
      JLiteral classLit = x.getClassLiteral();
      JExpression castableTypeMap = getOrCreateCastMap(sourceInfo, arrayType);
      JRuntimeTypeReference arrayElementRuntimeTypeReference =
          getElementRuntimeTypeReference(sourceInfo, arrayType);
      JIntLiteral elementTypeClass = JIntLiteral.get(getLeafElementClass(arrayType));
      JExpression dim = x.dims.get(0);
      JType elementType = arrayType.getElementType();
      call.addArgs(classLit, castableTypeMap, arrayElementRuntimeTypeReference, dim,
          elementTypeClass, getInitValueTypeFor(elementType));
      ctx.replaceMe(call);
    }

    private void processDims(JNewArray x, Context ctx, JArrayType arrayType, int dims) {
      // override the type of the called method with the array's type
      SourceInfo sourceInfo = x.getSourceInfo();
      JMethodCall call = new JMethodCall(sourceInfo, null, initDims, arrayType);
      JsonArray classLitList = new JsonArray(sourceInfo, program.getJavaScriptObject());
      JsonArray castableTypeMaps = new JsonArray(sourceInfo, program.getJavaScriptObject());
      JsonArray elementTypeReferences = new JsonArray(sourceInfo, program.getJavaScriptObject());
      JsonArray dimList = new JsonArray(sourceInfo, program.getJavaScriptObject());
      JIntLiteral leafElementTypeClass =
          JIntLiteral.get(getLeafElementClass(arrayType));
      JType cur = arrayType;
      for (int i = 0; i < dims; ++i) {
        // Walk down each type from most dims to least.
        JArrayType curArrayType = (JArrayType) cur;

        JLiteral classLit = x.getClassLiterals().get(i);
        classLitList.getExprs().add(classLit);

        JExpression castableTypeMap = getOrCreateCastMap(sourceInfo, curArrayType);
        castableTypeMaps.getExprs().add(castableTypeMap);

        JRuntimeTypeReference elementTypeIdLit = getElementRuntimeTypeReference(sourceInfo,
            curArrayType);
        elementTypeReferences.getExprs().add(elementTypeIdLit);

        dimList.getExprs().add(x.dims.get(i));
        cur = curArrayType.getElementType();
      }
      call.addArgs(classLitList, castableTypeMaps, elementTypeReferences, leafElementTypeClass,
          dimList, program.getLiteralInt(dims), getInitValueTypeFor(cur));
      ctx.replaceMe(call);
    }

    private void processInitializers(JNewArray x, Context ctx, JArrayType arrayType) {
      // override the type of the called method with the array's type
      SourceInfo sourceInfo = x.getSourceInfo();
      JMethodCall call = new JMethodCall(sourceInfo, null, initValues, arrayType);
      JLiteral classLit = x.getClassLiteral();
      JExpression castableTypeMap = getOrCreateCastMap(sourceInfo, arrayType);
      JRuntimeTypeReference elementTypeIds = getElementRuntimeTypeReference(sourceInfo, arrayType);
      JsonArray initList = new JsonArray(sourceInfo, program.getJavaScriptObject());
      JIntLiteral leafElementTypeClass =
          JIntLiteral.get(getLeafElementClass(arrayType));
      for (int i = 0; i < x.initializers.size(); ++i) {
        initList.getExprs().add(x.initializers.get(i));
      }
      call.addArgs(classLit, castableTypeMap, elementTypeIds, leafElementTypeClass, initList);
      ctx.replaceMe(call);
    }
  }

  public static void exec(JProgram program, boolean disableCastChecking) {
    new ArrayNormalizer(program, disableCastChecking).execImpl();
  }

  private final boolean disableCastChecking;
  private final JMethod initDim;
  private final JMethod initDims;
  private final JMethod initValues;
  private final JProgram program;
  private final JMethod setCheckMethod;

  private ArrayNormalizer(JProgram program, boolean disableCastChecking) {
    this.program = program;
    this.disableCastChecking = disableCastChecking;
    setCheckMethod = program.getIndexedMethod("Array.setCheck");

    initDim = program.getIndexedMethod("Array.initDim");
    initDims = program.getIndexedMethod("Array.initDims");
    initValues = program.getIndexedMethod("Array.initValues");
  }

  private void execImpl() {
    ArrayVisitor visitor = new ArrayVisitor();
    visitor.accept(program);
  }

  // Array initialization values types
  // These are also hard coded in the emulation library see {@link Array.java}
  static final int INIT_TO_NULL = 0;
  static final int INIT_TO_ZERO_INT  = 1;
  static final int INIT_TO_FALSE = 2;
  static final int INIT_TO_ZERO_LONG = 3;

  // Array element type classes
  // These are also hard coded in the emulation library see {@link Array.java}
  static final int TYPE_JAVA_OBJECT = 0;
  static final int TYPE_JAVA_OBJECT_OR_JSO = 1;
  static final int TYPE_JSO = 2;
  static final int TYPE_JAVA_LANG_OBJECT = 3;

}
