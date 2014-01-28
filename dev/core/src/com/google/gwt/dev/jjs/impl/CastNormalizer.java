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
import com.google.gwt.dev.jjs.SourceOrigin;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JArrayRef;
import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JCastMap;
import com.google.gwt.dev.jjs.ast.JCastOperation;
import com.google.gwt.dev.jjs.ast.JCharLiteral;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JInstanceOf;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JNullLiteral;
import com.google.gwt.dev.jjs.ast.JNullType;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JRuntimeTypeReference;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JTypeOracle;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.thirdparty.guava.common.collect.HashMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Replace cast and instanceof operations with calls to the Cast class. Depends
 * on {@link CatchBlockNormalizer}, {@link CompoundAssignmentNormalizer},
 * {@link JsoDevirtualizer}, and {@link LongCastNormalizer} having already run.
 */
public class CastNormalizer {
  private class AssignTypeCastabilityVisitor extends JVisitor {

    private final Set<JReferenceType> alreadyRan = Sets.newHashSet();
    private final Map<JReferenceType, JCastMap> castableTypesMap = Maps.newIdentityHashMap();
    private final List<JArrayType> instantiatedArrayTypes = Lists.newArrayList();
    private final Multimap<JReferenceType, JReferenceType> castSourceTypesPerCastTargetType =
        HashMultimap.create();

    {
      JTypeOracle typeOracle = program.typeOracle;
      for (JArrayType arrayType : program.getAllArrayTypes()) {
        if (typeOracle.isInstantiatedType(arrayType)) {
          instantiatedArrayTypes.add(arrayType);
        }
      }

      // Force entries for Object and String.
      recordCastInternal(program.getTypeJavaLangObject(), program.getTypeJavaLangObject());
      recordCastInternal(program.getTypeJavaLangString(), program.getTypeJavaLangObject());

      // Force entries for interfaces implemented by  String
      recordCastInternal(program.getIndexedType("Serializable"), program.getTypeJavaLangObject());
      recordCastInternal(program.getIndexedType("CharSequence"), program.getTypeJavaLangObject());
      recordCastInternal(program.getIndexedType("Comparable"), program.getTypeJavaLangObject());
    }

