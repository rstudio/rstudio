/*
 * Copyright 2014 Google Inc.
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

import com.google.gwt.dev.PrecompileTaskOptions;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.SourceOrigin;
import com.google.gwt.dev.jjs.ast.HasName;
import com.google.gwt.dev.jjs.ast.HasType;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JBlock;
import com.google.gwt.dev.jjs.ast.JBooleanLiteral;
import com.google.gwt.dev.jjs.ast.JCharLiteral;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JConstructor;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JDoubleLiteral;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JFloatLiteral;
import com.google.gwt.dev.jjs.ast.JIntLiteral;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JLiteral;
import com.google.gwt.dev.jjs.ast.JLongLiteral;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNewInstance;
import com.google.gwt.dev.jjs.ast.JNullLiteral;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JParameterRef;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JStatement;
import com.google.gwt.dev.jjs.ast.JStringLiteral;
import com.google.gwt.dev.jjs.ast.JThisRef;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.js.JMultiExpression;
import com.google.gwt.dev.js.ast.JsBooleanLiteral;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsLiteral;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsNullLiteral;
import com.google.gwt.dev.js.ast.JsNumberLiteral;
import com.google.gwt.dev.js.ast.JsObjectLiteral;
import com.google.gwt.dev.js.ast.JsStringLiteral;
import com.google.gwt.dev.util.StringInterner;
import com.google.gwt.lang.LongLib;
import com.google.gwt.thirdparty.guava.common.annotations.VisibleForTesting;
import com.google.gwt.thirdparty.guava.common.base.Function;
import com.google.gwt.thirdparty.guava.common.base.Joiner;
import com.google.gwt.thirdparty.guava.common.base.Predicate;
import com.google.gwt.thirdparty.guava.common.base.Predicates;
import com.google.gwt.thirdparty.guava.common.collect.Collections2;
import com.google.gwt.thirdparty.guava.common.collect.FluentIterable;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableMap;
import com.google.gwt.thirdparty.guava.common.collect.Lists;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * General utilities related to Java AST manipulation.
 */
public class JjsUtils {

  public static boolean closureStyleLiteralsNeeded(PrecompileTaskOptions options) {
    return closureStyleLiteralsNeeded(options.isIncrementalCompileEnabled(),
        options.isClosureCompilerFormatEnabled());
  }

  /**
   * Returns the class literal field name.
   */
  public static String classLiteralFieldNameFromJavahTypeSignatureName(String javahSignatureName) {
    return javahSignatureName + "_classLit";
  }

  /**
   * Java8 Method References such as String::equalsIgnoreCase should produce inner class names
   * that are a function of the samInterface (e.g. Runnable), the method being referred to,
   * and the qualifying disposition (this::foo vs Class::foo if foo is an instance method)
   */
  public static String classNameForMethodReference(JType cuType,
      JInterfaceType functionalInterface, JMethod referredMethod, boolean hasReceiver) {
    String prefix = classNamePrefixForMethodReference(cuType.getPackageName(), cuType.getName(),
        functionalInterface.getName(), referredMethod.getEnclosingType().getName(),
        referredMethod.getName(), hasReceiver);

    return StringInterner.get().intern(
        constructManglingSignature(referredMethod, prefix));
  }

  /**
   * Java8 Method References such as String::equalsIgnoreCase should produce inner class names
   * that are a function of the samInterface (e.g. Runnable), the method being referred to,
   * and the qualifying disposition (this::foo vs Class::foo if foo is an instance method)
   */
  @VisibleForTesting
  static String classNamePrefixForMethodReference(String packageName, String cuTypeName,
      String functionalInterfaceName, String referredMethodEnclosingClassName,
      String referredMethodName, boolean hasReceiver) {
    return packageName + "." + Joiner.on("$$").join(
        // Make sure references to the same methods in different compilation units do not create
        // inner classses with the same name.
        mangledNameString(cuTypeName),
        "__",
        mangledNameString(functionalInterfaceName),
        "__",
        hasReceiver ? "instance" : "static",
        mangledNameString(referredMethodEnclosingClassName),
        mangledNameString(referredMethodName));
  }

  public static boolean closureStyleLiteralsNeeded(boolean incremental,
      boolean closureOutputFormat) {
    return !incremental && closureOutputFormat;
  }

