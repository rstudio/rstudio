/*
 * Copyright 2015 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.MinimalRebuildCache;
import com.google.gwt.dev.javac.JsInteropUtil;
import com.google.gwt.dev.jjs.HasSourceInfo;
import com.google.gwt.dev.jjs.ast.CanHaveSuppressedWarnings;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.HasJsInfo.JsMemberType;
import com.google.gwt.dev.jjs.ast.HasJsName;
import com.google.gwt.dev.jjs.ast.HasType;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JConstructor;
import com.google.gwt.dev.jjs.ast.JDeclarationStatement;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JDeclaredType.NestedClassDisposition;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JExpressionStatement;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JInstanceOf;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JMember;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JStatement;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.js.JsniMethodBody;
import com.google.gwt.dev.js.JsUtils;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsParameter;
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.dev.util.Pair;
import com.google.gwt.dev.util.log.AbstractTreeLogger;
import com.google.gwt.thirdparty.guava.common.base.Predicate;
import com.google.gwt.thirdparty.guava.common.collect.FluentIterable;
import com.google.gwt.thirdparty.guava.common.collect.Iterables;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;
import com.google.gwt.thirdparty.guava.common.collect.Ordering;
import com.google.gwt.thirdparty.guava.common.collect.Sets;
import com.google.gwt.thirdparty.guava.common.collect.TreeMultimap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Checks and throws errors for invalid JsInterop constructs.
 */
public class JsInteropRestrictionChecker {

  public static void exec(TreeLogger logger, JProgram jprogram,
      MinimalRebuildCache minimalRebuildCache) throws UnableToCompleteException {
    JsInteropRestrictionChecker jsInteropRestrictionChecker =
        new JsInteropRestrictionChecker(jprogram, minimalRebuildCache);
    boolean success = jsInteropRestrictionChecker.checkProgram(logger);
    if (!success) {
      throw new UnableToCompleteException();
    }
  }

  private Multimap<String, String> errorsByFilename
      = TreeMultimap.create(Ordering.natural(), AbstractTreeLogger.LOG_LINE_COMPARATOR);
  private Multimap<String, String> warningsByFilename
      = TreeMultimap.create(Ordering.natural(), AbstractTreeLogger.LOG_LINE_COMPARATOR);
  private final JProgram jprogram;
  private final MinimalRebuildCache minimalRebuildCache;

  private JsInteropRestrictionChecker(JProgram jprogram,
      MinimalRebuildCache minimalRebuildCache) {
    this.jprogram = jprogram;
    this.minimalRebuildCache = minimalRebuildCache;
  }

  /**
   * Returns true if the constructor method is locally empty (allows calls to init and super
   * constructor).
   */
  private static boolean isConstructorEmpty(final JConstructor constructor) {
    return Iterables.all(constructor.getBody().getStatements(), new Predicate<JStatement>() {
      @Override
      public boolean apply(JStatement statement) {
        JClassType type = constructor.getEnclosingType();
        if (isImplicitSuperCall(statement, type.getSuperClass())) {
          return true;
        }
        if (isInitCall(statement, type)) {
          return true;
        }
        return false;
      }
    });
  }

  private static JMethodCall isMethodCall(JStatement statement) {
    if (!(statement instanceof JExpressionStatement)) {
      return null;
    }
    JExpression expression = ((JExpressionStatement) statement).getExpr();

    return expression instanceof JMethodCall ? (JMethodCall) expression : null;
  }

  private static boolean isInitCall(JStatement statement, JDeclaredType type) {
    JMethodCall methodCall = isMethodCall(statement);

    return methodCall != null
        && methodCall.getTarget() == type.getInitMethod();
  }

  private static boolean isImplicitSuperCall(JStatement statement, JDeclaredType superType) {
    JMethodCall methodCall = isMethodCall(statement);

    return methodCall != null
        && methodCall.isStaticDispatchOnly()
        && methodCall.getTarget().isConstructor()
        && methodCall.getTarget().getEnclosingType() == superType;
  }

  private static boolean isInitEmpty(JDeclaredType type) {
    return type.getInitMethod() == null
        || ((JMethodBody) type.getInitMethod().getBody()).getStatements().isEmpty();
  }

