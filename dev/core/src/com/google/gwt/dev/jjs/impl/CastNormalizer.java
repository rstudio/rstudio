/*
 * Copyright 2006 Google Inc.
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

import com.google.gwt.dev.jjs.ast.JArrayRef;
import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JCastOperation;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JInstanceOf;
import com.google.gwt.dev.jjs.ast.JIntLiteral;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNullLiteral;
import com.google.gwt.dev.jjs.ast.JNullType;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JTypeOracle;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.Mutator;
import com.google.gwt.dev.jjs.ast.change.ChangeList;
import com.google.gwt.dev.jjs.ast.js.JClassSeed;
import com.google.gwt.dev.jjs.ast.js.JsonObject;
import com.google.gwt.dev.jjs.ast.js.JsonObject.JsonPropInit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Replace cast and instanceof operations with calls to the Cast class. Depends
 * on {@link com.google.gwt.dev.jjs.impl.CatchBlockNormalizer},
 * {@link com.google.gwt.dev.jjs.impl.CompoundAssignmentNormalizer}, and
 * {@link com.google.gwt.dev.jjs.impl.JavaScriptObjectCaster} having already
 * run.
 */
public class CastNormalizer {

  private class AssignTypeIdsVisitor extends JVisitor {

    Set/* <JClassType> */alreadyRan = new HashSet/* <JClassType> */();
    private Map/* <JReferenceType, Set<JReferenceType>> */queriedTypes = new IdentityHashMap();
    private int nextQueryId = 1; // 0 is reserved

    private final List/* <JArrayType> */instantiatedArrayTypes = new ArrayList/* <JArrayType> */();

    private List/* <JClassType> */classes = new ArrayList/* <JClassType> */();

    private List/* <JsonObject> */jsonObjects = new ArrayList/* <JsonObject> */();

    {
      JTypeOracle typeOracle = program.typeOracle;
      for (Iterator it = program.getAllArrayTypes().iterator(); it.hasNext();) {
        JArrayType arrayType = (JArrayType) it.next();
        if (typeOracle.isInstantiatedType(arrayType)) {
          instantiatedArrayTypes.add(arrayType);
        }
      }
    }

    public void computeTypeIds() {

      // the 0th entry is the "always false" entry
      classes.add(null);
      jsonObjects.add(new JsonObject(program));

      /*
       * Compute the list of classes than can successfully satisfy cast
       * requests, along with the set of types they can be successfully cast to.
       * Do it in super type order.
       */
      for (Iterator it = program.getDeclaredTypes().iterator(); it.hasNext();) {
        JReferenceType type = (JReferenceType) it.next();
        if (type instanceof JClassType) {
          computeSourceClass((JClassType) type);
        }
      }

      for (Iterator it = program.getAllArrayTypes().iterator(); it.hasNext();) {
        JArrayType type = (JArrayType) it.next();
        computeSourceClass(type);
      }

      // pass our info to JProgram
      program.initTypeInfo(classes, jsonObjects);
      program.recordQueryIds(queryIds);
    }

    /*
     * If this expression could possibly generate an ArrayStoreException, we
     * must record a query on the element type being assigned to.
     */
    // @Override
    public void endVisit(JBinaryOperation x, Mutator m) {
      if (x.op == JBinaryOperator.ASG && x.getLhs() instanceof JArrayRef) {

        // first, calculate the transitive closure of all possible runtime types
        // the lhs could be
        JExpression instance = ((JArrayRef) x.getLhs()).getInstance();
        if (instance.getType() instanceof JNullType) {
          // will generate a null pointer exception instead
          return;
        }
        JArrayType lhsArrayType = (JArrayType) instance.getType();
        JType elementType = lhsArrayType.getElementType();

        // primitives are statically correct
        if (!(elementType instanceof JReferenceType)) {
          return;
        }

        // element type being final means the assignment is statically correct
        if (((JReferenceType) elementType).isFinal()) {
          return;
        }

        /*
         * For every instantiated array type that could -in theory- be the
         * runtime type of the lhs, we must record a cast from the rhs to the
         * prospective element type of the lhs.
         */
        JTypeOracle typeOracle = program.typeOracle;
        JType rhsType = x.getRhs().getType();
        assert (rhsType instanceof JReferenceType);
        JReferenceType refRhsType = (JReferenceType) rhsType;
        for (Iterator it = instantiatedArrayTypes.iterator(); it.hasNext();) {
          JArrayType arrayType = (JArrayType) it.next();
          if (typeOracle.canTheoreticallyCast(arrayType, lhsArrayType)) {
            JType itElementType = arrayType.getElementType();
            if (itElementType instanceof JReferenceType) {
              recordCastInternal((JReferenceType) itElementType, refRhsType);
            }
          }
        }
      }
    }

