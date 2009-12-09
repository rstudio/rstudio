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
import com.google.gwt.dev.jjs.ast.JCastOperation;
import com.google.gwt.dev.jjs.ast.JCharLiteral;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JInstanceOf;
import com.google.gwt.dev.jjs.ast.JIntLiteral;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JNullLiteral;
import com.google.gwt.dev.jjs.ast.JNullType;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JTypeOracle;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.js.JsonObject;
import com.google.gwt.dev.jjs.ast.js.JsonObject.JsonPropInit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Replace cast and instanceof operations with calls to the Cast class. Depends
 * on {@link CatchBlockNormalizer}, {@link CompoundAssignmentNormalizer},
 * {@link JsoDevirtualizer}, and {@link LongCastNormalizer} having already run.
 * 
 * <p>
 * Object and String always get a typeId of 1 and 2, respectively. 0 is reserved
 * as the typeId for any classes that can never be instance type of a successful
 * dynamic cast.
 * </p>
 * <p>
 * Object and String always get a queryId of 0 and 1, respectively. The 0
 * queryId always means "always succeeds". In practice, we never generate an
 * explicit cast with a queryId of 0; it is only used for array store checking,
 * where the 0 queryId means that anything can be stored into an Object[].
 * </p>
 * <p>
 * JavaScriptObject and subclasses have no typeId at all. JavaScriptObject has a
 * queryId of -1, which again is only used for array store checking, to ensure
 * that a non-JSO is not stored into a JavaScriptObject[].
 * </p>
 */
public class CastNormalizer {
  private class AssignTypeIdsVisitor extends JVisitor {

    Set<JReferenceType> alreadyRan = new HashSet<JReferenceType>();
    private List<JReferenceType> instantiableTypes = new ArrayList<JReferenceType>();
    private final List<JArrayType> instantiatedArrayTypes = new ArrayList<JArrayType>();
    private List<JsonObject> jsonObjects = new ArrayList<JsonObject>();
    private int nextQueryId = 0;
    private Map<JReferenceType, Set<JReferenceType>> queriedTypes = new IdentityHashMap<JReferenceType, Set<JReferenceType>>();

    {
      JTypeOracle typeOracle = program.typeOracle;
      for (JArrayType arrayType : program.getAllArrayTypes()) {
        if (typeOracle.isInstantiatedType(arrayType)) {
          instantiatedArrayTypes.add(arrayType);
        }
      }

      // Reserve query id 0 for java.lang.Object (for array stores on JSOs).
      recordCastInternal(program.getTypeJavaLangObject(),
          program.getTypeJavaLangObject());

      // Reserve query id 1 for java.lang.String to facilitate the mashup case.
      // Multiple GWT modules need to modify String's prototype the same way.
      recordCastInternal(program.getTypeJavaLangString(),
          program.getTypeJavaLangObject());
    }

    public void computeTypeIds() {

      // the 0th entry is the "always false" entry
      instantiableTypes.add(null);
      jsonObjects.add(new JsonObject(program.createSourceInfoSynthetic(
          AssignTypeIdsVisitor.class, "always-false typeinfo entry"),
          program.getJavaScriptObject()));

      /*
       * Do String first to reserve typeIds 1 and 2 for Object and String,
       * respectively. This ensures consistent modification of String's
       * prototype.
       */
      computeSourceType(program.getTypeJavaLangString());
      assert (instantiableTypes.size() == 3);

      /*
       * Compute the list of classes than can successfully satisfy cast
       * requests, along with the set of types they can be successfully cast to.
       * Do it in super type order.
       */
      for (JReferenceType type : program.getDeclaredTypes()) {
        if (type instanceof JClassType) {
          computeSourceType(type);
        }
      }

      for (JArrayType type : program.getAllArrayTypes()) {
        computeSourceType(type);
      }

      // pass our info to JProgram
      program.initTypeInfo(instantiableTypes, jsonObjects);

      // JSO's maker queryId is -1 (used for array stores).
      JClassType jsoType = program.getJavaScriptObject();
      if (jsoType != null) {
        queryIds.put(jsoType, -1);
      }
      program.recordQueryIds(queryIds);
    }

