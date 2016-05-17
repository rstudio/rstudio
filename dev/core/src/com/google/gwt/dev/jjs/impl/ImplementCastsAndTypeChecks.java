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
import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JCastOperation;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JInstanceOf;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JRuntimeTypeReference;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.RuntimeConstants;
import com.google.gwt.dev.jjs.ast.js.JsniMethodRef;
import com.google.gwt.thirdparty.guava.common.collect.Maps;

import java.util.Map;

/**
 * Replace cast and instanceof operations with calls to the Cast class. Depends
 * on {@link CatchBlockNormalizer}, {@link CompoundAssignmentNormalizer},
 * {@link Devirtualizer}, and {@link LongCastNormalizer} having already run.
 * <p>
 * May or may not prune trivial casts depending on configuration.
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

      SourceInfo info = x.getSourceInfo();
      if (pruneTrivialCasts && toType.isNullType()) {
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
        JMethod method = program.getIndexedMethod(
            RuntimeConstants.CAST_THROW_CLASS_CAST_EXCEPTION_UNLESS_NULL);
        // Note, we must update the method call to return the null type.
        JMethodCall call = new JMethodCall(info, null, method, expr);
        call.overrideReturnType(toType);
        ctx.replaceMe(call);
        return;
      }

      if (toType instanceof JReferenceType) {
        JExpression curExpr = expr;
        JReferenceType refType = (JReferenceType) toType.getUnderlyingType();
        JReferenceType argType = (JReferenceType) expr.getType();

        if (refType instanceof JArrayType) {
          // Arrays of any subclass of JavaScriptObject are considered arrays of JavaScriptObject
          // for casting and instanceof purposes.
          refType =  (JReferenceType) program.normalizeJsoType(refType);
        }

        if (pruneTrivialCasts && program.typeOracle.castSucceedsTrivially(argType, refType) ||
            determineTypeCategoryForType(refType) == TypeCategory.TYPE_JAVA_LANG_OBJECT) {
          // just remove the cast
          ctx.replaceMe(curExpr);
          return;
        } else if (program.typeOracle.isEffectivelyJavaScriptObject(argType)
            && program.typeOracle.isEffectivelyJavaScriptObject(refType)) {
          // leave the cast instance for Pruner/CFA, remove in GenJSAST
          return;
        }
        // A cast is still needed.  Substitute the appropriate Cast implementation.
        ctx.replaceMe(implementCastOrInstanceOfOperation(x.getSourceInfo(), curExpr, refType,
            dynamicCastMethodsByTargetTypeCategory, true));
        return;
      }

      // It is a primitive type, perform the necessary coercion.

      assert toType.isPrimitiveType();
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
          JMethod castMethod = program.getIndexedMethod(RuntimeConstants.LONG_LIB_TO_INT);
          JMethodCall call = new JMethodCall(info, null, castMethod);
          call.addArg(expr);
          expr = call;
          fromType = tInt;
        } else if (tInt == toType) {
          methodName = RuntimeConstants.LONG_LIB_TO_INT;
        } else if (tFloat == toType || tDouble == toType) {
          methodName = RuntimeConstants.LONG_LIB_TO_DOUBLE;
        }
      }

      if (toType == tLong && fromType != tLong) {
        // Longs get special treatment.
        if (tByte == fromType || tShort == fromType || tChar == fromType || tInt == fromType) {
          methodName = RuntimeConstants.LONG_LIB_FROM_INT;
        } else if (tFloat == fromType || tDouble == fromType) {
          methodName = RuntimeConstants.LONG_LIB_FROM_DOUBLE;
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
        JMethodCall call = new JMethodCall(info, null, castMethod, expr);
        call.overrideReturnType(toType);
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

      if (toType instanceof JArrayType) {
        // Arrays of any subclass of JavaScriptObject are considered arrays of JavaScriptObject
        // for casting and instanceof purposes.
        toType =  (JReferenceType) program.normalizeJsoType(toType);
      }

      assert !toType.isJsNative() || !(toType instanceof JInterfaceType);

      boolean isTrivialCast = program.typeOracle.castSucceedsTrivially(argType, toType)
          // don't depend on type-tightener having run
          || (program.typeOracle.isEffectivelyJavaScriptObject(argType)
              && program.typeOracle.isEffectivelyJavaScriptObject(toType));
      if (pruneTrivialCasts && isTrivialCast ||
          determineTypeCategoryForType(toType) == TypeCategory.TYPE_JAVA_LANG_OBJECT) {
        // trivially true if non-null; replace with a null test
        JBinaryOperation eq =
            new JBinaryOperation(x.getSourceInfo(), program.getTypePrimitiveBoolean(),
                JBinaryOperator.NEQ, x.getExpr(), program.getLiteralNull());
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

    assert typeCategory.castInstanceOfQualifier() != null;

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
    assert method != null;
    JMethodCall call = new JMethodCall(sourceInfo, null, method);
    if (overrideReturnType) {
      // Create a method call overriding the return type so that operations like Cast.dynamicCast
      // don't change the type of the original method call expression.
      call.overrideReturnType(targetType);
    }

    call.addArg(targetExpression);

    if (method.getParams().size() < 2) {
      // The cast checking method does not require an additional parameter. This situation arises
      // when the call is a cast check and cast checking has been disabled or when the type category
      // provides enough information, e.g. TYPE_UNTYPED_ARRAY.
      return call;
    } else if (targetTypeCategory.requiresTypeId()) {
      call.addArg((new JRuntimeTypeReference(sourceInfo, program.getTypeJavaLangObject(),
          targetType)));
      return call;
    } else if (targetTypeCategory.requiresJsConstructor()) {
      JDeclaredType declaredType = (JDeclaredType) targetType;

      JMethod jsConstructor = JjsUtils.getJsNativeConstructorOrNull(declaredType);
      assert jsConstructor != null &&  declaredType.isJsNative();
      call.addArg(new JsniMethodRef(sourceInfo, declaredType.getQualifiedJsName(), jsConstructor,
          program.getJavaScriptObject()));
      return call;
    } else {
      throw new AssertionError();
    }
  }

  public static void exec(JProgram program, boolean pruneTrivialCasts) {
    new ImplementCastsAndTypeChecks(program, pruneTrivialCasts).execImpl();
  }

  public static void exec(JProgram program) {
    new ImplementCastsAndTypeChecks(program, true).execImpl();
  }

  private final boolean pruneTrivialCasts;
  private final JProgram program;

  private Map<TypeCategory, JMethod> instanceOfMethodsByTargetTypeCategory =
      Maps.newEnumMap(TypeCategory.class);

  private Map<TypeCategory, JMethod> dynamicCastMethodsByTargetTypeCategory =
      Maps.newEnumMap(TypeCategory.class);

  private ImplementCastsAndTypeChecks(JProgram program, boolean pruneTrivialCasts) {
    this.program = program;
    this.pruneTrivialCasts = pruneTrivialCasts;

    for (TypeCategory t : TypeCategory.values()) {
      String castInstanceOfQualifier = t.castInstanceOfQualifier();
      if (castInstanceOfQualifier == null) {
        continue;
      }
      String instanceOfMethod = "Cast.instanceOf" + castInstanceOfQualifier;
      instanceOfMethodsByTargetTypeCategory.put(t, program.getIndexedMethod(instanceOfMethod));
      String castMethod = "Cast.castTo" + castInstanceOfQualifier;
      dynamicCastMethodsByTargetTypeCategory.put(t, program.getIndexedMethod(castMethod));
    }
  }

  private void execImpl() {
    new ReplaceTypeChecksVisitor().accept(program);
  }
}
