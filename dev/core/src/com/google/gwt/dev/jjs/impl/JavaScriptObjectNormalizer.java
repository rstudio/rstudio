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
import com.google.gwt.dev.jjs.ast.JCastOperation;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JConditional;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JInstanceOf;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JLocalRef;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JNewArray;
import com.google.gwt.dev.jjs.ast.JNonNullType;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.js.JMultiExpression;

import java.util.Stack;

/**
 * Replace references to JSO subtypes with JSO itself.
 */
public class JavaScriptObjectNormalizer {
  /**
   * Map types from JSO subtypes to JSO itself.
   */
  private class NormalizeVisitor extends JModVisitor {

    private final Stack<JMethodBody> currentMethodBody = new Stack<JMethodBody>();

    @Override
    public void endVisit(JCastOperation x, Context ctx) {
      JType newType = translate(x.getCastType());
      if (newType != x.getCastType()) {
        ctx.replaceMe(new JCastOperation(x.getSourceInfo(), newType,
            x.getExpr()));
      }
    }

    @Override
    public void endVisit(JClassLiteral x, Context ctx) {
      JType newType = translate(x.getRefType());
      if (newType != x.getRefType()) {
        ctx.replaceMe(program.getLiteralClass(newType));
      }
    }

    @Override
    public void endVisit(JField x, Context ctx) {
      x.setType(translate(x.getType()));
    }

    @Override
    public void endVisit(JInstanceOf x, Context ctx) {
      JReferenceType newType = (JReferenceType) translate(x.getTestType());
      if (newType != x.getTestType()) {
        ctx.replaceMe(new JInstanceOf(x.getSourceInfo(), newType, x.getExpr()));
      }
    }

    @Override
    public void endVisit(JLocal x, Context ctx) {
      x.setType(translate(x.getType()));
    }

    @Override
    public void endVisit(JMethod x, Context ctx) {
      x.setType(translate(x.getType()));
    }

    @Override
    public void endVisit(JMethodBody x, Context ctx) {
      if (currentMethodBody.pop() != x) {
        throw new RuntimeException("Unexpected JMethodBody popped");
      }
    }

    /**
     * Polymorphic dispatches to interfaces implemented by both a JSO and a
     * regular type require special dispatch handling. If the instance is not a
     * Java-derived object, the method from the single JSO implementation will
     * be invoked. Otherwise, a polymorphic dispatch to the Java-derived object
     * is made.
     */
    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      JDeclaredType targetClass = x.getTarget().getEnclosingType();
      if (program.typeOracle.getSingleJsoImpl(targetClass) != null) {

        SourceInfo info = x.getSourceInfo().makeChild(
            JavaScriptObjectNormalizer.class,
            "Polymorphic invocation of SingleJsoImpl interface");

        // Find the method in the JSO type
        JMethod jsoMethod = findJsoMethod(x.getTarget());
        assert jsoMethod != null;

        if (program.typeOracle.isDualJsoInterface(targetClass)) {
          /*
           * This is the special-case code to handle interfaces.
           */
          JMultiExpression multi = new JMultiExpression(info);
          JExpression instance = maybeMakeTempAssignment(multi, x.getInstance());

          // instance.method(arg, arg)
          JMethodCall localCall = new JMethodCall(info, instance, x.getTarget());
          localCall.addArgs(x.getArgs());

          // We need a second copy of the arguments for the else expression
          CloneExpressionVisitor cloner = new CloneExpressionVisitor(program);

          // instance.jsoMethod(arg, arg)
          JMethodCall jsoCall = new JMethodCall(info,
              cloner.cloneExpression(instance), jsoMethod);
          jsoCall.addArgs(cloner.cloneExpressions(x.getArgs()));

          // Cast.isJavaScriptObject() ? instance.jsoMethod() :
          // instance.method();
          JConditional newExpr = makeIsJsoConditional(info,
              cloner.cloneExpression(instance), x.getType(), jsoCall, localCall);

          multi.exprs.add(newExpr);
          // We may only have the ternary operation if there's no side-effect
          ctx.replaceMe(multi.exprs.size() == 1 ? multi.exprs.get(0) : multi);
        } else {
          /*
           * ... otherwise, if there's only a JSO implementation, we'll just
           * call that directly.
           */
          JMethodCall jsoCall = new JMethodCall(info, x.getInstance(),
              jsoMethod);
          jsoCall.addArgs(x.getArgs());
          ctx.replaceMe(jsoCall);
        }
      }
    }