  /**
   * Returns true if the clinit for a type is locally empty (except for the call to its super
   * clinit).
   */
  private static boolean isClinitEmpty(JDeclaredType type, final boolean skipDeclaration) {
    JMethod clinit = type.getClinitMethod();
    List<JStatement> statements = FluentIterable
        .from(((JMethodBody) clinit.getBody()).getStatements())
        .filter(new Predicate<JStatement>() {
          @Override
          public boolean apply(JStatement statement) {
            if (!(statement instanceof JDeclarationStatement)) {
              return true;
            }
            if (skipDeclaration) {
              return false;
            }
            JDeclarationStatement declarationStatement = (JDeclarationStatement) statement;
            JField field = (JField) declarationStatement.getVariableRef().getTarget();
            return !field.isCompileTimeConstant();
          }
        }).toList();
    if (statements.isEmpty()) {
      return true;
    }
    return statements.size() == 1 && isClinitCall(statements.get(0), type.getSuperClass());
  }

  private static boolean isClinitCall(JStatement statement, JClassType superClass) {
    if (superClass == null || !(statement instanceof JExpressionStatement)) {
      return false;
    }

    JExpression expression = ((JExpressionStatement) statement).getExpr();
    if (!(expression instanceof JMethodCall)) {
      return false;
    }
    return ((JMethodCall) expression).getTarget() == superClass.getClinitMethod();
  }

  private void checkJsConstructors(JDeclaredType x) {
    List<JMethod> jsConstructors = FluentIterable
        .from(x.getMethods())
        .filter(new Predicate<JMethod>() {
          @Override
          public boolean apply(JMethod m) {
            return m.isJsConstructor();
          }
        }).toList();

    if (x.isJsNative()) {
      return;
    }

    if (jsConstructors.isEmpty()) {
      return;
    }

    if (jsConstructors.size() > 1) {
      logError(x, "More than one JsConstructor exists for %s.", JjsUtils.getReadableDescription(x));
    }

    final JConstructor jsConstructor = (JConstructor) jsConstructors.get(0);

    boolean anyNonDelegatingConstructor = Iterables.any(x.getMethods(), new Predicate<JMethod>() {
      @Override
      public boolean apply(JMethod method) {
        return method != jsConstructor && method instanceof JConstructor
            && !isDelegatingToConstructor((JConstructor) method, jsConstructor);
      }
    });

    if (anyNonDelegatingConstructor) {
      logError(jsConstructor,
          "Constructor %s can be a JsConstructor only if all constructors in the class are "
          + "delegating to it.", getMemberDescription(jsConstructor));
    }
  }

  private boolean isDelegatingToConstructor(JConstructor ctor, JConstructor targetCtor) {
    List<JStatement> statements = ctor.getBody().getBlock().getStatements();
    JExpressionStatement statement = (JExpressionStatement) statements.get(0);
    JMethodCall call = (JMethodCall) statement.getExpr();
    assert call.isStaticDispatchOnly() : "Every ctor should either have this() or super() call";
    return call.getTarget().equals(targetCtor);
  }

  private void checkMember(
      JMember member, Map<String, JsMember> localNames, Map<String, JsMember> ownGlobalNames) {
    if (member.getEnclosingType().isJsNative()) {
      checkMemberOfNativeJsType(member);
    }

    if (member.needsDynamicDispatch()) {
      checkIllegalOverrides(member);
    }

    if (member instanceof JMethod) {
      checkMethodParameters((JMethod) member);
    }

    if (member.isJsOverlay()) {
      checkJsOverlay(member);
      return;
    }

    if (member.canBeReferencedExternally()) {
      checkUnusableByJs(member);
    }

    if (member.getJsMemberType() == JsMemberType.NONE) {
      return;
    }

    if (!checkJsPropertyAccessor(member)) {
      return;
    }

    checkMemberQualifiedJsName(member);

    if (isCheckedLocalName(member)) {
      checkLocalName(localNames, member);
    }

    if (isCheckedGlobalName(member)) {
      checkGlobalName(ownGlobalNames, member);
    }
  }

