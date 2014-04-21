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
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JCastOperation;
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
import com.google.gwt.thirdparty.guava.common.collect.Maps;

import java.util.EnumSet;
import java.util.Map;

/**
 * Replace cast and instanceof operations with calls to the Cast class. Depends
 * on {@link CatchBlockNormalizer}, {@link CompoundAssignmentNormalizer},
 * {@link Devirtualizer}, and {@link LongCastNormalizer} having already run.
 */
public class ImplementCastsAndTypeChecks {
  /**
   * Replaces all casts and instanceof operations with calls to implementation
   * methods.
   */
  private class ReplaceTypeChecksVisitor extends JModVisitor {

    @Override
    public void endVisit(JCastOperation x, Context ctx) {
      JType toType = x.getCastType();
      JExpression expr = x.getExpr();

      // Even if disableCastChecking is enabled, we need to rescue JSOs
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
         * See {@link TypeTightener.TightenTypesVisitor#endVisit(JCastOperation, Context)}
         *
         * We handle this cast by throwing a ClassCastException, unless the
         * argument is null.
         */
        JMethod method = program.getIndexedMethod("Cast.throwClassCastExceptionUnlessNull");
        // Note, we must update the method call to return the null type.
        JMethodCall call = new JMethodCall(info, null, method, toType);
        call.addArg(expr);
        ctx.replaceMe(call);
        return;
      }

      if (toType instanceof JReferenceType) {
        JExpression curExpr = expr;
        JReferenceType refType = ((JReferenceType) toType).getUnderlyingType();
        JReferenceType argType = (JReferenceType) expr.getType();
        if (program.typeOracle.canTriviallyCast(argType, refType)) {
          // just remove the cast
          ctx.replaceMe(curExpr);
          return;
        } else if (program.typeOracle.willCrossCastLikeJso(argType)
            && program.typeOracle.willCrossCastLikeJso(refType)) {
          // leave the cast instance for Pruner/CFA, remove in GenJSAST
          return;
        }
        // A cast is still needed.  Substitute the appropriate Cast implementation.
        ctx.replaceMe(implementCastOrInstanceOfOperation(x.getSourceInfo(), curExpr, refType,
            dynamicCastMethodsByTargetTypeCategory, true));
        return;
      }

      // It is a primitive type, perform the necessary coercion.

      assert toType instanceof JPrimitiveType;
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
        ctx.replaceMe(call);
      } else {
        // Just remove the cast
        ctx.replaceMe(expr);
      }
    }