  public static String computeSignature(
      String name, List<JType> params, JType returnType, boolean isCtor) {
    StringBuilder sb = new StringBuilder(name);
    sb.append('(');
    for (JType type : params) {
      sb.append(type.getJsniSignatureName());
    }
    sb.append(')');
    if (!isCtor) {
      sb.append(returnType.getJsniSignatureName());
    } else {
      sb.append(" <init>");
    }
    return sb.toString();
  }

  public static String constructManglingSignature(JMethod x, String partialSignature) {
    StringBuilder sb = new StringBuilder(partialSignature);
    sb.append("__");
    for (int i = 0; i < x.getOriginalParamTypes().size(); ++i) {
      JType type = x.getOriginalParamTypes().get(i);
      sb.append(type.getJavahSignatureName());
    }
    sb.append(x.getOriginalReturnType().getJavahSignatureName());
    return sb.toString();
  }

  /**
   * Returns an instantiation expression for {@code type} using the default constructor,
   * Returns {@code null} if {@code type} does not have a default constructor.
   */
  public static JExpression createDefaultConstructorInstantiation(
      SourceInfo info, JClassType type) {
    /*
     * Find the appropriate (noArg) constructor. In our AST, constructors are
     * instance methods that should be qualified with a new expression.
     */
    JConstructor noArgCtor = (JConstructor) FluentIterable.from(type.getMethods()).firstMatch(
        new Predicate<JMethod>() {
          @Override
          public boolean apply(JMethod method) {
            return method instanceof JConstructor &&  method.getOriginalParamTypes().size() == 0;
          }
        }).get();
    if (noArgCtor == null) {
      return null;
    }
    // Call it, using a new expression as a qualifier
    return new JNewInstance(info, noArgCtor);
  }

  /**
   * Creates a synthetic forwarding  stub in {@code type} with the same signature as
   * {@code superTypeMethod} that dispatchs to that method..
   */
  public static JMethod createForwardingMethod(JDeclaredType type,
      JMethod methodToDelegateTo) {
    JMethod forwardingMethod = createEmptyMethodFromExample(type, methodToDelegateTo, false);
    forwardingMethod.setForwarding();

    // This is a synthetic forwading method due to a default.
    if (methodToDelegateTo.isDefaultMethod()) {
      forwardingMethod.setDefaultMethod();
    }

    // Create the forwarding body.
    JMethodBody body = (JMethodBody) forwardingMethod.getBody();
    // Invoke methodToDelegate
    JMethodCall forwardingCall = new JMethodCall(methodToDelegateTo.getSourceInfo(),
        new JThisRef(methodToDelegateTo.getSourceInfo(), methodToDelegateTo.getEnclosingType()),
        methodToDelegateTo);
    forwardingCall.setStaticDispatchOnly();
    // copy params
    for (JParameter p : forwardingMethod.getParams()) {
      forwardingCall.addArg(new JParameterRef(p.getSourceInfo(), p));
    }

    // return statement if not void return type
    body.getBlock().addStmt(makeMethodEndStatement(forwardingMethod.getType(), forwardingCall));
    return forwardingMethod;
  }

  /**
   * Creates a multi expression from a list of expressions, removing expressions that do
   * not have side effects if possible.
   */
  public static JExpression createOptimizedMultiExpression(JExpression... expressions) {
    return createOptimizedMultiExpression(false, Arrays.asList(expressions));
  }

  /**
   * Creates a multi expression from a list of expressions, removing expressions that do
   * not have side effects if possible.
   */
  public static JExpression createOptimizedMultiExpression(boolean ignoringResult,
      List<JExpression> expressions) {

    int numberOfExpressions = expressions.size();
    JExpression result = expressions.get(numberOfExpressions - 1);

    numberOfExpressions = expressions.size();
    if (numberOfExpressions == 0) {
      return new JMultiExpression(SourceOrigin.UNKNOWN);
    }

    expressions =  Lists.newArrayList(Collections2.filter(
        expressions.subList(0, numberOfExpressions - 1),
        Predicates.and(
            Predicates.notNull(),
            new Predicate<JExpression>() {
              @Override
              public boolean apply(JExpression expression) {
                return expression.hasSideEffects();
              }
            })));

    if (result != null && (!ignoringResult || result.hasSideEffects())) {
      expressions.add(result);
    }

    if (expressions.size() == 1) {
      // Do not create a multi expression if it consists only of the result.
      return expressions.iterator().next();
    }

    SourceInfo info = expressions.size() > 0 ? expressions.get(0).getSourceInfo() :
        SourceOrigin.UNKNOWN;
    return new JMultiExpression(info, expressions);
  }