  private void checkIllegalOverrides(JMember member) {
    if (member instanceof JField) {
      return;
    }

    JMethod method = (JMethod) member;

    if (method.isSynthetic()) {
      // Ignore synthetic methods. These synthetic methods might be accidental overrides or
      // default method implementations, and they forward to the same implementation so it is
      // safe to allow them.
      return;
    }
    for (JMethod overriddenMethod : method.getOverriddenMethods()) {
      if (overriddenMethod.isSynthetic()) {
        // Ignore synthetic methods for a better error message.
        continue;
      }
      if (overriddenMethod.isJsOverlay()) {
        logError(member, "Method '%s' cannot override a JsOverlay method '%s'.",
            JjsUtils.getReadableDescription(method),
            JjsUtils.getReadableDescription(overriddenMethod));
        return;
      }
    }
  }

  private void checkJsOverlay(JMember member) {
    if (member.getEnclosingType().isJsoType()) {
      return;
    }

    String memberDescription = JjsUtils.getReadableDescription(member);

    if (!member.getEnclosingType().isJsNative()) {
      logError(member, "JsOverlay '%s' can only be declared in a native type.", memberDescription);
    }

    if (member instanceof JConstructor) {
      logError(member, "JsOverlay method '%s' cannot be a constructor.", memberDescription);
      return;
    }

    if (member.getJsMemberType() != JsMemberType.NONE) {
      logError(member, "JsOverlay method '%s' cannot be nor override a JsProperty or a JsMethod.",
          memberDescription);
      return;
    }

    if (member instanceof JField) {
      JField field = (JField) member;
      if (field.needsDynamicDispatch()) {
        logError(member, "JsOverlay field '%s' can only be static.", memberDescription);
      }
      return;
    }

    JMethod method = (JMethod) member;

    assert method.getOverriddenMethods().isEmpty();

    if (method.getBody() == null || (!method.isFinal() && !method.isStatic()
        && !method.isDefaultMethod())) {
      logError(member, "JsOverlay method '%s' cannot be non-final nor native.", memberDescription);
    }
  }

  private void checkMemberOfNativeJsType(JMember member) {
    if (member instanceof JMethod && ((JMethod) member).isJsniMethod()) {
      logError(member, "JSNI method %s is not allowed in a native JsType.",
          getMemberDescription(member));
      return;
    }

    if (member.isSynthetic() || member.isJsOverlay()) {
      return;
    }

    JsMemberType jsMemberType = member.getJsMemberType();
    switch (jsMemberType) {
      case CONSTRUCTOR:
        if (!isConstructorEmpty((JConstructor) member)) {
          logError(member, "Native JsType constructor %s cannot have non-empty method body.",
              getMemberDescription(member));
        }
        break;
      case METHOD:
      case GETTER:
      case SETTER:
      case UNDEFINED_ACCESSOR:
        JMethod method = (JMethod) member;
        if (!method.isAbstract() && method.getBody() != null) {
          logError(member, "Native JsType method %s should be native or abstract.",
              getMemberDescription(member));
        }
        break;
      case PROPERTY:
        JField field = (JField) member;
        if (field.hasInitializer()) {
          logError(member, "Native JsType field %s cannot have initializer.",
              getMemberDescription(member));
        }
        break;
      case NONE:
        logError(member, "Native JsType member %s cannot have @JsIgnore.",
            getMemberDescription(member));
        break;
    }
  }

  private void checkMethodParameters(JMethod method) {
    boolean hasOptionalParameters = false;
    for (JParameter parameter : method.getParams()) {
      if (parameter.isOptional()) {
        hasOptionalParameters = true;
        continue;
      }
      if (hasOptionalParameters && !parameter.isVarargs()) {
        logError(method, "JsOptional parameter '%s' in method %s cannot precede parameters "
            + "that are not optional.", parameter.getName(), getMemberDescription(method));
        break;
      }
    }

    if (hasOptionalParameters
        && method.getJsMemberType() != JsMemberType.CONSTRUCTOR
        && method.getJsMemberType() != JsMemberType.METHOD
        && !method.isOrOverridesJsFunctionMethod()) {
      logError(method, "Method %s has JsOptional parameters and is not a JsMethod, "
          + "a JsConstructor or a JsFunction method.", getMemberDescription(method));
    }

    if (method.isJsMethodVarargs()) {
      checkJsVarargs(method);
    }
  }