    // @Override
    public void endVisit(JCastOperation x, Mutator m) {
      recordCast(x.castType, x.getExpression());
    }

    // @Override
    public void endVisit(JInstanceOf x, Mutator m) {
      recordCast(x.testType, x.getExpression());
    }

    /**
     * Create the data for JSON table to capture the mapping from a class to its
     * query types.
     */
    private void computeSourceClass(JClassType type) {
      if (type == null || alreadyRan.contains(type)) {
        return;
      }

      alreadyRan.add(type);

      /*
       * IMPORTANT: Visit my supertype first. The implementation of
       * {@link com.google.gwt.lang.Cast#wrapJSO()} depends on all superclasses
       * having typeIds that are less than all their subclasses. This allows the
       * same JSO to be wrapped stronger but not weaker.
       */
      computeSourceClass(type.extnds);

      if (!program.typeOracle.isInstantiatedType(type)) {
        return;
      }

      // Find all possible query types which I can satisfy
      Set/* <JReferenceType> */yesSet = null;
      for (Iterator iter = queriedTypes.keySet().iterator(); iter.hasNext();) {
        JReferenceType qType = (JReferenceType) iter.next();
        Set/* <JReferenceType> */querySet = (Set) queriedTypes.get(qType);
        if (program.typeOracle.canTriviallyCast(type, qType)) {
          for (Iterator it = querySet.iterator(); it.hasNext();) {
            JReferenceType argType = (JReferenceType) it.next();
            if (program.typeOracle.canTriviallyCast(type, argType)) {
              if (yesSet == null) {
                yesSet = new HashSet/* <JReferenceType> */();
              }
              yesSet.add(qType);
              break;
            }
          }
        }
      }

      /*
       * Weird: JavaScriptObjects MUST have a typeId, the implementation of
       * Cast.wrapJSO depends on it.
       */
      if (yesSet == null && !program.isJavaScriptObject(type)) {
        return; // won't satisfy anything
      }

      // use an array to sort my yes set
      JReferenceType[] yesArray = new JReferenceType[nextQueryId];
      if (yesSet != null) {
        for (Iterator it = yesSet.iterator(); it.hasNext();) {
          JReferenceType yesType = (JReferenceType) it.next();
          Integer boxedInt = (Integer) queryIds.get(yesType);
          yesArray[boxedInt.intValue()] = yesType;
        }
      }

      // create a sparse lookup object
      JsonObject jsonObject = new JsonObject(program);
      for (int i = 0; i < nextQueryId; ++i) {
        if (yesArray[i] != null) {
          JIntLiteral labelExpr = program.getLiteralInt(i);
          JIntLiteral valueExpr = program.getLiteralInt(1);
          jsonObject.propInits.add(new JsonPropInit(program, labelExpr,
              valueExpr));
        }
      }

      // add an entry for me
      classes.add(type);
      jsonObjects.add(jsonObject);
    }

    private void recordCast(JType targetType, JExpression rhs) {
      if (targetType instanceof JReferenceType) {
        // unconditional cast b/c it would've been a semantic error earlier
        JReferenceType rhsType = (JReferenceType) rhs.getType();
        // don't record a type for trivial casts that won't generate code
        if (rhsType instanceof JClassType) {
          if (program.typeOracle.canTriviallyCast(rhsType,
              (JReferenceType) targetType)) {
            return;
          }
        }

        recordCastInternal((JReferenceType) targetType, rhsType);
      }
    }

    private void recordCastInternal(JReferenceType targetType,
        JReferenceType rhsType) {
      JReferenceType toType = targetType;
      Set/* <JReferenceType> */querySet = (Set) queriedTypes.get(toType);
      if (querySet == null) {
        queryIds.put(toType, new Integer(nextQueryId++));
        querySet = new HashSet/* <JReferenceType> */();
        queriedTypes.put(toType, querySet);
      }
      querySet.add(rhsType);
    }
  }

  /**
   * Explicitly convert any char-typed expressions within a concat operation
   * into strings.
   */
  private class ConcatVisitor extends JVisitor {

    private final ChangeList changeList = new ChangeList(
        "Convert chars to Strings inside of concat operations.");

    private JMethod stringValueOfChar = null;

