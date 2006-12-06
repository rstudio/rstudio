// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.dev.jjs.ast.JAbsentArrayDimension;
import com.google.gwt.dev.jjs.ast.JArrayRef;
import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JLiteral;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNewArray;
import com.google.gwt.dev.jjs.ast.JNullType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.Mutator;
import com.google.gwt.dev.jjs.ast.change.ChangeList;
import com.google.gwt.dev.jjs.ast.js.JsonArray;

/**
 * Replace array accesses and instantiations with calls to the Array class.
 * Depends on {@link com.google.gwt.dev.jjs.impl.CompoundAssignmentNormalizer}
 * and {@link com.google.gwt.dev.jjs.impl.CastNormalizer} having already run.
 */
public class ArrayNormalizer {

  private final JMethod setCheckMethod;
  private final JMethod initDims;
  private final JMethod initValues;

  private class ArrayVisitor extends JVisitor {

    private final ChangeList changeList = new ChangeList(
      "Transform array accesses to check bounds.");

    public ChangeList getChangeList() {
      return changeList;
    }

    // @Override
    public boolean visit(JBinaryOperation x, Mutator m) {
      if (x.op == JBinaryOperator.ASG && x.getLhs() instanceof JArrayRef) {
        JArrayRef arrayRef = (JArrayRef) x.getLhs();
        if (arrayRef.getType() instanceof JNullType) {
          // will generate a null pointer exception instead
          return false;
        }
        JArrayType arrayType = (JArrayType) arrayRef.getInstance().getType();
        JType elementType = arrayType.getElementType();

        // see if we need to do a checked store
        // primitives and (effectively) final are statically correct
        if (elementType instanceof JReferenceType
          && !((JReferenceType) elementType).isFinal()) {
          // replace this assignment with a call to setCheck()

          // DON'T VISIT ARRAYREF, but do visit its children
          arrayRef.instance.traverse(this);
          arrayRef.indexExpr.traverse(this);
          x.rhs.traverse(this);

          JMethodCall call = new JMethodCall(program, null, setCheckMethod);
          ChangeList myChanges = new ChangeList("Replace " + x
            + " with a call to Array.setCheck()");
          myChanges.replaceExpression(m, call);
          myChanges.addExpression(arrayRef.instance, call.args);
          myChanges.addExpression(arrayRef.indexExpr, call.args);
          myChanges.addExpression(x.rhs, call.args);
          changeList.add(myChanges);
          return false;
        }
      }
      return true;
    }

    // @Override
    public void endVisit(JNewArray x, Mutator m) {
      JArrayType type = x.getArrayType();
      JLiteral litTypeName = program.getLiteralString(calcClassName(type));

      if (x.initializers != null) {
        processInitializers(x, m, type, litTypeName);
      } else {
        processDims(x, m, type, litTypeName);
      }
    }

    private void processDims(JNewArray x, Mutator m, JArrayType arrayType,
        JLiteral litTypeName) {
      ChangeList myChanges = new ChangeList("Replace " + x
        + " with a call to Array.initDims()");
      // override the type of the called method with the array's type
      JMethodCall call = new JMethodCall(program, null, initDims, arrayType);
      JsonArray typeIdList = new JsonArray(program);
      JsonArray queryIdList = new JsonArray(program);
      JsonArray dimList = new JsonArray(program);
      JType leafType = arrayType.getLeafType();
      int outstandingDims = arrayType.dims;
      for (int i = 0; i < x.dims.size(); ++i) {
        Mutator dim = x.dims.getMutator(i);
        if (dim.get() instanceof JAbsentArrayDimension) {
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
        myChanges.addExpression(typeIdLit, typeIdList.exprs);
        JLiteral queryIdLit = program.getLiteralInt(tryGetQueryId(cur));
        myChanges.addExpression(queryIdLit, queryIdList.exprs);
        myChanges.addExpression(dim, dimList.exprs);
      }
      JType targetType = leafType;
      if (outstandingDims > 0) {
        targetType = program.getTypeArray(targetType, outstandingDims);
      }
      
      myChanges.addExpression(litTypeName, call.args);
      myChanges.addExpression(typeIdList, call.args);
      myChanges.addExpression(queryIdList, call.args);
      myChanges.addExpression(dimList, call.args);
      myChanges.addExpression(targetType.getDefaultValue(), call.args);
      myChanges.replaceExpression(m, call);
      changeList.add(myChanges);
    }

    private void processInitializers(JNewArray x, Mutator m,
        JArrayType arrayType, JLiteral litTypeName) {
      // override the type of the called method with the array's type
      JMethodCall call = new JMethodCall(program, null, initValues, arrayType);
      JLiteral typeIdLit = program.getLiteralInt(program.getTypeId(arrayType));
      JLiteral queryIdLit = program.getLiteralInt(tryGetQueryId(arrayType));
      JsonArray initList = new JsonArray(program);
      ChangeList myChanges = new ChangeList("Replace " + x
        + " with a call to Array.initValues()");
      for (int i = 0; i < x.initializers.size(); ++i) {
        Mutator initializer = x.initializers.getMutator(i);
        myChanges.addExpression(initializer, initList.exprs);
      }
      myChanges.addExpression(litTypeName, call.args);
      myChanges.addExpression(typeIdLit, call.args);
      myChanges.addExpression(queryIdLit, call.args);
      myChanges.addExpression(initList, call.args);
      myChanges.replaceExpression(m, call);
      changeList.add(myChanges);
    }

    private int tryGetQueryId(JArrayType type) {
      JType elementType = type.getElementType();
      int leafTypeId = -1;
      if (elementType instanceof JReferenceType) {
        leafTypeId = program.getQueryId((JReferenceType) elementType);
      }
      return leafTypeId;
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
  }

  private void execImpl() {
    ArrayVisitor visitor = new ArrayVisitor();
    program.traverse(visitor);
    ChangeList changes = visitor.getChangeList();
    if (!changes.empty()) {
      changes.apply();
    }
  }

  private final JProgram program;

  private ArrayNormalizer(JProgram program) {
    this.program = program;
    setCheckMethod = program.getSpecialMethod("Array.setCheck");
    initDims = program.getSpecialMethod("Array.initDims");
    initValues = program.getSpecialMethod("Array.initValues");
  }

  public static void exec(JProgram program) {
    new ArrayNormalizer(program).execImpl();
  }

}