  private void checkJsVarargs(final JMethod method) {
    if (!method.isJsniMethod()) {
      return;
    }
    final JsFunction function = ((JsniMethodBody) method.getBody()).getFunc();
    final JsParameter varargParameter = Iterables.getLast(function.getParameters());
    new JsVisitor() {
      @Override
      public void endVisit(JsNameRef x, JsContext ctx) {
        if (x.getName() == varargParameter.getName()) {
          logError(x, "Cannot access vararg parameter '%s' from JSNI in JsMethod %s."
              + " Use 'arguments' instead.", x.getIdent(),
              getMemberDescription(method));
        }
      }
    }.accept(function);
  }

  private boolean checkJsPropertyAccessor(JMember member) {
    JsMemberType memberType = member.getJsMemberType();

    if (member.getJsName().equals(JsInteropUtil.INVALID_JSNAME)) {
      assert memberType.isPropertyAccessor();
      logError(
          member,
          "JsProperty %s should either follow Java Bean naming conventions or provide a name.",
          getMemberDescription(member));
      return false;
    }

    switch (memberType) {
      case UNDEFINED_ACCESSOR:
        logError(member, "JsProperty %s should have a correct setter or getter signature.",
            getMemberDescription(member));
        break;
      case GETTER:
        if (member.getType() != JPrimitiveType.BOOLEAN && member.getName().startsWith("is")) {
          logError(member, "JsProperty %s cannot have a non-boolean return.",
              getMemberDescription(member));
        }
        break;
      case SETTER:
        if (((JMethod) member).getParams().get(0).isVarargs()) {
          logError(member, "JsProperty %s cannot have a vararg parameter.",
              getMemberDescription(member));
        }
        break;
    }

    if (memberType.isPropertyAccessor() && member.isStatic() && !member.isJsNative()) {
        logError(member, "Static property accessor '%s' can only be native.",
            JjsUtils.getReadableDescription(member));
    }

    return true;
  }

  private void checkMemberQualifiedJsName(JMember member) {
    if (member instanceof JConstructor) {
      // Constructors always inherit their name and namespace from the enclosing type.
      // The corresponding checks are done for the type separately.
      return;
    }

    checkJsName(member);

    if (member.getJsNamespace().equals(member.getEnclosingType().getQualifiedJsName())) {
      // Namespace set by the enclosing type has already been checked.
      return;
    }

    if (member.needsDynamicDispatch()) {
      logError(member, "Instance member %s cannot declare a namespace.",
          getMemberDescription(member));
      return;
    }

    checkJsNamespace(member);
  }

  private <T extends HasJsName & HasSourceInfo> void checkJsName(T item) {
    if (item.getJsName().isEmpty()) {
      logError(item, "%s cannot have an empty name.", getDescription(item));
    } else if (!JsUtils.isValidJsIdentifier(item.getJsName())) {
      logError(item, "%s has invalid name '%s'.", getDescription(item), item.getJsName());
    }
  }

  private <T extends HasJsName & HasSourceInfo> void checkJsNamespace(T item) {
    if (JsInteropUtil.isGlobal(item.getJsNamespace())) {
      return;
    }
    if (item.getJsNamespace().isEmpty()) {
      logError(item, "%s cannot have an empty namespace.", getDescription(item));
    } else if (!JsUtils.isValidJsQualifiedName(item.getJsNamespace())) {
      logError(item, "%s has invalid namespace '%s'.", getDescription(item), item.getJsNamespace());
    }
  }

  private void checkLocalName(Map<String, JsMember> localNames, JMember member) {
    Pair<JsMember, JsMember> oldAndNewJsMember = updateJsMembers(localNames, member);
    JsMember oldJsMember = oldAndNewJsMember.left;
    JsMember newJsMember = oldAndNewJsMember.right;

    checkNameConsistency(member);
    checkJsPropertyConsistency(member, newJsMember);

    if (oldJsMember == null || oldJsMember == newJsMember) {
      return;
    }

    if (oldJsMember.isNativeMethod() && newJsMember.isNativeMethod()) {
      return;
    }

    logError(member, "%s and %s cannot both use the same JavaScript name '%s'.",
        getMemberDescription(member), getMemberDescription(oldJsMember.member), member.getJsName());
  }