    /*
     * If this expression could possibly generate an ArrayStoreException, we
     * must record a query on the element type being assigned to.
     */
    @Override
    public void endVisit(JBinaryOperation x, Context ctx) {
      if (x.getOp().isAssignment() && x.getLhs() instanceof JArrayRef) {

        // first, calculate the transitive closure of all possible runtime types
        // the lhs could be
        JArrayRef lhsArrayRef = (JArrayRef) x.getLhs();
        JType elementType = lhsArrayRef.getType();
        if (elementType instanceof JNullType) {
          // will generate a null pointer exception instead
          return;
        }
        JArrayType lhsArrayType = lhsArrayRef.getArrayType();

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

        for (JArrayType arrayType : instantiatedArrayTypes) {
          if (typeOracle.canTheoreticallyCast(arrayType, lhsArrayType)) {
            JType itElementType = arrayType.getElementType();
            if (itElementType instanceof JReferenceType) {
              recordCast(itElementType, x.getRhs());
            }
          }
        }
      }
    }

    @Override
    public void endVisit(JCastOperation x, Context ctx) {
      if (disableCastChecking) {
        return;
      }
      if (x.getCastType() != program.getTypeNull()) {
        recordCast(x.getCastType(), x.getExpr());
      }
    }

    @Override
    public void endVisit(JInstanceOf x, Context ctx) {
      assert (x.getTestType() != program.getTypeNull());
      recordCast(x.getTestType(), x.getExpr());
    }

    /**
     * Create the data for JSON table to capture the mapping from a class to its
     * query types.
     */
    private void computeSourceType(JReferenceType type) {
      if (type == null || alreadyRan.contains(type)) {
        return;
      }
      assert (type == program.getRunTimeType(type));

      alreadyRan.add(type);

      // Visit super type.
      computeSourceType(type.getSuperClass());

      if (!program.typeOracle.isInstantiatedType(type)
          || program.isJavaScriptObject(type)) {
        return;
      }

      // Find all possible query types which I can satisfy
      Set<JReferenceType> yesSet = null;

      // NOTE: non-deterministic iteration over HashSet and HashMap. This is
      // okay here because we're just adding things to another HashSet.
      for (JReferenceType qType : queriedTypes.keySet()) {

        Set<JReferenceType> querySet = queriedTypes.get(qType);
        if (program.typeOracle.canTriviallyCast(type, qType)) {

          for (JReferenceType argType : querySet) {

            if (program.typeOracle.canTriviallyCast(type, argType)) {
              if (yesSet == null) {
                yesSet = new HashSet<JReferenceType>();
              }
              yesSet.add(qType);
              break;
            }
          }
        }
      }

      // Use a sparse array to sort my yes set.
      JReferenceType[] yesArray = new JReferenceType[nextQueryId];
      if (yesSet != null) {
        for (JReferenceType yesType : yesSet) {
          yesArray[queryIds.get(yesType)] = yesType;
        }
      }

      // Create a sparse lookup object.
      SourceInfo sourceInfo = program.createSourceInfoSynthetic(
          AssignTypeIdsVisitor.class, "typeinfo lookup");
      JsonObject jsonObject = new JsonObject(sourceInfo,
          program.getJavaScriptObject());
      // Start at 1; 0 is Object and always true.
      for (int i = 1; i < nextQueryId; ++i) {
        if (yesArray[i] != null) {
          JIntLiteral labelExpr = program.getLiteralInt(i);
          JIntLiteral valueExpr = program.getLiteralInt(1);
          jsonObject.propInits.add(new JsonPropInit(sourceInfo, labelExpr,
              valueExpr));
        }
      }

      /*
       * Don't add an entry for empty answer sets, except for Object and String
       * which require typeIds.
       */
      if (jsonObject.propInits.isEmpty()
          && type != program.getTypeJavaLangObject()
          && type != program.getTypeJavaLangString()) {
        return;
      }

      // add an entry for me
      instantiableTypes.add(type);
      jsonObjects.add(jsonObject);
    }