    public void computeTypeCastabilityMaps() {
      // do String first (which will pull in Object also, it's superclass).
      computeSourceType(program.getTypeJavaLangString());
      assert (castableTypesMap.size() == 2);

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

      for (JArrayType type : instantiatedArrayTypes) {
        computeSourceType(type);
      }

      // pass our info to JProgram
      program.initTypeInfo(castableTypesMap);
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

        JArrayType lhsArrayType = lhsArrayRef.getArrayType();
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

    private boolean canTriviallyCastJsoSemantics(JReferenceType type, JReferenceType qType) {
      type = type.getUnderlyingType();
      qType = qType.getUnderlyingType();

      if (type instanceof JArrayType && qType instanceof JArrayType) {
        JArrayType aType = (JArrayType) type;
        JArrayType aqType = (JArrayType) qType;
        return program.typeOracle.canTriviallyCast(type, qType)
            || (program.isJavaScriptObject(aType.getLeafType()) && program
                .isJavaScriptObject(aqType.getLeafType()));
      }

      return program.typeOracle.canTriviallyCast(type, qType)
          || (program.isJavaScriptObject(type) && program.isJavaScriptObject(qType));
    }

    /**
     * Create the mapping from a class to its query types.
     */
    private void computeSourceType(JReferenceType type) {
      if (type == null || alreadyRan.contains(type)) {
        return;
      }
      assert (type == type.getUnderlyingType());

      alreadyRan.add(type);

      // Visit super type.
      if (type instanceof JClassType) {
        computeSourceType(((JClassType) type).getSuperClass());
      }

      if (!program.typeOracle.isInstantiatedType(type) || program.isJavaScriptObject(type)) {
        return;
      }

      // Find all possible query types which I can satisfy
      Set<JReferenceType> castableTypes = new TreeSet<JReferenceType>(TYPE_COMPARATOR);

      /*
       * NOTE: non-deterministic iteration over HashSet and HashMap. Okay
       * because we're sorting the results.
       */
      for (JReferenceType castTargetType : castSourceTypesPerCastTargetType.keySet()) {

        if (!canTriviallyCastJsoSemantics(type, castTargetType)) {
          continue;
        }

        Collection<JReferenceType> castSourceTypes =
            castSourceTypesPerCastTargetType.get(castTargetType);
        /**
         * Handles JSO[] -> JSO[] case now that canCastTrivially doesn't deal
         * with JSO cross-casts anymore.
         */
        for (JReferenceType castSourceType : castSourceTypes) {
          if (canTriviallyCastJsoSemantics(type, castSourceType) ||
              program.isJavaScriptObject(castTargetType)) {
            if (castTargetType != program.getTypeJavaLangObject() &&
                castTargetType != program.getJavaScriptObject()) {
              // ignore java.lang.Object.
              castableTypes.add(castTargetType);
            }
            break;
          }
        }
      }

      /*
       * Don't add an entry for empty answer sets, except for Object and String
       * which require entries.
       */
      if (castableTypes.isEmpty() && type != program.getTypeJavaLangObject()
          && type != program.getTypeJavaLangString()) {
        return;
      }

      // add an entry for me
      castableTypesMap.put(type, new JCastMap(SourceOrigin.UNKNOWN, program.getTypeJavaLangObject(),
          Collections.unmodifiableSet(castableTypes)));
    }

    private void recordCast(JType targetType, JExpression rhs) {
      if (targetType instanceof JReferenceType) {
        targetType = ((JReferenceType) targetType).getUnderlyingType();
        // unconditional cast b/c it would've been a semantic error earlier
        JReferenceType rhsType = ((JReferenceType) rhs.getType()).getUnderlyingType();
        // don't record a type for trivial casts that won't generate code
        if (program.typeOracle.canTriviallyCast(rhsType, (JReferenceType) targetType)) {
          return;
        }

        // If the target type is a JavaScriptObject, don't record an id.
        if (program.isJavaScriptObject(targetType)) {
          return;
        }

        recordCastInternal((JReferenceType) targetType, rhsType);
      }
    }

    private void recordCastInternal(JReferenceType toType, JReferenceType rhsType) {
      toType = toType.getUnderlyingType();
      rhsType = rhsType.getUnderlyingType();
      castSourceTypesPerCastTargetType.put(toType, rhsType);
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
          JBinaryOperation newExpr =
              new JBinaryOperation(x.getSourceInfo(), program.getTypeJavaLangString(),
                  JBinaryOperator.CONCAT, newLhs, newRhs);
          ctx.replaceMe(newExpr);
        }
      } else if (x.getOp() == JBinaryOperator.ASG_CONCAT) {
        JExpression newRhs = convertString(x.getRhs());
        if (newRhs != x.getRhs()) {
          JBinaryOperation newExpr =
              new JBinaryOperation(x.getSourceInfo(), program.getTypeJavaLangString(),
                  JBinaryOperator.ASG_CONCAT, x.getLhs(), newRhs);
          ctx.replaceMe(newExpr);
        }
      }
    }

