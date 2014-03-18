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
package com.google.gwt.dev.javac;

import com.google.gwt.dev.jdt.SafeASTVisitor;
import com.google.gwt.dev.util.InstalledHelpInfo;
import com.google.gwt.dev.util.collect.HashSet;
import com.google.gwt.thirdparty.guava.common.base.Strings;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * Check a compilation unit for violations of
 * {@link com.google.gwt.core.client.JavaScriptObject JavaScriptObject} (JSO)
 * restrictions. The restrictions are summarized in
 * <code>jsoRestrictions.html</code>.
 *
 *
 * Any violations found are attached as errors on the
 * CompilationUnitDeclaration.
 *
 * @see <a
 *      href="http://code.google.com/p/google-web-toolkit/wiki/OverlayTypes">Overlay
 *      types design doc</a>
 * @see jsoRestrictions.html
 */
public class JSORestrictionsChecker {

  public static final String ERR_JSINTERFACE_ONLY_ON_INTERFACES = "@JsInterface only permitted on interfaces";
  public static final String ERR_JSINTERFACE_OVERLOADS_NOT_ALLOWED =
      "JsInterface methods cannot overload another method.";
  public static final String ERR_JSEXPORT_ONLY_CTORS_AND_STATIC_METHODS =
      "@JsExport may only be applied to constructors and static methods.";
  public static final String ERR_JSPROPERTY_ONLY_BEAN_OR_FLUENT_STYLE_NAMING =
      "@JsProperty is only allowed on JavaBean-style or fluent-style named methods";
  public static final String ERR_MUST_EXTEND_MAGIC_PROTOTYPE_CLASS =
      "Classes implementing @JsInterface with a prototype must extend that interface's Prototype class";
  public static final String ERR_CLASS_EXTENDS_MAGIC_PROTOTYPE_BUT_NO_PROTOTYPE_ATTRIBUTE =
      "Classes implementing a @JsInterface without a prototype should not extend the Prototype class";
  public static final String ERR_JSEXPORT_USED_ON_JSINTERFACE =
      "@JsExport used on @JsInterface interface, instead of implementing class";
  public static final String ERR_JSPROPERTY_ONLY_ON_INTERFACES =
      "@JsProperty not allowed on concrete class methods";
  public static final String ERR_CONSTRUCTOR_WITH_PARAMETERS =
      "Constructors must not have parameters in subclasses of JavaScriptObject";
  public static final String ERR_INSTANCE_FIELD = "Instance fields cannot be used in subclasses of JavaScriptObject";
  public static final String ERR_INSTANCE_METHOD_NONFINAL =
      "Instance methods must be 'final' in non-final subclasses of JavaScriptObject";
  public static final String ERR_IS_NONSTATIC_NESTED = "Nested classes must be 'static' if they extend JavaScriptObject";
  public static final String ERR_NEW_JSO =
      "'new' cannot be used to create instances of JavaScriptObject subclasses; instances must originate in JavaScript";
  public static final String ERR_NONEMPTY_CONSTRUCTOR =
      "Constructors must be totally empty in subclasses of JavaScriptObject";
  public static final String ERR_NONPROTECTED_CONSTRUCTOR =
      "Constructors must be 'protected' in subclasses of JavaScriptObject";
  public static final String ERR_OVERRIDDEN_METHOD =
      "Methods cannot be overridden in JavaScriptObject subclasses";
  public static final String JSO_CLASS = "com/google/gwt/core/client/JavaScriptObject";
  public static final String ERR_FORGOT_TO_MAKE_PROTOTYPE_IMPL_JSINTERFACE = "@JsInterface subtype extends magic _Prototype class, but _Prototype class doesn't implement JsInterface";
  public static final String ERR_SUBCLASSING_NATIVE_NOT_ALLOWED = "Subclassing prototypes of native browser prototypes not allowed.";
  static boolean LINT_MODE = false;

  private enum ClassState {
    NORMAL, JSO, JSINTERFACE, JSINTERFACE_IMPL;
  }

  /**
   * The order in which the checker will process types is undefined, so this
   * type accumulates the information necessary for sanity-checking the JSO
   * types.
   */
  public static class CheckerState {

    private final Map<String, String> interfacesToJsoImpls = new HashMap<String, String>();