    private void recordCast(JType targetType, JExpression rhs) {
      if (targetType instanceof JReferenceType) {
        targetType = program.getRunTimeType((JReferenceType) targetType);
        // unconditional cast b/c it would've been a semantic error earlier
        JReferenceType rhsType = program.getRunTimeType((JReferenceType) rhs.getType());
        // don't record a type for trivial casts that won't generate code
        if (program.typeOracle.canTriviallyCast(rhsType,
            (JReferenceType) targetType)) {
          return;
        }

        // If the target type is a JavaScriptObject, don't record an id.
        if (program.isJavaScriptObject(targetType)) {
          return;
        }

        recordCastInternal((JReferenceType) targetType, rhsType);
      }
    }

    private void recordCastInternal(JReferenceType toType,
        JReferenceType rhsType) {
      toType = program.getRunTimeType(toType);
      rhsType = program.getRunTimeType(rhsType);
      Set<JReferenceType> querySet = queriedTypes.get(toType);
      if (querySet == null) {
        queryIds.put(toType, nextQueryId++);
        querySet = new HashSet<JReferenceType>();
        queriedTypes.put(toType, querySet);
      }
      querySet.add(rhsType);
    }
  }

  /**
   * Explicitly convert any char or long type expressions within a concat
   * operation into strings because normal JavaScript conversion does not work
   * correctly.
   */
  private class ConcatVisitor extends JModVisitor {

    @Override
    public void endVisit(JBinaryOperation x, Context ctx) {
      if (x.getOp() == JBinaryOperator.CONCAT) {
        JExpression newLhs = convertString(x.getLhs());
        JExpression newRhs = convertString(x.getRhs());
        if (newLhs != x.getLhs() || newRhs != x.getRhs()) {
          JBinaryOperation newExpr = new JBinaryOperation(x.getSourceInfo(),
              program.getTypeJavaLangString(), JBinaryOperator.CONCAT, newLhs,
              newRhs);
          ctx.replaceMe(newExpr);
        }
      } else if (x.getOp() == JBinaryOperator.ASG_CONCAT) {
        JExpression newRhs = convertString(x.getRhs());
        if (newRhs != x.getRhs()) {
          JBinaryOperation newExpr = new JBinaryOperation(x.getSourceInfo(),
              program.getTypeJavaLangString(), JBinaryOperator.ASG_CONCAT,
              x.getLhs(), newRhs);
          ctx.replaceMe(newExpr);
        }
      }
    }

    private JExpression convertString(JExpression expr) {
      JPrimitiveType charType = program.getTypePrimitiveChar();
      if (expr.getType() == charType) {
        if (expr instanceof JCharLiteral) {
          JCharLiteral charLit = (JCharLiteral) expr;
          return program.getLiteralString(expr.getSourceInfo(),
              new char[] {charLit.getValue()});
        } else {
          // Replace with Cast.charToString(c)
          JMethodCall call = new JMethodCall(expr.getSourceInfo(), null,
              program.getIndexedMethod("Cast.charToString"));
          call.addArg(expr);
          return call;
        }
      } else if (expr.getType() == program.getTypePrimitiveLong()) {
        // Replace with LongLib.toString(l)
        JMethodCall call = new JMethodCall(expr.getSourceInfo(), null,
            program.getIndexedMethod("LongLib.toString"));
        call.addArg(expr);
        return call;
      }
      return expr;
    }
  }

  /**
   * Handle integral divide operations which may have floating point results.
   */
  private class DivVisitor extends JModVisitor {

    @Override
    public void endVisit(JBinaryOperation x, Context ctx) {
      JType type = x.getType();
      if (x.getOp() == JBinaryOperator.DIV
          && type != program.getTypePrimitiveFloat()
          && type != program.getTypePrimitiveDouble()) {
        /*
         * If the numerator was already in range, we can assume the output is
         * also in range. Therefore, we don't need to do the full conversion,
         * but rather a narrowing int conversion instead.
         */
        String methodName = "Cast.narrow_" + type.getName();
        JMethod castMethod = program.getIndexedMethod(methodName);
        JMethodCall call = new JMethodCall(x.getSourceInfo(), null, castMethod,
            type);
        x.setType(program.getTypePrimitiveDouble());
        call.addArg(x);
        ctx.replaceMe(call);
      }
    }
  }

