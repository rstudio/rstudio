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
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JInstanceOf;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
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

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

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
      JExpression replaceExpr;
      JType toType = x.getCastType();
      JExpression expr = x.getExpr();
      boolean isJsoCast = false;
      boolean isJsInterfaceCast = false;

      // Even if disableCastChecking is enabled, we need to rescue JSOs
      if (toType instanceof JReferenceType && !(toType instanceof JNullType)) {
        JReferenceType refType = ((JReferenceType) toType).getUnderlyingType();

        isJsoCast = program.typeOracle.willCrossCastLikeJso(refType);
        isJsInterfaceCast =
            program.typeOracle.isOrExtendsJsInterface(toType, true);

        if (isJsoCast || isJsInterfaceCast) {
          instantiateJsoInterface(refType);
        }
      }

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
        if (program.typeOracle.canTriviallyCast(argType, refType)) {
          // just remove the cast
          replaceExpr = curExpr;
        } else if (program.typeOracle.willCrossCastLikeJso(argType)
            && program.typeOracle.willCrossCastLikeJso(refType)) {
          // leave the cast instance for Pruner/CFA, remove in GenJSAST
          return;
        } else {
          // A cast is still needed.  Substitute the appropriate Cast implementation.
          JMethod method;
          if (isJsoCast) {
            // A cast to a concrete JSO subtype
            method = program.getIndexedMethod("Cast.dynamicCastJso");
          } else if (program.typeOracle.isDualJsoInterface(refType)) {
            // An interface that should succeed when the object is a JSO
            method = program.getIndexedMethod("Cast.dynamicCastAllowJso");
          } else {
            if (program.typeOracle.isOrExtendsJsInterface(toType, true)) {
            if (isJsInterfaceCast) {
              method = program.getIndexedMethod("Cast.dynamicCastWithPrototype");
            } else {
              // A regular cast
              method = program.getIndexedMethod("Cast.dynamicCast");
            }
              isJsInterfaceCast = true;
            } else {
              // A regular cast
              method = program.getIndexedMethod("Cast.dynamicCast");
            }
          }
          // override the type of the called method with the target cast type
          JMethodCall call = new JMethodCall(info, null, method, toType);
          call.addArg(curExpr);
          if (!isJsoCast) {
            call.addArg((new JRuntimeTypeReference(x.getSourceInfo(), program.getTypeJavaLangObject(),
                refType)));
          }
          if (isJsInterfaceCast) {
            call.addArg(program.getStringLiteral(x.getSourceInfo(),
                program.typeOracle.getNearestJsInterface(toType,
                    true).getJsPrototype()));
          }
          if (isJsoCast || isJsInterfaceCast) {
            instantiateJsoInterface((JReferenceType) toType);
          }
          if (isJsInterfaceCast) {
            call.addArg(program.getStringLiteral(x.getSourceInfo(),
                program.typeOracle.getNearestJsInterface(toType,
                    true).getJsPrototype()));
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

    private void instantiateJsoInterface(JReferenceType toType) {
      if (instantiatedJsoTypes.add(toType)) {
        if (program.typeOracle.getSingleJsoImpl(toType) != null) {
          // rescuing an Interface via Cast, we record the JSO implementing it
          instantiateJsoInterface(program.typeOracle.getSingleJsoImpl(toType));
        }
        // if it's a class, and the superType is JSO, rescue it too
        if (toType instanceof JClassType) {
          JClassType superType = ((JClassType) toType).getSuperClass();
          if (superType != null && program.isJavaScriptObject(superType)) {
            instantiateJsoInterface(superType);
          }
        }

        // if we extend another JsInterface, or Interface with JSO implementation, rescue it
        for (JInterfaceType intf : ((JDeclaredType) toType).getImplements()) {
          if (intf.isJsInterface() || program.typeOracle.getSingleJsoImpl(intf) != null) {
            instantiateJsoInterface(intf);
          }
        }
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
        JMethod method;
        boolean isJsoCast = false;
        boolean isJsInterfaceCast = false;
        // TODO(cromwellian) fully deal with JSO concrete implements of
        // JsInterface
        if (program.typeOracle.isDualJsoInterface(toType)) {
          method = program.getIndexedMethod("Cast.instanceOfOrJso");
        } else if (program.typeOracle.willCrossCastLikeJso(toType)) {
          isJsoCast = true;
          method = program.getIndexedMethod("Cast.instanceOfJso");
        } else {
          // a real castableTypeMap check or JS prototype check
          if (program.typeOracle.isOrExtendsJsInterface(toType, true)) {
            method = program.getIndexedMethod("Cast.instanceOfJsInterface");
            isJsInterfaceCast = true;
          } else {
            method = program.getIndexedMethod("Cast.instanceOf");
          }
        }
        JMethodCall call = new JMethodCall(x.getSourceInfo(), null, method);
        call.addArg(x.getExpr());
        if (!isJsoCast) {
          call.addArg((new JRuntimeTypeReference(x.getSourceInfo(), program.getTypeJavaLangObject(), toType)));
        }
        if (isJsInterfaceCast) {
          call.addArg(program.getStringLiteral(x.getSourceInfo(),
              program.typeOracle.getNearestJsInterface(toType,
                  true).getJsPrototype()));
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
    new ImplementCastsAndTypeChecks(program, disableCastChecking).execImpl();
  }

  private final boolean disableCastChecking;
  private final Set<JReferenceType> instantiatedJsoTypes = new HashSet<JReferenceType>();

  private final JProgram program;

  private ImplementCastsAndTypeChecks(JProgram program, boolean disableCastChecking) {
    this.program = program;
    this.disableCastChecking = disableCastChecking;
  }

  private void execImpl() {
    ReplaceTypeChecksVisitor replacer = new ReplaceTypeChecksVisitor();
    replacer.accept(program);
    program.typeOracle.setInstantiatedJsoTypesViaCast(instantiatedJsoTypes);
  }
}
