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

import com.google.gwt.dev.jjs.ast.HasJsInfo.JsMemberType;
import com.google.gwt.dev.jjs.ast.JConstructor;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMember;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;

import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding;

/**
 * Utility functions to interact with JDT classes for JsInterop.
 */
public final class JsInteropUtil {
  public static final String UNUSABLE_BY_JS = "unusable-by-js";
  public static final String INVALID_JSNAME = "<invalid>";

  public static boolean isGlobal(String jsNamespace) {
    return "<global>".equals(jsNamespace);
  }

  public static void maybeSetJsInteropProperties(JDeclaredType type, Annotation[] annotations) {
    AnnotationBinding jsType = getInteropAnnotation(annotations, "JsType");
    String namespace = JdtUtil.getAnnotationParameterString(jsType, "namespace");
    String name = JdtUtil.getAnnotationParameterString(jsType, "name");
    boolean isJsNative = JdtUtil.getAnnotationParameterBoolean(jsType, "isNative", false);

    AnnotationBinding jsPackage = getInteropAnnotation(annotations, "JsPackage");
    String packageNamespace = JdtUtil.getAnnotationParameterString(jsPackage, "namespace");
    if (packageNamespace != null) {
      namespace = packageNamespace;
    }

    boolean isJsType = jsType != null;
    boolean isJsFunction = getInteropAnnotation(annotations, "JsFunction") != null;
    type.setJsTypeInfo(isJsType, isJsNative, isJsFunction, namespace, name, isJsType);
  }

  public static void maybeSetJsInteropProperties(
      JMethod method, boolean generateExport, Annotation... annotations) {
    AnnotationBinding annotation = getInteropAnnotation(annotations, "JsMethod");
    if (annotation == null) {
      annotation = getInteropAnnotation(annotations, "JsConstructor");
    }
    if (annotation == null) {
      annotation = getInteropAnnotation(annotations, "JsProperty");
    }

    boolean isPropertyAccessor = getInteropAnnotation(annotations, "JsProperty") != null;
    setJsInteropProperties(method, annotations, annotation, isPropertyAccessor, generateExport);
  }

  public static void maybeSetJsInteropProperties(JParameter parameter,  Annotation... annotations) {
    if (getInteropAnnotation(annotations, "JsOptional") != null) {
      parameter.setOptional();
    }
  }

  public static void maybeSetJsInteropProperties(
      JField field, boolean generateExport, Annotation... annotations) {
    AnnotationBinding annotation = getInteropAnnotation(annotations, "JsProperty");
    setJsInteropProperties(field, annotations, annotation, false, generateExport);
  }

  private static void setJsInteropProperties(JMember member, Annotation[] annotations,
      AnnotationBinding memberAnnotation, boolean isAccessor, boolean generateExport) {
    if (getInteropAnnotation(annotations, "JsOverlay") != null) {
      member.setJsOverlay();
    }

    if (getInteropAnnotation(annotations, "JsIgnore") != null) {
      return;
    }

    boolean isPublicMemberForJsType = member.getEnclosingType().isJsType() && member.isPublic();
    boolean memberForNativeType = member.getEnclosingType().isJsNative();
    if (memberAnnotation == null
        && (!isPublicMemberForJsType && !memberForNativeType || member.isJsOverlay())) {
      return;
    }

    String namespace = JdtUtil.getAnnotationParameterString(memberAnnotation, "namespace");
    String name = JdtUtil.getAnnotationParameterString(memberAnnotation, "name");
    JsMemberType memberType = getJsMemberType(member, isAccessor);
    member.setJsMemberInfo(memberType, namespace, name, generateExport);
  }

  private static JsMemberType getJsMemberType(JMember member, boolean isPropertyAccessor) {
    if (member instanceof JField) {
      return JsMemberType.PROPERTY;
    }
    if (member instanceof JConstructor) {
      return JsMemberType.CONSTRUCTOR;
    }
    if (isPropertyAccessor) {
      return getJsPropertyAccessorType((JMethod) member);
    }
    return JsMemberType.METHOD;
  }

  private static JsMemberType getJsPropertyAccessorType(JMethod method) {
    if (method.getParams().size() == 1 && method.getType() == JPrimitiveType.VOID) {
      return JsMemberType.SETTER;
    } else if (method.getParams().isEmpty() && method.getType() != JPrimitiveType.VOID) {
      return JsMemberType.GETTER;
    }
    return JsMemberType.UNDEFINED_ACCESSOR;
  }

  private static AnnotationBinding getInteropAnnotation(Annotation[] annotations, String name) {
    return JdtUtil.getAnnotation(annotations, "jsinterop.annotations." + name);
  }
}