  /**
   * Returns an ast node representing the expression {@code expression != null}.
   */
  public static JExpression createOptimizedNotNullComparison(
      JProgram program, SourceInfo info, JExpression expression) {
    JReferenceType type = (JReferenceType) expression.getType();
    if (type.isNullType()) {
      return program.getLiteralBoolean(false);
    }

    if (!type.canBeNull()) {
      return createOptimizedMultiExpression(expression, program.getLiteralBoolean(true));
    }

    return new JBinaryOperation(info, program.getTypePrimitiveBoolean(),
        JBinaryOperator.NEQ, expression, program.getLiteralNull());
  }

  /**
   * Creates a synthetic abstract stub in {@code type} with the same signature as
   * {@code superTypeMethod}.
   */
  public static JMethod createSyntheticAbstractStub(JDeclaredType type, JMethod superTypeMethod) {
    assert type.isAbstract();
    assert superTypeMethod.isAbstract();
    return createEmptyMethodFromExample(type, superTypeMethod, true);
  }

  public static void replaceMethodBody(JMethod method, JExpression returnValue) {
    JMethodBody body = (JMethodBody) method.getBody();
    JBlock block = body.getBlock();
    block.clear();
    block.addStmt(returnValue.makeReturnStatement());
  }

  /**
   * Returns types from typed nodes.
   */
  public static Iterable<JReferenceType> getExpressionTypes(Iterable<? extends HasType> nodes) {
    if (nodes == null) {
      return Collections.emptyList();
    }
    return FluentIterable.from(nodes).transform(
        new Function<HasType, JReferenceType>() {
          @Override
          public JReferenceType apply(HasType typedNode) {
            return (JReferenceType) typedNode.getType();
          }
        });
  }

  /**
   * Mangles a qualified name into a Javah signature.
   */
  public static String javahSignatureFromName(String name) {
    return "L" + mangledNameString(name) + "_2";
  }

  public static String mangleMemberName(String enclosingTypeName, String fieldName) {
    return mangledNameString(enclosingTypeName) + '_' + mangledNameString(fieldName);
  }

  /**
   * Returns an valid identifier for a named Java entity.
   */
  public static String mangledNameString(HasName hasName) {
    return mangledNameString(hasName.getName());
  }

  /**
   * Returns an valid identifier for a named Java entity.
   */
  public static String mangledNameString(String name) {
    return name.replaceAll("_", "_1").replace('.', '_');
  }

  /**
   * Returns the ending statement for a method based on an expression. If the return type is void
   * then the ending statement just executes the expression otherwise it returns it.
   */
  public static JStatement makeMethodEndStatement(JType returnType, JExpression expression) {
    // TODO(rluble): Check if something needs to be done here regarding boxing/unboxing/coercions
    // when one of the types of expression and returnType is a boxed type and the other a primitive
    // type or both are primitive of differnent coerceable types. Add the proper tests first.
    return returnType == JPrimitiveType.VOID ?
        expression.makeStatement() :
        expression.makeReturnStatement();
  }

  /**
   * Translates a Java literal into a JavaScript literal.
   */
  public static JsLiteral translateLiteral(JLiteral literal) {
    return translatorByLiteralClass.get(literal.getClass()).translate(literal);
  }

  private static Map<Class<? extends JLiteral>, LiteralTranslators> translatorByLiteralClass =
      new ImmutableMap.Builder<Class<? extends JLiteral>, LiteralTranslators>()
          .put(JBooleanLiteral.class, LiteralTranslators.BOOLEAN_LITERAL_TRANSLATOR)
          .put(JCharLiteral.class, LiteralTranslators.CHAR_LITERAL_TRANSLATOR)
          .put(JFloatLiteral.class, LiteralTranslators.FLOAT_LITERAL_TRANSLATOR)
          .put(JDoubleLiteral.class, LiteralTranslators.DOUBLE_LITERAL_TRANSLATOR)
          .put(JIntLiteral.class, LiteralTranslators.INT_LITERAL_TRANSLATOR)
          .put(JLongLiteral.class, LiteralTranslators.LONG_LITERAL_TRANSLATOR)
          .put(JNullLiteral.class, LiteralTranslators.NULL_LITERAL_TRANSLATOR)
          .put(JStringLiteral.class, LiteralTranslators.STRING_LITERAL_TRANSLATOR)
          .build();