    public void addJsoInterface(TypeDeclaration jsoType,
        CompilationUnitDeclaration cud, ReferenceBinding interf) {
      String intfName = CharOperation.toString(interf.compoundName);
      String alreadyImplementor = interfacesToJsoImpls.get(intfName);
      String myName = CharOperation.toString(jsoType.binding.compoundName);

      if (!areInSameModule(jsoType, interf)) {
        String msg = errMustBeDefinedInTheSameModule(intfName,myName);
        errorOn(jsoType, cud, msg);
        return;
      }

      if (alreadyImplementor != null) {
        String msg = errAlreadyImplemented(intfName, alreadyImplementor, myName);
        errorOn(jsoType, cud, msg);
        return;
      }

      interfacesToJsoImpls.put(intfName, myName);
    }

    // TODO(rluble): (Separate compilation) Implement a real check that a JSO must is defined in
    // the same module as the interface(s) it implements. Depends on upcoming JProgram changes.
    private boolean areInSameModule(TypeDeclaration jsoType, ReferenceBinding interf) {
      return true;
    }

    public String getJsoImplementor(ReferenceBinding binding) {
      String name = CharOperation.toString(binding.compoundName);
      return interfacesToJsoImpls.get(name);
    }

    public boolean isJsoInterface(ReferenceBinding binding) {
      String name = CharOperation.toString(binding.compoundName);
      return interfacesToJsoImpls.containsKey(name);
    }
  }