    private JExpression convertString(JExpression expr) {
      JPrimitiveType charType = program.getTypePrimitiveChar();
      if (expr.getType() == charType) {
        if (expr instanceof JCharLiteral) {
          JCharLiteral charLit = (JCharLiteral) expr;
          return program.getLiteralString(expr.getSourceInfo(), new char[]{charLit.getValue()});
        } else {
          // Replace with Cast.charToString(c)
          JMethodCall call =
              new JMethodCall(expr.getSourceInfo(), null, program
                  .getIndexedMethod("Cast.charToString"));
          call.addArg(expr);
          return call;
        }
      } else if (expr.getType() == program.getTypePrimitiveLong()) {
        // Replace with LongLib.toString(l)
        JMethodCall call =
            new JMethodCall(expr.getSourceInfo(), null, program
                .getIndexedMethod("LongLib.toString"));
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
      if (x.getOp() == JBinaryOperator.DIV && type != program.getTypePrimitiveFloat()
          && type != program.getTypePrimitiveDouble()) {
        /*
         * If the numerator was already in range, we can assume the output is
         * also in range. Therefore, we don't need to do the full conversion,
         * but rather a narrowing int conversion instead.
         */
        String methodName = "Cast.narrow_" + type.getName();
        JMethod castMethod = program.getIndexedMethod(methodName);
        JMethodCall call = new JMethodCall(x.getSourceInfo(), null, castMethod, type);
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
        // Just leave the cast in, GenerateJavaScriptAST will ignore it.
        return;
      }
      SourceInfo info = x.getSourceInfo();
      if (toType instanceof JNullType) {
        /**
         * A null type cast is used as a placeholder value to indicate that the
         * user tried a cast that couldn't possibly work. Typically this means
         * either the statically resolvable arg type is incompatible with the
         * target type, or the target type was globally uninstantiable.
         *
         * See {@link com.google.gwt.dev.jjs.impl.TypeTightener.TightenTypesVisitor#endVisit(JCastOperation,
         * Context)}
         *
         * We handle this cast by throwing a ClassCastException, unless the
         * argument is null.
         */
        JMethod method = program.getIndexedMethod("Cast.throwClassCastExceptionUnlessNull");
        // Note, we must update the method call to return the null type.
        JMethodCall call = new JMethodCall(info, null, method, toType);
        call.addArg(expr);
        replaceExpr = call;
      } else if (toType instanceof JReferenceType) {
        JExpression curExpr = expr;
        JReferenceType refType = ((JReferenceType) toType).getUnderlyingType();
        JReferenceType argType = (JReferenceType) expr.getType();
        if (program.typeOracle.canTriviallyCast(argType, refType)
            || (program.typeOracle.isEffectivelyJavaScriptObject(argType) && program.typeOracle
                .isEffectivelyJavaScriptObject(refType))) {
          // just remove the cast
          replaceExpr = curExpr;
        } else {
          // A cast is still needed.  Substitute the appropriate Cast implementation.
          JMethod method;
          boolean isJsoCast = program.typeOracle.isEffectivelyJavaScriptObject(refType);
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
          JMethodCall call = new JMethodCall(info, null, method, toType);
          call.addArg(curExpr);
          if (!isJsoCast) {
            call.addArg((new JRuntimeTypeReference(x.getSourceInfo(), program.getTypeJavaLangObject(),
                refType)));
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
            JMethodCall call = new JMethodCall(info, null, castMethod);
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
          if (tByte == fromType || tShort == fromType || tChar == fromType || tInt == fromType) {
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
          if (tByte == toType || tShort == toType || tChar == toType || tInt == toType) {
            methodName = "Cast.round_" + toType.getName();
          }
        }

        if (methodName != null) {
          JMethod castMethod = program.getIndexedMethod(methodName);
          JMethodCall call = new JMethodCall(info, null, castMethod, toType);
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
      assert (toType == toType.getUnderlyingType());
      if (program.typeOracle.canTriviallyCast(argType, toType)
      // don't depend on type-tightener having run
          || (program.typeOracle.isEffectivelyJavaScriptObject(argType) && program.typeOracle
              .isEffectivelyJavaScriptObject(toType))) {
        // trivially true if non-null; replace with a null test
        JNullLiteral nullLit = program.getLiteralNull();
        JBinaryOperation eq =
            new JBinaryOperation(x.getSourceInfo(), program.getTypePrimitiveBoolean(),
                JBinaryOperator.NEQ, x.getExpr(), nullLit);
        ctx.replaceMe(eq);
      } else {
        JMethod method;
        boolean isJsoCast = false;
        if (program.typeOracle.isDualJsoInterface(toType)) {
          method = program.getIndexedMethod("Cast.instanceOfOrJso");
        } else if (program.typeOracle.isEffectivelyJavaScriptObject(toType)) {
          isJsoCast = true;
          method = program.getIndexedMethod("Cast.instanceOfJso");
        } else {
          method = program.getIndexedMethod("Cast.instanceOf");
        }
        JMethodCall call = new JMethodCall(x.getSourceInfo(), null, method);
        call.addArg(x.getExpr());
        if (!isJsoCast) {
          call.addArg((new JRuntimeTypeReference(x.getSourceInfo(), program.getTypeJavaLangObject(), toType)));
        }
        ctx.replaceMe(call);
      }
    }
  }

  private static final Comparator<JType> TYPE_COMPARATOR = new Comparator<JType>() {
    @Override
    public int compare(JType o1, JType o2) {
      return o1.getName().compareTo(o2.getName());
    }
  };

  public static void exec(JProgram program, boolean disableCastChecking) {
    new CastNormalizer(program, disableCastChecking).execImpl();
  }

  private final boolean disableCastChecking;

  private final JProgram program;

  private Map<JReferenceType, Integer> queryIdsByType;

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
      AssignTypeCastabilityVisitor assigner = new AssignTypeCastabilityVisitor();
      assigner.accept(program);
      assigner.computeTypeCastabilityMaps();
    }
    {
      ReplaceTypeChecksVisitor replacer = new ReplaceTypeChecksVisitor();
      replacer.accept(program);
    }
  }
}