    @Override
    public void endVisit(JNewArray x, Context ctx) {
      x.setType((JNonNullType) translate(x.getType()));
    }

    @Override
    public void endVisit(JParameter x, Context ctx) {
      x.setType(translate(x.getType()));
    }

    @Override
    public boolean visit(JMethodBody x, Context ctx) {
      currentMethodBody.push(x);
      return true;
    }

    private JMethod findConcreteImplementation(JMethod method,
        JClassType concreteType) {
      /*
       * Search supertypes for virtual overrides via subclass. See the javadoc
       * on JTypeOracle.getAllVirtualOverrides for an example.
       */
      while (concreteType != null) {
        for (JMethod m : concreteType.getMethods()) {
          if (program.typeOracle.getAllOverrides(m).contains(method)) {
            if (!m.isAbstract()) {
              return m;
            }
          }
        }
        concreteType = concreteType.getSuperClass();
      }

      return null;
    }

    private JMethod findJsoMethod(JMethod interfaceMethod) {
      JClassType jsoClass = program.typeOracle.getSingleJsoImpl(interfaceMethod.getEnclosingType());
      assert program.isJavaScriptObject(jsoClass);
      assert jsoClass != null;

      JMethod toReturn = findConcreteImplementation(interfaceMethod, jsoClass);
      assert toReturn != null;
      assert !toReturn.isAbstract();
      assert jsoClass.isFinal() || toReturn.isFinal();

      return toReturn;
    }

    private JConditional makeIsJsoConditional(SourceInfo info,
        JExpression instance, JType conditionalType, JExpression isJsoExpr,
        JExpression notJsoExpr) {
      // Cast.isJavaScriptObjectOrString(instance)
      JMethod isJavaScriptObjectMethod = program.getIndexedMethod("Cast.isJavaScriptObjectOrString");
      JMethodCall isJavaScriptObjectExpr = new JMethodCall(info, null,
          isJavaScriptObjectMethod);
      isJavaScriptObjectExpr.addArg(instance);
      return new JConditional(info, conditionalType, isJavaScriptObjectExpr,
          isJsoExpr, notJsoExpr);
    }

    private JExpression maybeMakeTempAssignment(JMultiExpression multi,
        JExpression instance) {
      if (instance.hasSideEffects()) {
        /*
         * It may be necessary to save off the instance expression into a local
         * variable if its evaluation would produce side-effects. The
         * multi-expression is used for this purpose.
         */
        SourceInfo info = instance.getSourceInfo().makeChild(
            JavaScriptObjectNormalizer.class,
            "Temporary assignment for instance with side-effects");
        JLocal local = program.createLocal(info,
            "maybeJsoInvocation".toCharArray(), instance.getType(), true,
            currentMethodBody.peek());
        multi.exprs.add(program.createAssignmentStmt(info,
            new JLocalRef(info, local), instance).getExpr());

        instance = new JLocalRef(info, local);
      }
      return instance;
    }

    private JType translate(JType type) {
      if (!(type instanceof JReferenceType)) {
        return type;
      }
      JReferenceType refType = (JReferenceType) type;
      boolean canBeNull = refType.canBeNull();
      refType = refType.getUnderlyingType();

      if (program.isJavaScriptObject(refType)) {
        refType = program.getJavaScriptObject();
      } else if (program.typeOracle.getSingleJsoImpl(refType) != null
          && !program.typeOracle.isDualJsoInterface(refType)) {
        // Optimization: narrow to JSO if it's not a dual impl.
        refType = program.getJavaScriptObject();
      } else if (refType instanceof JArrayType) {
        JArrayType arrayType = (JArrayType) refType;
        JType leafType = arrayType.getLeafType();
        JType replacement = translate(leafType);
        if (leafType != replacement) {
          refType = program.getTypeArray(replacement, arrayType.getDims());
        }
      }
      return canBeNull ? refType : program.getNonNullType(refType);
    }
  }

  public static void exec(JProgram program) {
    new JavaScriptObjectNormalizer(program).execImpl();
  }

  private final JProgram program;

  private JavaScriptObjectNormalizer(JProgram program) {
    this.program = program;
  }

  private void execImpl() {
    NormalizeVisitor visitor = new NormalizeVisitor();
    visitor.accept(program);
  }
}