  private void checkGlobalName(Map<String, JsMember> ownGlobalNames, JMember member) {
    Pair<JsMember, JsMember> oldAndNewJsMember = updateJsMembers(ownGlobalNames, member);
    JsMember oldJsMember = oldAndNewJsMember.left;
    JsMember newJsMember = oldAndNewJsMember.right;

    if (oldJsMember == newJsMember) {
      // We allow setter-getter to share the name if they are both defined in the same class, so
      // skipping the global name check. However still need to do a consistency check.
      checkJsPropertyConsistency(member, newJsMember);
      return;
    }

    String currentGlobalNameDescription =
        minimalRebuildCache.addExportedGlobalName(member.getQualifiedJsName(),
            JjsUtils.getReadableDescription(member), member.getEnclosingType().getName());
    if (currentGlobalNameDescription == null) {
      return;
    }
    logError(member, "%s cannot be exported because the global name '%s' is already taken by '%s'.",
        getMemberDescription(member), member.getQualifiedJsName(), currentGlobalNameDescription);
  }

  private void checkJsPropertyConsistency(JMember member, JsMember newMember) {
    if (newMember.setter != null && newMember.getter != null) {
      List<JParameter> setterParams = ((JMethod) newMember.setter).getParams();
      if (newMember.getter.getType() != setterParams.get(0).getType()) {
        logError(member, "JsProperty setter %s and getter %s cannot have inconsistent types.",
            getMemberDescription(newMember.setter), getMemberDescription(newMember.getter));
      }
    }
  }

  private void checkNameConsistency(JMember member) {
    if (member instanceof JMethod) {
      String jsName = member.getJsName();
      for (JMethod jMethod : ((JMethod) member).getOverriddenMethods()) {
        String parentName = jMethod.getJsName();
        if (parentName != null && !parentName.equals(jsName)) {
          logError(
              member,
              "%s cannot be assigned a different JavaScript name than the method it overrides.",
              getMemberDescription(member));
          break;
        }
      }
    }
  }

  private void checkStaticJsPropertyCalls() {
    new JVisitor() {
      @Override
      public boolean visit(JMethod x, Context ctx) {
        // Skip unnecessary synthetic override, as they will not be generated.
        return !JjsUtils.isUnnecessarySyntheticAccidentalOverride(x);
      }

      @Override
      public void endVisit(JMethodCall x, Context ctx) {
        JMethod target = x.getTarget();
        if (x.isStaticDispatchOnly() && target.getJsMemberType().isPropertyAccessor()) {
          logError(x, "Cannot call property accessor %s via super.",
              getMemberDescription(target));
        }
      }
    }.accept(jprogram);
  }

  private void checkInstanceOfNativeJsTypes() {
    new JVisitor() {
      @Override
      public boolean visit(JInstanceOf x, Context ctx) {
        JReferenceType type = x.getTestType();
        if (type.isJsNative() && type instanceof JInterfaceType) {
          logError(x, "Cannot do instanceof against native JsType interface '%s'.",
              JjsUtils.getReadableDescription(type));
        }
        return true;
      }
    }.accept(jprogram);
  }

  private boolean checkJsType(JDeclaredType type) {
    // Java (at least up to Java 8) does not allow to annotate anonymous classes or lambdas; if
    // it ever becomes possible we should emit an error.
    assert type.getClassDisposition() != NestedClassDisposition.ANONYMOUS
        && type.getClassDisposition() != NestedClassDisposition.LAMBDA;

    if  (type.getClassDisposition() == NestedClassDisposition.LOCAL) {
      logError("Local class '%s' cannot be a JsType.", type);
      return false;
    }

    return true;
  }

