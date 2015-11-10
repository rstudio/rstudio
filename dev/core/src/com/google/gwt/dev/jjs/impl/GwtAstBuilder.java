/*
 * Copyright 2010 Google Inc.
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

import com.google.gwt.dev.CompilerContext;
import com.google.gwt.dev.common.InliningMode;
import com.google.gwt.dev.javac.JdtUtil;
import com.google.gwt.dev.javac.JsInteropUtil;
import com.google.gwt.dev.javac.JsniMethod;
import com.google.gwt.dev.jdt.SafeASTVisitor;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.SourceOrigin;
import com.google.gwt.dev.jjs.ast.AccessModifier;
import com.google.gwt.dev.jjs.ast.CanHaveSuppressedWarnings;
import com.google.gwt.dev.jjs.ast.JArrayLength;
import com.google.gwt.dev.jjs.ast.JArrayRef;
import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JAssertStatement;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JBlock;
import com.google.gwt.dev.jjs.ast.JBooleanLiteral;
import com.google.gwt.dev.jjs.ast.JBreakStatement;
import com.google.gwt.dev.jjs.ast.JCaseStatement;
import com.google.gwt.dev.jjs.ast.JCastOperation;
import com.google.gwt.dev.jjs.ast.JCharLiteral;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JConditional;
import com.google.gwt.dev.jjs.ast.JConstructor;
import com.google.gwt.dev.jjs.ast.JContinueStatement;
import com.google.gwt.dev.jjs.ast.JDeclarationStatement;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JDoStatement;
import com.google.gwt.dev.jjs.ast.JDoubleLiteral;
import com.google.gwt.dev.jjs.ast.JEnumField;
import com.google.gwt.dev.jjs.ast.JEnumType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JExpressionStatement;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JField.Disposition;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JFloatLiteral;
import com.google.gwt.dev.jjs.ast.JForStatement;
import com.google.gwt.dev.jjs.ast.JIfStatement;
import com.google.gwt.dev.jjs.ast.JInstanceOf;
import com.google.gwt.dev.jjs.ast.JIntLiteral;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JLabel;
import com.google.gwt.dev.jjs.ast.JLabeledStatement;
import com.google.gwt.dev.jjs.ast.JLiteral;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JLocalRef;
import com.google.gwt.dev.jjs.ast.JLongLiteral;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNewArray;
import com.google.gwt.dev.jjs.ast.JNewInstance;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JNullLiteral;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JParameterRef;
import com.google.gwt.dev.jjs.ast.JPostfixOperation;
import com.google.gwt.dev.jjs.ast.JPrefixOperation;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JReturnStatement;
import com.google.gwt.dev.jjs.ast.JStatement;
import com.google.gwt.dev.jjs.ast.JStringLiteral;
import com.google.gwt.dev.jjs.ast.JSwitchStatement;
import com.google.gwt.dev.jjs.ast.JThisRef;
import com.google.gwt.dev.jjs.ast.JThrowStatement;
import com.google.gwt.dev.jjs.ast.JTryStatement;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JUnaryOperator;
import com.google.gwt.dev.jjs.ast.JVariable;
import com.google.gwt.dev.jjs.ast.JWhileStatement;
import com.google.gwt.dev.jjs.ast.js.JMultiExpression;
import com.google.gwt.dev.jjs.ast.js.JsniClassLiteral;
import com.google.gwt.dev.jjs.ast.js.JsniFieldRef;
import com.google.gwt.dev.jjs.ast.js.JsniMethodBody;
import com.google.gwt.dev.jjs.ast.js.JsniMethodRef;
import com.google.gwt.dev.js.JsAbstractSymbolResolver;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsModVisitor;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsNode;
import com.google.gwt.dev.js.ast.JsParameter;
import com.google.gwt.dev.util.StringInterner;
import com.google.gwt.dev.util.arg.OptionJsInteropMode.Mode;
import com.google.gwt.dev.util.collect.Stack;
import com.google.gwt.thirdparty.guava.common.base.Function;
import com.google.gwt.thirdparty.guava.common.base.Preconditions;
import com.google.gwt.thirdparty.guava.common.base.Predicate;
import com.google.gwt.thirdparty.guava.common.base.Predicates;
import com.google.gwt.thirdparty.guava.common.collect.Collections2;
import com.google.gwt.thirdparty.guava.common.collect.FluentIterable;
import com.google.gwt.thirdparty.guava.common.collect.Interner;
import com.google.gwt.thirdparty.guava.common.collect.Iterables;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.AND_AND_Expression;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.AnnotationMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.ArrayAllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.ArrayInitializer;
import org.eclipse.jdt.internal.compiler.ast.ArrayReference;
import org.eclipse.jdt.internal.compiler.ast.AssertStatement;
import org.eclipse.jdt.internal.compiler.ast.Assignment;
import org.eclipse.jdt.internal.compiler.ast.BinaryExpression;
import org.eclipse.jdt.internal.compiler.ast.Block;
import org.eclipse.jdt.internal.compiler.ast.BreakStatement;
import org.eclipse.jdt.internal.compiler.ast.CaseStatement;
import org.eclipse.jdt.internal.compiler.ast.CastExpression;
import org.eclipse.jdt.internal.compiler.ast.CharLiteral;
import org.eclipse.jdt.internal.compiler.ast.ClassLiteralAccess;
import org.eclipse.jdt.internal.compiler.ast.Clinit;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.CompoundAssignment;
import org.eclipse.jdt.internal.compiler.ast.ConditionalExpression;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ContinueStatement;
import org.eclipse.jdt.internal.compiler.ast.DoStatement;
import org.eclipse.jdt.internal.compiler.ast.DoubleLiteral;
import org.eclipse.jdt.internal.compiler.ast.EmptyStatement;
import org.eclipse.jdt.internal.compiler.ast.EqualExpression;
import org.eclipse.jdt.internal.compiler.ast.ExplicitConstructorCall;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.ExtendedStringLiteral;
import org.eclipse.jdt.internal.compiler.ast.FalseLiteral;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.FieldReference;
import org.eclipse.jdt.internal.compiler.ast.FloatLiteral;
import org.eclipse.jdt.internal.compiler.ast.ForStatement;
import org.eclipse.jdt.internal.compiler.ast.ForeachStatement;
import org.eclipse.jdt.internal.compiler.ast.FunctionalExpression;
import org.eclipse.jdt.internal.compiler.ast.IfStatement;
import org.eclipse.jdt.internal.compiler.ast.Initializer;
import org.eclipse.jdt.internal.compiler.ast.InstanceOfExpression;
import org.eclipse.jdt.internal.compiler.ast.IntLiteral;
import org.eclipse.jdt.internal.compiler.ast.LabeledStatement;
import org.eclipse.jdt.internal.compiler.ast.LambdaExpression;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.LongLiteral;
import org.eclipse.jdt.internal.compiler.ast.MarkerAnnotation;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.NameReference;
import org.eclipse.jdt.internal.compiler.ast.NormalAnnotation;
import org.eclipse.jdt.internal.compiler.ast.NullLiteral;
import org.eclipse.jdt.internal.compiler.ast.OR_OR_Expression;
import org.eclipse.jdt.internal.compiler.ast.OperatorIds;
import org.eclipse.jdt.internal.compiler.ast.PostfixExpression;
import org.eclipse.jdt.internal.compiler.ast.PrefixExpression;
import org.eclipse.jdt.internal.compiler.ast.QualifiedAllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedSuperReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedThisReference;
import org.eclipse.jdt.internal.compiler.ast.ReferenceExpression;
import org.eclipse.jdt.internal.compiler.ast.ReturnStatement;
import org.eclipse.jdt.internal.compiler.ast.SingleMemberAnnotation;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.StringLiteral;
import org.eclipse.jdt.internal.compiler.ast.StringLiteralConcatenation;
import org.eclipse.jdt.internal.compiler.ast.SuperReference;
import org.eclipse.jdt.internal.compiler.ast.SwitchStatement;
import org.eclipse.jdt.internal.compiler.ast.SynchronizedStatement;
import org.eclipse.jdt.internal.compiler.ast.ThisReference;
import org.eclipse.jdt.internal.compiler.ast.ThrowStatement;
import org.eclipse.jdt.internal.compiler.ast.TrueLiteral;
import org.eclipse.jdt.internal.compiler.ast.TryStatement;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.ast.UnaryExpression;
import org.eclipse.jdt.internal.compiler.ast.UnionTypeReference;
import org.eclipse.jdt.internal.compiler.ast.WhileStatement;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding;
import org.eclipse.jdt.internal.compiler.lookup.BaseTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.BinaryTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.IntersectionTypeBinding18;
import org.eclipse.jdt.internal.compiler.lookup.LocalTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.MethodVerifier;
import org.eclipse.jdt.internal.compiler.lookup.NestedTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.SyntheticArgumentBinding;
import org.eclipse.jdt.internal.compiler.lookup.SyntheticMethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;
import org.eclipse.jdt.internal.compiler.lookup.VariableBinding;
import org.eclipse.jdt.internal.compiler.util.Util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javaemul.internal.annotations.DoNotInline;
import javaemul.internal.annotations.ForceInline;

/**
 * Constructs a GWT Java AST from a single isolated compilation unit. The AST is
 * not associated with any {@link com.google.gwt.dev.jjs.ast.JProgram} and will
 * contain unresolved references.
 */
public class GwtAstBuilder {

  public static final String CLINIT_METHOD_NAME = "$clinit";
  public static final String GET_CLASS_METHOD_NAME = "getClass";
  public static final String HAS_NEXT_METHOD_NAME = "hasNext";
  public static final String ITERATOR_METHOD_NAME = "iterator";
  public static final String INIT_NAME_METHOD_NAME = "$init";
  public static final String NEXT_METHOD_NAME = "next";
  public static final String ORDINAL_METHOD_NAME = "ordinal";
  public static final String OUTER_LAMBDA_PARAM_NAME = "$$outer_0";
  public static final String STATIC_INIT_METHOD_NAME =  "$" + INIT_NAME_METHOD_NAME;
  public static final String VALUE_OF_METHOD_NAME = "valueOf";
  public static final String VALUES_METHOD_NAME = "values";

  public static final int CLINIT_METHOD_INDEX = 0;
  public static final int INIT_METHOD_INDEX = 1;
  public static final int GET_CLASS_METHOD_INDEX = 2;
  public static final int VALUE_OF_METHOD_INDEX = 3;
  public static final int VALUES_METHOD_INDEX = 4;

  /**
   * Visit the JDT AST and produce our own AST. By the end of this pass, the
   * produced AST should contain every piece of information we'll ever need
   * about the code. The JDT nodes should never again be referenced after this.
   */
  class AstVisitor extends SafeASTVisitor {
    /**
     * Collects JSNI references from native method bodies and replaces the ones referring to
     * compile time constants by their corresponding constant value.
     */
    private class JsniReferenceCollector extends JsModVisitor {
      private final JsniMethodBody nativeMethodBody;

      private JsniReferenceCollector(JsniMethodBody nativeMethodBody) {
        this.nativeMethodBody = nativeMethodBody;
      }

      @Override
      public void endVisit(JsNameRef x, JsContext ctx) {
        if (!x.isJsniReference()) {
          return;
        }
        String ident = x.getIdent();
        Binding binding = jsniRefs.get(ident);
        SourceInfo info = x.getSourceInfo();
        assert binding != null;
        if (binding instanceof TypeBinding) {
          JType type = typeMap.get((TypeBinding) binding);
          processClassLiteral(x, info, type, ctx);
        } else if (binding instanceof FieldBinding) {
          FieldBinding fieldBinding = (FieldBinding) binding;
          if (isOptimizableCompileTimeConstant(fieldBinding)) {
            // Replace any compile-time constants with the constant value of the field.
            assert !ctx.isLvalue();
            JExpression constant = getConstant(info, fieldBinding.constant());
            JsExpression result = JjsUtils.translateLiteral((JLiteral) constant);
            assert (result != null);
            ctx.replaceMe(result);
          } else {
            // Normal: create a jsniRef.
            JField field = typeMap.get(fieldBinding);
            processField(x, info, field, ctx);
          }
        } else {
          JMethod method = typeMap.get((MethodBinding) binding);
          processMethod(x, info, method);
        }
      }

      private void processClassLiteral(JsNameRef nameRef, SourceInfo info, JType type,
          JsContext ctx) {
        assert !ctx.isLvalue();
        JsniClassLiteral classLiteral = new JsniClassLiteral(info, nameRef.getIdent(), type);
        nativeMethodBody.addClassRef(classLiteral);
      }

      private void processField(JsNameRef nameRef, SourceInfo info, JField field, JsContext ctx) {
        JsniFieldRef fieldRef =
            new JsniFieldRef(info, nameRef.getIdent(), field, curClass.type, ctx.isLvalue());
        nativeMethodBody.addJsniRef(fieldRef);
      }

      private void processMethod(JsNameRef nameRef, SourceInfo info, JMethod method) {
        JsniMethodRef methodRef =
            new JsniMethodRef(info, nameRef.getIdent(), method, javaLangObject);
        nativeMethodBody.addJsniRef(methodRef);
      }
    }

    /**
     * Resolves the scope of JS identifiers solely within the scope of a method.
     */
    private class JsParameterResolver extends JsAbstractSymbolResolver {
      private final JsFunction jsFunction;

      public JsParameterResolver(JsFunction jsFunction) {
        this.jsFunction = jsFunction;
      }

      @Override
      public void resolveQualifiedName(JsNameRef x) {
      }

      @Override
      protected void resolveUnqualifiedName(JsNameRef x) {
        JsName name = getScope().findExistingName(x.getIdent());

        // Ensure that we're resolving a name from the function's parameters
        JsNode node = name == null ? null : name.getStaticRef();
        if (jsFunction.getParameters().contains(node)) {
          assert node instanceof JsParameter;
          x.resolve(name);
        }
      }
    }

    private final Stack<ClassInfo> classStack = new Stack<ClassInfo>();

    private ClassInfo curClass = null;

    private MethodInfo curMethod = null;

    private final Stack<MethodInfo> methodStack = new Stack<MethodInfo>();

    private final List<JNode> nodeStack = Lists.newArrayList();

