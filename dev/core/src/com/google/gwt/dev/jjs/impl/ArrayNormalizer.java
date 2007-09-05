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

import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JAbsentArrayDimension;
import com.google.gwt.dev.jjs.ast.JArrayRef;
import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
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
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.js.JsonArray;

/**
 * Replace array accesses and instantiations with calls to the Array class.
 * Depends on {@link com.google.gwt.dev.jjs.impl.CompoundAssignmentNormalizer}
 * and {@link com.google.gwt.dev.jjs.impl.CastNormalizer} having already run.
 */
public class ArrayNormalizer {

  private class ArrayVisitor extends JModVisitor {

    // @Override
    public void endVisit(JBinaryOperation x, Context ctx) {
      if (x.getOp() == JBinaryOperator.ASG && x.getLhs() instanceof JArrayRef) {
        JArrayRef arrayRef = (JArrayRef) x.getLhs();
        if (arrayRef.getType() instanceof JNullType) {
          // will generate a null pointer exception instead
          return;
        }
        JArrayType arrayType = (JArrayType) arrayRef.getInstance().getType();
        JType elementType = arrayType.getElementType();

        /*
         * See if we need to do a checked store. Primitives and (effectively)
         * final are statically correct.
         */
        if (elementType instanceof JReferenceType) {
          if (!((JReferenceType) elementType).isFinal()
              || elementType != x.getRhs().getType()) {
            // replace this assignment with a call to setCheck()
            JMethodCall call = new JMethodCall(program, x.getSourceInfo(),
                null, setCheckMethod);
            call.getArgs().add(arrayRef.getInstance());
            call.getArgs().add(arrayRef.getIndexExpr());
            call.getArgs().add(x.getRhs());
            ctx.replaceMe(call);
          }
        }
      }
    }

    // @Override
    public void endVisit(JNewArray x, Context ctx) {
      JArrayType type = x.getArrayType();
      JLiteral litTypeName = program.getLiteralString(calcClassName(type));

      if (x.initializers != null) {
        processInitializers(x, ctx, type, litTypeName);
      } else if (type.getDims() == 1) {
        processDim(x, ctx, type, litTypeName);
      } else {
        processDims(x, ctx, type, litTypeName);
      }
    }

    private char[] calcClassName(JArrayType type) {
      String leafName = type.getLeafType().getJsniSignatureName();
      leafName = leafName.replace('/', '.');
      int leafLength = leafName.length();
      int nDims = type.getDims();
      char[] className = new char[leafLength + nDims];
      for (int i = 0; i < nDims; ++i) {
        className[i] = '[';
      }

      leafName.getChars(0, leafLength, className, nDims);
      return className;
    }

    /**
     * @see com.google.gwt.lang.Array regarding seed types
     */
    private JIntLiteral getSeedTypeLiteralFor(JType type) {
      if (type instanceof JPrimitiveType) {
        if (type == program.getTypePrimitiveBoolean()) {
          // The boolean type, thus false (index 2)
          return program.getLiteralInt(2);
        } else {
          // A numeric type, thus zero (index 1).
          return program.getLiteralInt(1);
        }
      }
      // An Object type, thus null (index 0).
      return program.getLiteralInt(0);
    }

    private void processDim(JNewArray x, Context ctx, JArrayType arrayType,
        JLiteral litTypeName) {
      // override the type of the called method with the array's type
      JMethodCall call = new JMethodCall(program, x.getSourceInfo(), null,
          initDim, arrayType);
      JLiteral typeIdLit = program.getLiteralInt(program.getTypeId(arrayType));
      JLiteral queryIdLit = program.getLiteralInt(tryGetQueryId(arrayType));
      JType leafType = arrayType.getLeafType();
      JExpression dim = (JExpression) x.dims.get(0);

      call.getArgs().add(litTypeName);
      call.getArgs().add(typeIdLit);
      call.getArgs().add(queryIdLit);
      call.getArgs().add(dim);
      call.getArgs().add(getSeedTypeLiteralFor(leafType));
      ctx.replaceMe(call);
    }

    private void processDims(JNewArray x, Context ctx, JArrayType arrayType,
        JLiteral litTypeName) {
      // override the type of the called method with the array's type
      JMethodCall call = new JMethodCall(program, x.getSourceInfo(), null,
          initDims, arrayType);
      JsonArray typeIdList = new JsonArray(program);
      JsonArray queryIdList = new JsonArray(program);
      JsonArray dimList = new JsonArray(program);
      JType leafType = arrayType.getLeafType();
      int outstandingDims = arrayType.getDims();
      for (int i = 0; i < x.dims.size(); ++i) {
        JExpression dim = (JExpression) x.dims.get(i);
        if (dim instanceof JAbsentArrayDimension) {
          break;
        }

        /*
         * For each non-empty dimension, reduce the number of dims on the end
         * type.
         * 
         * new int[2][ ][ ]->int[][]
         * 
         * new int[2][3][ ]->int[]
         * 
         * new int[2][3][4]->int
         * 
         */
        JArrayType cur = program.getTypeArray(leafType, outstandingDims--);
        JLiteral typeIdLit = program.getLiteralInt(program.getTypeId(cur));
        typeIdList.exprs.add(typeIdLit);
        JLiteral queryIdLit = program.getLiteralInt(tryGetQueryId(cur));
        queryIdList.exprs.add(queryIdLit);
        dimList.exprs.add(dim);
      }
      JType targetType = leafType;
      if (outstandingDims > 0) {
        targetType = program.getTypeArray(targetType, outstandingDims);
      }

      call.getArgs().add(litTypeName);
      call.getArgs().add(typeIdList);
      call.getArgs().add(queryIdList);
      call.getArgs().add(dimList);
      call.getArgs().add(getSeedTypeLiteralFor(targetType));
      ctx.replaceMe(call);
    }

    private void processInitializers(JNewArray x, Context ctx,
        JArrayType arrayType, JLiteral litTypeName) {
      // override the type of the called method with the array's type
      JMethodCall call = new JMethodCall(program, x.getSourceInfo(), null,
          initValues, arrayType);
      JLiteral typeIdLit = program.getLiteralInt(program.getTypeId(arrayType));
      JLiteral queryIdLit = program.getLiteralInt(tryGetQueryId(arrayType));
      JsonArray initList = new JsonArray(program);
      for (int i = 0; i < x.initializers.size(); ++i) {
        initList.exprs.add(x.initializers.get(i));
      }
      call.getArgs().add(litTypeName);
      call.getArgs().add(typeIdLit);
      call.getArgs().add(queryIdLit);
      call.getArgs().add(initList);
      ctx.replaceMe(call);
    }

    private int tryGetQueryId(JArrayType type) {
      JType elementType = type.getElementType();
      int leafTypeId = -1;
      if (elementType instanceof JReferenceType) {
        leafTypeId = program.getQueryId((JReferenceType) elementType);
      }
      return leafTypeId;
    }
  }

  public static void exec(JProgram program) {
    new ArrayNormalizer(program).execImpl();
  }

  private final JMethod initDim;
  private final JMethod initDims;
  private final JMethod initValues;
  private final JProgram program;
  private final JMethod setCheckMethod;

  private ArrayNormalizer(JProgram program) {
    this.program = program;
    setCheckMethod = program.getIndexedMethod("Array.setCheck");
    initDim = program.getIndexedMethod("Array.initDim");
    initDims = program.getIndexedMethod("Array.initDims");
    initValues = program.getIndexedMethod("Array.initValues");
  }

  private void execImpl() {
    ArrayVisitor visitor = new ArrayVisitor();
    visitor.accept(program);
  }

}