  private class JSORestrictionsVisitor extends SafeASTVisitor implements
      ClassFileConstants {

    private final Stack<ClassState> classStateStack = new Stack<ClassState>();

    @Override
    public void endVisit(AllocationExpression exp, BlockScope scope) {
      // In rare cases we might not be able to resolve the expression.
      if (exp.type == null) {
        return;
      }
      TypeBinding resolvedType = exp.resolvedType;
      if (resolvedType == null) {
        if (scope == null) {
          return;
        }
        resolvedType = exp.type.resolveType(scope);
      }
      // Anywhere an allocation occurs is wrong.
      if (isJsoSubclass(resolvedType)) {
        errorOn(exp, ERR_NEW_JSO);
      }
    }

    @Override
    public void endVisit(ConstructorDeclaration meth, ClassScope scope) {
      if (!isJso()) {
        return;
      }
      if ((meth.arguments != null) && (meth.arguments.length > 0)) {
        errorOn(meth, ERR_CONSTRUCTOR_WITH_PARAMETERS);
      }
      if ((meth.modifiers & AccProtected) == 0) {
        errorOn(meth, ERR_NONPROTECTED_CONSTRUCTOR);
      }
      if (meth.statements != null && meth.statements.length > 0) {
        errorOn(meth, ERR_NONEMPTY_CONSTRUCTOR);
      }
    }

    @Override
    public void endVisit(FieldDeclaration field, MethodScope scope) {
      if (!isJso()) {
        return;
      }
      if (!field.isStatic()) {
        errorOn(field, ERR_INSTANCE_FIELD);
      }
    }

    @Override
    public void endVisit(MethodDeclaration meth, ClassScope scope) {
      if (!isJso()) {
        return;
      }
      if ((meth.modifiers & (AccFinal | AccPrivate | AccStatic)) == 0) {
        // The method's modifiers allow it to be overridden. Make
        // one final check to see if the surrounding class is final.
        if ((meth.scope == null) || !meth.scope.enclosingSourceType().isFinal()) {
          errorOn(meth, ERR_INSTANCE_METHOD_NONFINAL);
        }
      }

      // Should not have to check isStatic() here, but isOverriding() appears
      // to be set for static methods.
      if (!meth.isStatic()
          && (meth.binding != null && meth.binding.isOverriding())) {
        errorOn(meth, ERR_OVERRIDDEN_METHOD);
      }
    }

    @Override
    public void endVisit(TypeDeclaration type, ClassScope scope) {
      popState();
    }

    @Override
    public void endVisit(TypeDeclaration type, CompilationUnitScope scope) {
      popState();
    }

    @Override
    public void endVisitValid(TypeDeclaration type, BlockScope scope) {
      popState();
    }

    @Override
    public boolean visit(TypeDeclaration type, ClassScope scope) {
      pushState(checkType(type));
      return true;
    }

    @Override
    public boolean visit(TypeDeclaration type, CompilationUnitScope scope) {
      pushState(checkType(type));
      return true;
    }

    @Override
    public boolean visitValid(TypeDeclaration type, BlockScope scope) {
      pushState(checkType(type));
      return true;
    }

    private void checkJsInterface(TypeDeclaration type, TypeBinding typeBinding) {
      ReferenceBinding binding = (ReferenceBinding) typeBinding;
      if (!binding.isInterface()) {
        errorOn(type, ERR_JSINTERFACE_ONLY_ON_INTERFACES);
      }

      Set<String> methodNames = new HashSet<String>();
      checkJsInterfaceMethodsForOverloads(methodNames, binding);
      for (MethodBinding mb : binding.methods()) {
        checkJsProperty(mb, true);
        checkJsExport(mb, false);
      }
    }

    private void checkJsExport(MethodBinding mb, boolean allowed) {
      AnnotationBinding jsExport = JdtUtil.getAnnotation(mb, JsInteropUtil.JSEXPORT_CLASS);
      if (jsExport != null && allowed) {
        if (!mb.isConstructor() && !mb.isStatic()) {
          errorOn(mb.sourceMethod(), ERR_JSEXPORT_ONLY_CTORS_AND_STATIC_METHODS);
        }
      }
      if (jsExport != null && !allowed) {
        errorOn(mb.sourceMethod(), ERR_JSEXPORT_USED_ON_JSINTERFACE);
      }
    }

    private void checkJsProperty(MethodBinding mb, boolean allowed) {
      AnnotationBinding jsProperty = JdtUtil.getAnnotation(mb, JsInteropUtil.JSPROPERTY_CLASS);
      if (jsProperty != null) {
        if (!allowed) {
          errorOn(mb.sourceMethod(), ERR_JSPROPERTY_ONLY_ON_INTERFACES);
          return;
        }
        String methodName = String.valueOf(mb.selector);
        if (!isGetter(methodName, mb) && !isSetter(methodName, mb) && !isHas(methodName, mb)) {
          errorOn(mb.sourceMethod(), ERR_JSPROPERTY_ONLY_BEAN_OR_FLUENT_STYLE_NAMING);
        }
      }
    }

    private boolean isGetter(String name, MethodBinding mb) {
      // zero arg non-void getX()
      if (name.length() > 3 && name.startsWith("get") && Character.isUpperCase(name.charAt(3)) &&
         mb.returnType == TypeBinding.VOID && mb.parameters.length == 0) {
        return true;
      } else  if (name.length() > 3 && name.startsWith("is")
          && Character.isUpperCase(name.charAt(2)) &&  mb.returnType == TypeBinding.BOOLEAN
          && mb.parameters.length == 0) {
        return true;
      } else if (mb.parameters.length == 0 && mb.returnType != TypeBinding.VOID) {
        return true;
      }
      return false;
    }

    private boolean isSetter(String name, MethodBinding mb) {
      if (mb.returnType == TypeBinding.VOID || mb.returnType == mb.declaringClass) {
        if (name.length() > 3 && name.startsWith("set") && Character.isUpperCase(name.charAt(3))
            && mb.parameters.length == 1) {
          return true;
        } else if (mb.parameters.length == 1) {
          return true;
        }
      }
      return false;
    }

    private boolean isHas(String name, MethodBinding mb) {
      if (name.length() > 3 && name.startsWith("has") && Character.isUpperCase(name.charAt(3))
          && mb.parameters.length == 0 && mb.returnType == TypeBinding.BOOLEAN) {
        return true;
      }
      return false;
    }

    private void checkJsInterfaceMethodsForOverloads(Set<String> methodNames, ReferenceBinding binding) {
      for (MethodBinding mb : binding.methods()) {
        String methodName = String.valueOf(mb.selector);
        if (mb.isConstructor()) {
          continue;
        }
        if (JdtUtil.getAnnotation(mb, JsInteropUtil.JSPROPERTY_CLASS) != null) {
          if (isGetter(methodName, mb) || isSetter(methodName, mb) || isHas(methodName, mb)) {
            // js properties are allowed to be overloaded (setter/getter)
            continue;
          }
        }
        if (!methodNames.add(methodName)) {
          errorOn(mb.sourceMethod(), ERR_JSINTERFACE_OVERLOADS_NOT_ALLOWED);
        }
      }
      for (ReferenceBinding rb : binding.superInterfaces()) {
        checkJsInterfaceMethodsForOverloads(methodNames, rb);
      }
    }

    private ClassState checkType(TypeDeclaration type) {
      SourceTypeBinding binding = type.binding;

      if (isJsInterface(type.binding)) {
        checkJsInterface(type, type.binding);
        return ClassState.JSINTERFACE;
      }

      if (checkClassImplementingJsInterface(type)) {
        return ClassState.JSINTERFACE_IMPL;
      }

      if (!isJsoSubclass(binding)) {
        return ClassState.NORMAL;
      }

      if (type.enclosingType != null && !binding.isStatic()) {
        errorOn(type, ERR_IS_NONSTATIC_NESTED);
      }

      ReferenceBinding[] interfaces = binding.superInterfaces();
      if (interfaces != null) {
        for (ReferenceBinding interf : interfaces) {
          if (interf.methods() == null) {
            continue;
          }

          if (interf.methods().length > 0) {
            // See if any of my superTypes implement it.
            ReferenceBinding superclass = binding.superclass();
            if (superclass == null
                || !superclass.implementsInterface(interf, true)) {
              state.addJsoInterface(type, cud, interf);
            }
          }
        }
      }

      return ClassState.JSO;
    }

    private boolean checkClassImplementingJsInterface(TypeDeclaration type) {
      ReferenceBinding jsInterface = findNearestJsInterfaceRecursive(type.binding);
      if (jsInterface == null) {
        return false;
      }

      for (MethodBinding mb : type.binding.methods()) {
        checkJsExport(mb, true);
        checkJsProperty(mb, false);
      }

      AnnotationBinding jsinterfaceAnn = JdtUtil.getAnnotation(jsInterface,
          JsInteropUtil.JSINTERFACE_CLASS);
      String jsPrototype = JdtUtil.getAnnotationParameterString(jsinterfaceAnn, "prototype");
      boolean isNative = JdtUtil.getAnnotationParameterBoolean(jsinterfaceAnn, "isNative");
      if (!Strings.isNullOrEmpty(jsPrototype)) {
        checkClassExtendsMagicPrototype(type, jsInterface, !isNative, isNative);
      } else {
        checkClassExtendsMagicPrototype(type, jsInterface, false, isNative);
      }

      // TODO(cromwellian) add multiple-inheritance checks when ambiguity in spec is resolved
      return true;
    }

    private void checkClassExtendsMagicPrototype(TypeDeclaration type, ReferenceBinding jsInterface,
                                                 boolean shouldExtend, boolean isNative) {
      ReferenceBinding superClass = type.binding.superclass();
      // if type is the _Prototype stub (implements JsInterface) exit
      if (isMagicPrototype(type.binding, jsInterface)) {
        return;
      } else if (isMagicPrototypeStub(type)) {
        errorOn(type, ERR_FORGOT_TO_MAKE_PROTOTYPE_IMPL_JSINTERFACE);
      }

      if (shouldExtend) {
        // super class should be SomeInterface.Prototype, so enclosing type should match the jsInterface
        if (LINT_MODE && (superClass == null || !isMagicPrototype(superClass, jsInterface))) {
          errorOn(type, ERR_MUST_EXTEND_MAGIC_PROTOTYPE_CLASS);
        }
      } else {
        if (superClass != null && isMagicPrototype(superClass, jsInterface)) {
          if (!isNative) {
            errorOn(type, ERR_CLASS_EXTENDS_MAGIC_PROTOTYPE_BUT_NO_PROTOTYPE_ATTRIBUTE);
          } else {
            errorOn(type, ERR_SUBCLASSING_NATIVE_NOT_ALLOWED);
          }
        }
      }
    }

    // Roughly parallels JProgram.isJsInterfacePrototype()
    private boolean isMagicPrototype(ReferenceBinding type, ReferenceBinding jsInterface) {
      if (isMagicPrototypeStub(type)) {
        for (ReferenceBinding intf : type.superInterfaces()) {
          if (intf == jsInterface) {
            return true;
          }
        }
      }
      return false;
    }

    private boolean isMagicPrototypeStub(TypeDeclaration type) {
      return isMagicPrototypeStub(type.binding);
    }

    private boolean isMagicPrototypeStub(ReferenceBinding binding) {
      return JdtUtil.getAnnotation(binding, JsInteropUtil.JSINTERFACEPROTOTYPE_CLASS) != null;
    }

    /**
     * Walks up chain of interfaces and superinterfaces to find the first one marked with @JsInterface.
     */
    private ReferenceBinding findNearestJsInterface(ReferenceBinding binding, boolean mustHavePrototype) {
      if (isJsInterface(binding)) {
        return binding;
      }

      for (ReferenceBinding intb : binding.superInterfaces()) {
        ReferenceBinding checkSuperInt = findNearestJsInterface(intb, false);
        if (checkSuperInt != null) {
          return checkSuperInt;
        }
      }
      return null;
    }

    private ReferenceBinding findNearestJsInterfaceRecursive(ReferenceBinding binding) {
      ReferenceBinding nearest = findNearestJsInterface(binding, false);
      if (nearest != null) {
        return nearest;
      } else if (binding.superclass() != null) {
        return findNearestJsInterfaceRecursive(binding.superclass());
      }
      return null;
    }

    private boolean isJso() {
      return classStateStack.peek() == ClassState.JSO;
    }

    private void popState() {
      classStateStack.pop();
    }

    private void pushState(ClassState cstate) {
      classStateStack.push(cstate);
    }
  }