  /**
   * Replaces all casts and instanceof operations with calls to implementation
   * methods.
   */
  private class ReplaceTypeChecksVisitor extends JModVisitor {

    @Override
    public void endVisit(JCastOperation x, Context ctx) {
      JExpression replaceExpr;
      JType toType = x.getCastType();
      JExpression expr = x.getExpr();
      if (disableCastChecking && toType instanceof JReferenceType) {
        ctx.replaceMe(expr);
        return;
      }
      if (toType instanceof JNullType) {
        /*
         * Magic: a null type cast means the user tried a cast that couldn't
         * possibly work. Typically this means either the statically resolvable
         * arg type is incompatible with the target type, or the target type was
         * globally uninstantiable. We handle this cast by throwing a
         * ClassCastException, unless the argument is null.
         */
        JMethod method = program.getIndexedMethod("Cast.throwClassCastExceptionUnlessNull");
        /*
         * Override the type of the magic method with the null type.
         */
        JMethodCall call = new JMethodCall(x.getSourceInfo(), null, method,
            program.getTypeNull());
        call.addArg(expr);
        replaceExpr = call;
      } else if (toType instanceof JReferenceType) {
        JExpression curExpr = expr;
        JReferenceType refType = program.getRunTimeType((JReferenceType) toType);
        JReferenceType argType = (JReferenceType) expr.getType();
        if (program.typeOracle.canTriviallyCast(argType, refType)) {
          // just remove the cast
          replaceExpr = curExpr;
        } else {

          JMethod method;
          boolean isJsoCast = program.isJavaScriptObject(refType);
          if (isJsoCast) {
            // A cast to a concrete JSO subtype
            method = program.getIndexedMethod("Cast.dynamicCastJso");
          } else if (program.typeOracle.isDualJsoInterface(refType)) {
            // An interface that should succeed when the object is a JSO
            method = program.getIndexedMethod("Cast.dynamicCastAllowJso");
          } else {
            // A regular cast
            method = program.getIndexedMethod("Cast.dynamicCast");
          }
          // override the type of the called method with the target cast type
          JMethodCall call = new JMethodCall(x.getSourceInfo(), null, method,
              toType);
          call.addArg(curExpr);
          if (!isJsoCast) {
            JIntLiteral qId = program.getLiteralInt(queryIds.get(refType));
            call.addArg(qId);
          }
          replaceExpr = call;
        }
      } else {
        /*
         * See JLS 5.1.3: if a cast narrows from one type to another, we must
         * call a narrowing conversion function. EXCEPTION: we currently have no
         * way to narrow double to float, so don't bother.
         */
        JPrimitiveType tByte = program.getTypePrimitiveByte();
        JPrimitiveType tChar = program.getTypePrimitiveChar();
        JPrimitiveType tShort = program.getTypePrimitiveShort();
        JPrimitiveType tInt = program.getTypePrimitiveInt();
        JPrimitiveType tLong = program.getTypePrimitiveLong();
        JPrimitiveType tFloat = program.getTypePrimitiveFloat();
        JPrimitiveType tDouble = program.getTypePrimitiveDouble();
        JType fromType = expr.getType();

        String methodName = null;

        if (tLong == fromType && tLong != toType) {
          if (tByte == toType || tShort == toType || tChar == toType) {
            /*
             * We need a double call here, one to convert long->int, and another
             * one to narrow. Construct the inner call here and fall through to
             * do the narrowing conversion.
             */
            JMethod castMethod = program.getIndexedMethod("LongLib.toInt");
            JMethodCall call = new JMethodCall(x.getSourceInfo(), null,
                castMethod);
            call.addArg(expr);
            expr = call;
            fromType = tInt;
          } else if (tInt == toType) {
            methodName = "LongLib.toInt";
          } else if (tFloat == toType || tDouble == toType) {
            methodName = "LongLib.toDouble";
          }
        }

        if (toType == tLong && fromType != tLong) {
          // Longs get special treatment.
          if (tByte == fromType || tShort == fromType || tChar == fromType
              || tInt == fromType) {
            methodName = "LongLib.fromInt";
          } else if (tFloat == fromType || tDouble == fromType) {
            methodName = "LongLib.fromDouble";
          }
        } else if (tByte == fromType) {
          if (tChar == toType) {
            methodName = "Cast.narrow_" + toType.getName();
          }
        } else if (tShort == fromType) {
          if (tByte == toType || tChar == toType) {
            methodName = "Cast.narrow_" + toType.getName();
          }
        } else if (tChar == fromType) {
          if (tByte == toType || tShort == toType) {
            methodName = "Cast.narrow_" + toType.getName();
          }
        } else if (tInt == fromType) {
          if (tByte == toType || tShort == toType || tChar == toType) {
            methodName = "Cast.narrow_" + toType.getName();
          }
        } else if (tFloat == fromType || tDouble == fromType) {
          if (tByte == toType || tShort == toType || tChar == toType
              || tInt == toType) {
            methodName = "Cast.round_" + toType.getName();
          }
        }

        if (methodName != null) {
          JMethod castMethod = program.getIndexedMethod(methodName);
          JMethodCall call = new JMethodCall(x.getSourceInfo(), null,
              castMethod, toType);
          call.addArg(expr);
          replaceExpr = call;
        } else {
          // Just remove the cast
          replaceExpr = expr;
        }
      }
      ctx.replaceMe(replaceExpr);
    }

