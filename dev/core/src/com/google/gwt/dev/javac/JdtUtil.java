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
package com.google.gwt.dev.javac;

import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.util.Name.InternalName;
import com.google.gwt.thirdparty.guava.common.base.Function;
import com.google.gwt.thirdparty.guava.common.base.Joiner;
import com.google.gwt.thirdparty.guava.common.base.Predicate;
import com.google.gwt.thirdparty.guava.common.base.Strings;
import com.google.gwt.thirdparty.guava.common.collect.FluentIterable;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableSet;
import com.google.gwt.thirdparty.guava.common.collect.Iterables;
import com.google.gwt.thirdparty.guava.common.collect.Lists;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.impl.BooleanConstant;
import org.eclipse.jdt.internal.compiler.impl.StringConstant;
import org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding;
import org.eclipse.jdt.internal.compiler.lookup.ElementValuePair;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.LocalTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.NestedTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.SyntheticArgumentBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Utility functions to interact with JDT classes.
 */
public final class JdtUtil {
  private static final String JSO_CLASS = "com/google/gwt/core/client/JavaScriptObject";

  /**
   * Returns a source name from an array of names.
   */
  public static String asDottedString(char[][] name) {
    return join(name, ".");
  }

  /**
   * Returns a string name from an array of names using {@code separator}.
   */
  public static String join(char[][] name, String separator) {
    StringBuilder result = new StringBuilder();
    if (name.length > 0) {
      result.append(name[0]);
    }

    for (int i = 1; i < name.length; ++i) {
      result.append(separator);
      result.append(name[i]);
    }
    return result.toString();
  }

  /**
   * Returns the name of the class from reference binding.
   * <p>
   * JDT Core (at least 3.11.0.v20150407) returns <code>$Local$</code> synthetic name for local
   * classes.<br>
   * This method aware about local classes and instead of <code>$Local$</code> synthetic name
   * returns fully qualified name of the local class in form of anonymous class (e.g.
   * <code>test.Class1$2</code>).
   * </p>
   */
  public static String getClassName(ReferenceBinding binding) {
    if (binding instanceof LocalTypeBinding) {
      // Using here constantPoolName() instead of coumpoundName due JDT not computing the
      // right compoundName for lambdas inside local class.
      return InternalName.toBinaryName(String.valueOf(binding.constantPoolName()));
    }

    return asDottedString(binding.compoundName);
  }

  /**
   * Returns the top type of the compilation unit that defines
   * {@code binding}.
   */
  public static String getDefiningCompilationUnitType(ReferenceBinding binding) {
    // Get the compilation unit type name.
    // TODO(rluble): check that this is valid for classes declared in the same compilation unit
    // top scope.
    return asDottedString(binding.outermostEnclosingType().compoundName);
  }

  public static String getSourceName(TypeBinding classBinding) {
    return getSourceName(CharOperation.charToString(classBinding.qualifiedPackageName()),
        CharOperation.charToString(classBinding.qualifiedSourceName()));
  }

  public static String getSourceName(String qualifiedPackageName, String qualifiedSourceName) {
    return Joiner.on(".").skipNulls().join(new String[] {
        Strings.emptyToNull(qualifiedPackageName),
        qualifiedSourceName});
  }

  public static String getBinaryName(TypeBinding classBinding) {
    return getBinaryName(CharOperation.charToString(classBinding.qualifiedPackageName()),
        CharOperation.charToString(classBinding.qualifiedSourceName()));
  }

  public static String getBinaryName(String qualifiedPackageName, String qualifiedSourceName) {
    return Joiner.on(".").skipNulls().join(new String[] {
        Strings.emptyToNull(qualifiedPackageName),
        qualifiedSourceName.replace('.','$')});
  }

  public static boolean isInnerClass(ReferenceBinding binding) {
    return binding.isNestedType() && !binding.isStatic();
  }

  /**
   * Get a readable method description from {@code methodBinding} conforming with JSNI formatting.
   * <p>
   * See examples:
   * <ul>
   * <li>a constructor of class A with a java.lang.String parameter will be formatted as
   * "new(Ljava/lang/String;).</li>
   * <li>a method with name m with an parameter of class java.lang.Object and return type boolean
   * will be formatted as "m(Ljava/lang/Object;</li>
   * </ul>,
   */
  public static String formatMethodSignature(MethodBinding methodBinding) {
    ReferenceBinding declaringClassBinding = methodBinding.declaringClass;
    StringBuilder methodNameWithSignature = new StringBuilder();
    String methodName = String.valueOf(methodBinding.selector);
    List<TypeBinding> parameterTypeBindings = Lists.newArrayList();
    if (methodName.equals("<init>")) {
      // It is a constructor.
      // (1) use the JSNI methodName instead of <init>.
      methodName = "new";
      // (2) add the implicit constructor parameters types for non static inner classes.
      if (isInnerClass(declaringClassBinding)) {
        NestedTypeBinding nestedBinding = (NestedTypeBinding) declaringClassBinding;
        if (nestedBinding.enclosingInstances != null) {
          for (SyntheticArgumentBinding argumentBinding : nestedBinding.enclosingInstances) {
            parameterTypeBindings.add(argumentBinding.type);
          }
        }
      }
    }

    parameterTypeBindings.addAll(Arrays.asList(methodBinding.parameters));
    methodNameWithSignature.append(methodName);
    methodNameWithSignature.append("(");
    for (TypeBinding parameterTypeBinding : parameterTypeBindings) {
      methodNameWithSignature.append(parameterTypeBinding.signature());
    }
    methodNameWithSignature.append(")");
    return methodNameWithSignature.toString();
  }