  private boolean checkNativeJsType(JDeclaredType type) {
    if (type.isEnumOrSubclass() != null) {
      logError("Enum '%s' cannot be a native JsType.", type);
      return false;
    }

    if (type.getClassDisposition() == NestedClassDisposition.INNER) {
      logError("Non static inner class '%s' cannot be a native JsType.", type);
      return false;
    }

    JClassType superClass = type.getSuperClass();
    if (superClass != null && superClass != jprogram.getTypeJavaLangObject() &&
        !superClass.isJsNative()) {
      logError("Native JsType '%s' can only extend native JsType classes.", type);
    }

    for (JInterfaceType interfaceType : type.getImplements()) {
      if (!interfaceType.isJsNative()) {
        logError(type, "Native JsType '%s' can only %s native JsType interfaces.",
            getDescription(type),
            type instanceof JInterfaceType ? "extend" : "implement");
      }
    }

    if (!isInitEmpty(type)) {
      logError("Native JsType '%s' cannot have initializer.", type);
    }

    if (!isClinitEmpty(type, true)) {
      logError("Native JsType '%s' cannot have static initializer.", type);
    }

    return true;
  }

  private void checkJsFunction(JDeclaredType type) {
    if (!isClinitEmpty(type, false)) {
      logError("JsFunction '%s' cannot have static initializer.", type);
    }

    if (type.getImplements().size() > 0) {
      logError("JsFunction '%s' cannot extend other interfaces.", type);
    }

    if (type.isJsType()) {
      logError("'%s' cannot be both a JsFunction and a JsType at the same time.", type);
    }
  }

  private void checkJsFunctionImplementation(JDeclaredType type) {
    if (type.getImplements().size() != 1) {
      logError("JsFunction implementation '%s' cannot implement more than one interface.",
          type);
    }

    if (type.isJsType()) {
      logError("'%s' cannot be both a JsFunction implementation and a JsType at the same time.",
          type);
    }

    if (type.getSuperClass() != jprogram.getTypeJavaLangObject()) {
      logError("JsFunction implementation '%s' cannot extend a class.", type);
    }
  }

  private void checkJsFunctionSubtype(JDeclaredType type) {
    JClassType superClass = type.getSuperClass();
    if (superClass != null && superClass.isJsFunctionImplementation()) {
      logError(type, "'%s' cannot extend JsFunction implementation '%s'.",
          JjsUtils.getReadableDescription(type), JjsUtils.getReadableDescription(superClass));
    }
    for (JInterfaceType superInterface : type.getImplements()) {
      if (superInterface.isJsFunction()) {
        logError(type, "'%s' cannot extend JsFunction '%s'.",
            JjsUtils.getReadableDescription(type), JjsUtils.getReadableDescription(superInterface));
      }
    }
  }

  private boolean checkProgram(TreeLogger logger) {
    for (JDeclaredType type : jprogram.getModuleDeclaredTypes()) {
      checkType(type);
    }
    checkStaticJsPropertyCalls();
    checkInstanceOfNativeJsTypes();

    boolean hasErrors = reportErrorsAndWarnings(logger);
    return !hasErrors;
  }

  private void checkType(JDeclaredType type) {
    minimalRebuildCache.removeExportedNames(type.getName());

    if (type.isJsType()) {
      if (!checkJsType(type)) {
        return;
      }
      checkJsName(type);
      checkJsNamespace(type);
    }

    if (type.isJsNative()) {
      if (!checkNativeJsType(type)) {
        return;
      }
    }

    if (type.isJsFunction()) {
      checkJsFunction(type);
    } else if (type.isJsFunctionImplementation()) {
      checkJsFunctionImplementation(type);
    } else {
      checkJsFunctionSubtype(type);
      checkJsConstructors(type);
    }

    Map<String, JsMember> ownGlobalNames = Maps.newHashMap();
    Map<String, JsMember> localNames = collectLocalNames(type.getSuperClass());
    for (JMember member : type.getMembers()) {
      checkMember(member, localNames, ownGlobalNames);
    }
  }

  private void checkUnusableByJs(JMember member) {
    logIfUnusableByJs(member, member instanceof JField ? "Type of" : "Return type of", member);

    if (member instanceof JMethod) {
      for (JParameter parameter : ((JMethod) member).getParams()) {
        String prefix = String.format("Type of parameter '%s' in", parameter.getName());
        logIfUnusableByJs(parameter, prefix, member);
      }
    }
  }

