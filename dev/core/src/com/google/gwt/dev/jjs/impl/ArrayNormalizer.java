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
 * Depends on {@link CompoundAssignmentNormalizer} and {@link CastNormalizer}
 * having already run.
 */
public class ArrayNormalizer {

  private class ArrayVisitor extends JModVisitor {

    @Override
    public void endVisit(JBinaryOperation x, Context ctx) {
      if (x.getOp() == JBinaryOperator.ASG && x.getLhs() instanceof JArrayRef) {
        JArrayRef arrayRef = (JArrayRef) x.getLhs();
        JType elementType = arrayRef.getType();
        if (elementType instanceof JNullType) {
          // will generate a null pointer exception instead
          return;
        }

        /*
         * See if we need to do a checked store. Primitives and (effectively)
         * final are statically correct.
         */
        if (elementType instanceof JReferenceType) {
          if (!((JReferenceType) elementType).isFinal()
              || !program.typeOracle.canTriviallyCast(
                  (JReferenceType) x.getRhs().getType(),
                  (JReferenceType) elementType)) {
            // replace this assignment with a call to setCheck()
            JMethodCall call = new JMethodCall(x.getSourceInfo(), null,
                setCheckMethod);
            call.addArgs(arrayRef.getInstance(), arrayRef.getIndexExpr(),
                x.getRhs());
            ctx.replaceMe(call);
          }
        }
      }
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

    /**
     * @see com.google.gwt.lang.Array regarding seed types
     */
    private JIntLiteral getSeedTypeLiteralFor(JType type) {
      if (type instanceof JPrimitiveType) {
        if (type == program.getTypePrimitiveLong()) {
          // The long type, thus 0L (index 3)
          return program.getLiteralInt(3);
        } else if (type == program.getTypePrimitiveBoolean()) {
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

    private void processDim(JNewArray x, Context ctx, JArrayType arrayType) {
      // override the type of the called method with the array's type
      JMethodCall call = new JMethodCall(x.getSourceInfo(), null, initDim,
          arrayType);
      JLiteral classLit = x.getClassLiteral();
      JLiteral typeIdLit = program.getLiteralInt(program.getTypeId(arrayType));
      JLiteral queryIdLit = program.getLiteralInt(tryGetQueryId(arrayType));
      JExpression dim = x.dims.get(0);
      JType elementType = arrayType.getElementType();
      call.addArgs(classLit, typeIdLit, queryIdLit, dim,
          getSeedTypeLiteralFor(elementType));
      ctx.replaceMe(call);
    }

    private void processDims(JNewArray x, Context ctx, JArrayType arrayType,
        int dims) {
      // override the type of the called method with the array's type
      SourceInfo sourceInfo = x.getSourceInfo().makeChild(ArrayVisitor.class,
          "Creating dimensions");
      JMethodCall call = new JMethodCall(sourceInfo, null, initDims, arrayType);
      JsonArray classLitList = new JsonArray(sourceInfo,
          program.getJavaScriptObject());
      JsonArray typeIdList = new JsonArray(sourceInfo,
          program.getJavaScriptObject());
      JsonArray queryIdList = new JsonArray(sourceInfo,
          program.getJavaScriptObject());
      JsonArray dimList = new JsonArray(sourceInfo,
          program.getJavaScriptObject());
      JType cur = arrayType;
      for (int i = 0; i < dims; ++i) {
        // Walk down each type from most dims to least.
        JArrayType curArrayType = (JArrayType) cur;

        JLiteral classLit = x.getClassLiterals().get(i);
        classLitList.exprs.add(classLit);

        JLiteral typeIdLit = program.getLiteralInt(program.getTypeId(curArrayType));
        typeIdList.exprs.add(typeIdLit);

        JLiteral queryIdLit = program.getLiteralInt(tryGetQueryId(curArrayType));
        queryIdList.exprs.add(queryIdLit);

        dimList.exprs.add(x.dims.get(i));
        cur = curArrayType.getElementType();
      }
      call.addArgs(classLitList, typeIdList, queryIdList, dimList,
          program.getLiteralInt(dims), getSeedTypeLiteralFor(cur));
      ctx.replaceMe(call);
    }

    private void processInitializers(JNewArray x, Context ctx,
        JArrayType arrayType) {
      // override the type of the called method with the array's type
      SourceInfo sourceInfo = x.getSourceInfo().makeChild(ArrayVisitor.class,
          "Array initializer");
      JMethodCall call = new JMethodCall(sourceInfo, null, initValues,
          arrayType);
      JLiteral classLit = x.getClassLiteral();
      JLiteral typeIdLit = program.getLiteralInt(program.getTypeId(arrayType));
      JLiteral queryIdLit = program.getLiteralInt(tryGetQueryId(arrayType));
      JsonArray initList = new JsonArray(sourceInfo,
          program.getJavaScriptObject());
      for (int i = 0; i < x.initializers.size(); ++i) {
        initList.exprs.add(x.initializers.get(i));
      }
      call.addArgs(classLit, typeIdLit, queryIdLit, initList);
      ctx.replaceMe(call);
    }

    private int tryGetQueryId(JArrayType type) {
      JType elementType = type.getElementType();
      int leafTypeId = -1;
      if (elementType instanceof JReferenceType) {
        leafTypeId = program.getQueryId(program.getRunTimeType((JReferenceType) elementType));
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