  public static String formatBinding(MethodBinding methodBinding) {
    String accessModifier = null;
    if (methodBinding.isProtected()) {
      accessModifier = "protected";
    } else if (methodBinding.isPrivate()) {
      accessModifier = "private";
    } else if (methodBinding.isPublic()) {
      accessModifier = "public";
    }
    return Joiner.on(" ").skipNulls().join(
        accessModifier,
        methodBinding.isStatic() ? "static" : null,
        getSourceName(methodBinding.declaringClass) + "." +
            formatMethodSignature(methodBinding)
    );
  }

  private JdtUtil() {
  }

  public static String getAnnotationParameterString(
      AnnotationBinding annotationBinding, String parameterName) {
    if (annotationBinding != null) {
      for (ElementValuePair parameterNameValuePair : annotationBinding.getElementValuePairs()) {
        if (parameterNameValuePair.getValue() instanceof StringConstant &&
            parameterName.equals(String.valueOf(parameterNameValuePair.getName()))) {
          return ((StringConstant) parameterNameValuePair.getValue()).stringValue();
        }
      }
    }
    return null;
  }

  public static boolean getAnnotationParameterBoolean(
      AnnotationBinding annotationBinding, String parameterName, boolean defaultValue) {
    Boolean booleanParameterValue = getAnnotationParameterBoolean(annotationBinding, parameterName);
    return booleanParameterValue == null ? defaultValue : booleanParameterValue;
  }

  public static Boolean getAnnotationParameterBoolean(
      AnnotationBinding annotationBinding, String parameterName) {
    if (annotationBinding != null) {
      for (ElementValuePair parameterNameValuePair : annotationBinding.getElementValuePairs()) {
        if (parameterNameValuePair.getValue() instanceof BooleanConstant &&
            parameterName.equals(String.valueOf(parameterNameValuePair.getName()))) {
          return ((BooleanConstant) parameterNameValuePair.getValue()).booleanValue();
        }
      }
    }
    return null;
  }

  public static AnnotationBinding getAnnotationByName(Annotation[] annotations, String name) {
    if (annotations == null) {
      return null;
    }
    for (Annotation annotation : annotations) {
      AnnotationBinding annotationBinding = annotation.getCompilerAnnotation();
      if (matchAnnotationName(annotationBinding, name)) {
        return annotationBinding;
      }
    }
    return null;
  }

  public static AnnotationBinding getAnnotationByName(
      AnnotationBinding[] annotationsBindings, String name) {
    if (annotationsBindings == null) {
      return null;
    }
    for (AnnotationBinding annotationBinding : annotationsBindings) {
      if (matchAnnotationName(annotationBinding, name)) {
        return annotationBinding;
      }
    }
    return null;
  }

  private static boolean matchAnnotationName(AnnotationBinding annotationBinding, String name) {
    if (annotationBinding == null) {
      return false;
    }
    return name.equals(CharOperation.toString(annotationBinding.getAnnotationType().compoundName));
  }

  public static TypeBinding getAnnotationParameterTypeBinding(
      AnnotationBinding annotationBinding, String parameterName) {
    if (annotationBinding != null) {
      for (ElementValuePair parameterNameValuePair : annotationBinding.getElementValuePairs()) {
        if (parameterNameValuePair.getValue() instanceof Class &&
            parameterName.equals(String.valueOf(parameterNameValuePair.getName()))) {
          return (TypeBinding) parameterNameValuePair.getValue();
        }
      }
    }
    return null;
  }

  public static TypeBinding[] getAnnotationParameterTypeBindingArray(
      AnnotationBinding annotationBinding, String parameterName) {
    if (annotationBinding == null) {
      return null;
    }

    for (ElementValuePair parameterNameValuePair : annotationBinding.getElementValuePairs()) {
      Object value = parameterNameValuePair.getValue();
      if (!parameterName.equals(String.valueOf(parameterNameValuePair.getName()))) {
        continue;
      }
      if (value instanceof Object[]) {
        Object[] values = (Object[]) value;
        TypeBinding bindings[] = new TypeBinding[values.length];
        System.arraycopy(values, 0, bindings, 0, values.length);
        return bindings;
      }
      assert value instanceof TypeBinding;
      return new TypeBinding[] {(TypeBinding) value};
    }
    return null;
  }