  private <T extends HasType & CanHaveSuppressedWarnings> void logIfUnusableByJs(
      T hasType, String prefix, JMember x) {
    if (hasType.getType().canBeReferencedExternally()) {
      return;
    }
    if (isUnusableByJsSuppressed(x.getEnclosingType()) || isUnusableByJsSuppressed(x)
        || isUnusableByJsSuppressed(hasType)) {
      return;
    }
    logWarning(x, "[unusable-by-js] %s %s is not usable by but exposed to JavaScript.", prefix,
        getMemberDescription(x));
  }

  private static class JsMember {
    private JMember member;
    private JMember setter;
    private JMember getter;

    public JsMember(JMember member) {
      this.member = member;
    }

    public JsMember(JMember member, JMember setter, JMember getter) {
      this.member = member;
      this.setter = setter;
      this.getter = getter;
    }

    public boolean isNativeMethod() {
      return member instanceof JMethod && member.isJsNative() && !isPropertyAccessor();
    }

    public boolean isPropertyAccessor() {
      return setter != null || getter != null;
    }
  }

  private LinkedHashMap<String, JsMember> collectLocalNames(JDeclaredType type) {
    if (type == null) {
      return Maps.newLinkedHashMap();
    }

    LinkedHashMap<String, JsMember> memberByLocalMemberNames = collectLocalNames(type.getSuperClass());
    for (JMember member : type.getMembers()) {
      if (isCheckedLocalName(member)) {
        updateJsMembers(memberByLocalMemberNames, member);
      }
    }
    return memberByLocalMemberNames;
  }

  private boolean isCheckedLocalName(JMember method) {
    return method.needsDynamicDispatch() && method.getJsMemberType() != JsMemberType.NONE
        && !isSyntheticBridgeMethod(method);
  }

  private boolean isSyntheticBridgeMethod(JMember member) {
    if (!(member instanceof JMethod)) {
      return false;
    }
    // A name slot taken up by a synthetic method, such as a bridge method for a generic method,
    // is not the fault of the user and so should not be reported as an error. JS generation
    // should take responsibility for ensuring that only the correct method version (in this
    // particular set of colliding method names) is exported. Forwarding synthetic methods
    // (such as an accidental override forwarding method that occurs when a JsType interface
    // starts exposing a method in class B that is only ever implemented in its parent class A)
    // though should be checked since they are exported and do take up an name slot.
    return member.isSynthetic() && !((JMethod) member).isForwarding();
  }

  private boolean isCheckedGlobalName(JMember member) {
    return !member.needsDynamicDispatch() && !member.isJsNative();
  }

  private Pair<JsMember, JsMember> updateJsMembers(
      Map<String, JsMember> memberByNames, JMember member) {
    JsMember oldJsMember = memberByNames.get(member.getJsName());
    JsMember newJsMember = createOrUpdateJsMember(oldJsMember, member);
    memberByNames.put(member.getJsName(), newJsMember);
    return Pair.create(oldJsMember, newJsMember);
  }

  private JsMember createOrUpdateJsMember(JsMember jsMember, JMember member) {
    switch (member.getJsMemberType()) {
      case GETTER:
        if (jsMember != null && jsMember.isPropertyAccessor()) {
          if (jsMember.getter == null || overrides(member, jsMember.getter)) {
            jsMember.getter = member;
            jsMember.member = member;
            return jsMember;
          }
        }
        return new JsMember(member, jsMember == null ? null : jsMember.setter, member);
      case SETTER:
        if (jsMember != null && jsMember.isPropertyAccessor()) {
          if (jsMember.setter == null || overrides(member, jsMember.setter)) {
            jsMember.setter = member;
            jsMember.member = member;
            return jsMember;
          }
        }
        return new JsMember(member, member, jsMember == null ? null : jsMember.getter);
      default:
        if (jsMember != null && !jsMember.isPropertyAccessor()) {
          if (overrides(member, jsMember.member)) {
            jsMember.member = member;
            return jsMember;
          }
        }
        return new JsMember(member);
    }
  }