    // @Override
    public void endVisit(JBinaryOperation x, Mutator m) {
      if (x.getType() != program.getTypeJavaLangString()) {
        return;
      }

      if (x.op == JBinaryOperator.ADD) {
        convertCharString(x.lhs);
        convertCharString(x.rhs);
      } else if (x.op == JBinaryOperator.ASG_ADD) {
        convertCharString(x.rhs);
      }
    }

    public ChangeList getChangeList() {
      return changeList;
    }

    private void convertCharString(Mutator m) {
      JPrimitiveType charType = program.getTypePrimitiveChar();
      JExpression expr = m.get();
      if (expr.getType() == charType) {
        if (stringValueOfChar == null) {
          stringValueOfChar = program.getSpecialMethod("Cast.charToString");
          assert (stringValueOfChar != null);
        }
        JMethodCall call = new JMethodCall(program, null, stringValueOfChar);
        ChangeList myChangeList = new ChangeList("Replace '" + expr
            + "' with a call to Cast.charToString()");
        myChangeList.addExpression(m, call.args);
        myChangeList.replaceExpression(m, call);
        changeList.add(myChangeList);
      }
    }
  }

  /**
   * Explicitly cast all integral divide operations to trigger replacements with
   * narrowing calls in the next pass.
   */
  private class DivVisitor extends JVisitor {

    private final ChangeList changeList = new ChangeList(
        "Explicitly cast all integral division operations.");

    // @Override
    public void endVisit(JBinaryOperation x, Mutator m) {
      JType type = x.getType();
      if (x.op == JBinaryOperator.DIV
          && type != program.getTypePrimitiveFloat()
          && type != program.getTypePrimitiveDouble()) {
        JCastOperation cast = new JCastOperation(program, type,
            program.getLiteralNull());
        ChangeList myChangeList = new ChangeList("Cast '" + x + "' to type '"
            + type + "'");
        myChangeList.changeType(x, program.getTypePrimitiveDouble());
        myChangeList.replaceExpression(cast.expr, m);
        myChangeList.replaceExpression(m, cast);
        changeList.add(myChangeList);
      }
    }

    public ChangeList getChangeList() {
      return changeList;
    }
  }

  private class ReplaceTypeChecksVisitor extends JVisitor {

    private final ChangeList changeList = new ChangeList(
        "Replace all casts and instanceof operations.");

    // @Override
    public void endVisit(JCastOperation x, Mutator m) {
      JType toType = x.castType;
      if (toType instanceof JReferenceType) {
        JReferenceType refType = (JReferenceType) toType;
        JType argType = x.getExpression().getType();
        if (program.isJavaScriptObject(argType)) {
          /*
           * A JSO-derived class that is about to be cast must be "wrapped"
           * first. Since a JSO was never constructed, it may not have an
           * accessible prototype. Instead we copy fields from the seed
           * function's prototype directly onto the target object as expandos.
           * See {@link com.google.gwt.lang.Cast#wrapJSO()}.
           */
          ChangeList myChangeList = new ChangeList("Wrap a JavaScript Object");
          JMethod wrap = program.getSpecialMethod("Cast.wrapJSO");
          // override the type of the called method with the JSO's type
          JMethodCall call = new JMethodCall(program, null, wrap, argType);
          myChangeList.addExpression(x.expr, call.args);
          JClassSeed seed = program.getLiteralClassSeed((JClassType) argType);
          myChangeList.addExpression(seed, call.args);
          myChangeList.replaceExpression(x.expr, call);
          changeList.add(myChangeList);
        }
        if (argType instanceof JClassType
            && program.typeOracle.canTriviallyCast((JClassType) argType,
                refType)) {
          // just remove the cast
          changeList.replaceExpression(m, x.expr);
        } else {
          JMethod method = program.getSpecialMethod("Cast.dynamicCast");
          // override the type of the called method with the target cast type
          JMethodCall call = new JMethodCall(program, null, method, toType);
          replaceCast(call, refType, m, x.expr);
        }
      } else {
        /*
         * See JLS 5.1.3: if a cast narrows from one type to another, we must
         * call a narrowing conversion function. EXCEPTION: we currently have no
         * way to narrow double to float, so don't bother.
         */
        boolean narrow = false, round = false;
        JPrimitiveType tByte = program.getTypePrimitiveByte();
        JPrimitiveType tChar = program.getTypePrimitiveChar();
        JPrimitiveType tShort = program.getTypePrimitiveShort();
        JPrimitiveType tInt = program.getTypePrimitiveInt();
        JPrimitiveType tLong = program.getTypePrimitiveLong();
        JPrimitiveType tFloat = program.getTypePrimitiveFloat();
        JPrimitiveType tDouble = program.getTypePrimitiveDouble();
        JType fromType = x.getExpression().getType();
        if (tByte == fromType) {
          if (tChar == toType) {
            narrow = true;
          }
        } else if (tShort == fromType) {
          if (tByte == toType || tChar == toType) {
            narrow = true;
          }
        } else if (tChar == fromType) {
          if (tByte == toType || tShort == toType) {
            narrow = true;
          }
        } else if (tInt == fromType) {
          if (tByte == toType || tShort == toType || tChar == toType) {
            narrow = true;
          }
        } else if (tLong == fromType) {
          if (tByte == toType || tShort == toType || tChar == toType
              || tInt == toType) {
            narrow = true;
          }
        } else if (tFloat == fromType || tDouble == fromType) {
          if (tByte == toType || tShort == toType || tChar == toType
              || tInt == toType || tLong == toType) {
            round = true;
          }
        }

        ChangeList myChangeList;
        if (narrow || round) {
          String methodName = "Cast." + (narrow ? "narrow_" : "round_")
              + toType.getName();
          myChangeList = new ChangeList("Replace '" + x + "' with a call to "
              + methodName);
          JMethod castMethod = program.getSpecialMethod(methodName);
          JMethodCall call = new JMethodCall(program, null, castMethod);
          myChangeList.addExpression(x.expr, call.args);
          myChangeList.replaceExpression(m, call);
        } else {
          myChangeList = new ChangeList("Remove the cast from '" + x + "'");
          myChangeList.replaceExpression(m, x.expr);
        }
        changeList.add(myChangeList);
      }
    }