  public static StringConstant[] getAnnotationParameterStringConstantArray(
      AnnotationBinding annotationBinding, String parameterName) {
    if (annotationBinding == null) {
      return null;
    }
    for (ElementValuePair parameterNameValuePair : annotationBinding.getElementValuePairs()) {
      if (!parameterName.equals(String.valueOf(parameterNameValuePair.getName()))) {
        continue;
      }
      Object value = parameterNameValuePair.getValue();
      if (value instanceof Object[]) {
        Object[] values = (Object[]) value;
        StringConstant[] stringConstants = new StringConstant[values.length];
        System.arraycopy(values, 0, stringConstants, 0, values.length);
        return stringConstants;
      }
      assert value instanceof StringConstant;
      return new StringConstant[] {(StringConstant) value};
    }
    return null;
  }

  public static Set<String> getSuppressedWarnings(Annotation[] annotations) {
    if (annotations == null) {
      return ImmutableSet.of();
    }
    AnnotationBinding suppressWarnings =
        getAnnotationByName(annotations, SuppressWarnings.class.getName());
    if (suppressWarnings != null) {
      StringConstant[] values =
          JdtUtil.getAnnotationParameterStringConstantArray(suppressWarnings, "value");
      return FluentIterable.from(Arrays.asList(values))
          .transform(new Function<StringConstant, String>() {
            @Override
            public String apply(StringConstant value) {
              return value.stringValue();
            }
          })
          .toSet();
    }
    return ImmutableSet.of();
  }

  public static String signature(FieldBinding binding) {
    StringBuilder sb = new StringBuilder();
    sb.append(binding.declaringClass.constantPoolName());
    sb.append('.');
    sb.append(binding.name);
    sb.append(':');
    sb.append(binding.type.signature());
    return sb.toString();
  }

  public static String signature(MethodBinding binding) {
    StringBuilder sb = new StringBuilder();
    sb.append(binding.declaringClass.constantPoolName());
    sb.append('.');
    sb.append(binding.selector);
    sb.append('(');
    for (TypeBinding paramType : binding.parameters) {
      sb.append(paramType.signature());
    }
    sb.append(')');
    sb.append(binding.returnType.signature());
    return sb.toString();
  }

  public static String signature(TypeBinding binding) {
    if (binding.isBaseType()) {
      return String.valueOf(binding.sourceName());
    } else {
      return String.valueOf(binding.constantPoolName());
    }
  }

  public static void setClassDispositionFromBinding(SourceTypeBinding binding, JDeclaredType type) {
    if (binding.isNestedType()) {
      if (isLocalClass(binding)) {
        type.setClassDisposition(JDeclaredType.NestedClassDisposition.LOCAL);
      } else if (binding.isAnonymousType()) {
        type.setClassDisposition(JDeclaredType.NestedClassDisposition.ANONYMOUS);
      } else if (isInnerClass(binding)) {
        type.setClassDisposition(JDeclaredType.NestedClassDisposition.INNER);
      } else if (isStaticClass(binding)) {
        type.setClassDisposition(JDeclaredType.NestedClassDisposition.STATIC);
      }
    } else {
      type.setClassDisposition(JDeclaredType.NestedClassDisposition.TOP_LEVEL);
    }
  }

  public static boolean isLocalClass(SourceTypeBinding binding) {
    return binding.isLocalType() && !binding.isAnonymousType();
  }

  public static boolean isStaticClass(SourceTypeBinding binding) {
    return binding.isNestedType() && binding.isStatic();
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

  public static Iterable<ReferenceBinding> getSuperInterfacesRequiringInitialization(
      ReferenceBinding type) {
    Iterable<ReferenceBinding> interfaces = Collections.emptyList();
    for (ReferenceBinding interfaceType : type.superInterfaces()) {
      interfaces =
          Iterables.concat(interfaces, getSuperInterfacesRequiringInitialization(interfaceType));
      if (hasDefaultMethods(interfaceType)) {
        interfaces = Iterables.concat(interfaces, Collections.singleton(interfaceType));
      }
    }
    return interfaces;
  }

  private static boolean hasDefaultMethods(ReferenceBinding interfaceType) {
    return Iterables.any(Arrays.asList(interfaceType.methods()), new Predicate<MethodBinding>() {
      @Override
      public boolean apply(MethodBinding methodBinding) {
        return methodBinding.isDefaultMethod();
      }
    });
  }
}