    @Override
    public void endVisit(JInstanceOf x, Context ctx) {
      JReferenceType argType = (JReferenceType) x.getExpr().getType();
      JReferenceType toType = x.getTestType();
      // Only tests on run-time types are supported
      assert (toType == program.getRunTimeType(toType));
      if (program.typeOracle.canTriviallyCast(argType, toType)) {
        // trivially true if non-null; replace with a null test
        JNullLiteral nullLit = program.getLiteralNull();
        JBinaryOperation eq = new JBinaryOperation(x.getSourceInfo(),
            program.getTypePrimitiveBoolean(), JBinaryOperator.NEQ,
            x.getExpr(), nullLit);
        ctx.replaceMe(eq);
      } else {
        JMethod method;
        boolean isJsoCast = false;
        if (program.typeOracle.getSingleJsoImpl(toType) != null) {
          method = program.getIndexedMethod("Cast.instanceOfOrJso");
        } else if (program.isJavaScriptObject(toType)) {
          isJsoCast = true;
          method = program.getIndexedMethod("Cast.instanceOfJso");
        } else {
          method = program.getIndexedMethod("Cast.instanceOf");
        }
        JMethodCall call = new JMethodCall(x.getSourceInfo(), null, method);
        call.addArg(x.getExpr());
        if (!isJsoCast) {
          JIntLiteral qId = program.getLiteralInt(queryIds.get(toType));
          call.addArg(qId);
        }
        ctx.replaceMe(call);
      }
    }
  }

  public static void exec(JProgram program, boolean disableCastChecking) {
    new CastNormalizer(program, disableCastChecking).execImpl();
  }

  private final boolean disableCastChecking;

  private final JProgram program;

  private Map<JReferenceType, Integer> queryIds = new IdentityHashMap<JReferenceType, Integer>();

  private CastNormalizer(JProgram program, boolean disableCastChecking) {
    this.program = program;
    this.disableCastChecking = disableCastChecking;
  }

  private void execImpl() {
    {
      ConcatVisitor visitor = new ConcatVisitor();
      visitor.accept(program);
    }
    {
      DivVisitor visitor = new DivVisitor();
      visitor.accept(program);
    }
    {
      AssignTypeIdsVisitor assigner = new AssignTypeIdsVisitor();
      assigner.accept(program);
      assigner.computeTypeIds();
    }
    {
      ReplaceTypeChecksVisitor replacer = new ReplaceTypeChecksVisitor();
      replacer.accept(program);
    }
  }

}