  /**
   * Checks an entire
   * {@link org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration}.
   *
   */
  public static void check(CheckerState state, CompilationUnitDeclaration cud) {
    JSORestrictionsChecker checker = new JSORestrictionsChecker(state, cud);
    checker.check();
  }

  /**
   * Returns {@code true} if {@code typeBinding} is {@code JavaScriptObject} or
   * any subtype.
   */
  public static boolean isJso(TypeBinding typeBinding) {
    if (!(typeBinding instanceof ReferenceBinding)) {
      return false;
    }
    ReferenceBinding binding = (ReferenceBinding) typeBinding;
    while (binding != null) {
      if (JSO_CLASS.equals(String.valueOf(binding.constantPoolName()))) {
        return true;
      }
      binding = binding.superclass();
    }
    return false;
  }

  /**
   * Returns the first JsInterface annotation encountered traversing the type hierarchy upwards from the type.
   */
  private boolean isJsInterface(TypeBinding typeBinding) {

    if (!(typeBinding instanceof ReferenceBinding) || !(typeBinding instanceof SourceTypeBinding)) {
      return false;
    }

    AnnotationBinding jsInterface = JdtUtil.getAnnotation(typeBinding, JsInteropUtil.JSINTERFACE_CLASS);
    return jsInterface != null;
  }