  private enum LiteralTranslators {
    BOOLEAN_LITERAL_TRANSLATOR() {
      @Override
      JsLiteral translate(JExpression literal) {
        return JsBooleanLiteral.get(((JBooleanLiteral) literal).getValue());
      }
    },
    CHAR_LITERAL_TRANSLATOR() {
      @Override
      JsLiteral translate(JExpression literal) {
        return new JsNumberLiteral(literal.getSourceInfo(), ((JCharLiteral) literal).getValue());
      }
    },
    FLOAT_LITERAL_TRANSLATOR() {
      @Override
      JsLiteral translate(JExpression literal) {
        return new JsNumberLiteral(literal.getSourceInfo(), ((JFloatLiteral) literal).getValue());
      }
    },
    DOUBLE_LITERAL_TRANSLATOR() {
      @Override
      JsLiteral translate(JExpression literal) {
        return new JsNumberLiteral(literal.getSourceInfo(), ((JDoubleLiteral) literal).getValue());
      }
    },
    INT_LITERAL_TRANSLATOR() {
      @Override
      JsLiteral translate(JExpression literal) {
        return new JsNumberLiteral(literal.getSourceInfo(), ((JIntLiteral) literal).getValue());
      }
    },
    LONG_LITERAL_TRANSLATOR() {
      @Override
      JsLiteral translate(JExpression literal) {
        SourceInfo sourceInfo = literal.getSourceInfo();
        long[] values = LongLib.getAsLongArray(((JLongLiteral) literal).getValue());
        if (values.length == 1) {
          return new JsNumberLiteral(literal.getSourceInfo(), ((JLongLiteral) literal).getValue());
        }
        JsObjectLiteral objectLiteral = new JsObjectLiteral(sourceInfo);
        objectLiteral.setInternable();

        assert values.length == names.length;
        for (int i = 0; i < names.length; i++) {
          addPropertyToObject(sourceInfo, names[i], values[i], objectLiteral);
        }
        return objectLiteral;
      }
    },
    STRING_LITERAL_TRANSLATOR() {
      @Override
      JsLiteral translate(JExpression literal) {
        return new JsStringLiteral(literal.getSourceInfo(), ((JStringLiteral) literal).getValue());
      }
    },
    NULL_LITERAL_TRANSLATOR() {
      @Override
      JsLiteral translate(JExpression literal) {
        return JsNullLiteral.INSTANCE;
      }
    };

    private static final JsName[] names;

    static {
      // The names of the components in an emulated long ('l', 'm', and 'h') are accessed directly
      // through JSNI in LongLib (the implementor of emulated long operations), hence it is
      // important that they don't get renamed hence the corresponding JsNames are created
      // unscoped (null scope) and unobfuscatable.
      String[] stringNames = {"l","m","h"};
      names = new JsName[stringNames.length];
      for (int i = 0; i < stringNames.length; i++) {
        names[i] = new JsName(null, stringNames[i], stringNames[i]);
        names[i].setObfuscatable(false);
      }
    }

    abstract JsLiteral translate(JExpression literal);
  }

  private static void addPropertyToObject(SourceInfo sourceInfo, JsName propertyName,
      long propertyValue, JsObjectLiteral objectLiteral) {
    JsExpression label = propertyName.makeRef(sourceInfo);
    JsExpression value = new JsNumberLiteral(sourceInfo, propertyValue);
    objectLiteral.addProperty(sourceInfo, label, value);
  }

  private static JMethod createEmptyMethodFromExample(
      JDeclaredType inType, JMethod exampleMethod, boolean isAbstract) {
    JMethod emptyMethod = new JMethod(exampleMethod.getSourceInfo(), exampleMethod.getName(),
        inType, exampleMethod.getType(), isAbstract, false, false, exampleMethod.getAccess());
    emptyMethod.addThrownExceptions(exampleMethod.getThrownExceptions());
    emptyMethod.setSynthetic();
    // Copy parameters.
    for (JParameter param : exampleMethod.getParams()) {
      emptyMethod.addParam(new JParameter(param.getSourceInfo(), param.getName(), param.getType(),
          param.isFinal(), param.isThis(), emptyMethod));
    }
    JMethodBody body = new JMethodBody(exampleMethod.getSourceInfo());
    emptyMethod.setBody(body);
    emptyMethod.freezeParamTypes();
    inType.addMethod(emptyMethod);
    return emptyMethod;
  }

  private JjsUtils() {
  }
}