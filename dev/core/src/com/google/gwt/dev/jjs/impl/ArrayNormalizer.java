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
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JRuntimeTypeReference;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.RuntimeConstants;
import com.google.gwt.dev.jjs.ast.js.JsonArray;

import java.util.Collections;
import java.util.List;

/**
 * Replace array accesses and instantiations with calls to the Array class.
 * Depends on {@link CompoundAssignmentNormalizer} and {@link ImplementCastsAndTypeChecks}
 * having already run.
 */
public class ArrayNormalizer {

  private class ArrayVisitor extends JModVisitor {

    @Override
    public void endVisit(JBinaryOperation x, Context ctx) {
      JArrayRef arrayRef = needsSetCheck(x);
      if (arrayRef == null) {
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

      List<JExpression> initializers = x.getInitializers();
      if (initializers != null) {
        JsonArray initializerArray = getInitializerArray(x);
        if (program.isUntypedArrayType(type)) {
          ctx.replaceMe(initializerArray);
          return;
        }
        ctx.replaceMe(createArrayFromInitializers(x, type));
        return;
      }

      if (program.isUntypedArrayType(type) && type.getDims() == 1) {
          // Create a plain array.
        ctx.replaceMe(new JMethodCall(x.getSourceInfo(), null,
            program.getIndexedMethod(RuntimeConstants.ARRAY_NEW_ARRAY),
            x.getDimensionExpressions().get(0)));
        return;
      }

      int suppliedDimensions = x.getDimensionExpressions().size();
      assert (suppliedDimensions >= 1);

      if (suppliedDimensions == 1) {
        ctx.replaceMe(initializeUnidimensionalArray(x, type));
        return;
      }

      ctx.replaceMe(initializeMultidimensionalArray(x, type));
    }

    private JRuntimeTypeReference getElementRuntimeTypeReference(SourceInfo sourceInfo,
        JArrayType arrayType) {
      JType elementType = arrayType.getElementType();
      if (!(elementType instanceof JReferenceType)) {
        // elementType is a primitive type, store check will be performed statically.
        elementType = JReferenceType.NULL_TYPE;
      }

      if (program.typeOracle.isEffectivelyJavaScriptObject(elementType)) {
        /*
         * treat types that are effectively JSO's as JSO's, for the purpose of
         * castability checking
         */
        elementType = program.getJavaScriptObject();
      } else {
        elementType = elementType.getUnderlyingType();
      }

      elementType = program.normalizeJsoType(elementType);
      return new JRuntimeTypeReference(sourceInfo, program.getTypeJavaLangObject(),
          (JReferenceType) elementType);
    }

    private JExpression getOrCreateCastMap(SourceInfo sourceInfo, JArrayType arrayType) {
      JCastMap castableTypeMap = program.getCastMap(arrayType);
      if (castableTypeMap == null) {
        return new JCastMap(sourceInfo, program.getTypeJavaLangObject(),
            Collections.<JReferenceType>emptyList());
      }
      return castableTypeMap;
    }

    private JExpression initializeUnidimensionalArray(JNewArray x, JArrayType arrayType) {
      // override the type of the called method with the array's type
      SourceInfo sourceInfo = x.getSourceInfo();
      JLiteral classLit = x.getLeafTypeClassLiteral();
      JExpression castableTypeMap = getOrCreateCastMap(sourceInfo, arrayType);
      JRuntimeTypeReference arrayElementRuntimeTypeReference =
          getElementRuntimeTypeReference(sourceInfo, arrayType);
      JType elementType = arrayType.getElementType();
      JIntLiteral elementTypeCategory = getTypeCategoryLiteral(elementType);
      JExpression dim = x.getDimensionExpressions().get(0);
      JMethodCall call =
          new JMethodCall(sourceInfo, null, initializeUnidimensionalArrayMethod);
      call.overrideReturnType(arrayType);
      call.addArgs(classLit, castableTypeMap, arrayElementRuntimeTypeReference, dim,
          elementTypeCategory, program.getLiteralInt(arrayType.getDims()));
      return call;
    }

    private JExpression initializeMultidimensionalArray(JNewArray x, JArrayType arrayType) {
      // override the type of the called method with the array's type
      SourceInfo sourceInfo = x.getSourceInfo();
      JsonArray castableTypeMaps = new JsonArray(sourceInfo, program.getJavaScriptObject());
      JsonArray elementTypeReferences = new JsonArray(sourceInfo, program.getJavaScriptObject());
      JsonArray dimList = new JsonArray(sourceInfo, program.getJavaScriptObject());
      JType currentElementType = arrayType;
      JLiteral classLit = x.getLeafTypeClassLiteral();
      for (int i = 0; i < x.getDimensionExpressions().size(); ++i) {
        // Walk down each type from most dims to least.
        JArrayType curArrayType = (JArrayType) currentElementType;

        JExpression castableTypeMap = getOrCreateCastMap(sourceInfo, curArrayType);
        castableTypeMaps.getExpressions().add(castableTypeMap);

        JRuntimeTypeReference elementTypeIdLit = getElementRuntimeTypeReference(sourceInfo,
            curArrayType);
        elementTypeReferences.getExpressions().add(elementTypeIdLit);

        dimList.getExpressions().add(x.getDimensionExpressions().get(i));
        currentElementType = curArrayType.getElementType();
      }
      JType leafElementType = currentElementType;
      JIntLiteral leafElementTypeCategory = getTypeCategoryLiteral(leafElementType);
      JMethodCall call =
          new JMethodCall(sourceInfo, null, initializeMultidimensionalArrayMethod);
      call.overrideReturnType(arrayType);
      call.addArgs(classLit, castableTypeMaps, elementTypeReferences, leafElementTypeCategory,
          dimList, program.getLiteralInt(x.getDimensionExpressions().size()));
      return call;
    }

    private JExpression createArrayFromInitializers(JNewArray x, JArrayType arrayType) {
      // override the type of the called method with the array's type
      SourceInfo sourceInfo = x.getSourceInfo();
      JExpression classLitExpression = program.createArrayClassLiteralExpression(x.getSourceInfo(),
          x.getLeafTypeClassLiteral(), arrayType.getDims());
      JExpression castableTypeMap = getOrCreateCastMap(sourceInfo, arrayType);
      JRuntimeTypeReference elementTypeIds = getElementRuntimeTypeReference(sourceInfo, arrayType);
      JsonArray initializers =
          new JsonArray(sourceInfo, program.getJavaScriptObject(), x.getInitializers());
      JIntLiteral leafElementTypeCategory = getTypeCategoryLiteral(arrayType.getElementType());
      JMethodCall call = new JMethodCall(sourceInfo, null, stampJavaTypeInfoMethod);
      call.overrideReturnType(arrayType);
      call.addArgs(classLitExpression, castableTypeMap, elementTypeIds, leafElementTypeCategory,
          initializers);
      return call;
    }

    /**
     * Returns a literal that represent the type category for a type.
     */
    private JIntLiteral getTypeCategoryLiteral(JType type) {
      return JIntLiteral.get(TypeCategory.typeCategoryForType(type, program).ordinal());
    }
  }