    @Override
    public void endVisit(AllocationExpression x, BlockScope scope) {
      try {
        SourceInfo info = makeSourceInfo(x);
        List<JExpression> arguments = popCallArgs(info, x.arguments, x.binding);
        pushNewExpression(info, x, null, arguments, scope);
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisit(AND_AND_Expression x, BlockScope scope) {
      pushBinaryOp(x, JBinaryOperator.AND);
    }

    @Override
    public void endVisit(AnnotationMethodDeclaration x, ClassScope classScope) {
      endVisit((MethodDeclaration) x, classScope);
    }

    @Override
    public void endVisit(ArrayAllocationExpression x, BlockScope scope) {
      try {
        SourceInfo info = makeSourceInfo(x);
        JArrayType type = (JArrayType) typeMap.get(x.resolvedType);

        if (x.initializer != null) {
          // handled by ArrayInitializer.
        } else {
          List<JExpression> dims = pop(x.dimensions);
          push(JNewArray.createArrayWithDimensionExpressions(info, type, dims));
        }
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisit(ArrayInitializer x, BlockScope scope) {
      try {
        SourceInfo info = makeSourceInfo(x);
        JArrayType type = (JArrayType) typeMap.get(x.resolvedType);
        List<JExpression> expressions = pop(x.expressions);
        push(JNewArray.createArrayWithInitializers(info, type, expressions));
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisit(ArrayReference x, BlockScope scope) {
      try {
        SourceInfo info = makeSourceInfo(x);
        JExpression position = pop(x.position);
        JExpression receiver = pop(x.receiver);
        push(new JArrayRef(info, receiver, position));
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisit(AssertStatement x, BlockScope scope) {
      try {
        SourceInfo info = makeSourceInfo(x);
        JExpression exceptionArgument = pop(x.exceptionArgument);
        JExpression assertExpression = pop(x.assertExpression);
        push(new JAssertStatement(info, assertExpression, exceptionArgument));
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisit(Assignment x, BlockScope scope) {
      pushBinaryOp(x, JBinaryOperator.ASG);
    }

    @Override
    public void endVisit(BinaryExpression x, BlockScope scope) {
      JBinaryOperator op;
      int binOp = (x.bits & ASTNode.OperatorMASK) >> ASTNode.OperatorSHIFT;
      switch (binOp) {
        case OperatorIds.LEFT_SHIFT:
          op = JBinaryOperator.SHL;
          break;
        case OperatorIds.RIGHT_SHIFT:
          op = JBinaryOperator.SHR;
          break;
        case OperatorIds.UNSIGNED_RIGHT_SHIFT:
          op = JBinaryOperator.SHRU;
          break;
        case OperatorIds.PLUS:
          if (javaLangString == typeMap.get(x.resolvedType)) {
            op = JBinaryOperator.CONCAT;
          } else {
            op = JBinaryOperator.ADD;
          }
          break;
        case OperatorIds.MINUS:
          op = JBinaryOperator.SUB;
          break;
        case OperatorIds.REMAINDER:
          op = JBinaryOperator.MOD;
          break;
        case OperatorIds.XOR:
          op = JBinaryOperator.BIT_XOR;
          break;
        case OperatorIds.AND:
          op = JBinaryOperator.BIT_AND;
          break;
        case OperatorIds.MULTIPLY:
          op = JBinaryOperator.MUL;
          break;
        case OperatorIds.OR:
          op = JBinaryOperator.BIT_OR;
          break;
        case OperatorIds.DIVIDE:
          op = JBinaryOperator.DIV;
          break;
        case OperatorIds.LESS_EQUAL:
          op = JBinaryOperator.LTE;
          break;
        case OperatorIds.GREATER_EQUAL:
          op = JBinaryOperator.GTE;
          break;
        case OperatorIds.GREATER:
          op = JBinaryOperator.GT;
          break;
        case OperatorIds.LESS:
          op = JBinaryOperator.LT;
          break;
        default:
          throw translateException(x, new InternalCompilerException(
              "Unexpected operator for BinaryExpression"));
      }
      pushBinaryOp(x, op);
    }

    @Override
    public void endVisit(Block x, BlockScope scope) {
      try {
        SourceInfo info = makeSourceInfo(x);
        JBlock block = popBlock(info, x.statements);
        push(block);
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisit(BreakStatement x, BlockScope scope) {
      try {
        SourceInfo info = makeSourceInfo(x);
        push(new JBreakStatement(info, getOrCreateLabel(info, x.label)));
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisit(CaseStatement x, BlockScope scope) {
      try {
        SourceInfo info = makeSourceInfo(x);
        JExpression caseExpression = pop(x.constantExpression);
        if (caseExpression != null && x.constantExpression.resolvedType.isEnum()) {
          caseExpression = synthesizeCallToOrdinal(scope, info, caseExpression);
        }
        push(new JCaseStatement(info, caseExpression));
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisit(CastExpression x, BlockScope scope) {
      /**
       * Our output of a ((A & I1 & I2) a) looks like this:
       *
       * ((A)(I1)(I2)a).
       */
      try {
        SourceInfo info = makeSourceInfo(x);
        JType[] type = processCastType(x.resolvedType);
        JExpression expression = pop(x.expression);
        push(buildCastOperation(info, type, expression));
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisit(CharLiteral x, BlockScope scope) {
      try {
        push(JCharLiteral.get(x.constant.charValue()));
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisit(ClassLiteralAccess x, BlockScope scope) {
      try {
        SourceInfo info = makeSourceInfo(x);
        JType type = typeMap.get(x.targetType);
        push(new JClassLiteral(info, type));
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisit(CompoundAssignment x, BlockScope scope) {
      JBinaryOperator op;
      switch (x.operator) {
        case OperatorIds.PLUS:
          if (javaLangString == typeMap.get(x.resolvedType)) {
            op = JBinaryOperator.ASG_CONCAT;
          } else {
            op = JBinaryOperator.ASG_ADD;
          }
          break;
        case OperatorIds.MINUS:
          op = JBinaryOperator.ASG_SUB;
          break;
        case OperatorIds.MULTIPLY:
          op = JBinaryOperator.ASG_MUL;
          break;
        case OperatorIds.DIVIDE:
          op = JBinaryOperator.ASG_DIV;
          break;
        case OperatorIds.AND:
          op = JBinaryOperator.ASG_BIT_AND;
          break;
        case OperatorIds.OR:
          op = JBinaryOperator.ASG_BIT_OR;
          break;
        case OperatorIds.XOR:
          op = JBinaryOperator.ASG_BIT_XOR;
          break;
        case OperatorIds.REMAINDER:
          op = JBinaryOperator.ASG_MOD;
          break;
        case OperatorIds.LEFT_SHIFT:
          op = JBinaryOperator.ASG_SHL;
          break;
        case OperatorIds.RIGHT_SHIFT:
          op = JBinaryOperator.ASG_SHR;
          break;
        case OperatorIds.UNSIGNED_RIGHT_SHIFT:
          op = JBinaryOperator.ASG_SHRU;
          break;
        default:
          throw translateException(x, new InternalCompilerException(
              "Unexpected operator for CompoundAssignment"));
      }
      pushBinaryOp(x, op);
    }

    @Override
    public void endVisit(ConditionalExpression x, BlockScope scope) {
      try {
        SourceInfo info = makeSourceInfo(x);
        JType type = typeMap.get(x.resolvedType);
        JExpression valueIfFalse = pop(x.valueIfFalse);
        JExpression valueIfTrue = pop(x.valueIfTrue);
        JExpression condition = pop(x.condition);
        push(new JConditional(info, type, condition, valueIfTrue, valueIfFalse));
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisit(ConstructorDeclaration x, ClassScope scope) {
      try {
        List<JStatement> statements = pop(x.statements);
        JStatement constructorCall = pop(x.constructorCall);
        JBlock block = curMethod.body.getBlock();
        SourceInfo info = curMethod.method.getSourceInfo();

        /*
         * Determine if we have an explicit this call. The presence of an
         * explicit this call indicates we can skip certain initialization steps
         * (as the callee will perform those steps for us). These skippable
         * steps are 1) assigning synthetic args to fields and 2) running
         * initializers.
         */
        boolean hasExplicitThis = (x.constructorCall != null) && !x.constructorCall.isSuperAccess();

        /*
         * All synthetic fields must be assigned, unless we have an explicit
         * this constructor call, in which case the callee will assign them for
         * us.
         */
        if (!hasExplicitThis) {
          ReferenceBinding declaringClass = (ReferenceBinding) x.binding.declaringClass.erasure();
          if (JdtUtil.isInnerClass(declaringClass)) {
            NestedTypeBinding nestedBinding = (NestedTypeBinding) declaringClass;
            if (nestedBinding.enclosingInstances != null) {
              for (SyntheticArgumentBinding arg : nestedBinding.enclosingInstances) {
                JBinaryOperation asg = assignSyntheticField(info, arg);
                block.addStmt(asg.makeStatement());
              }
            }

            if (nestedBinding.outerLocalVariables != null) {
              for (SyntheticArgumentBinding arg : nestedBinding.outerLocalVariables) {
                JBinaryOperation asg = assignSyntheticField(info, arg);
                block.addStmt(asg.makeStatement());
              }
            }
          }
        }

        if (constructorCall != null) {
          block.addStmt(constructorCall);
        }

        /*
         * Call the synthetic instance initializer method, unless we have an
         * explicit this constructor call, in which case the callee will.
         */
        if (!hasExplicitThis) {
          JMethod initMethod = curClass.type.getInitMethod();
          JMethodCall initCall = new JMethodCall(info, makeThisRef(info), initMethod);
          block.addStmt(initCall.makeStatement());
        }

        // user code (finally!)
        block.addStmts(statements);
        popMethodInfo();
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisit(ContinueStatement x, BlockScope scope) {
      try {
        SourceInfo info = makeSourceInfo(x);
        push(new JContinueStatement(info, getOrCreateLabel(info, x.label)));
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisit(DoStatement x, BlockScope scope) {
      try {
        SourceInfo info = makeSourceInfo(x);
        JExpression condition = pop(x.condition);
        JStatement action = pop(x.action);
        push(new JDoStatement(info, condition, action));
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisit(DoubleLiteral x, BlockScope scope) {
      try {
        push(JDoubleLiteral.get(x.constant.doubleValue()));
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisit(EmptyStatement x, BlockScope scope) {
      push(null);
    }

    @Override
    public void endVisit(EqualExpression x, BlockScope scope) {
      JBinaryOperator op;
      switch ((x.bits & ASTNode.OperatorMASK) >> ASTNode.OperatorSHIFT) {
        case OperatorIds.EQUAL_EQUAL:
          op = JBinaryOperator.EQ;
          break;
        case OperatorIds.NOT_EQUAL:
          op = JBinaryOperator.NEQ;
          break;
        default:
          throw translateException(x, new InternalCompilerException(
              "Unexpected operator for EqualExpression"));
      }
      pushBinaryOp(x, op);
    }

    @Override
    public void endVisit(ExplicitConstructorCall x, BlockScope scope) {
      try {
        SourceInfo info = makeSourceInfo(x);
        JConstructor ctor = (JConstructor) typeMap.get(x.binding);
        JExpression trueQualifier = makeThisRef(info);
        JMethodCall call = new JMethodCall(info, trueQualifier, ctor);
        List<JExpression> callArgs = popCallArgs(info, x.arguments, x.binding);

        if (curClass.classType.isEnumOrSubclass() != null) {
          // Enums: wire up synthetic name/ordinal params to the super method.
          JParameterRef enumNameRef = curMethod.method.getParams().get(0).makeRef(info);
          call.addArg(enumNameRef);
          JParameterRef enumOrdinalRef = curMethod.method.getParams().get(1).makeRef(info);
          call.addArg(enumOrdinalRef);
        }

        if (x.isSuperAccess()) {
          JExpression qualifier = pop(x.qualification);
          ReferenceBinding superClass = x.binding.declaringClass;
          boolean nestedSuper = JdtUtil.isInnerClass(superClass);
          if (nestedSuper) {
            processSuperCallThisArgs(superClass, call, qualifier, x.qualification);
          }
          call.addArgs(callArgs);
          if (nestedSuper) {
            processSuperCallLocalArgs(superClass, call);
          }
        } else {
          assert (x.qualification == null);
          ReferenceBinding declaringClass = x.binding.declaringClass;
          boolean nested = JdtUtil.isInnerClass(declaringClass);
          if (nested) {
            processThisCallThisArgs(declaringClass, call);
          }
          call.addArgs(callArgs);
          if (nested) {
            processThisCallLocalArgs(declaringClass, call);
          }
        }
        call.setStaticDispatchOnly();
        push(call.makeStatement());
      } catch (Throwable e) {
        throw translateException(x, e);
      } finally {
        scope.methodScope().isConstructorCall = false;
      }
    }

    @Override
    public void endVisit(ExtendedStringLiteral x, BlockScope scope) {
      endVisit((StringLiteral) x, scope);
    }

    @Override
    public void endVisit(FalseLiteral x, BlockScope scope) {
      push(JBooleanLiteral.FALSE);
    }

    @Override
    public void endVisit(FieldDeclaration x, MethodScope scope) {
      try {
        JExpression initialization = pop(x.initialization);
        JField field = typeMap.get(x.binding);
        if (field instanceof JEnumField) {
          // An enum field must be initialized!
          assert (initialization instanceof JNewInstance);
        }

        if (initialization != null) {
          SourceInfo info = makeSourceInfo(x);
          JExpression instance = null;
          if (!x.isStatic()) {
            instance = makeThisRef(info);
          }
          // JDeclarationStatement's ctor sets up the field's initializer.
          JStatement decl =
              new JDeclarationStatement(info, new JFieldRef(info, instance, field, curClass.type),
                  initialization);
          // will either be init or clinit
          curMethod.body.getBlock().addStmt(decl);
        }
        popMethodInfo();
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisit(FieldReference x, BlockScope scope) {
      try {
        FieldBinding fieldBinding = x.binding;
        SourceInfo info = makeSourceInfo(x);
        JExpression instance = pop(x.receiver);
        JExpression expr = createFieldRef(instance, info, fieldBinding);
        if (x.genericCast != null) {
          JType castType = typeMap.get(x.genericCast);
          /*
           * Note, this may result in an invalid AST due to an LHS cast
           * operation. We fix this up in FixAssignmentsToUnboxOrCast.
           */
          expr = maybeCast(castType, expr);
        }
        push(expr);
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisit(FloatLiteral x, BlockScope scope) {
      try {
        push(JFloatLiteral.get(x.constant.floatValue()));
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisit(ForeachStatement x, BlockScope scope) {
      try {
        SourceInfo info = makeSourceInfo(x);

        JBlock body = popBlock(info, x.action);
        JExpression collection = pop(x.collection);
        JDeclarationStatement elementDecl = pop(x.elementVariable);
        assert (elementDecl.initializer == null);

        JLocal elementVar = (JLocal) curMethod.locals.get(x.elementVariable.binding);
        String elementVarName = elementVar.getName();

        JForStatement result;
        if (x.collectionVariable != null) {
          /**
           * <pre>
         * for (final T[] i$array = collection,
         *          int i$index = 0,
         *          final int i$max = i$array.length;
         *      i$index < i$max; ++i$index) {
         *   T elementVar = i$array[i$index];
         *   // user action
         * }
         * </pre>
           */
          JLocal arrayVar = JProgram.createLocal(info, elementVarName + "$array",
              typeMap.get(x.collection.resolvedType), true, curMethod.body);
          JLocal indexVar =
              JProgram.createLocal(info, elementVarName + "$index", JPrimitiveType.INT, false,
                  curMethod.body);
          JLocal maxVar =
              JProgram.createLocal(info, elementVarName + "$max", JPrimitiveType.INT, true,
                  curMethod.body);

          List<JStatement> initializers = Lists.newArrayListWithCapacity(3);
          // T[] i$array = arr
          initializers.add(makeDeclaration(info, arrayVar, collection));
          // int i$index = 0
          initializers.add(makeDeclaration(info, indexVar, JIntLiteral.get(0)));
          // int i$max = i$array.length
          initializers.add(makeDeclaration(info, maxVar,
              new JArrayLength(info, arrayVar.makeRef(info))));

          // i$index < i$max
          JExpression condition =
              new JBinaryOperation(info, JPrimitiveType.BOOLEAN, JBinaryOperator.LT,
                  indexVar.makeRef(info), maxVar.makeRef(info));

          // ++i$index
          JExpression increments = new JPrefixOperation(info, JUnaryOperator.INC,
              indexVar.makeRef(info));

          // T elementVar = i$array[i$index];
          elementDecl.initializer =
              new JArrayRef(info, arrayVar.makeRef(info), indexVar.makeRef(info));
          body.addStmt(0, elementDecl);

          result = new JForStatement(info, initializers, condition, increments, body);
        } else {
          /**
           * <pre>
           * for (Iterator&lt;T&gt; i$iterator = collection.iterator(); i$iterator.hasNext();) {
           *   T elementVar = i$iterator.next();
           *   // user action
           * }
           * </pre>
           */
          CompilationUnitScope cudScope = scope.compilationUnitScope();
          ReferenceBinding javaUtilIterator = scope.getJavaUtilIterator();
          ReferenceBinding javaLangIterable = scope.getJavaLangIterable();
          MethodBinding iterator = javaLangIterable.getExactMethod(ITERATOR_, NO_TYPES, cudScope);
          MethodBinding hasNext = javaUtilIterator.getExactMethod(HAS_NEXT_, NO_TYPES, cudScope);
          MethodBinding next = javaUtilIterator.getExactMethod(NEXT_, NO_TYPES, cudScope);
          JLocal iteratorVar =
              JProgram.createLocal(info, (elementVarName + "$iterator"), typeMap
                  .get(javaUtilIterator), false, curMethod.body);

          List<JStatement> initializers = Lists.newArrayListWithCapacity(1);
          // Iterator<T> i$iterator = collection.iterator()
          initializers.add(makeDeclaration(info, iteratorVar, new JMethodCall(info, collection,
              typeMap.get(iterator))));

          // i$iterator.hasNext()
          JExpression condition =
              new JMethodCall(info, iteratorVar.makeRef(info), typeMap.get(hasNext));

          // T elementVar = (T) i$iterator.next();
          elementDecl.initializer =
              new JMethodCall(info, iteratorVar.makeRef(info), typeMap.get(next));

          // Perform any implicit reference type casts (due to generics).
          // Note this occurs before potential unboxing.
          if (elementVar.getType() != javaLangObject) {
            TypeBinding collectionElementType = (TypeBinding) collectionElementTypeField.get(x);
            JType toType = typeMap.get(collectionElementType);
            assert (toType instanceof JReferenceType);
            elementDecl.initializer = maybeCast(toType, elementDecl.initializer);
          }

          body.addStmt(0, elementDecl);

          result = new JForStatement(info, initializers, condition,
              null, body);
        }

        // May need to box or unbox the element assignment.
        elementDecl.initializer =
            maybeBoxOrUnbox(elementDecl.initializer, x.elementVariableImplicitWidening);
        push(result);
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisit(ForStatement x, BlockScope scope) {
      try {
        SourceInfo info = makeSourceInfo(x);
        JStatement action = pop(x.action);

        // JDT represents the 3rd for component (increments) as a list of statements. These
        // statements are always expression statements as per JLS 14.14.1
        // Here the List<JExpressionStatement> is transformed into a more adequate List<Expression>.
        List<JExpression> incrementsExpressions = Lists.transform(pop(x.increments),
            new Function<JStatement, JExpression>() {
              @Override
              public JExpression apply(JStatement statement) {
                Preconditions.checkArgument(statement instanceof JExpressionStatement);
                return ((JExpressionStatement) statement).getExpr();
              }
            });

        // And turned into a single expression (possibly null if empty).
        JExpression incrementsExpression =
            singleExpressionFromExpressionList(info, incrementsExpressions);

        JExpression condition = pop(x.condition);
        List<JStatement> initializations = pop(x.initializations);
        push(new JForStatement(info, initializations, condition, incrementsExpression, action));
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisit(IfStatement x, BlockScope scope) {
      try {
        SourceInfo info = makeSourceInfo(x);
        JStatement elseStatement = pop(x.elseStatement);
        JStatement thenStatement = pop(x.thenStatement);
        JExpression condition = pop(x.condition);
        push(new JIfStatement(info, condition, thenStatement, elseStatement));
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisit(Initializer x, MethodScope scope) {
      try {
        JBlock block = pop(x.block);
        if (block != null) {
          curMethod.body.getBlock().addStmt(block);
        }
        popMethodInfo();
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisit(InstanceOfExpression x, BlockScope scope) {
      try {
        SourceInfo info = makeSourceInfo(x);
        JExpression expr = pop(x.expression);
        JReferenceType testType = (JReferenceType) typeMap.get(x.type.resolvedType);
        push(new JInstanceOf(info, testType, expr));
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisit(IntLiteral x, BlockScope scope) {
      try {
        push(JIntLiteral.get(x.constant.intValue()));
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisit(LabeledStatement x, BlockScope scope) {
      try {
        JStatement statement = pop(x.statement);
        if (statement == null) {
          push(null);
          return;
        }
        SourceInfo info = makeSourceInfo(x);
        push(new JLabeledStatement(info, getOrCreateLabel(info, x.label), statement));
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public boolean visit(ReferenceExpression x, BlockScope blockScope) {
      // T[][][]::new => lambda$n(int x) { return new T[int x][][]; }
      if (x.isArrayConstructorReference()) {
        // ensure array[]::new synthetic method (created by JDT) has an associated JMethod
        JMethod synthMethod = typeMap.get(x.binding);
        if (synthMethod.getBody() == null) {
          JMethodBody body = new JMethodBody(synthMethod.getSourceInfo());
          List<JExpression> dims = new ArrayList<JExpression>();
          JArrayType arrayType = (JArrayType) synthMethod.getType();
          JParameter dimParam = synthMethod.getParams().get(0);
          JExpression dimArgExpr = dimParam.makeRef(dimParam.getSourceInfo());
          dims.add(dimArgExpr);
          JNewArray newArray = JNewArray.createArrayWithDimensionExpressions(
              synthMethod.getSourceInfo(), arrayType, dims);
          body.getBlock().addStmt(newArray.makeReturnStatement());
          synthMethod.setBody(body);
        }
        push(null); // no qualifier
      }
      return true;
    }

    @Override
    public boolean visit(LambdaExpression x, BlockScope blockScope) {
      // Fetch the variables 'captured' by this lambda
      SyntheticArgumentBinding[] synthArgs = x.outerLocalVariables;
      // Get the parameter names, captured locals + lambda arguments
      String paramNames[] = computeCombinedParamNames(x, synthArgs);
      SourceInfo info = makeSourceInfo(x);
      // JDT synthesizes a method lambda$n(capture1, capture2, ..., lambda_arg1, lambda_arg2, ...)
      // Here we create a JMethod from this
      JMethod lambdaMethod = createMethodFromBinding(info, x.binding, paramNames);
      JMethodBody methodBody = new JMethodBody(info);
      lambdaMethod.setBody(methodBody);
      // We need to push this method  on the stack as it introduces a scope, and
      // expressions in the body need to lookup variable refs like parameters from it
      pushMethodInfo(new MethodInfo(lambdaMethod, methodBody, x.scope));
      pushLambdaExpressionLocalsIntoMethodScope(x, synthArgs, lambdaMethod);
      // now the body of the lambda is processed
      return true;
    }

    private void pushLambdaExpressionLocalsIntoMethodScope(LambdaExpression x,
        SyntheticArgumentBinding[] syntheticArguments, JMethod lambdaMethod) {
      Iterator<JParameter> it = lambdaMethod.getParams().iterator();
      if (syntheticArguments != null) {
        MethodScope scope = x.getScope();
        for (SyntheticArgumentBinding sa : syntheticArguments) {
          VariableBinding[] path = scope.getEmulationPath(sa.actualOuterLocalVariable);
          assert path.length == 1 && path[0] instanceof LocalVariableBinding;
          JParameter param = it.next();
          curMethod.locals.put((LocalVariableBinding) path[0], param);
        }
        for (Argument a : x.arguments) {
          curMethod.locals.put(a.binding, it.next());
        }
      }
    }

    /**
     * Calculate the names of all the parameters a lambda method will need, that is, the
     * combination of all captured locals plus all arguments to the lambda expression.
     */
    private String[] computeCombinedParamNames(LambdaExpression x,
        SyntheticArgumentBinding[] syntheticArguments) {
      String[] paramNames;
      paramNames = new String[x.binding.parameters.length];
      int numSynthArgs = syntheticArguments != null ? syntheticArguments.length : 0;
      for (int i = 0; i < paramNames.length; i++) {
        if (i < numSynthArgs) {
          paramNames[i] = nameForSyntheticArgument(syntheticArguments[i]);
        } else {
          paramNames[i] = nameForArgument(x.arguments, i - numSynthArgs, i);
        }
      }
      return paramNames;
    }

    private String nameForArgument(Argument[] arguments, int argIndex, int argPosition) {
      return new String(arguments[argIndex].name) + "_" + argPosition;
    }

    private String nameForSyntheticArgument(SyntheticArgumentBinding synthArg) {
      return synthArg.actualOuterLocalVariable != null ?
          intern(intern(synthArg.actualOuterLocalVariable.name) + "_" + synthArg.resolvedPosition) :
          intern(synthArg.name);
    }

    @Override
    public void endVisit(LambdaExpression x, BlockScope blockScope) {

      /**
       * Our output of a (args) -> expression_using_locals(locals) looks like this.
       *
       * class Enclosing {
       *
       *   T lambda$0(locals, args) {...lambda expr }
       *
       *   class lambda$0$type implements I {
       *       ctor([outer], locals) { ... }
       *       R <SAM lambdaMethod>(args) { return [outer].lambda$0(locals, args); }
       *   }
       * }
       *
       * And replaces the lambda with new lambda$0$Type([outer this], captured locals...).
       */

      // The target accepting this lambda is looking for which type? (e.g. ClickHandler, Runnable)
      TypeBinding binding = x.expectedType();
      // Find the single abstract method of this interface
      MethodBinding samBinding = binding.getSingleAbstractMethod(blockScope, false);
      assert (samBinding != null && samBinding.isValidBinding());

      // Lookup the JMethod version
      JMethod interfaceMethod = typeMap.get(samBinding);
      // And its JInterface container we must implement
      // There may be more than more JInterface containers to be implemented
      // if the lambda expression is cast to a IntersectionCastType.
      JInterfaceType[] funcType;
      if (binding instanceof IntersectionTypeBinding18) {
        funcType = processIntersectionTypeForLambda((IntersectionTypeBinding18) binding, blockScope,
            JdtUtil.signature(samBinding));
      } else {
        funcType = new JInterfaceType[] {(JInterfaceType) typeMap.get(binding)};
      }
      SourceInfo info = makeSourceInfo(x);

      // Create an inner class to implement the interface and SAM method.
      // class lambda$0$Type implements T {}

      String innerLambdaClassName =
          JdtUtil.getClassName(x.binding.declaringClass) + "$" + String.valueOf(x.binding.selector);
      JClassType innerLambdaClass = createInnerClass(innerLambdaClassName, x, info, funcType);
      JConstructor ctor = new JConstructor(info, innerLambdaClass, AccessModifier.PRIVATE);

      // locals captured by the lambda and saved as fields on the anonymous inner class
      List<JField> locals = new ArrayList<JField>();
      SyntheticArgumentBinding[] synthArgs = x.outerLocalVariables;

      // create the constructor for the anonymous inner and return the field used to store the
      // enclosing 'this' which is needed by the SAM method implementation later
      JField outerField =
          createLambdaConstructor(x, info, innerLambdaClass, ctor, locals, synthArgs);

      // the method containing the lambda expression that the anonymous inner class delegates to,
      // it corresponds directly to the lambda expression itself, produced by JDT as a helper method
      JMethod lambdaMethod = createLambdaMethod(x);

      // Now that we've added an implementation method for the lambda, we must create the inner
      // class method that implements the target interface type that delegates to the target lambda
      // method
      JMethod samMethod = new JMethod(info, interfaceMethod.getName(), innerLambdaClass,
          interfaceMethod.getType(), false, false, true, interfaceMethod.getAccess());

      // implements the SAM, e.g. Callback.onCallback(), Runnable.run(), etc
      createLambdaSamMethod(x, interfaceMethod, info, innerLambdaClass, locals, outerField,
          lambdaMethod,
          samMethod);

      ctor.freezeParamTypes();
      samMethod.freezeParamTypes();

      // replace (x,y,z) -> expr with 'new Lambda(args)'
      replaceLambdaWithInnerClassAllocation(x, info, innerLambdaClass, ctor, synthArgs);
      popMethodInfo();
      // Add the newly generated type
      newTypes.add(innerLambdaClass);
    }

    private void createLambdaSamMethod(LambdaExpression x, JMethod interfaceMethod, SourceInfo info,
        JClassType innerLambdaClass, List<JField> locals, JField outerField, JMethod lambdaMethod,
        JMethod samMethod) {
      // The parameters to this method will be the same as the Java interface that must be
      // implemented
      for (JParameter origParam : interfaceMethod.getParams()) {
        samMethod.cloneParameter(origParam);
      }
      // Create a body like void onClick(ClickEvent e) { OuterClass.lambdaMethod(locals, e); }
      JMethodBody samMethodBody = new JMethodBody(info);
      // First we create the method call to the outer lambda method
      JMethodCall samCall = new JMethodCall(info, x.shouldCaptureInstance ?
          new JFieldRef(info, new JThisRef(info, innerLambdaClass), outerField, innerLambdaClass) :
          null, lambdaMethod);

      // and add any locals that were storing captured outer variables as arguments to the call
      // first
      for (JField localField : locals) {
        samCall.addArg(new JFieldRef(info, new JThisRef(info, innerLambdaClass),
            localField, innerLambdaClass));
      }

      // and now we propagate the rest of the actual interface method parameters on the end
      // (e.g. ClickEvent e)
      for (JParameter param : samMethod.getParams()) {
        samCall.addArg(param.makeRef(info));
      }

      // we either add a return statement, or don't, depending on what the interface wants
      samMethodBody.getBlock().addStmt(
          JjsUtils.makeMethodEndStatement(samMethod.getType(), samCall));

      samMethod.setBody(samMethodBody);
      innerLambdaClass.addMethod(samMethod);
    }

    private JField createLambdaConstructor(LambdaExpression x, SourceInfo info,
        JClassType innerLambdaClass, JConstructor ctor, List<JField> locals,
        SyntheticArgumentBinding[] synthArgs) {
      // Create a constructor to accept all "captured" locals
      // CTor([OuterClassRef ref], capture1, capture2) { }
      JMethodBody ctorBody = new JMethodBody(info);
      JField outerField = null;
      // if this lambda refers to fields on the enclosing instance
      if (x.shouldCaptureInstance) {
        // ctor($$outer) { this.$$outer = $$outer; }
        outerField = createAndBindCapturedLambdaParameter(info, OUTER_LAMBDA_PARAM_NAME,
            innerLambdaClass.getEnclosingType(),
            ctor, ctorBody);
      }

      // Now we add parameters to the ctor
      // this is the outer instance (if needed), plus any method local variables captured
      String paramNames[] = new String[x.binding.parameters.length];
      int numSynthArgs = synthArgs != null ? synthArgs.length : 0;

      for (int i = 0; i < paramNames.length; i++) {
        // Setup params, fields, and ctor assignments for the outer captured vars
        if (i < numSynthArgs) {
          paramNames[i] = nameForSyntheticArgument(synthArgs[i]);
          JType captureType = typeMap.get(synthArgs[i].type);
          // adds ctor(..., param, ...) { ...this.param = param }
          JField captureField = createAndBindCapturedLambdaParameter(
              info, paramNames[i], captureType, ctor, ctorBody);
          locals.add(captureField);
        } else {
          // Record the names of the actual closure arguments,
          // e.g. (ClickEvent x) -> expr will be 'x'
          paramNames[i] = nameForArgument(x.arguments, i - numSynthArgs, i);
        }
      }

      ctor.setBody(ctorBody);
      innerLambdaClass.addMethod(ctor);
      return outerField;
    }

    private JMethod createLambdaMethod(LambdaExpression x) {
      // First let's get that synthetic method we created in the visit() call on the
      // containing class?
      JMethod lambdaMethod = curMethod.method;

      // And pop off the body nodes of the LambdaExpression that was processed as children
      // Deal with any boxing/unboxing needed
      JNode node = pop();
      if (node instanceof JExpression) {
        node = simplify((JExpression) node, (Expression) x.body);
      }

      JMethodBody body = (JMethodBody) curMethod.method.getBody();
      // and copy those nodes into the body of our synthetic method
      JStatement lambdaStatement = getOrCreateLambdaStatement(node);
      body.getBlock().addStmt(lambdaStatement);
      lambdaMethod.setBody(body);
      return lambdaMethod;
    }

    private void replaceLambdaWithInnerClassAllocation(LambdaExpression x, final SourceInfo info,
        JClassType innerLambdaClass, JConstructor ctor, SyntheticArgumentBinding[] synthArgs) {
      // Finally, we replace the LambdaExpression with
      // new InnerLambdaClass(this, local1, local2, ...);
      assert ctor.getEnclosingType() == innerLambdaClass;
      JNewInstance allocLambda = new JNewInstance(info, ctor);
      // only pass 'this' if lambda refers to fields on outer class
      if (x.shouldCaptureInstance) {
        allocLambda.addArg(new JThisRef(info, innerLambdaClass.getEnclosingType()));
      }
      for (final SyntheticArgumentBinding sa : synthArgs) {
        final MethodInfo method = methodStack.peek();
        JExpression capturedLocalReference = null;
        // Find the local variable in the current method context that is referred by the inner
        // lambda.
        LocalVariableBinding localVariable = FluentIterable.from(method.locals.keySet()).firstMatch(
            new Predicate<LocalVariableBinding>() {
              @Override
              public boolean apply(LocalVariableBinding enclosingLocal) {
                // Either the inner lambda refers directly to the enclosing scope variable, or
                // it is a capture from an enclosing scope, in which case both synthetic
                // arguments point to the same outer local variable.
                return enclosingLocal == sa.actualOuterLocalVariable
                    || (enclosingLocal instanceof SyntheticArgumentBinding)
                        && ((SyntheticArgumentBinding) enclosingLocal).actualOuterLocalVariable == 
                          sa.actualOuterLocalVariable;
              }
            }).orNull();
        if (localVariable != null) {
          // lambda is capturing a local from the immediate context
          capturedLocalReference = makeLocalRef(info, localVariable, method);
        } else {
          // Local variable not found in current method context. Trying to find corresponding
          // synthetic field in case if lambda is placed in anonymous/local class
          // e.g. { int x = 1; new Outer(){ void m (){ Lambda l = () -> x+1;} }; }
          Entry<SyntheticArgumentBinding, JField> capturedLocalInOuterClass = FluentIterable.from(
              curClass.syntheticFields.entrySet()).firstMatch(
                  new Predicate<Entry<SyntheticArgumentBinding, JField>>() {
                    @Override
                    public boolean apply(Entry<SyntheticArgumentBinding, JField> entry) {
                      return entry.getKey().actualOuterLocalVariable == sa.actualOuterLocalVariable;
                    }
                  }).orNull();
          if (capturedLocalInOuterClass != null) {
            // local from outer scope has already been captured by enclosing class.
            capturedLocalReference = makeInstanceFieldRef(info, capturedLocalInOuterClass
                .getValue());
          }
        }

        assert capturedLocalReference != null;
        allocLambda.addArg(capturedLocalReference);
      }
      // put the result on the stack, and pop out synthetic method from the scope
      push(allocLambda);
    }

    private JField createAndBindCapturedLambdaParameter(SourceInfo info,
        String paramName, JType captureType,
        JConstructor ctor, JMethodBody ctorBody) {
      JField paramField;
      JParameter param = createLambdaParameter(info, paramName, captureType, ctor);

      // Plus a field to store it
      paramField = createLambdaField(info, paramName, captureType, ctor.getEnclosingType());

      // Now add the initializers to bind the param to field
      // this.paramField = param
      JThisRef thisRef = new JThisRef(info, ctor.getEnclosingType());
      JFieldRef paramFieldRef = new JFieldRef(info, thisRef, paramField, ctor.getEnclosingType());
      JParameterRef paramRef = param.makeRef(info);
      ctorBody.getBlock().addStmt(
          new JBinaryOperation(info, paramFieldRef.getType(),
              JBinaryOperator.ASG,
              paramFieldRef, paramRef).makeStatement());
      return paramField;
    }

    private JField createLambdaField(SourceInfo info, String fieldName, JType fieldType,
        JClassType enclosingType) {
      JField outerField;
      outerField = new JField(info, fieldName, enclosingType, fieldType, false, Disposition.NONE,
          AccessModifier.PRIVATE);
      enclosingType.addField(outerField);
      return outerField;
    }

    private JParameter createLambdaParameter(SourceInfo info, String paramName, JType paramType,
        JConstructor ctor) {
      return ctor.createFinalParameter(info, paramName, paramType);
    }

    private JClassType createInnerClass(String name, FunctionalExpression x, SourceInfo info,
        JInterfaceType... funcType) {
      JClassType innerLambdaClass = new JClassType(info, name + "$Type", false, true);
      innerLambdaClass.setEnclosingType((JDeclaredType) typeMap.get(x.binding.declaringClass));
      for (JInterfaceType type : funcType) {
        innerLambdaClass.addImplements(type);
      }
      innerLambdaClass.setSuperClass(javaLangObject);

      createSyntheticMethod(info, CLINIT_METHOD_NAME, innerLambdaClass, JPrimitiveType.VOID, false,
          true, true, AccessModifier.PRIVATE);

      createSyntheticMethod(info, INIT_NAME_METHOD_NAME, innerLambdaClass, JPrimitiveType.VOID,
          false, false, true, AccessModifier.PRIVATE);

      // Add a getClass() implementation for all non-Object classes.
      createSyntheticMethod(info, GwtAstBuilder.GET_CLASS_METHOD_NAME, innerLambdaClass,
          javaLangClass, false, false, false, AccessModifier.PUBLIC,
          new JClassLiteral(info, innerLambdaClass).makeReturnStatement());

      innerLambdaClass.setClassDisposition(JDeclaredType.NestedClassDisposition.LAMBDA);
      return innerLambdaClass;
    }

      @Override
    public void endVisit(LocalDeclaration x, BlockScope scope) {
      try {
        SourceInfo info = makeSourceInfo(x);
        JLocal local = (JLocal) curMethod.locals.get(x.binding);
        assert local != null;
        JLocalRef localRef = local.makeRef(info);
        JExpression initialization = pop(x.initialization);
        push(new JDeclarationStatement(info, localRef, initialization));
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisit(LongLiteral x, BlockScope scope) {
      try {
        push(JLongLiteral.get(x.constant.longValue()));
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisit(MessageSend x, BlockScope scope) {
      try {
        SourceInfo info = makeSourceInfo(x);
        JMethod method = typeMap.get(x.binding);

        List<JExpression> arguments = popCallArgs(info, x.arguments, x.binding);
        JExpression receiver = pop(x.receiver);
        if (x.receiver instanceof ThisReference) {
          if (method.isStatic()) {
            // don't bother qualifying it, it's a no-op
            receiver = null;
          } else if ((x.bits & ASTNode.DepthMASK) != 0) {
            // outer method can be reached through emulation if implicit access
            ReferenceBinding targetType =
                scope.enclosingSourceType().enclosingTypeAt(
                    (x.bits & ASTNode.DepthMASK) >> ASTNode.DepthSHIFT);
            receiver = resolveThisReference(info, targetType, true, scope);
          } else if (x.receiver.sourceStart == 0) {
            // Synthetic this ref with bad source info; fix the info.
            JThisRef oldRef = (JThisRef) receiver;
            receiver = new JThisRef(info, oldRef.getClassType());
          }
        }

        JMethodCall call = new JMethodCall(info, receiver, method);

        // On a super ref, don't allow polymorphic dispatch. Oddly enough,
        // QualifiedSuperReference not derived from SuperReference!
        boolean isSuperRef =
            x.receiver instanceof SuperReference || x.receiver instanceof QualifiedSuperReference;
        if (isSuperRef) {
          call.setStaticDispatchOnly();
        }

        // The arguments come first...
        call.addArgs(arguments);

        if (x.valueCast != null) {
          JType castType = typeMap.get(x.valueCast);
          push(maybeCast(castType, call));
        } else {
          push(call);
        }
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisit(MethodDeclaration x, ClassScope scope) {
      try {

        if (x.isNative()) {
          processNativeMethod(x);
        } else {
          List<JStatement> statements = pop(x.statements);
          curMethod.body.getBlock().addStmts(statements);
        }
        popMethodInfo();
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisit(NullLiteral x, BlockScope scope) {
      push(JNullLiteral.INSTANCE);
    }

    @Override
    public void endVisit(OR_OR_Expression x, BlockScope scope) {
      pushBinaryOp(x, JBinaryOperator.OR);
    }

    @Override
    public void endVisit(PostfixExpression x, BlockScope scope) {
      try {
        SourceInfo info = makeSourceInfo(x);
        JUnaryOperator op;
        switch (x.operator) {
          case OperatorIds.MINUS:
            op = JUnaryOperator.DEC;
            break;

          case OperatorIds.PLUS:
            op = JUnaryOperator.INC;
            break;

          default:
            throw new InternalCompilerException("Unexpected postfix operator");
        }

        JExpression lhs = pop(x.lhs);
        push(new JPostfixOperation(info, op, lhs));
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisit(PrefixExpression x, BlockScope scope) {
      try {
        SourceInfo info = makeSourceInfo(x);
        JUnaryOperator op;
        switch (x.operator) {
          case OperatorIds.MINUS:
            op = JUnaryOperator.DEC;
            break;

          case OperatorIds.PLUS:
            op = JUnaryOperator.INC;
            break;

          default:
            throw new InternalCompilerException("Unexpected prefix operator");
        }

        JExpression lhs = pop(x.lhs);
        push(new JPrefixOperation(info, op, lhs));
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisit(QualifiedAllocationExpression x, BlockScope scope) {
      try {
        SourceInfo info = makeSourceInfo(x);
        List<JExpression> arguments = popCallArgs(info, x.arguments, x.binding);
        pushNewExpression(info, x, x.enclosingInstance(), arguments, scope);
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisit(QualifiedNameReference x, BlockScope scope) {
      try {
        JExpression curRef = resolveNameReference(x, scope);
        if (curRef == null) {
          push(null);
          return;
        }
        if (x.genericCast != null) {
          JType castType = typeMap.get(x.genericCast);
          curRef = maybeCast(castType, curRef);
        }
        SourceInfo info = curRef.getSourceInfo();

        /*
         * JDT represents multiple field access as an array of fields, each
         * qualified by everything to the left. So each subsequent item in
         * otherBindings takes the current expression as a qualifier.
         */
        if (x.otherBindings != null) {
          for (int i = 0; i < x.otherBindings.length; ++i) {
            FieldBinding fieldBinding = x.otherBindings[i];
            curRef = createFieldRef(curRef, info, fieldBinding);
            if (x.otherGenericCasts != null && x.otherGenericCasts[i] != null) {
              JType castType = typeMap.get(x.otherGenericCasts[i]);
              curRef = maybeCast(castType, curRef);
            }
          }
        }
        push(curRef);
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    private JExpression createFieldRef(JExpression instance, SourceInfo info,
        FieldBinding fieldBinding) {
      if (fieldBinding.declaringClass == null) {
        // probably array.length
        if (!LENGTH_FIELD_NAME.equals(String.valueOf(fieldBinding.name))) {
          throw new InternalCompilerException("Expected [array].length.");
        }
        instance = new JArrayLength(info, instance);
      } else {
        JField field = typeMap.get(fieldBinding);
        instance = new JFieldRef(info, instance, field, curClass.type);
      }
      return instance;
    }

    @Override
    public void endVisit(QualifiedSuperReference x, BlockScope scope) {
      try {
        // Oddly enough, super refs can be modeled as this refs, because
        // whatever expression they qualify has already been resolved.
        SourceInfo info = makeSourceInfo(x);
        ReferenceBinding targetType = (ReferenceBinding) x.qualification.resolvedType.erasure();
        if (targetType.isInterface()) {
          // Java8 super reference to default method from subtype, X.super.someDefaultMethod
          push(makeThisRef(info));
        } else {
          push(resolveThisReference(info, targetType, true, scope));
        }
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisit(QualifiedThisReference x, BlockScope scope) {
      try {
        SourceInfo info = makeSourceInfo(x);
        ReferenceBinding targetType = (ReferenceBinding) x.qualification.resolvedType;
        push(resolveThisReference(info, targetType, true, scope));
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    private Map<String, JClassType> lambdaNameToInnerLambdaType = Maps.newHashMap();

    @Override
    public void endVisit(ReferenceExpression x, BlockScope blockScope) {
      /**
       * Converts an expression like foo(qualifier::someMethod) into
       *
       * class Enclosing {
       *
       *   [static] T someMethod(locals, args) {...lambda expr }
       *
       *   class lambda$someMethodType implements I {
       *       ctor([qualifier]) { ... }
       *       R <SAM lambdaMethod>(args) { return [outer]someMethod(args); }
       *   }
       * }
       *
       * and replaces qualifier::someMethod with new lambda$someMethodType([outer this])
       *
       * [x] denotes optional, depending on context of whether outer this scope is needed.
       */

      // Calculate what type this reference is going to bind to, and what single abstract method
      TypeBinding binding = x.expectedType();
      MethodBinding samBinding = binding.getSingleAbstractMethod(blockScope, false).original();

      // Get the interface method is binds to
      JMethod interfaceMethod = typeMap.get(samBinding);
      JInterfaceType funcType = (JInterfaceType) typeMap.get(binding);
      SourceInfo info = makeSourceInfo(x);

      // Get the method that the Type::method is actually referring to
      MethodBinding referredMethodBinding = x.binding;
      if (referredMethodBinding instanceof SyntheticMethodBinding) {
        SyntheticMethodBinding synthRefMethodBinding =
            (SyntheticMethodBinding) referredMethodBinding;
        if (synthRefMethodBinding.targetMethod != null) {
          // generated in cases were a private method in an outer class needed to be called
          // e.g. outer.access$0 calls some outer.private_method
          referredMethodBinding = synthRefMethodBinding.targetMethod;
          // privateCtor::new generates overloaded <init> references with fake args that delegate
          // to the real ctor (JDT WTF!). Will we ever need to go deeper?
          if (synthRefMethodBinding.fakePaddedParameters != 0
              && synthRefMethodBinding.targetMethod instanceof SyntheticMethodBinding) {
            referredMethodBinding = ((SyntheticMethodBinding) referredMethodBinding).targetMethod;
          }
        }
      }
      JMethod referredMethod = typeMap.get(referredMethodBinding);
      boolean haveReceiver = false;
      try {
        haveReceiver = (Boolean) haveReceiverField.get(x);
      } catch (IllegalAccessException e) {
        throw translateException(x, e);
      }

      // Constructors and overloading means we need generate unique names
      String lambdaName = classNameForMethodReference(funcType,
          referredMethod,
          haveReceiver);

      List<JExpression> enclosingThisRefs = Lists.newArrayList();

      // Create an inner class to hold the implementation of the interface
      JClassType innerLambdaClass = lambdaNameToInnerLambdaType.get(lambdaName);
      if (innerLambdaClass == null) {
        innerLambdaClass = createInnerClass(lambdaName, x, info, funcType);
        lambdaNameToInnerLambdaType.put(lambdaName, innerLambdaClass);
        newTypes.add(innerLambdaClass);

        JConstructor ctor = new JConstructor(info, innerLambdaClass, AccessModifier.PRIVATE);

        JMethodBody ctorBody = new JMethodBody(info);
        JThisRef thisRef = new JThisRef(info, innerLambdaClass);
        JExpression instance = null;

        List<JField> enclosingInstanceFields = new ArrayList<JField>();
        // If we have a qualifier instance, we have to stash it in the constructor
        if (haveReceiver) {
          // this.$$outer = $$outer
          JField outerField = createAndBindCapturedLambdaParameter(info, OUTER_LAMBDA_PARAM_NAME,
              innerLambdaClass.getEnclosingType(), ctor, ctorBody);
          instance = new JFieldRef(info,
              new JThisRef(info, innerLambdaClass), outerField, innerLambdaClass);
        } else if (referredMethod instanceof JConstructor) {
          // the method we are invoking is a constructor and may need enclosing instances passed to
          // it.
          // For example, an class Foo { class Inner { Inner(int x) { } } } needs
          // it's constructor invoked with an enclosing instance, Inner::new
          // Java8 doesn't allow the qualifified case, e.g. x.new Foo() -> x.Foo::new
          ReferenceBinding targetBinding = referredMethodBinding.declaringClass;
          if (JdtUtil.isInnerClass(targetBinding)) {
            for (ReferenceBinding argType : targetBinding.syntheticEnclosingInstanceTypes()) {
              argType = (ReferenceBinding) argType.erasure();
              JExpression enclosingThisRef = resolveThisReference(info, argType, false, blockScope);
              JField enclosingInstance = createAndBindCapturedLambdaParameter(info,
                  String.valueOf(argType.readableName()).replace('.', '_'),
                  enclosingThisRef.getType(), ctor, ctorBody);
              enclosingInstanceFields.add(enclosingInstance);
              enclosingThisRefs.add(enclosingThisRef);
            }
          }
        }
        ctor.setBody(ctorBody);
        innerLambdaClass.addMethod(ctor);

        // Create an implementation of the target interface that invokes the method referred to
        // void onClick(ClickEvent e) { outer.referredMethod(e); }
        JMethod samMethod = new JMethod(info, interfaceMethod.getName(),
            innerLambdaClass, interfaceMethod.getType(),
            false, false, true, interfaceMethod.getAccess());
        for (JParameter origParam : interfaceMethod.getParams()) {
          samMethod.cloneParameter(origParam);
        }
        JMethodBody samMethodBody = new JMethodBody(info);

        Iterator<JParameter> paramIt = samMethod.getParams().iterator();
        // here's where it gets tricky. A method can have an implicit qualifier, e.g.
        // String::compareToIgnoreCase, it's non-static, it only has one argument, but it binds to
        // Comparator<T>.
        // The first argument serves as the qualifier, so for example, the method dispatch looks
        // like this: int compare(T a, T b) { a.compareTo(b); }
        if (!haveReceiver && !referredMethod.isStatic() && instance == null &&
            samMethod.getParams().size() == referredMethod.getParams().size() + 1) {
          // the instance qualifier is the first parameter in this case.
          // Needs to be cast the actual type due to generics.
          instance = new JCastOperation(info, typeMap.get(referredMethodBinding.declaringClass),
              paramIt.next().makeRef(info));
        }
        JMethodCall samCall = null;

        if (referredMethod.isConstructor()) {
          // Constructors must be invoked with JNewInstance
          samCall = new JNewInstance(info, (JConstructor) referredMethod);
          for (JField enclosingInstance : enclosingInstanceFields) {
            samCall.addArg(new JFieldRef(enclosingInstance.getSourceInfo(), thisRef,
                enclosingInstance, innerLambdaClass));
          }
        } else {
          // For static methods, instance will be null
          samCall = new JMethodCall(info, instance, referredMethod);
          // if super::method, we need static dispatch
          if (x.lhs instanceof SuperReference) {
            samCall.setStaticDispatchOnly();
          }
        }

        // Add the rest of the parameters from the interface method to methodcall
        // boxing or unboxing and dealing with varargs
        int paramNumber = 0;

        // need to build up an array of passed parameters if we have varargs
        List<JExpression> varArgInitializers = null;
        int varArg = referredMethodBinding.parameters.length - 1;

        // interface Foo { m(int x, int y); } bound to reference foo(int... args)
        // if varargs and incoming param is not already a var-arg, we'll need to convert
        // trailing args of the target interface into an array
        if (referredMethodBinding.isVarargs() && !samBinding.parameters[varArg].isArrayType()) {
          varArgInitializers = Lists.newArrayList();
        }

        while (paramIt.hasNext()) {
          JParameter param = paramIt.next();
          JExpression paramExpr = param.makeRef(info);
          // params may need to be boxed or unboxed
          TypeBinding destParam = null;
          // if it is not the trailing param or varargs, or interface method is already varargs
          if (varArgInitializers == null || !referredMethodBinding.isVarargs() || (paramNumber < varArg)) {
            destParam = referredMethodBinding.parameters[paramNumber];
            paramExpr = boxOrUnboxExpression(paramExpr, samBinding.parameters[paramNumber],
                destParam);
            samCall.addArg(paramExpr);
          } else if (!samBinding.parameters[paramNumber].isArrayType()) {
            // else add trailing parameters to var-args initializer list for an array
            destParam = referredMethodBinding.parameters[varArg].leafComponentType();
            paramExpr = boxOrUnboxExpression(paramExpr, samBinding.parameters[paramNumber],
                destParam);
            varArgInitializers.add(paramExpr);
          }
          paramNumber++;
        }

        // add trailing new T[] { initializers } var-arg array
        if (varArgInitializers != null) {
          JArrayType lastParamType =
              (JArrayType) typeMap.get(
                  referredMethodBinding.parameters[referredMethodBinding.parameters.length - 1]);
          JNewArray newArray =
              JNewArray.createArrayWithInitializers(info, lastParamType, varArgInitializers);
          samCall.addArg(newArray);
        }

        // TODO(rluble): Make this a call to JjsUtils.makeMethodEndStatement once boxing/unboxing
        // is handled there.
        if (samMethod.getType() != JPrimitiveType.VOID) {
          JExpression samExpression = boxOrUnboxExpression(samCall, referredMethodBinding.returnType,
              samBinding.returnType);
          samMethodBody.getBlock().addStmt(simplify(samExpression, x).makeReturnStatement());
        } else {
          samMethodBody.getBlock().addStmt(samCall.makeStatement());
        }
        samMethod.setBody(samMethodBody);
        innerLambdaClass.addMethod(samMethod);
        ctor.freezeParamTypes();
        samMethod.freezeParamTypes();
      }

      JConstructor lambdaCtor = null;
      for (JMethod method : innerLambdaClass.getMethods()) {
        if (method instanceof JConstructor) {
          lambdaCtor = (JConstructor) method;
          break;
        }
      }

      assert lambdaCtor != null;

      // Replace the ReferenceExpression qualifier::method with new lambdaType(qualifier)
      assert lambdaCtor.getEnclosingType() == innerLambdaClass;
      JNewInstance allocLambda = new JNewInstance(info, lambdaCtor);
      JExpression qualifier = (JExpression) pop();
      if (haveReceiver) {
        // pop qualifier from stack
        allocLambda.addArg(qualifier);
      } else {
        // you can't simultaneously have a qualifier, and have enclosing inner class refs
        // because Java8 won't allow a qualified constructor method reference, e.g. x.Foo::new
        for (JExpression enclosingRef : enclosingThisRefs) {
          allocLambda.addArg(enclosingRef);
        }
      }

      push(allocLambda);
    }

    /**
     * Java8 Method References such as String::equalsIgnoreCase should produce inner class names
     * that are a function of the samInterface (e.g. Runnable), the method being referred to,
     * and the qualifying disposition (this::foo vs Class::foo if foo is an instance method)
     */
    private String classNameForMethodReference(
        JInterfaceType functionalInterface, JMethod referredMethod, boolean hasReceiver) {

      return JjsUtils.classNameForMethodReference(typeMap.get(curCud.cud.types[0].binding),
          functionalInterface, referredMethod, hasReceiver);
    }

    private JExpression boxOrUnboxExpression(JExpression expr, TypeBinding fromType,
        TypeBinding toType) {
      if (fromType == TypeBinding.VOID || toType == TypeBinding.VOID) {
        return expr;
      }

      if (fromType.isBaseType() && !toType.isBaseType()) {
        int implicitConversion = (fromType.id & TypeIds.IMPLICIT_CONVERSION_MASK) << 4;
        implicitConversion = implicitConversion | TypeIds.BOXING;
        return box(expr, implicitConversion);
      }

      if (!fromType.isBaseType() && toType.isBaseType()) {
        int implicitConversion = (toType.id & TypeIds.IMPLICIT_CONVERSION_MASK) << 4;
        implicitConversion = implicitConversion | TypeIds.UNBOXING;
        return unbox(expr, implicitConversion);
      }

      TypeBinding castToType = fromType.genericCast(toType);
      if (castToType == null) {
        return expr;
      }
      return new JCastOperation(expr.getSourceInfo(), typeMap.get(castToType), expr);
    }

    @Override
    public void endVisit(ReturnStatement x, BlockScope scope) {
      try {
        SourceInfo info = makeSourceInfo(x);
        JExpression expression = pop(x.expression);
        push(new JReturnStatement(info, expression));
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisit(SingleNameReference x, BlockScope scope) {
      try {
        JExpression result = resolveNameReference(x, scope);
        if (result == null) {
          push(null);
          return;
        }
        if (x.genericCast != null) {
          JType castType = typeMap.get(x.genericCast);
          result = maybeCast(castType, result);
        }
        push(result);
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisit(StringLiteral x, BlockScope scope) {
      try {
        SourceInfo info = makeSourceInfo(x);
        push(getStringLiteral(info, x.constant.stringValue()));
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisit(StringLiteralConcatenation x, BlockScope scope) {
      try {
        SourceInfo info = makeSourceInfo(x);
        push(getStringLiteral(info, x.constant.stringValue()));
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisit(SuperReference x, BlockScope scope) {
      try {
        assert (typeMap.get(x.resolvedType) == curClass.getClassOrInterface().getSuperClass());
        // Super refs can be modeled as a this ref.
        push(makeThisRef(makeSourceInfo(x)));
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisit(SwitchStatement x, BlockScope scope) {
      try {
        SourceInfo info = makeSourceInfo(x);

        JBlock block = popBlock(info, x.statements);
        JExpression expression = pop(x.expression);

        if (x.expression.resolvedType.isEnum()) {
          // synthesize a call to ordinal().
          expression = synthesizeCallToOrdinal(scope, info, expression);
        }
        push(new JSwitchStatement(info, expression, block));
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisit(SynchronizedStatement x, BlockScope scope) {
      try {
        JBlock block = pop(x.block);
        JExpression expression = pop(x.expression);
        block.addStmt(0, expression.makeStatement());
        push(block);
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisit(ThisReference x, BlockScope scope) {
      try {
        assert typeMap.get(x.resolvedType) == curClass.getClassOrInterface();
        push(makeThisRef(makeSourceInfo(x)));
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisit(ThrowStatement x, BlockScope scope) {
      try {
        SourceInfo info = makeSourceInfo(x);
        JExpression exception = pop(x.exception);
        push(new JThrowStatement(info, exception));
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisit(TrueLiteral x, BlockScope scope) {
      push(JBooleanLiteral.TRUE);
    }

    @Override
    public void endVisit(TryStatement x, BlockScope scope) {
      try {
        SourceInfo info = makeSourceInfo(x);

        JBlock finallyBlock = pop(x.finallyBlock);
        List<JBlock> catchBlocks = pop(x.catchBlocks);
        JBlock tryBlock = pop(x.tryBlock);

        if (x.resources.length > 0) {
          tryBlock = normalizeTryWithResources(info, x, tryBlock);
        }
        List<JTryStatement.CatchClause> catchClauses = Lists.newArrayList();
        if (x.catchBlocks != null) {
          for (int i = 0; i < x.catchArguments.length; i++) {
            Argument argument = x.catchArguments[i];
            JLocal local = (JLocal) curMethod.locals.get(argument.binding);

            List<JType> catchTypes = Lists.newArrayList();
            if (argument.type instanceof UnionTypeReference) {
              // This is a multiexception
              for (TypeReference type : ((UnionTypeReference) argument.type).typeReferences) {
                catchTypes.add(typeMap.get(type.resolvedType));
              }
            } else {
              // Regular exception
              catchTypes.add(local.getType());
            }
            catchClauses.add(new JTryStatement.CatchClause(catchTypes, local.makeRef(info),
                catchBlocks.get(i)));
          }
        }
        push(new JTryStatement(info, tryBlock, catchClauses, finallyBlock));
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    private JBlock normalizeTryWithResources(SourceInfo info, TryStatement x, JBlock tryBlock) {
      /**
       * Apply the following source transformation:
       *
       * try (A1 a1 = new A1(); ... ; An an = new An()) {
       *   ... tryBlock...
       *  } ...catch/finally blocks
       *
       *  to
       *
       * try {
       *   A1 a1 = null; ...; An an = null;
       *   Throwable $exception = null;
       *   try {
       *     a1 = new A1();... ; an = new An();
       *     ... tryBlock...
       *   } catch (Throwable t) {
       *     $exception = t;
       *     throw t;
       *   } finally {
       *    $exception = Exceptions.safeClose(an, $exception);
       *    ...
       *    $exception = Exceptions.safeClose(a1, $exception);
       *  if ($exception != null) {
       *    throw $exception;
       *  }
       * } ...catch/finally blocks
       *
       */

      JBlock outerTryBlock = new JBlock(info);
      // add resource variables
      List<JLocal> resourceVariables = Lists.newArrayList();
      for (int i = x.resources.length - 1; i >= 0; i--) {
        // Needs to iterate back to front to be inline with the contents of the stack.

        JDeclarationStatement resourceDecl = pop(x.resources[i]);

        JLocal resourceVar = (JLocal) curMethod.locals.get(x.resources[i].binding);
        resourceVariables.add(0, resourceVar);
        tryBlock.addStmt(0, resourceDecl);
      }

      // add exception variable
      JLocal exceptionVar = createLocalThrowable(info, "$primary_ex");

      outerTryBlock.addStmt(makeDeclaration(info, exceptionVar, JNullLiteral.INSTANCE));

      // create catch block
      List<JTryStatement.CatchClause> catchClauses = Lists.newArrayListWithCapacity(1);

      List<JType> clauseTypes = Lists.newArrayListWithCapacity(1);
      clauseTypes.add(javaLangThrowable);

      //     add catch exception variable.
      JLocal catchVar = createLocalThrowable(info, "$caught_ex");

      JBlock catchBlock = new JBlock(info);
      catchBlock.addStmt(createAssignment(info, javaLangThrowable, exceptionVar, catchVar));
      catchBlock.addStmt(new JThrowStatement(info, exceptionVar.makeRef(info)));

      catchClauses.add(new JTryStatement.CatchClause(clauseTypes, catchVar.makeRef(info),
          catchBlock));

      // create finally block
      JBlock finallyBlock = new JBlock(info);
      for (int i = x.resources.length - 1; i >= 0; i--) {
        finallyBlock.addStmt(createCloseBlockFor(info,
            resourceVariables.get(i), exceptionVar));
      }

      // if (exception != null) throw exception
      JExpression exceptionNotNull = new JBinaryOperation(info, JPrimitiveType.BOOLEAN,
          JBinaryOperator.NEQ, exceptionVar.makeRef(info), JNullLiteral.INSTANCE);
      finallyBlock.addStmt(new JIfStatement(info, exceptionNotNull,
          new JThrowStatement(info, exceptionVar.makeRef(info)), null));

      // Stitch all together into a inner try block
      outerTryBlock.addStmt(new JTryStatement(info, tryBlock, catchClauses,
            finallyBlock));
      return outerTryBlock;
    }

    private JLocal createLocalThrowable(SourceInfo info, String prefix) {
      int index = curMethod.body.getLocals().size() + 1;
      return JProgram.createLocal(info, prefix + "_" + index,
          javaLangThrowable, false, curMethod.body);
    }

    private JStatement createCloseBlockFor(
        SourceInfo info, JLocal resourceVar, JLocal exceptionVar) {
      /**
       * Create the following code:
       *
       * $ex = Exceptions.safeClose(resource, $ex);
       *
       * which is equivalent to
       *
       * if (resource != null) {
       *   try {
       *     resource.close();
       *   } catch (Throwable t) {
       *     if ($ex == null) {
       *       $ex = t;
       *     } else {
       *      $ex.addSuppressed(t);
       *     }
       *   }
       */

      JMethodCall safeCloseCall = new JMethodCall(info, null, SAFE_CLOSE_METHOD);
      safeCloseCall.addArg(0, resourceVar.makeRef(info));
      safeCloseCall.addArg(1, exceptionVar.makeRef(info));

      return new JBinaryOperation(info, javaLangThrowable, JBinaryOperator.ASG,
          exceptionVar.makeRef(info), safeCloseCall).makeStatement();
    }

    private JStatement createAssignment(SourceInfo info, JType type, JLocal lhs, JLocal rhs) {
      return new JBinaryOperation(info, type, JBinaryOperator.ASG, lhs.makeRef(info),
          rhs.makeRef(info)).makeStatement();
    }

    @Override
    public void endVisit(TypeDeclaration x, ClassScope scope) {
      endVisit(x);
    }

    @Override
    public void endVisit(TypeDeclaration x, CompilationUnitScope scope) {
      endVisit(x);
    }

    @Override
    public void endVisit(UnaryExpression x, BlockScope scope) {
      try {
        SourceInfo info = makeSourceInfo(x);
        JUnaryOperator op;
        int operator = ((x.bits & ASTNode.OperatorMASK) >> ASTNode.OperatorSHIFT);

        switch (operator) {
          case OperatorIds.MINUS:
            op = JUnaryOperator.NEG;
            break;

          case OperatorIds.NOT:
            op = JUnaryOperator.NOT;
            break;

          case OperatorIds.PLUS:
            // Odd case.. useless + operator; just leave the operand on the
            // stack.
            return;

          case OperatorIds.TWIDDLE:
            op = JUnaryOperator.BIT_NOT;
            break;

          default:
            throw new InternalCompilerException("Unexpected operator for unary expression");
        }

        JExpression expression = pop(x.expression);
        push(new JPrefixOperation(info, op, expression));
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisit(WhileStatement x, BlockScope scope) {
      try {
        SourceInfo info = makeSourceInfo(x);
        JStatement action = pop(x.action);
        JExpression condition = pop(x.condition);
        push(new JWhileStatement(info, condition, action));
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public void endVisitValid(TypeDeclaration x, BlockScope scope) {
      endVisit(x);
      if (!x.binding.isAnonymousType()) {
        // Class declaration as a statement; insert a dummy statement.
        push(null);
      }
    }

    @Override
    public boolean visit(AnnotationMethodDeclaration x, ClassScope classScope) {
      return visit((MethodDeclaration) x, classScope);
    }

    @Override
    public boolean visit(Argument x, BlockScope scope) {
      // handled by parents
      return true;
    }

    @Override
    public boolean visit(Block x, BlockScope scope) {
      x.statements = reduceToReachable(x.statements);
      return true;
    }

    @Override
    public boolean visit(ConstructorDeclaration x, ClassScope scope) {
      try {
        JConstructor method = (JConstructor) typeMap.get(x.binding);
        assert !method.isExternal();
        JMethodBody body = new JMethodBody(method.getSourceInfo());
        method.setBody(body);
        pushMethodInfo(new MethodInfo(method, body, x.scope));

        // Map all arguments.
        Iterator<JParameter> it = method.getParams().iterator();

        // Enum arguments have no mapping.
        if (curClass.classType.isEnumOrSubclass() != null) {
          // Skip past name and ordinal.
          it.next();
          it.next();
        }

        // Map synthetic arguments for outer this.
        ReferenceBinding declaringClass = (ReferenceBinding) x.binding.declaringClass.erasure();
        boolean isNested = JdtUtil.isInnerClass(declaringClass);
        if (isNested) {
          NestedTypeBinding nestedBinding = (NestedTypeBinding) declaringClass;
          if (nestedBinding.enclosingInstances != null) {
            for (SyntheticArgumentBinding argument : nestedBinding.enclosingInstances) {
              curMethod.locals.put(argument, it.next());
            }
          }
        }

        // Map user arguments.
        if (x.arguments != null) {
          for (Argument argument : x.arguments) {
            curMethod.locals.put(argument.binding, it.next());
          }
        }

        // Map synthetic arguments for locals.
        if (isNested) {
          // add synthetic args for locals
          NestedTypeBinding nestedBinding = (NestedTypeBinding) declaringClass;
          // add synthetic args for outer this and locals
          if (nestedBinding.outerLocalVariables != null) {
            for (SyntheticArgumentBinding argument : nestedBinding.outerLocalVariables) {
              curMethod.locals.put(argument, it.next());
            }
          }
        }

        x.statements = reduceToReachable(x.statements);
        return true;
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public boolean visit(ExplicitConstructorCall explicitConstructor, BlockScope scope) {
      scope.methodScope().isConstructorCall = true;
      return true;
    }

    @Override
    public boolean visit(FieldDeclaration x, MethodScope scope) {
      try {
        assert !typeMap.get(x.binding).isExternal();
        pushInitializerMethodInfo(x, scope);
        return true;
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public boolean visit(Initializer x, MethodScope scope) {
      try {
        pushInitializerMethodInfo(x, scope);
        return true;
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public boolean visit(LocalDeclaration x, BlockScope scope) {
      try {
        createLocal(x);
        return true;
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public boolean visit(MarkerAnnotation annotation, BlockScope scope) {
      return false;
    }

    @Override
    public boolean visit(MethodDeclaration x, ClassScope scope) {
      try {
        JMethod method = typeMap.get(x.binding);
        assert !method.isExternal();
        JMethodBody body = null;
        if (!method.isJsniMethod()) {
          body = new JMethodBody(method.getSourceInfo());
          method.setBody(body);
        }
        pushMethodInfo(new MethodInfo(method, body, x.scope));

        // Map user arguments.
        Iterator<JParameter> it = method.getParams().iterator();
        if (x.arguments != null) {
          for (Argument argument : x.arguments) {
            curMethod.locals.put(argument.binding, it.next());
          }
        }
        x.statements = reduceToReachable(x.statements);
        return true;
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public boolean visit(NormalAnnotation annotation, BlockScope scope) {
      return false;
    }

    @Override
    public boolean visit(SingleMemberAnnotation annotation, BlockScope scope) {
      return false;
    }

    @Override
    public boolean visit(SwitchStatement x, BlockScope scope) {
      x.statements = reduceToReachable(x.statements);
      return true;
    }

    @Override
    public boolean visit(TryStatement x, BlockScope scope) {
      try {
        if (x.catchBlocks != null) {
          for (Argument argument : x.catchArguments) {
            createLocal(argument);
          }
        }
        return true;
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    @Override
    public boolean visit(TypeDeclaration x, ClassScope scope) {
      return visit(x);
    }

    @Override
    public boolean visit(TypeDeclaration x, CompilationUnitScope scope) {
      return visit(x);
    }

    @Override
    public boolean visitValid(TypeDeclaration x, BlockScope scope) {
      // Local types actually need to be created now.
      createTypes(x);
      resolveTypeRefs(x);
      createMembers(x);
      return visit(x);
    }

    protected void endVisit(TypeDeclaration x) {
      JDeclaredType type = curClass.type;

      // Synthesize super clinit calls.
      if (type instanceof JClassType) {
        Iterable<JInterfaceType> interfacesToInitialize =
            Iterables.transform(
                JdtUtil.getSuperInterfacesRequiringInitialization(x.binding),
                new Function<ReferenceBinding, JInterfaceType>() {
                  @Override
                  public JInterfaceType apply(ReferenceBinding referenceBinding) {
                    return (JInterfaceType) typeMap.get(referenceBinding);
                  }
                });
        JjsUtils.synthesizeStaticInitializerChain(type, interfacesToInitialize);
      }

      // Implement getClass() implementation for all non-Object classes.
      if (isSyntheticGetClassNeeded(x, type) && !type.isAbstract()) {
        implementGetClass(type);
      }

      if (type instanceof JEnumType) {
        processEnumType((JEnumType) type);
      }

      if (type instanceof JClassType) {
        addBridgeMethods(x.binding);
      }

      curClass = classStack.pop();
    }

    protected JBlock pop(Block x) {
      return (x == null) ? null : (JBlock) pop();
    }

    protected JExpression pop(Expression x) {
      if (x == null) {
        return null;
      }
      JExpression result = (JExpression) pop();
      if (result == null) {
        assert x instanceof NameReference;
        return null;
      }
      result = simplify(result, x);
      return result;
    }

    @SuppressWarnings("unchecked")
    protected <T extends JExpression> List<T> pop(Expression[] expressions) {
      if (expressions == null) {
        return Collections.emptyList();
      }

      List<T> result = (List<T>) popList(Collections2.filter(Arrays.asList(expressions),
          Predicates.notNull()).size());

      for (int i = 0; i < result.size(); i++) {
        result.set(i, (T) simplify(result.get(i), expressions[i]));
      }
      return result;
    }

    protected JDeclarationStatement pop(LocalDeclaration decl) {
      return (decl == null) ? null : (JDeclarationStatement) pop();
    }

    protected JStatement pop(Statement x) {
      JNode pop = (x == null) ? null : pop();
      if (x instanceof Expression) {
        return simplify((JExpression) pop, (Expression) x).makeStatement();
      }
      return (JStatement) pop;
    }

    @SuppressWarnings("unchecked")
    protected <T extends JStatement> List<T> pop(Statement[] statements) {
      if (statements == null) {
        return Collections.emptyList();
      }
      List<T> result = (List<T>) popList(statements.length);
      int i = 0;
      for (ListIterator<T> it = result.listIterator(); it.hasNext(); ++i) {
        Object element = it.next();
        if (element == null) {
          it.remove();
        } else if (element instanceof JExpression) {
          it.set((T) simplify((JExpression) element, (Expression) statements[i]).makeStatement());
        }
      }
      return result;
    }

    protected JBlock popBlock(SourceInfo info, Statement statement) {
      JStatement stmt = pop(statement);
      if (stmt instanceof JBlock) {
        return (JBlock) stmt;
      }
      JBlock block = new JBlock(info);
      if (stmt != null) {
        block.addStmt(stmt);
      }
      return block;
    }

    protected JBlock popBlock(SourceInfo info, Statement[] statements) {
      List<JStatement> stmts = pop(statements);
      JBlock block = new JBlock(info);
      block.addStmts(stmts);
      return block;
    }

    protected void pushBinaryOp(Assignment x, JBinaryOperator op) {
      pushBinaryOp(x, op, x.lhs, x.expression);
    }

    protected void pushBinaryOp(BinaryExpression x, JBinaryOperator op) {
      pushBinaryOp(x, op, x.left, x.right);
    }

    protected boolean visit(TypeDeclaration x) {
      JDeclaredType type = (JDeclaredType) typeMap.get(x.binding);
      assert !type.isExternal();
      classStack.push(curClass);
      curClass = new ClassInfo(type, x);

      /*
       * It's okay to defer creation of synthetic fields, they can't be
       * referenced until we analyze the code.
       */
      SourceTypeBinding binding = x.binding;
      if (JdtUtil.isInnerClass(binding)) {
        // add synthetic fields for outer this and locals
        assert (type instanceof JClassType);
        NestedTypeBinding nestedBinding = (NestedTypeBinding) binding;
        if (nestedBinding.enclosingInstances != null) {
          for (SyntheticArgumentBinding argument : nestedBinding.enclosingInstances) {
            createSyntheticField(argument, type, Disposition.THIS_REF);
          }
        }

        if (nestedBinding.outerLocalVariables != null) {
          for (SyntheticArgumentBinding argument : nestedBinding.outerLocalVariables) {
            // See InnerClassTest.testOuterThisFromSuperCall().
            boolean isReallyThisRef = false;
            if (argument.actualOuterLocalVariable instanceof SyntheticArgumentBinding) {
              SyntheticArgumentBinding outer =
                  (SyntheticArgumentBinding) argument.actualOuterLocalVariable;
              if (outer.matchingField != null) {
                JField field = typeMap.get(outer.matchingField);
                if (field.isThisRef()) {
                  isReallyThisRef = true;
                }
              }
            }
            createSyntheticField(argument, type, isReallyThisRef ? Disposition.THIS_REF
                : Disposition.FINAL);
          }
        }
      }
      return true;
    }

    /**
     * <p>
     * Add a bridge method to <code>clazzBinding</code> for any method it
     * inherits that implements an interface method but that has a different
     * erased signature from the interface method.
     * </p>
     *
     * <p>
     * The need for these bridges was pointed out in issue 3064. The goal is
     * that virtual method calls through an interface type are translated to
     * JavaScript that will function correctly. If the interface signature
     * matches the signature of the implementing method, then nothing special
     * needs to be done. If they are different, due to the use of generics, then
     * GenerateJavaScriptAST is careful to do the right thing. There is a
     * remaining case, though, that GenerateJavaScriptAST is not in a good
     * position to fix: a method could be inherited from a superclass, used to
     * implement an interface method that has a different type signature, and
     * does not have the interface method in its list of overrides. In that
     * case, a bridge method should be added that overrides the interface method
     * and then calls the implementation method.
     * </p>
     *
     * <p>
     * This method should only be called once all regular, non-bridge methods
     * have been installed on the GWT types.
     * </p>
     */
    private void addBridgeMethods(SourceTypeBinding clazzBinding) {
      /*
       * JDT adds bridge methods in all the places GWT needs them. Use JDT's
       * bridge methods.
       */
      if (clazzBinding.syntheticMethods() != null) {
        for (SyntheticMethodBinding synthmeth : clazzBinding.syntheticMethods()) {
          if (synthmeth.purpose == SyntheticMethodBinding.BridgeMethod && !synthmeth.isStatic()) {
            createBridgeMethod(synthmeth);
          }
        }
      }
    }

    private JBinaryOperation assignSyntheticField(SourceInfo info, SyntheticArgumentBinding arg) {
      JParameter param = (JParameter) curMethod.locals.get(arg);
      assert param != null;
      JField field = curClass.syntheticFields.get(arg);
      assert field != null;
      JFieldRef lhs = makeInstanceFieldRef(info, field);
      return
          new JBinaryOperation(info, lhs.getType(), JBinaryOperator.ASG, lhs, param.makeRef(info));
    }

    private JExpression box(JExpression original, int implicitConversion) {
      int typeId = (implicitConversion & TypeIds.IMPLICIT_CONVERSION_MASK) >> 4;
      ClassScope scope = curClass.scope;
      BaseTypeBinding primitiveType = (BaseTypeBinding) TypeBinding.wellKnownType(scope, typeId);
      ReferenceBinding boxType = (ReferenceBinding) scope.boxing(primitiveType);
      MethodBinding valueOfMethod = boxType.getExactMethod(VALUE_OF_,
          new TypeBinding[]{primitiveType}, scope.compilationUnitScope());
      assert valueOfMethod != null;

      // Add a cast to the correct primitive type if needed.
      JType targetPrimitiveType = typeMap.get(primitiveType);
      if (original.getType() != targetPrimitiveType) {
        original = new JCastOperation(original.getSourceInfo(), targetPrimitiveType, original);
      }

      JMethod boxMethod = typeMap.get(valueOfMethod);
      JMethodCall call = new JMethodCall(original.getSourceInfo(), null, boxMethod);
      call.addArg(original);
      return call;
    }

    /**
     * Create a bridge method. It calls a same-named method with the same
     * arguments, but with a different type signature.
     */
    private void createBridgeMethod(SyntheticMethodBinding jdtBridgeMethod) {
      JMethod implmeth = typeMap.get(jdtBridgeMethod.targetMethod);
      SourceInfo info = implmeth.getSourceInfo();
      JMethod bridgeMethod =
          new JMethod(info, implmeth.getName(), curClass.type, typeMap
              .get(jdtBridgeMethod.returnType), false, false, implmeth.isFinal(), implmeth
              .getAccess());
      typeMap.setMethod(jdtBridgeMethod, bridgeMethod);
      bridgeMethod.setBody(new JMethodBody(info));
      curClass.type.addMethod(bridgeMethod);
      bridgeMethod.setSynthetic();
      int paramIdx = 0;
      List<JParameter> implParams = implmeth.getParams();
      for (TypeBinding jdtParamType : jdtBridgeMethod.parameters) {
        JParameter param = implParams.get(paramIdx++);
        JType paramType = typeMap.get(jdtParamType.erasure());
        bridgeMethod.createFinalParameter(param.getSourceInfo(), param.getName(), paramType);
      }
      for (ReferenceBinding exceptionReference : jdtBridgeMethod.thrownExceptions) {
        bridgeMethod.addThrownException((JClassType) typeMap.get(exceptionReference.erasure()));
      }
      bridgeMethod.freezeParamTypes();

      // create a call and pass all arguments through, casting if necessary
      JMethodCall call = new JMethodCall(info, makeThisRef(info), implmeth);
      for (int i = 0; i < bridgeMethod.getParams().size(); i++) {
        JParameter param = bridgeMethod.getParams().get(i);
        call.addArg(maybeCast(implParams.get(i).getType(), param.makeRef(info)));
      }

      JMethodBody body = (JMethodBody) bridgeMethod.getBody();
      if (bridgeMethod.getType() == JPrimitiveType.VOID) {
        body.getBlock().addStmt(call.makeStatement());
      } else {
        body.getBlock().addStmt(call.makeReturnStatement());
      }
    }

    private void writeEnumValuesMethod(JEnumType type, JMethod method) {
      // return new E[]{A,B,C};
      JArrayType enumArrayType = new JArrayType(type);
      SourceInfo info = type.getSourceInfo();
      List<JExpression> initializers = Lists.newArrayList();
      for (JEnumField field : type.getEnumList()) {
        JFieldRef fieldRef = new JFieldRef(info, null, field, type);
        initializers.add(fieldRef);
      }
      JNewArray valuesArrayCopy =
          JNewArray.createArrayWithInitializers(info, enumArrayType, initializers);
      if (type.getEnumList().size() > MAX_INLINEABLE_ENUM_SIZE) {
        // Only inline values() if it is small.
        method.setInliningMode(InliningMode.DO_NOT_INLINE);
      }
      JjsUtils.replaceMethodBody(method, valuesArrayCopy);
    }

    private JLocal createLocal(LocalDeclaration x) {
      LocalVariableBinding b = x.binding;
      TypeBinding resolvedType = x.type.resolvedType;
      JType localType;
      if (resolvedType.constantPoolName() != null) {
        localType = typeMap.get(resolvedType);
      } else {
        // Special case, a statically unreachable local type.
        localType = JReferenceType.NULL_TYPE;
      }
      SourceInfo info = makeSourceInfo(x);
      JLocal newLocal =
          JProgram.createLocal(info, intern(x.name), localType, b.isFinal(), curMethod.body);
      curMethod.locals.put(b, newLocal);
      return newLocal;
    }

    private JField createSyntheticField(SyntheticArgumentBinding arg, JDeclaredType enclosingType,
        Disposition disposition) {
      JType type = typeMap.get(arg.type);
      SourceInfo info = enclosingType.getSourceInfo();
      // Construct field name including position because JDT can sometimes create multiple synthetic
      // fields with the same name. The increased name size won't affect optimized output since
      // references are pruned and renamed.
      String fieldName = intern(intern(arg.name) + arg.resolvedPosition);
      JField field = new JField(info, fieldName, enclosingType, type, false, disposition,
          AccessModifier.PRIVATE);
      enclosingType.addField(field);
      curClass.syntheticFields.put(arg, field);
      if (arg.matchingField != null) {
        typeMap.setField(arg.matchingField, field);
      }
      return field;
    }

    private JExpression getConstant(SourceInfo info, Constant constant) {
      switch (constant.typeID()) {
        case TypeIds.T_int:
          return JIntLiteral.get(constant.intValue());
        case TypeIds.T_byte:
          return JIntLiteral.get(constant.byteValue());
        case TypeIds.T_short:
          return JIntLiteral.get(constant.shortValue());
        case TypeIds.T_char:
          return JCharLiteral.get(constant.charValue());
        case TypeIds.T_float:
          return JFloatLiteral.get(constant.floatValue());
        case TypeIds.T_double:
          return JDoubleLiteral.get(constant.doubleValue());
        case Constant.T_boolean:
          return JBooleanLiteral.get(constant.booleanValue());
        case Constant.T_long:
          return JLongLiteral.get(constant.longValue());
        case Constant.T_JavaLangString:
          return getStringLiteral(info, constant.stringValue());
        case Constant.T_null:
          return JNullLiteral.INSTANCE;
        default:
          throw new InternalCompilerException("Unknown Constant type: " + constant.typeID());
      }
    }

    /**
     * Get a new label of a particular name, or create a new one if it doesn't
     * exist already.
     */
    private JLabel getOrCreateLabel(SourceInfo info, char[] name) {
      if (name == null) {
        return null;
      }
      String sname = intern(name);
      JLabel jlabel = curMethod.labels.get(sname);
      if (jlabel == null) {
        jlabel = new JLabel(info, sname);
        curMethod.labels.put(sname, jlabel);
      }
      return jlabel;
    }

    private JStringLiteral getStringLiteral(SourceInfo info, char[] chars) {
      return new JStringLiteral(info, intern(chars), javaLangString);
    }

    private JStringLiteral getStringLiteral(SourceInfo info, String string) {
      return new JStringLiteral(info, intern(string), javaLangString);
    }

    private void implementGetClass(JDeclaredType type) {
      // TODO(rluble): Object.getClass() should be final our JRE, when that is done, GwtAstBuilder
      // creates overrides for convenience and should unmark Object.getClass as final for
      // consistency.
      JMethod method = type.getMethods().get(GET_CLASS_METHOD_INDEX);
      assert (GET_CLASS_METHOD_NAME.equals(method.getName()));
      SourceInfo info = method.getSourceInfo();
      if (type.isJsoType() || type.isJsNative()) {
        // Native types and JSOs get a synthetic get class that return JavaScriptObject.class.
        //
        // return Cast.getClass(this)
        JjsUtils.replaceMethodBody(method,
            new JMethodCall(info, null, CAST_GET_CLASS_METHOD, new JThisRef(info, type)));
      } else {
        JjsUtils.replaceMethodBody(method, new JClassLiteral(info, type));
      }
    }

    private JDeclarationStatement makeDeclaration(SourceInfo info, JLocal local,
        JExpression value) {
      return new JDeclarationStatement(info, local.makeRef(info), value);
    }

    private JFieldRef makeInstanceFieldRef(SourceInfo info, JField field) {
      return new JFieldRef(info, makeThisRef(info), field, curClass.classType);
    }

    private JExpression makeLocalRef(SourceInfo info, LocalVariableBinding b, MethodInfo method) {
      return method.locals.get(b).makeRef(info);
    }

    private JThisRef makeThisRef(SourceInfo info) {
      return new JThisRef(info, curClass.getClassOrInterface());
    }

    private JExpression resolveThisReference(SourceInfo info, ReferenceBinding targetType,
        boolean exactMatch, BlockScope scope) {
      targetType = (ReferenceBinding) targetType.erasure();

      Object[] path = scope.getEmulationPath(targetType, exactMatch, false);
      if (path == null) {
        throw new InternalCompilerException("No emulation path.");
      }
      if (path == BlockScope.EmulationPathToImplicitThis) {
        return makeThisRef(info);
      }
      JExpression ref;
      ReferenceBinding type;
      if (curMethod.scope.isInsideInitializer() && path[0] instanceof SyntheticArgumentBinding) {
        SyntheticArgumentBinding b = (SyntheticArgumentBinding) path[0];
        JField field = curClass.syntheticFields.get(b);
        assert field != null;
        ref = makeInstanceFieldRef(info, field);
        type = (ReferenceBinding) b.type.erasure();
      } else if (path[0] instanceof SyntheticArgumentBinding) {
        SyntheticArgumentBinding b = (SyntheticArgumentBinding) path[0];
        JParameter param = (JParameter) curMethod.locals.get(b);
        assert param != null;
        ref = param.makeRef(info);
        type = (ReferenceBinding) b.type.erasure();
      } else if (path[0] instanceof FieldBinding) {
        FieldBinding b = (FieldBinding) path[0];
        JField field = typeMap.get(b);
        assert field != null;
        ref = makeInstanceFieldRef(info, field);
        type = (ReferenceBinding) b.type.erasure();
      } else {
        throw new InternalCompilerException("Unknown emulation path.");
      }
      for (int i = 1; i < path.length; ++i) {
        SyntheticMethodBinding b = (SyntheticMethodBinding) path[i];
        assert type == b.declaringClass.erasure();
        FieldBinding fieldBinding = b.targetReadField;
        JField field = typeMap.get(fieldBinding);
        assert field != null;
        ref = new JFieldRef(info, ref, field, curClass.classType);
        type = (ReferenceBinding) fieldBinding.type.erasure();
      }
      return ref;
    }

    private JExpression maybeBoxOrUnbox(JExpression original, int implicitConversion) {
      if (implicitConversion != -1) {
        if ((implicitConversion & TypeIds.BOXING) != 0) {
          return box(original, implicitConversion);
        } else if ((implicitConversion & TypeIds.UNBOXING) != 0) {
          return unbox(original, implicitConversion);
        }
      }
      return original;
    }

    private JExpression maybeCast(JType expected, JExpression expression) {
      if (expected != expression.getType()) {
        // Must be a generic; insert a cast operation.
        JReferenceType toType = (JReferenceType) expected;
        return new JCastOperation(expression.getSourceInfo(), toType, expression);
      } else {
        return expression;
      }
    }

    private JNode pop() {
      return nodeStack.remove(nodeStack.size() - 1);
    }

    private List<JExpression> popCallArgs(SourceInfo info, Expression[] jdtArgs,
        MethodBinding binding) {
      List<JExpression> args = pop(jdtArgs);
      if (!binding.isVarargs()) {
        return args;
      }

      // Handle the odd var-arg case.
      if (jdtArgs == null) {
        // Get writable collection (args is currently Collections.emptyList()).
        args = Lists.newArrayListWithCapacity(1);
      }

      TypeBinding[] params = binding.parameters;
      int varArg = params.length - 1;

      // See if there's a single varArg which is already an array.
      if (args.size() == params.length) {
        if (jdtArgs[varArg].resolvedType.isCompatibleWith(params[varArg])) {
          // Already the correct array type.
          return args;
        }
      }

      // Need to synthesize an appropriately-typed array.
      List<JExpression> tail = args.subList(varArg, args.size());
      List<JExpression> initializers = Lists.newArrayList(tail);
      tail.clear();
      JArrayType lastParamType = (JArrayType) typeMap.get(params[varArg]);
      JNewArray newArray = JNewArray.createArrayWithInitializers(info, lastParamType, initializers);
      args.add(newArray);
      return args;
    }

    private List<? extends JNode> popList(int count) {
      List<JNode> tail = nodeStack.subList(nodeStack.size() - count, nodeStack.size());
      // Make a copy.
      List<JNode> result = Lists.newArrayList(tail);
      // Causes the tail to be removed.
      tail.clear();
      return result;
    }

    private void popMethodInfo() {
      curMethod = methodStack.pop();
    }

    private void processEnumType(JEnumType type) {
      // $clinit, $init, getClass, valueOf, values
      JMethod valueOfMethod = type.getMethods().get(VALUE_OF_METHOD_INDEX);
      JMethod valuesMethod = type.getMethods().get(VALUES_METHOD_INDEX);
      {
        assert VALUE_OF_METHOD_NAME.equals(valueOfMethod.getName());
        writeEnumValueOfMethod(type, valueOfMethod, valuesMethod);
      }
      {
        assert VALUES_METHOD_NAME.equals(valuesMethod.getName());
        writeEnumValuesMethod(type, valuesMethod);
      }
    }

    private void processNativeMethod(MethodDeclaration x) {
      JMethod method = curMethod.method;
      JsniMethod jsniMethod = jsniMethods.get(x);
      if (jsniMethod == null) {
        method.setBody(null);
        return;
      }
      SourceInfo info = method.getSourceInfo();
      JsFunction jsFunction = jsniMethod.function();
      JsniMethodBody body = new JsniMethodBody(info);
      method.setBody(body);
      jsFunction.setFromJava(true);
      body.setFunc(jsFunction);
      // Resolve locals, params, and JSNI.
      JsParameterResolver localResolver = new JsParameterResolver(jsFunction);
      localResolver.accept(jsFunction);
      JsniReferenceCollector jsniReferenceCollector = new JsniReferenceCollector(body);
      jsniReferenceCollector.accept(jsFunction);
    }

    private void processSuperCallLocalArgs(ReferenceBinding superClass, JMethodCall call) {
      if (superClass.syntheticOuterLocalVariables() != null) {
        for (SyntheticArgumentBinding arg : superClass.syntheticOuterLocalVariables()) {
          // TODO: use emulation path here.
          // Got to be one of my params
          JType varType = typeMap.get(arg.type);
          String varName = intern(arg.name);
          JParameter param = null;
          for (JParameter paramIt : curMethod.method.getParams()) {
            if (varType == paramIt.getType() && varName.equals(paramIt.getName())) {
              param = paramIt;
            }
          }
          if (param == null) {
            throw new InternalCompilerException(
                "Could not find matching local arg for explicit super ctor call.");
          }
          call.addArg(param.makeRef(call.getSourceInfo()));
        }
      }
    }

    // Only called on nested instances constructors (explicitConstructorCalls) that are of the
    // form: outer.super(...) or super(...)
    //
    // Will set outer (in the first case) or the implicit enclosing object reference to
    // be the first parameter of super(...)
    private void processSuperCallThisArgs(ReferenceBinding superClass, JMethodCall call,
        JExpression qualifier, Expression qualification) {
      // Explicit super calls can only happend inside constructors
      assert curMethod.scope.isInsideConstructor();
      if (superClass.syntheticEnclosingInstanceTypes() != null) {
        // there can only be ONE immediate enclosing instance.
        assert superClass.syntheticEnclosingInstanceTypes().length == 1;
        ReferenceBinding targetType = superClass.syntheticEnclosingInstanceTypes()[0];
        if (qualification != null) {
          // Outer object is the qualifier.
          call.addArg(qualifier);
        } else {
          // Get implicit outer object.
          call.addArg(
              resolveThisReference(call.getSourceInfo(), targetType, false, curMethod.scope));
        }
      }
    }

    private void processThisCallLocalArgs(ReferenceBinding binding, JMethodCall call) {
      if (binding.syntheticOuterLocalVariables() != null) {
        for (SyntheticArgumentBinding arg : binding.syntheticOuterLocalVariables()) {
          JParameter param = (JParameter) curMethod.locals.get(arg);
          assert param != null;
          call.addArg(param.makeRef(call.getSourceInfo()));
        }
      }
    }

    private void processThisCallThisArgs(ReferenceBinding binding, JMethodCall call) {
      if (binding.syntheticEnclosingInstanceTypes() != null) {
        Iterator<JParameter> paramIt = curMethod.method.getParams().iterator();
        if (curClass.classType.isEnumOrSubclass() != null) {
          // Skip past the enum args.
          paramIt.next();
          paramIt.next();
        }
        for (@SuppressWarnings("unused")
        ReferenceBinding argType : binding.syntheticEnclosingInstanceTypes()) {
          JParameter param = paramIt.next();
          call.addArg(param.makeRef(call.getSourceInfo()));
        }
      }
    }

    private void push(JNode node) {
      nodeStack.add(node);
    }

    private void pushBinaryOp(Expression x, JBinaryOperator op, Expression lhs, Expression rhs) {
      try {
        JType type = typeMap.get(x.resolvedType);
        SourceInfo info = makeSourceInfo(x);
        JExpression exprArg2 = pop(rhs);
        JExpression exprArg1 = pop(lhs);
        push(new JBinaryOperation(info, type, op, exprArg1, exprArg2));
      } catch (Throwable e) {
        throw translateException(x, e);
      }
    }

    private void pushInitializerMethodInfo(FieldDeclaration x, MethodScope scope) {
      JMethod initMethod;
      if (x.isStatic()) {
        initMethod = curClass.type.getClinitMethod();
      } else {
        initMethod = curClass.type.getInitMethod();
      }
      pushMethodInfo(new MethodInfo(initMethod, (JMethodBody) initMethod.getBody(), scope));
    }

    private void pushMethodInfo(MethodInfo newInfo) {
      methodStack.push(curMethod);
      curMethod = newInfo;
    }

    private void pushNewExpression(SourceInfo info, AllocationExpression x, Expression qualifier,
        List<JExpression> arguments, BlockScope scope) {
      TypeBinding typeBinding = x.resolvedType;
      if (typeBinding.constantPoolName() == null) {
        /*
         * Weird case: if JDT determines that this local class is totally
         * uninstantiable, it won't bother allocating a local name.
         */
        push(JNullLiteral.INSTANCE);
        return;
      }
      assert typeBinding.isClass() || typeBinding.isEnum();

      MethodBinding b = x.binding;
      assert b.isConstructor();
      JConstructor ctor = (JConstructor) typeMap.get(b);
      JMethodCall call = new JNewInstance(info, ctor);
      JExpression qualExpr = pop(qualifier);

      // Enums: hidden arguments for the name and id.
      if (x.enumConstant != null) {
        call.addArgs(getStringLiteral(info, x.enumConstant.name), JIntLiteral
            .get(x.enumConstant.binding.original().id));
      }

      // Synthetic args for inner classes
      ReferenceBinding targetBinding = (ReferenceBinding) b.declaringClass.erasure();
      boolean isNested = JdtUtil.isInnerClass(targetBinding);
      if (isNested) {
        // Synthetic this args for inner classes
        if (targetBinding.syntheticEnclosingInstanceTypes() != null) {
          ReferenceBinding checkedTargetType =
              targetBinding.isAnonymousType() ? (ReferenceBinding) targetBinding.superclass()
                  .erasure() : targetBinding;
          ReferenceBinding targetEnclosingType = checkedTargetType.enclosingType();
          for (ReferenceBinding argType : targetBinding.syntheticEnclosingInstanceTypes()) {
            argType = (ReferenceBinding) argType.erasure();
            if (qualifier != null && argType == targetEnclosingType) {
              call.addArg(qualExpr);
            } else {
              JExpression thisRef = resolveThisReference(info, argType, false, scope);
              call.addArg(thisRef);
            }
          }
        }
      }

      // Plain old regular user arguments
      call.addArgs(arguments);

      // Synthetic args for inner classes
      if (isNested) {
        // Synthetic locals for local classes
        if (targetBinding.syntheticOuterLocalVariables() != null) {
          for (SyntheticArgumentBinding arg : targetBinding.syntheticOuterLocalVariables()) {
            LocalVariableBinding targetVariable = arg.actualOuterLocalVariable;
            VariableBinding[] path = scope.getEmulationPath(targetVariable);
            assert path.length == 1;
            if (curMethod.scope.isInsideInitializer()
                && path[0] instanceof SyntheticArgumentBinding) {
              SyntheticArgumentBinding sb = (SyntheticArgumentBinding) path[0];
              JField field = curClass.syntheticFields.get(sb);
              assert field != null;
              call.addArg(makeInstanceFieldRef(info, field));
            } else if (path[0] instanceof LocalVariableBinding) {
              JExpression localRef = makeLocalRef(info, (LocalVariableBinding) path[0], curMethod);
              call.addArg(localRef);
            } else if (path[0] instanceof FieldBinding) {
              JField field = typeMap.get((FieldBinding) path[0]);
              assert field != null;
              call.addArg(makeInstanceFieldRef(info, field));
            } else {
              throw new InternalCompilerException("Unknown emulation path.");
            }
          }
        }
      }

      push(call);
    }

    /**
     * Don't process unreachable statements, because JDT doesn't always fully
     * resolve them, which can crash us.
     */
    private Statement[] reduceToReachable(Statement[] statements) {
      if (statements == null) {
        return null;
      }
      int reachableCount = 0;
      for (Statement statement : statements) {
        if ((statement.bits & ASTNode.IsReachable) != 0) {
          ++reachableCount;
        }
      }
      if (reachableCount == statements.length) {
        return statements;
      }
      Statement[] newStatments = new Statement[reachableCount];
      int index = 0;
      for (Statement statement : statements) {
        if ((statement.bits & ASTNode.IsReachable) != 0) {
          newStatments[index++] = statement;
        }
      }
      return newStatments;
    }

    private JExpression resolveNameReference(NameReference x, BlockScope scope) {
      SourceInfo info = makeSourceInfo(x);
      Binding binding = x.binding;
      if (isOptimizableCompileTimeConstant(binding)) {
        return getConstant(info, x.constant);
      }
      JExpression result = null;
      if (binding instanceof LocalVariableBinding) {
        LocalVariableBinding b = (LocalVariableBinding) binding;
        MethodScope nearestMethodScope =
            scope instanceof MethodScope ? (MethodScope) scope : scope.enclosingMethodScope();
        if ((x.bits & ASTNode.DepthMASK) != 0 || nearestMethodScope.isLambdaScope()) {
          VariableBinding[] path = scope.getEmulationPath(b);
          if (path == null) {
            /*
             * Don't like this, but in rare cases (e.g. the variable is only
             * ever used as an unnecessary qualifier) JDT provides no emulation
             * to the desired variable.
             */
            // throw new InternalCompilerException("No emulation path.");
            return null;
          }
          assert path.length == 1;
          if (curMethod.scope.isInsideInitializer()
              && path[0] instanceof SyntheticArgumentBinding) {
            SyntheticArgumentBinding sb = (SyntheticArgumentBinding) path[0];
            JField field = curClass.syntheticFields.get(sb);
            assert field != null;
            result = makeInstanceFieldRef(info, field);
          } else if (path[0] instanceof LocalVariableBinding) {
            result = makeLocalRef(info, (LocalVariableBinding) path[0], curMethod);
          } else if (path[0] instanceof FieldBinding) {
            FieldBinding fb = (FieldBinding) path[0];
            assert curClass.typeDecl.binding.isCompatibleWith(x.actualReceiverType.erasure());
            JField field = typeMap.get(fb);
            assert field != null;
            result = makeInstanceFieldRef(info, field);
          } else {
            throw new InternalCompilerException("Unknown emulation path.");
          }
        } else {
          result = makeLocalRef(info, b, curMethod);
        }
      } else if (binding instanceof FieldBinding) {
        FieldBinding b = ((FieldBinding) x.binding).original();
        JField field = typeMap.get(b);
        assert field != null;
        JExpression thisRef = null;
        if (!b.isStatic()) {
          thisRef =
              resolveThisReference(info, (ReferenceBinding) x.actualReceiverType, false, scope);
        }
        result = new JFieldRef(info, thisRef, field, curClass.type);
      } else {
        return null;
      }
      assert result != null;
      return result;
    }

    private JExpression simplify(JExpression result, Expression x) {
      return maybeBoxOrUnbox(result, x.implicitConversion);
    }

    private JExpression synthesizeCallToOrdinal(BlockScope scope, SourceInfo info,
        JExpression expression) {
      ReferenceBinding javaLangEnum = scope.getJavaLangEnum();
      MethodBinding ordinal = javaLangEnum.getMethods(ORDINAL_)[0];
      expression = new JMethodCall(info, expression, typeMap.get(ordinal));
      return expression;
    }

    private JExpression unbox(JExpression original, int implicitConversion) {
      int compileTypeId = implicitConversion & TypeIds.COMPILE_TYPE_MASK;
      ClassScope scope = curClass.scope;
      TypeBinding targetBinding = TypeBinding.wellKnownType(scope, compileTypeId);
      if (!(targetBinding instanceof BaseTypeBinding)) {
        // Direct cast from non-boxed-type reference type to a primitive type,
        // wrap with a cast operation of the (boxed) expected type.
        int runtimeTypeId = (implicitConversion & TypeIds.IMPLICIT_CONVERSION_MASK) >> 4;
        TypeBinding runtimeTypeBinding = TypeBinding.wellKnownType(scope, runtimeTypeId);
        ReferenceBinding boxType = (ReferenceBinding) scope.boxing(runtimeTypeBinding);
        original =
            new JCastOperation(original.getSourceInfo(), typeMap.get(boxType), original);
        targetBinding = runtimeTypeBinding;
        assert (targetBinding instanceof BaseTypeBinding);
      }

      BaseTypeBinding primitiveType = (BaseTypeBinding) targetBinding;

      ReferenceBinding boxType = (ReferenceBinding) scope.boxing(primitiveType);
      char[] selector = CharOperation.concat(primitiveType.simpleName, VALUE_SUFFIX_);
      MethodBinding valueMethod =
          boxType.getExactMethod(selector, NO_TYPES, scope.compilationUnitScope());
      assert valueMethod != null;
      JMethod unboxMethod = typeMap.get(valueMethod);
      JMethodCall call = new JMethodCall(original.getSourceInfo(), original, unboxMethod);
      return call;
    }

    private void writeEnumValueOfMethod(JEnumType type, JMethod method, JMethod valuesMethod) {
      JField mapField;
      TypeBinding mapType;
      ReferenceBinding enumType = curCud.scope.getJavaLangEnum();
      {
        /*
         * Make an inner class to hold a lazy-init name-value map. We use a
         * class to take advantage of its clinit.
         *
         * class Map { $MAP = Enum.createValueOfMap(values()); }
         */
        SourceInfo info = type.getSourceInfo();
        JClassType mapClass = new JClassType(info, intern(type.getName() + "$Map"), false, true);
        mapClass.setSuperClass(javaLangObject);
        mapClass.setEnclosingType(type);
        newTypes.add(mapClass);

        MethodBinding[] createValueOfMapBindings = enumType.getMethods(CREATE_VALUE_OF_MAP_);
        if (createValueOfMapBindings.length == 0) {
          throw new RuntimeException(
              "Unexpectedly unable to access Enum.createValueOfMap via reflection. "
              + "Likely a dependency on the com.google.gwt.user.User module is missing.");
        }
        MethodBinding createValueOfMapBinding = createValueOfMapBindings[0];
        mapType = createValueOfMapBinding.returnType;

        mapField = new JField(info, "$MAP", mapClass, typeMap.get(mapType), true, Disposition.FINAL,
            AccessModifier.PRIVATE);
        mapClass.addField(mapField);

        JMethodCall call = new JMethodCall(info, null, typeMap.get(createValueOfMapBinding));
        call.addArg(new JMethodCall(info, null, valuesMethod));
        JFieldRef mapRef = new JFieldRef(info, null, mapField, mapClass);
        JDeclarationStatement declStmt = new JDeclarationStatement(info, mapRef, call);
        JMethod clinit =
            createSyntheticMethod(info, "$clinit", mapClass, JPrimitiveType.VOID, false, true,
                true, AccessModifier.PRIVATE);
        JBlock clinitBlock = ((JMethodBody) clinit.getBody()).getBlock();
        clinitBlock.addStmt(declStmt);
      }

      /*
       * return Enum.valueOf(Enum$Map.Map.$MAP, name);
       */
      {
        SourceInfo info = method.getSourceInfo();

        MethodBinding valueOfBinding =
            enumType.getExactMethod(VALUE_OF_, new TypeBinding[]{
                mapType, curCud.scope.getJavaLangString()}, curCud.scope);
        assert valueOfBinding != null;

        JFieldRef mapRef = new JFieldRef(info, null, mapField, type);
        JParameterRef nameRef = method.getParams().get(0).makeRef(info);
        JMethodCall call = new JMethodCall(info, null, typeMap.get(valueOfBinding));
        call.addArgs(mapRef, nameRef);
        JjsUtils.replaceMethodBody(method, call);
      }
    }

    private JCastOperation buildCastOperation(SourceInfo info, JType[] castTypes,
        JExpression expression) {
      return buildCastOperation(info, castTypes, expression, 0);
    }

    private JCastOperation buildCastOperation(SourceInfo info, JType[] castTypes,
        JExpression expression, int idx) {
      if (idx == castTypes.length - 1) {
        return new JCastOperation(info, castTypes[idx], expression);
      } else {
        return new JCastOperation(info, castTypes[idx],
            buildCastOperation(info, castTypes, expression, idx + 1));
      }
    }

    private JReferenceType[] processIntersectionCastType(IntersectionTypeBinding18 type) {
      JReferenceType[] castTypes = new JReferenceType[type.intersectingTypes.length];
      int i = 0;
      for (ReferenceBinding intersectingTypeBinding : type.intersectingTypes) {
        JType intersectingType = typeMap.get(intersectingTypeBinding);
        assert (intersectingType instanceof JReferenceType);
        castTypes[i++] = ((JReferenceType) intersectingType);
      }
      return castTypes;
    }

    private JType[] processCastType(TypeBinding type) {
      if (type instanceof IntersectionTypeBinding18) {
        return processIntersectionCastType((IntersectionTypeBinding18) type);
      } else {
        return new JType[] {typeMap.get(type)};
      }
    }

    private JInterfaceType[] processIntersectionTypeForLambda(IntersectionTypeBinding18 type,
        BlockScope scope, String samSignature) {
      List<JInterfaceType> interfaces = Lists.newArrayList();
      for (ReferenceBinding intersectingTypeBinding : type.intersectingTypes) {
        if (shouldImplements(intersectingTypeBinding, scope, samSignature)) {
          JType intersectingType = typeMap.get(intersectingTypeBinding);
          assert (intersectingType instanceof JInterfaceType);
          interfaces.add(((JInterfaceType) intersectingType));
        }
      }
      return Iterables.toArray(interfaces, JInterfaceType.class);
    }

    private boolean isFunctionalInterfaceWithMethod(ReferenceBinding referenceBinding, Scope scope,
        String samSignature) {
      if (!referenceBinding.isInterface()) {
        return false;
      }
      MethodBinding abstractMethod = referenceBinding.getSingleAbstractMethod(scope, false);
      return abstractMethod != null && abstractMethod.isValidBinding()
          && JdtUtil.signature(abstractMethod).equals(samSignature);
    }

    private boolean isInterfaceHasNoAbstractMethod(ReferenceBinding referenceBinding, Scope scope) {
      List<MethodBinding> abstractMethods = getInterfaceAbstractMethods(referenceBinding, scope);
      return abstractMethods != null && abstractMethods.size() == 0;
    }

    private boolean shouldImplements(ReferenceBinding referenceBinding, Scope scope,
        String samSignature) {
      return isFunctionalInterfaceWithMethod(referenceBinding, scope, samSignature)
          || isInterfaceHasNoAbstractMethod(referenceBinding, scope);
    }

    /**
     * Collect all abstract methods in an interface and its super interfaces.
     *
     * In the case of multiple inheritance like this,
     *
     * interface I {m();}
     * interface J {default m();}
     * interface K extends I, J{}
     *
     * the abstract methods of K include m();
     */
    private List<MethodBinding> getInterfaceAbstractMethods(ReferenceBinding referenceBinding,
        Scope scope) {
      if (!referenceBinding.isInterface() || !referenceBinding.isValidBinding()) {
        return null;
      }
      List<MethodBinding> abstractMethods = Lists.newLinkedList();

      // add all abstract methods from super interfaces.
      for (ReferenceBinding superInterface : referenceBinding.superInterfaces()) {
        List<MethodBinding> abstractMethodsFromSupers =
            getInterfaceAbstractMethods(superInterface, scope);
        if (abstractMethodsFromSupers != null && abstractMethodsFromSupers.size() > 0) {
          abstractMethods.addAll(abstractMethodsFromSupers);
        }
      }
      for (MethodBinding method : referenceBinding.methods()) {
        if (method == null || method.isStatic() || method.redeclaresPublicObjectMethod(scope)) {
          continue;
        }
        // remove the overridden methods in the super interfaces.
        for (MethodBinding abstractMethodFromSupers : abstractMethods) {
          if (MethodVerifier.doesMethodOverride(method, abstractMethodFromSupers,
              scope.environment())) {
            abstractMethods.remove(abstractMethodFromSupers);
          }
        }
        // add method to abstract methods if it is not a default method.
        if (!method.isDefaultMethod()) {
          abstractMethods.add(method);
        }
      }
      return abstractMethods;
    }
  }

  static class ClassInfo {
    public final JClassType classType;
    public final ClassScope scope;
    public final Map<SyntheticArgumentBinding, JField> syntheticFields = Maps.newIdentityHashMap();
    public final JDeclaredType type;
    public final TypeDeclaration typeDecl;

    public ClassInfo(JDeclaredType type, TypeDeclaration x) {
      this.type = type;
      this.classType = (type instanceof JClassType) ? (JClassType) type : null;
      this.typeDecl = x;
      this.scope = x.scope;
    }

    public JDeclaredType getClassOrInterface() {
      return classType == null ? type : classType;
    }
  }

  static class CudInfo {
    public final CompilationUnitScope scope;
    public final int[] separatorPositions;
    public final CompilationUnitDeclaration cud;

    public CudInfo(CompilationUnitDeclaration cud) {
      separatorPositions = cud.compilationResult().getLineSeparatorPositions();
      scope = cud.scope;
      this.cud = cud;
    }
  }

  static class MethodInfo {
    public final JMethodBody body;
    public final Map<String, JLabel> labels = Maps.newHashMap();
    public final Map<LocalVariableBinding, JVariable> locals = Maps.newIdentityHashMap();
    public final JMethod method;
    public final MethodScope scope;

    public MethodInfo(JMethod method, JMethodBody methodBody, MethodScope methodScope) {
      this.method = method;
      this.body = methodBody;
      this.scope = methodScope;
    }
  }

  /**
   * Manually tracked version count.
   */
  private static final long AST_VERSION = 3;
  private static final int MAX_INLINEABLE_ENUM_SIZE = 10;

  private static final String CREATE_VALUE_OF_MAP_METHOD_NAME = "createValueOfMap";
  private static final String LENGTH_FIELD_NAME = "length";
  private static final String VALUE_SUFFIX = "Value";

  private static final char[] CREATE_VALUE_OF_MAP_ = CREATE_VALUE_OF_MAP_METHOD_NAME.toCharArray();
  private static final char[] VALUE_SUFFIX_ = VALUE_SUFFIX.toCharArray();
  private static final char[] VALUE_OF_ = VALUE_OF_METHOD_NAME.toCharArray();
  private static final char[] VALUES_ = VALUES_METHOD_NAME.toCharArray();
  private static final char[] ORDINAL_ = ORDINAL_METHOD_NAME.toCharArray();
  private static final char[] NEXT_ = NEXT_METHOD_NAME.toCharArray();
  private static final char[] ITERATOR_ = ITERATOR_METHOD_NAME.toCharArray();
  private static final char[] HAS_NEXT_ = HAS_NEXT_METHOD_NAME.toCharArray();

  /**
   * Reflective access to {@link ForeachStatement#collectionElementType}.
   */
  private static final Field collectionElementTypeField;
  /**
   * Reflective access to {@link ReferenceExpression#haveReceiver}.
   */
  private static final Field haveReceiverField;

  private static final TypeBinding[] NO_TYPES = new TypeBinding[0];
  private static final Interner<String> stringInterner = StringInterner.get();

  static {
    InternalCompilerException.preload();
    try {
      collectionElementTypeField = ForeachStatement.class.getDeclaredField("collectionElementType");
      collectionElementTypeField.setAccessible(true);
    } catch (Exception e) {
      throw new RuntimeException(
          "Unexpectedly unable to access ForeachStatement.collectionElementType via reflection", e);
    }

    try {
      haveReceiverField = ReferenceExpression.class.getDeclaredField("haveReceiver");
      haveReceiverField.setAccessible(true);
    } catch (Exception e) {
      throw new RuntimeException(
          "Unexpectedly unable to access ReferenceExpression.haveReceiver via reflection", e);
    }
  }

  /**
   * Returns a serialization version number. Used to determine if the AST
   * contained within cached compilation units is compatible with the current
   * version of GWT.
   */
  public static long getSerializationVersion() {
    // TODO(zundel): something much awesomer.
    return AST_VERSION;
  }

  static Disposition getFieldDisposition(FieldBinding binding) {
    Disposition disposition;
    if (isCompileTimeConstant(binding)) {
      disposition = Disposition.COMPILE_TIME_CONSTANT;
    } else if (binding.isFinal()) {
      disposition = Disposition.FINAL;
    } else if (binding.isVolatile()) {
      disposition = Disposition.VOLATILE;
    } else {
      disposition = Disposition.NONE;
    }
    return disposition;
  }

  static String intern(char[] cs) {
    return intern(String.valueOf(cs));
  }

  static String intern(String s) {
    return stringInterner.intern(s);
  }

  /**
   * Creates the statement that will constitute the method body implementing a lambda expression.
   * <p>
   * Lambda expressions might be represented in the JDT AST as Java expressions or Java statements.
   */
  private static JStatement getOrCreateLambdaStatement(JNode node) {
    if (node instanceof JStatement) {
      return (JStatement) node;
    }
    assert node instanceof JExpression;
    JExpression expression = (JExpression) node;
    return JjsUtils.makeMethodEndStatement(expression.getType(), expression);
  }

  private boolean isOptimizableCompileTimeConstant(Binding binding) {
    if (binding instanceof LocalVariableBinding &&
        ((LocalVariableBinding) binding).constant() != Constant.NotAConstant) {
      // Propagate constants in local variables regardless whether we are optimizing compile time
      // constants or not. This is necessary as the JDT will not compute an emulation path for a
      // local constant referred in a nested class closure.
      return true;
    }
    if (!(binding instanceof FieldBinding)) {
      return false;
    }
    FieldBinding fieldBinding = (FieldBinding) binding;
    return (compilerContext.getOptions().shouldJDTInlineCompileTimeConstants()
        || isBinaryBinding(fieldBinding.declaringClass)) &&
        isCompileTimeConstant(fieldBinding);
  }

  private static boolean isCompileTimeConstant(FieldBinding binding) {
    assert !binding.isFinal() || !binding.isVolatile();
    boolean isCompileTimeConstant = binding.isStatic() && binding.isFinal() &&
        binding.constant() != Constant.NotAConstant;
    assert !isCompileTimeConstant || binding.type.isBaseType() ||
        (binding.type.id == TypeIds.T_JavaLangString);
    return isCompileTimeConstant;
  }

  private boolean isBinaryBinding(ReferenceBinding binding) {
    if (binding instanceof BinaryTypeBinding) {
      // Is it really a BinaryBinding? If a source resource exists, the BinaryTypeBinding is
      // considered a source type binding.
      return !compilerContext.getMinimalRebuildCache().isSourceCompilationUnit(
          JdtUtil.getDefiningCompilationUnitType(binding));
    }
    return false;
  }

  CudInfo curCud = null;

  JClassType javaLangClass = null;
  JClassType javaLangObject = null;
  JClassType javaLangString = null;
  JClassType javaLangThrowable = null;

  Map<MethodDeclaration, JsniMethod> jsniMethods;

  Map<String, Binding> jsniRefs;

  final ReferenceMapper typeMap = new ReferenceMapper();

  private final AstVisitor astVisitor = new AstVisitor();

  private List<JDeclaredType> newTypes;

  private String sourceMapPath;

  private CompilerContext compilerContext;

  private boolean isLegacyJsInteropEnabled;

  private boolean generateJsInteropExports;

  /**
   * Externalized class and method form for Exceptions.safeClose() to provide support
   * for try-with-resources.
   *
   * The externalized form will be resolved during AST stitching.
   */
  private static JMethod SAFE_CLOSE_METHOD =
      JMethod.getExternalizedMethod("com.google.gwt.lang.Exceptions",
      "safeClose(Ljava/lang/AutoCloseable;Ljava/lang/Throwable;)Ljava/lang/Throwable;", true);

  private static JMethod CAST_GET_CLASS_METHOD =
      JMethod.getExternalizedMethod("com.google.gwt.lang.Cast",
      "getClass(Ljava/lang/Object;)Ljava/lang/Class;", true);

  private List<JDeclaredType> processImpl() {
    CompilationUnitDeclaration cud = curCud.cud;
    if (cud.types == null) {
      return Collections.emptyList();
    }

    for (TypeDeclaration typeDecl : cud.types) {
      createTypes(typeDecl);
    }

    // Now that types exist, cache Object, String, etc.
    javaLangObject = (JClassType) typeMap.get(cud.scope.getJavaLangObject());
    javaLangString = (JClassType) typeMap.get(cud.scope.getJavaLangString());
    javaLangClass = (JClassType) typeMap.get(cud.scope.getJavaLangClass());
    javaLangThrowable = (JClassType) typeMap.get(cud.scope.getJavaLangThrowable());

    for (TypeDeclaration typeDecl : cud.types) {
      // Resolve super type / interface relationships.
      resolveTypeRefs(typeDecl);
    }
    for (TypeDeclaration typeDecl : cud.types) {
      // Create fields and empty methods.
      createMembers(typeDecl);
    }
    for (TypeDeclaration typeDecl : cud.types) {
      // Build the code.
      typeDecl.traverse(astVisitor, cud.scope);
    }
    return newTypes;
  }

  private GwtAstBuilder(CompilationUnitDeclaration cud, String sourceMapPath,
      Map<MethodDeclaration, JsniMethod> jsniMethods, Map<String, Binding> jsniRefs,
      CompilerContext compilerContext) {
    this.sourceMapPath = sourceMapPath;
    this.jsniRefs = jsniRefs;
    this.jsniMethods = jsniMethods;
    this.compilerContext = compilerContext;
    this.isLegacyJsInteropEnabled = compilerContext.getOptions().getJsInteropMode() == Mode.JS;
    this.generateJsInteropExports = compilerContext.getOptions().shouldGenerateJsInteropExports();
    this.newTypes = Lists.newArrayList();
    this.curCud = new CudInfo(cud);
  }

  /**
   * Builds all the GWT AST nodes that correspond to one Java source file.
   *
   * @param cud The compiled form of the Java source from the JDT.
   * @param sourceMapPath the path that should be included in a sourcemap.
   * @param jsniMethods Native methods to add to the AST.
   * @param jsniRefs Map from JSNI references to their JDT definitions.
   * @param compilerContext the compiler context.
   * @return All the types seen in this source file.
   */
  public static List<JDeclaredType> process(CompilationUnitDeclaration cud, String sourceMapPath,
      Map<MethodDeclaration, JsniMethod> jsniMethods, Map<String, Binding> jsniRefs,
      CompilerContext compilerContext) {
    return new GwtAstBuilder(cud, sourceMapPath, jsniMethods, jsniRefs, compilerContext)
        .processImpl();
  }

  SourceInfo makeSourceInfo(AbstractMethodDeclaration x) {
    int startLine =
        Util.getLineNumber(x.sourceStart, curCud.separatorPositions, 0,
            curCud.separatorPositions.length - 1);
    return SourceOrigin.create(x.sourceStart, x.bodyEnd, startLine, sourceMapPath);
  }

  SourceInfo makeSourceInfo(ASTNode x) {
    int startLine =
        Util.getLineNumber(x.sourceStart, curCud.separatorPositions, 0,
            curCud.separatorPositions.length - 1);
    return SourceOrigin.create(x.sourceStart, x.sourceEnd, startLine, sourceMapPath);
  }

  InternalCompilerException translateException(ASTNode node, Throwable e) {
    if (e instanceof VirtualMachineError) {
      // Always rethrow VM errors (an attempt to wrap may fail).
      throw (VirtualMachineError) e;
    }
    InternalCompilerException ice;
    if (e instanceof InternalCompilerException) {
      ice = (InternalCompilerException) e;
    } else {
      ice = new InternalCompilerException("Error constructing Java AST", e);
    }
    if (node != null) {
      ice.addNode(node.getClass().getName(), node.toString(), makeSourceInfo(node));
    }
    return ice;
  }

  private void createField(FieldDeclaration x) {
    if (x instanceof Initializer) {
      return;
    }
    SourceInfo info = makeSourceInfo(x);
    FieldBinding binding = x.binding;
    JType type = typeMap.get(binding.type);
    JDeclaredType enclosingType = (JDeclaredType) typeMap.get(binding.declaringClass);

    JField field;
    if (x.initialization != null && x.initialization instanceof AllocationExpression
        && ((AllocationExpression) x.initialization).enumConstant != null) {
      field = new JEnumField(info, intern(binding.name), binding.original().id,
          (JEnumType) enclosingType, (JClassType) type, AccessModifier.fromFieldBinding(binding));
    } else {
      field =
          new JField(info, intern(binding.name), enclosingType, type, binding.isStatic(),
              getFieldDisposition(binding), AccessModifier.fromFieldBinding(binding));
    }
    enclosingType.addField(field);
    if (isLegacyJsInteropEnabled) {
      JsInteropUtil.maybeSetJsInteropProperties(field, x.annotations);
    } else {
      JsInteropUtil.maybeSetJsInteropPropertiesNew(field, generateJsInteropExports, x.annotations);
    }
    processSuppressedWarnings(field, x.annotations);
    typeMap.setField(binding, field);
  }

  private void createMembers(TypeDeclaration x) {
    SourceTypeBinding binding = x.binding;
    JDeclaredType type = (JDeclaredType) typeMap.get(binding);
    SourceInfo info = type.getSourceInfo();
    try {
      /**
       * We emulate static initializers and instance initializers as methods. As
       * in other cases, this gives us: simpler AST, easier to optimize, more
       * like output JavaScript. Clinit is always in slot 0, init (if it exists)
       * is always in slot 1.
       */
      assert type.getMethods().size() == CLINIT_METHOD_INDEX;
      createSyntheticMethod(info, CLINIT_METHOD_NAME, type, JPrimitiveType.VOID, false, true, true,
          AccessModifier.PRIVATE);

      if (type instanceof JClassType) {
        assert type.getMethods().size() == INIT_METHOD_INDEX;
        createSyntheticMethod(info, INIT_NAME_METHOD_NAME, type, JPrimitiveType.VOID, false, false,
            true, AccessModifier.PRIVATE);

        // Add a getClass() implementation for all non-Object, non-String classes.
        if (isSyntheticGetClassNeeded(x, type)) {
          assert type.getMethods().size() == GET_CLASS_METHOD_INDEX;
          createSyntheticMethod(info, GET_CLASS_METHOD_NAME, type, javaLangClass, type.isAbstract(),
              false, false, AccessModifier.PUBLIC);
        }
      }

      if (type instanceof JEnumType) {
        {
          assert type.getMethods().size() == VALUE_OF_METHOD_INDEX;
          MethodBinding valueOfBinding =
              binding.getExactMethod(VALUE_OF_,
                  new TypeBinding[]{x.scope.getJavaLangString()}, curCud.scope);
          assert valueOfBinding != null;
          createMethodFromBinding(info, valueOfBinding, new String[] {"name"});
        }
        {
          assert type.getMethods().size() == VALUES_METHOD_INDEX;
          MethodBinding valuesBinding = binding.getExactMethod(VALUES_, NO_TYPES, curCud.scope);
          assert valuesBinding != null;
          createMethodFromBinding(info, valuesBinding, null);
        }
      }

      if (x.fields != null) {
        for (FieldDeclaration field : x.fields) {
          createField(field);
        }
      }

      if (x.methods != null) {
        for (AbstractMethodDeclaration method : x.methods) {
          createMethod(method);
        }
      }

      if (x.memberTypes != null) {
        for (TypeDeclaration memberType : x.memberTypes) {
          createMembers(memberType);
        }
      }
    } catch (Throwable e) {
      throw getInternalCompilerException(x, e);
    }
  }

  private boolean isSyntheticGetClassNeeded(TypeDeclaration typeDeclaration, JDeclaredType type) {
    // TODO(rluble): We should check whether getClass is implemented by type and only
    // instead of blacklisting.
    return type.getSuperClass() != null && !JdtUtil.isJsoSubclass(typeDeclaration.binding);
  }

  private void createMethod(AbstractMethodDeclaration x) {
    if (x instanceof Clinit) {
      return;
    }
    SourceInfo info = makeSourceInfo(x);
    MethodBinding b = x.binding;
    ReferenceBinding declaringClass = (ReferenceBinding) b.declaringClass.erasure();
    Set<String> alreadyNamedVariables = Sets.newHashSet();
    JDeclaredType enclosingType = (JDeclaredType) typeMap.get(declaringClass);
    assert !enclosingType.isExternal();
    JMethod method;
    boolean isNested = JdtUtil.isInnerClass(declaringClass);
    if (x.isConstructor()) {
      method =
          new JConstructor(info, (JClassType) enclosingType, AccessModifier.fromMethodBinding(b));
      if (x.isDefaultConstructor()) {
        ((JConstructor) method).setDefaultConstructor();
      }
      if (x.binding.declaringClass.isEnum()) {
        // Enums have hidden arguments for name and value
        method.createFinalParameter(info, "enum$name", typeMap.get(x.scope.getJavaLangString()));
        method.createFinalParameter(info, "enum$ordinal", JPrimitiveType.INT);
      }
      // add synthetic args for outer this
      if (isNested) {
        NestedTypeBinding nestedBinding = (NestedTypeBinding) declaringClass;
        if (nestedBinding.enclosingInstances != null) {
          for (int i = 0; i < nestedBinding.enclosingInstances.length; ++i) {
            SyntheticArgumentBinding arg = nestedBinding.enclosingInstances[i];
            String argName = String.valueOf(arg.name);
            if (alreadyNamedVariables.contains(argName)) {
              argName += "_" + i;
            }
            createParameter(info, arg, argName, method, false);
            alreadyNamedVariables.add(argName);
          }
        }
      }
    } else {
      method =
          new JMethod(info, intern(b.selector), enclosingType, typeMap.get(b.returnType), b
              .isAbstract(), b.isStatic(), b.isFinal(), AccessModifier.fromMethodBinding(b));
    }

    // User args.
    createParameters(method, x);

    if (x.isConstructor()) {
      if (isNested) {
        // add synthetic args for locals
        NestedTypeBinding nestedBinding = (NestedTypeBinding) declaringClass;
        // add synthetic args for outer this and locals
        if (nestedBinding.outerLocalVariables != null) {
          for (int i = 0; i < nestedBinding.outerLocalVariables.length; ++i) {
            SyntheticArgumentBinding arg = nestedBinding.outerLocalVariables[i];
            String argName = String.valueOf(arg.name);
            if (alreadyNamedVariables.contains(argName)) {
              argName += "_" + i;
            }
            createParameter(info, arg, argName, method, false);
            alreadyNamedVariables.add(argName);
          }
        }
      }
    }

    mapExceptions(method, b);

    if (b.isSynthetic()) {
      method.setSynthetic();
    }

    if (b.isDefaultMethod()) {
      method.setDefaultMethod();
    }

    enclosingType.addMethod(method);
    processAnnotations(x, method);
    typeMap.setMethod(b, method);
  }

  private void processAnnotations(AbstractMethodDeclaration x,
      JMethod method) {
    maybeAddMethodSpecialization(x, method);
    maybeSetInliningMode(x, method);
    maybeSetHasNoSideEffects(x, method);
    if (isLegacyJsInteropEnabled) {
      JsInteropUtil.maybeSetJsInteropProperties(method, x.annotations);
    } else {
      JsInteropUtil.maybeSetJsInteropPropertiesNew(method, generateJsInteropExports, x.annotations);
    }
    processSuppressedWarnings(method, x.annotations);
  }

  private void processSuppressedWarnings(CanHaveSuppressedWarnings x, Annotation... annotations) {
    x.setSuppressedWarnings(JdtUtil.getSuppressedWarnings(annotations));
  }

  private void maybeSetInliningMode(AbstractMethodDeclaration x,
      JMethod method) {
    if (JdtUtil.getAnnotation(x.binding, DoNotInline.class.getCanonicalName()) != null) {
      method.setInliningMode(InliningMode.DO_NOT_INLINE);
    } else if (JdtUtil.getAnnotation(x.binding, ForceInline.class.getCanonicalName()) != null) {
      method.setInliningMode(InliningMode.FORCE_INLINE);
    }
  }

  private void maybeSetHasNoSideEffects(AbstractMethodDeclaration x,
      JMethod method) {
    if (JdtUtil.getAnnotation(x.binding,
        "javaemul.internal.annotations.HasNoSideEffects") != null) {
      method.setHasSideEffects(false);
    }
  }

  private void maybeAddMethodSpecialization(AbstractMethodDeclaration x, JMethod method) {
    AnnotationBinding specializeAnnotation =
        JdtUtil.getAnnotation(x.binding, "javaemul.internal.annotations.SpecializeMethod");
    if (specializeAnnotation == null) {
      return;
    }

    TypeBinding[] params =
        JdtUtil.getAnnotationParameterTypeBindingArray(specializeAnnotation, "params");
    assert params != null : "params is a mandatory field";
    List<JType> paramTypes = new ArrayList<JType>();
    for (TypeBinding pType : params) {
      paramTypes.add(typeMap.get(pType));
    }

    TypeBinding returns =
        JdtUtil.getAnnotationParameterTypeBinding(specializeAnnotation, "returns");
    JType returnsType = null;
    if (returns != null) {
      returnsType = typeMap.get(returns);
    }

    String targetMethod = JdtUtil.getAnnotationParameterString(specializeAnnotation, "target");
    assert targetMethod != null : "target is a mandatory parameter";

    method.setSpecialization(paramTypes, returnsType, targetMethod);
  }

  private void createParameter(SourceInfo info, LocalVariableBinding binding, boolean isVarargs,
      JMethod method, Annotation... annotations) {
    createParameter(info, binding, intern(binding.name), method, isVarargs, annotations);
  }

  private void createParameter(SourceInfo info, LocalVariableBinding binding, String name,
      JMethod method, boolean isVarargs, Annotation... annotations) {
    JParameter param =
        method.createParameter(info, name, typeMap.get(binding.type), binding.isFinal(), isVarargs);
    processSuppressedWarnings(param, annotations);
  }

  private void createParameters(JMethod method, AbstractMethodDeclaration x) {
    if (x.arguments != null) {
      for (Argument argument : x.arguments) {
        SourceInfo info = makeSourceInfo(argument);
        LocalVariableBinding binding = argument.binding;
        boolean isVarargs = x.binding.isVarargs()
            && argument == x.arguments[x.arguments.length - 1];
        createParameter(info, binding, isVarargs, method, argument.annotations);
      }
    }
    method.freezeParamTypes();
  }

  private JMethod createSyntheticMethod(SourceInfo info, String name, JDeclaredType enclosingType,
      JType returnType, boolean isAbstract, boolean isStatic, boolean isFinal,
      AccessModifier access, JStatement ... statements) {
    JMethod method =
        new JMethod(info, name, enclosingType, returnType, isAbstract, isStatic, isFinal, access);
    method.freezeParamTypes();
    method.setSynthetic();
    JMethodBody body = new JMethodBody(info);
    for (JStatement statement : statements) {
      body.getBlock().addStmt(statement);
    }
    method.setBody(body);
    enclosingType.addMethod(method);
    return method;
  }

  private JMethod createMethodFromBinding(SourceInfo info, MethodBinding binding,
      String[] paramNames) {
    JMethod method = typeMap.createMethod(info, binding, paramNames);
    assert !method.isExternal();
    method.setBody(new JMethodBody(info));
    if (isLegacyJsInteropEnabled) {
      JsInteropUtil.maybeSetJsInteropProperties(method);
    } else {
      JsInteropUtil.maybeSetJsInteropPropertiesNew(method, generateJsInteropExports);
    }
    typeMap.setMethod(binding, method);
    return method;
  }

  private void createTypes(TypeDeclaration x) {
    SourceInfo info = makeSourceInfo(x);
    try {
      SourceTypeBinding binding = x.binding;
      String name;
      if (binding instanceof LocalTypeBinding) {
        char[] localName = binding.constantPoolName();
        name = new String(localName).replace('/', '.');
      } else {
        name = JdtUtil.asDottedString(binding.compoundName);
      }
      name = intern(name);

      JDeclaredType type;
      if (binding.isClass()) {
        type = new JClassType(info, name, binding.isAbstract(), binding.isFinal());
      } else if (binding.isInterface() || binding.isAnnotationType()) {
        type = new JInterfaceType(info, name);
      } else if (binding.isEnum()) {
        if (binding.isAnonymousType()) {
          // Don't model an enum subclass as a JEnumType.
          type = new JClassType(info, name, false, true);
        } else {
          type = new JEnumType(info, name, binding.isAbstract());
        }
      } else {
        throw new InternalCompilerException("ReferenceBinding is not a class, interface, or enum.");
      }
      if (isLegacyJsInteropEnabled) {
        JsInteropUtil.maybeSetJsInteropProperties(type, x.annotations);
      } else {
        JsInteropUtil.maybeSetJsInteropPropertiesNew(type, x.annotations);
      }
      processSuppressedWarnings(type, x.annotations);
      JdtUtil.setClassDispositionFromBinding(binding, type);
      typeMap.setSourceType(binding, type);
      newTypes.add(type);
      if (x.memberTypes != null) {
        for (TypeDeclaration memberType : x.memberTypes) {
          createTypes(memberType);
        }
      }
    } catch (Throwable e) {
      InternalCompilerException ice = translateException(null, e);
      StringBuffer sb = new StringBuffer();
      x.printHeader(0, sb);
      ice.addNode(x.getClass().getName(), sb.toString(), info);
      throw ice;
    }
  }

  private void mapExceptions(JMethod method, MethodBinding binding) {
    for (ReferenceBinding thrownBinding : binding.thrownExceptions) {
      JClassType type = (JClassType) typeMap.get(thrownBinding);
      method.addThrownException(type);
    }
  }

  private void resolveTypeRefs(TypeDeclaration x) {
    SourceTypeBinding binding = x.binding;
    JDeclaredType type = (JDeclaredType) typeMap.get(binding);
    try {
      ReferenceBinding superClassBinding = binding.superclass();
      if (type instanceof JClassType && superClassBinding != null) {
        assert (binding.superclass().isClass() || binding.superclass().isEnum());
        JClassType superClass = (JClassType) typeMap.get(superClassBinding);
        ((JClassType) type).setSuperClass(superClass);
      }

      ReferenceBinding[] superInterfaces = binding.superInterfaces();
      for (ReferenceBinding superInterfaceBinding : superInterfaces) {
        assert (superInterfaceBinding.isInterface());
        JInterfaceType superInterface = (JInterfaceType) typeMap.get(superInterfaceBinding);
        type.addImplements(superInterface);
      }

      ReferenceBinding enclosingBinding = binding.enclosingType();
      if (enclosingBinding != null) {
        type.setEnclosingType((JDeclaredType) typeMap.get(enclosingBinding));
      }
      if (x.memberTypes != null) {
        for (TypeDeclaration memberType : x.memberTypes) {
          resolveTypeRefs(memberType);
        }
      }
    } catch (Throwable e) {
      throw getInternalCompilerException(x, e);
    }
  }

  private InternalCompilerException getInternalCompilerException(TypeDeclaration x, Throwable e) {
    JDeclaredType type = (JDeclaredType) typeMap.get(x.binding);
    InternalCompilerException ice = translateException(null, e);
    StringBuffer sb = new StringBuffer();
    x.printHeader(0, sb);
    ice.addNode(x.getClass().getName(), sb.toString(), type.getSourceInfo());
    return ice;
  }

  /**
   * Returns the list of expressions as a single expression; returns {@code null} if the list
   * is empty.
   */
  private static JExpression singleExpressionFromExpressionList(SourceInfo info,
      List<JExpression> incrementsExpressions) {
    switch (incrementsExpressions.size()) {
      case 0:
        return null;
      case 1:
        return incrementsExpressions.get(0);
      default:
        return new JMultiExpression(info, incrementsExpressions);
    }
  }
}