  private boolean overrides(JMember member, JMember potentiallyOverriddenMember) {
    if (member instanceof JField || potentiallyOverriddenMember instanceof JField) {
      return false;
    }
    JMethod method = (JMethod) member;
    if (method.getOverriddenMethods().contains(potentiallyOverriddenMember)) {
      return true;
    }

    // Consider methods that have the same name and parameter signature to be overrides.
    // GWT models overrides similar to the JVM (not Java) in the sense that for a method to override
    // another they must have identical signatures (includes parameters and return type).
    // Methods that only differ in return types are Java overrides and need to be considered so
    // for local name collision checking.
    JMethod potentiallyOverriddenMethod = (JMethod) potentiallyOverriddenMember;

    // TODO(goktug): make this more precise to handle package visibilities.
    boolean visibilitiesMatchesForOverride =
        !method.isPackagePrivate() && !method.isPrivate()
        && !potentiallyOverriddenMethod.isPackagePrivate()
        && !potentiallyOverriddenMethod.isPrivate();

    return visibilitiesMatchesForOverride
        && method.getJsniSignature(false, false)
               .equals(potentiallyOverriddenMethod.getJsniSignature(false, false));
  }

  private static String getDescription(HasSourceInfo hasSourceInfo) {
    if (hasSourceInfo instanceof JDeclaredType) {
      return getTypeDescription((JDeclaredType) hasSourceInfo);
    } else {
      return getMemberDescription((JMember) hasSourceInfo);
    }
  }
  private static String getMemberDescription(JMember member) {
    if (member instanceof JField) {
      return String.format("'%s'", JjsUtils.getReadableDescription(member));
    }
    JMethod method = (JMethod) member;
    if ((method.isSyntheticAccidentalOverride() || method.isSynthetic())
        // Some synthetic methods are created by JDT, it is not save to assume
        // that they will always be overriding and crash the compiler.
        && !method.getOverriddenMethods().isEmpty()) {
      JMethod overridenMethod = method.getOverriddenMethods().iterator().next();
      return String.format("'%s' (exposed by '%s')",
          JjsUtils.getReadableDescription(overridenMethod),
          JjsUtils.getReadableDescription(method.getEnclosingType()));
    }
    return String.format("'%s'", JjsUtils.getReadableDescription(method));
  }

  private static String getTypeDescription(JDeclaredType type) {
    return String.format("'%s'", JjsUtils.getReadableDescription(type));
  }

  private boolean isUnusableByJsSuppressed(CanHaveSuppressedWarnings x) {
    return x.getSuppressedWarnings() != null &&
        x.getSuppressedWarnings().contains(JsInteropUtil.UNUSABLE_BY_JS);
  }

  private void logError(String format, JType type) {
    logError(type, format, JjsUtils.getReadableDescription(type));
  }

  private void logError(HasSourceInfo hasSourceInfo, String format, Object... args) {
    errorsByFilename.put(hasSourceInfo.getSourceInfo().getFileName(),
        String.format("Line %d: ", hasSourceInfo.getSourceInfo().getStartLine())
            + String.format(format, args));
  }

  private void logWarning(HasSourceInfo hasSourceInfo, String format, Object... args) {
    warningsByFilename.put(hasSourceInfo.getSourceInfo().getFileName(),
        String.format("Line %d: ", hasSourceInfo.getSourceInfo().getStartLine())
            + String.format(format, args));
  }

  private boolean reportErrorsAndWarnings(TreeLogger logger) {
    TreeSet<String> filenamesToReport = Sets.newTreeSet(
        Iterables.concat(errorsByFilename.keySet(), warningsByFilename.keySet()));
    for (String fileName : filenamesToReport) {
      boolean hasErrors = !errorsByFilename.get(fileName).isEmpty();
      TreeLogger branch = logger.branch(
          hasErrors ? Type.ERROR : Type.WARN,
          (hasErrors ? "Errors" : "Warnings") + " in " + fileName);
      for (String message : errorsByFilename.get(fileName)) {
        branch.log(Type.ERROR, message);
      }
      for (String message :warningsByFilename.get(fileName)) {
        branch.log(Type.WARN, message);
      }
    }
    return !errorsByFilename.isEmpty();
  }
}