    @Override
    public void endVisit(JInstanceOf x, Context ctx) {
      JReferenceType argType = (JReferenceType) x.getExpr().getType();
      JReferenceType toType = x.getTestType();
      // Only tests on run-time types are supported
      assert (toType == toType.getUnderlyingType());
      if (program.typeOracle.canTriviallyCast(argType, toType)
          // don't depend on type-tightener having run
          || (program.typeOracle.willCrossCastLikeJso(argType) && program.typeOracle
          .willCrossCastLikeJso(toType))) {
        // trivially true if non-null; replace with a null test
        JNullLiteral nullLit = program.getLiteralNull();
        JBinaryOperation eq =
            new JBinaryOperation(x.getSourceInfo(), program.getTypePrimitiveBoolean(),
                JBinaryOperator.NEQ, x.getExpr(), nullLit);
        ctx.replaceMe(eq);
      } else {
        // Replace the instance of check by a call to the appropriate instanceof method in class
        // Cast.
        ctx.replaceMe(implementCastOrInstanceOfOperation(x.getSourceInfo(), x.getExpr(), toType,
            instanceOfMethodsByTargetTypeCategory, false));
      }
    }
  }

  /**
   * Determines the type category for a specific reference type.
   */
  private TypeCategory determineTypeCategoryForType(JReferenceType type) {
    TypeCategory typeCategory = TypeCategory.typeCategoryForType(type, program);

    assert EnumSet.of(TypeCategory.TYPE_JSO, TypeCategory.TYPE_JAVA_OBJECT_OR_JSO,
        TypeCategory.TYPE_JAVA_LANG_STRING, TypeCategory.TYPE_JAVA_OBJECT,
        TypeCategory.TYPE_JS_INTERFACE).contains(typeCategory);

    return typeCategory;
  }

  /**
   * Returns an expression implementing the instanceof/dynamicCast operations.
   */
  private JMethodCall implementCastOrInstanceOfOperation(SourceInfo sourceInfo,
      JExpression targetExpression, JReferenceType targetType,
      Map<TypeCategory, JMethod> targetMethodByTypeCategory, boolean overrideReturnType) {

    TypeCategory targetTypeCategory = determineTypeCategoryForType(targetType);
    JMethod method = targetMethodByTypeCategory.get(targetTypeCategory);
    JMethodCall call;
    if (overrideReturnType) {
      // Create a method call overriding the return type so that operations like Cast.dynamicCast
      // don't change the type of the original method call expression.
      call = new JMethodCall(sourceInfo, null, method, targetType);
    } else {
      call = new JMethodCall(sourceInfo, null, method);
    }
    call.addArg(targetExpression);
    if (method.getParams().size() >= 2) {
      // checking/casting to JSOs or Strings does not require a second parameter
      call.addArg((new JRuntimeTypeReference(sourceInfo, program.getTypeJavaLangObject(),
          targetType)));
    }
    if (method.getParams().size() == 3) {

     assert targetTypeCategory == TypeCategory.TYPE_JS_INTERFACE;
     call.addArg(program.getStringLiteral(sourceInfo,
         program.typeOracle.getNearestJsInterface(targetType, true).getJsPrototype()));
    }
    return call;
  }

  public static void exec(JProgram program, boolean disableCastChecking) {
    new ImplementCastsAndTypeChecks(program, disableCastChecking).execImpl();
  }

  private final boolean disableCastChecking;
  private final JProgram program;

  private Map<TypeCategory, JMethod> instanceOfMethodsByTargetTypeCategory =
      Maps.newEnumMap(TypeCategory.class);

  private Map<TypeCategory, JMethod> dynamicCastMethodsByTargetTypeCategory =
      Maps.newEnumMap(TypeCategory.class);

  private ImplementCastsAndTypeChecks(JProgram program, boolean disableCastChecking) {
    this.program = program;
    this.disableCastChecking = disableCastChecking;

    // Populate the necessary instanceOf methods.
    this.instanceOfMethodsByTargetTypeCategory.put(
        TypeCategory.TYPE_JAVA_OBJECT, program.getIndexedMethod("Cast.instanceOf"));
    this.instanceOfMethodsByTargetTypeCategory.put(
        TypeCategory.TYPE_JAVA_OBJECT_OR_JSO, program.getIndexedMethod("Cast.instanceOfOrJso"));
    this.instanceOfMethodsByTargetTypeCategory.put(
        TypeCategory.TYPE_JSO, program.getIndexedMethod("Cast.instanceOfJso"));
    this.instanceOfMethodsByTargetTypeCategory.put(
        TypeCategory.TYPE_JAVA_LANG_STRING, program.getIndexedMethod("Cast.isJavaString"));
    this.instanceOfMethodsByTargetTypeCategory.put(
        TypeCategory.TYPE_JS_INTERFACE, program.getIndexedMethod("Cast.instanceOfJsInterface"));

    // Populate the necessary dynamicCast methods.
    this.dynamicCastMethodsByTargetTypeCategory.put(
        TypeCategory.TYPE_JAVA_OBJECT, program.getIndexedMethod("Cast.dynamicCast"));
    this.dynamicCastMethodsByTargetTypeCategory.put(
        TypeCategory.TYPE_JAVA_OBJECT_OR_JSO, program.getIndexedMethod("Cast.dynamicCastAllowJso"));
    this.dynamicCastMethodsByTargetTypeCategory.put(
        TypeCategory.TYPE_JSO, program.getIndexedMethod("Cast.dynamicCastJso"));
    this.dynamicCastMethodsByTargetTypeCategory.put(
        TypeCategory.TYPE_JAVA_LANG_STRING, program.getIndexedMethod("Cast.dynamicCastToString"));
    this.dynamicCastMethodsByTargetTypeCategory.put(
        TypeCategory.TYPE_JS_INTERFACE, program.getIndexedMethod("Cast.dynamicCastWithPrototype"));
  }

  private void execImpl() {
    ReplaceTypeChecksVisitor replacer = new ReplaceTypeChecksVisitor();
    replacer.accept(program);
  }
}