  private JArrayRef needsSetCheck(JBinaryOperation x) {
    if (x.getOp() != JBinaryOperator.ASG || !(x.getLhs() instanceof JArrayRef)) {
      return null;
    }
    JArrayRef arrayRef = (JArrayRef) x.getLhs();
    JType elementType = arrayRef.getType();
    JExpression arrayInstance = arrayRef.getInstance();
    if (elementType.isNullType()) {
      // JNullType will generate a null pointer exception instead,
      return null;
    } else if (!(elementType instanceof JReferenceType)) {
      // Primitive array types are statically correct, no need to set check.
      return null;
    } else if (!arrayInstance.getType().canBeSubclass() &&
        program.typeOracle.castSucceedsTrivially((JReferenceType) x.getRhs().getType(),
            (JReferenceType) elementType)) {
      // There is no need to check as the static check already proved the cast is correct.
      return null;
    }
    return arrayRef;
  }

  public static JsonArray getInitializerArray(JNewArray x) {
    return new JsonArray(x.getSourceInfo(), x.getType(), x.getInitializers());
  }

  public static void exec(JProgram program) {
    new ArrayNormalizer(program).execImpl();
  }

  private final JMethod initializeUnidimensionalArrayMethod;
  private final JMethod initializeMultidimensionalArrayMethod;
  private final JMethod stampJavaTypeInfoMethod;
  private final JMethod setCheckMethod;
  private final JProgram program;

  private ArrayNormalizer(JProgram program) {
    this.program = program;
    setCheckMethod = program.getIndexedMethod(RuntimeConstants.ARRAY_SET_CHECK);
    initializeUnidimensionalArrayMethod = program.getIndexedMethod(
        RuntimeConstants.ARRAY_INITIALIZE_UNIDIMENSIONAL_ARRAY);
    initializeMultidimensionalArrayMethod = program.getIndexedMethod(
        RuntimeConstants.ARRAY_INITIALIZE_MULTIDIMENSIONAL_ARRAY);
    stampJavaTypeInfoMethod = program.getIndexedMethod(RuntimeConstants.ARRAY_STAMP_JAVA_TYPE_INFO);
  }

  private void execImpl() {
    new ArrayVisitor().accept(program);
  }
}