    // @Override
    public void endVisit(JInstanceOf x, Mutator m) {
      JType argType = x.getExpression().getType();
      if (argType instanceof JClassType
          && program.typeOracle.canTriviallyCast((JClassType) argType,
              x.testType)) {
        // trivially true if non-null
        JNullLiteral nullLit = program.getLiteralNull();
        JBinaryOperation eq = new JBinaryOperation(program,
            program.getTypePrimitiveBoolean(), JBinaryOperator.NEQ, nullLit,
            nullLit);
        ChangeList myChangeList = new ChangeList("Replace '" + x
            + "' with a simple null test.");
        myChangeList.replaceExpression(eq.lhs, x.expr);
        myChangeList.replaceExpression(m, eq);
        changeList.add(myChangeList);
      } else {
        JMethod method = program.getSpecialMethod("Cast.instanceOf");
        JMethodCall call = new JMethodCall(program, null, method);
        replaceCast(call, x.testType, m, x.expr);
      }
    }

    public ChangeList getChangeList() {
      return changeList;
    }

    private void replaceCast(JMethodCall call, JReferenceType type,
        Mutator dest, Mutator arg) {
      Integer boxedInt = (Integer) queryIds.get(type);
      JIntLiteral qId = program.getLiteralInt(boxedInt.intValue());
      ChangeList myChangeList = new ChangeList("Replace '" + dest.get()
          + " with a call to " + call.getTarget().getName());
      myChangeList.addExpression(arg, call.args);
      myChangeList.addExpression(qId, call.args);
      myChangeList.replaceExpression(dest, call);
      changeList.add(myChangeList);
    }
  }

  public static void exec(JProgram program) {
    new CastNormalizer(program).execImpl();
  }

  private Map/* <JReferenceType, Integer> */queryIds = new IdentityHashMap();

  private final JProgram program;

  private CastNormalizer(JProgram program) {
    this.program = program;
  }

  private void execImpl() {
    {
      ConcatVisitor visitor = new ConcatVisitor();
      program.traverse(visitor);
      ChangeList changes = visitor.getChangeList();
      if (!changes.empty()) {
        changes.apply();
      }
    }
    {
      DivVisitor visitor = new DivVisitor();
      program.traverse(visitor);
      ChangeList changes = visitor.getChangeList();
      if (!changes.empty()) {
        changes.apply();
      }
    }
    {
      AssignTypeIdsVisitor assigner = new AssignTypeIdsVisitor();
      program.traverse(assigner);
      assigner.computeTypeIds();
    }
    {
      ReplaceTypeChecksVisitor replacer = new ReplaceTypeChecksVisitor();
      program.traverse(replacer);
      ChangeList changes = replacer.getChangeList();
      if (!changes.empty()) {
        changes.apply();
      }
    }
  }

}