  /**
   * Returns {@code true} if {@code typeBinding} is a subtype of
   * {@code JavaScriptObject}, but not {@code JavaScriptObject} itself.
   */
  public static boolean isJsoSubclass(TypeBinding typeBinding) {
    if (!(typeBinding instanceof ReferenceBinding)) {
      return false;
    }
    ReferenceBinding binding = (ReferenceBinding) typeBinding;
    return isJso(binding.superclass());
  }

  static String errAlreadyImplemented(String intfName, String impl1,
      String impl2) {
    return "Only one JavaScriptObject type may implement the methods of an "
        + "interface that declared methods. The interface (" + intfName
        + ") is implemented by both (" + impl1 + ") and (" + impl2 + ")";
  }

  // TODO(rluble): (Separate compilation) It would be nice to have the actual module names here.
  static String errMustBeDefinedInTheSameModule(String intfName, String jsoImplementation) {
    return "A JavaScriptObject type may only implement an interface that is defined in the same"
        + " module. The interface (" + intfName + ") and  the JavaScriptObject type (" +
        jsoImplementation + ") are defined different modules";
  }

  private static void errorOn(ASTNode node, CompilationUnitDeclaration cud,
      String error) {
    GWTProblem.recordError(node, cud, error, new InstalledHelpInfo(
        "jsoRestrictions.html"));
  }

  private final CompilationUnitDeclaration cud;

  private final CheckerState state;

  private JSORestrictionsChecker(CheckerState state,
      CompilationUnitDeclaration cud) {
    this.cud = cud;
    this.state = state;
  }

  private void check() {
    cud.traverse(new JSORestrictionsVisitor(), cud.scope);
  }

  private void errorOn(ASTNode node, String error) {
    errorOn(node, cud, error);
  }
}
