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
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JConstructor;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JMember;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;

import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding;

/**
 * Utility functions to interact with JDT classes for JsInterop.
 */
public final class JsInteropUtil {

  public static final String JSEXPORT_CLASS = "com.google.gwt.core.client.js.JsExport";
  public static final String JSFUNCTION_CLASS = "com.google.gwt.core.client.js.JsFunction";
  public static final String JSNAMESPACE_CLASS = "com.google.gwt.core.client.js.JsNamespace";
  public static final String JSNOEXPORT_CLASS = "com.google.gwt.core.client.js.JsNoExport";
  public static final String JSPROPERTY_CLASS = "com.google.gwt.core.client.js.JsProperty";
  public static final String JSTYPE_CLASS = "com.google.gwt.core.client.js.JsType";
  public static final String UNUSABLE_BY_JS = "unusable-by-js";
  public static final String INVALID_JSNAME = "<invalid>";

  public static void maybeSetJsInteropProperties(JDeclaredType type, Annotation... annotations) {
    AnnotationBinding jsType = JdtUtil.getAnnotation(annotations, JSTYPE_CLASS);
    String namespace = maybeGetJsNamespace(annotations);
    String exportName = maybeGetJsExportName(annotations);
    String jsPrototype = JdtUtil.getAnnotationParameterString(jsType, "prototype");
    boolean isJsNative = jsPrototype != null;
    if (isJsNative) {
      int indexOf = jsPrototype.lastIndexOf(".");
      namespace = indexOf == -1 ? "" : jsPrototype.substring(0, indexOf);
      exportName = jsPrototype.substring(indexOf + 1);
    }
    boolean isJsType = jsType != null;
    boolean isClassWideExport =
        isJsNative || JdtUtil.getAnnotation(annotations, JSEXPORT_CLASS) != null;
    boolean isJsFunction = JdtUtil.getAnnotation(annotations, JSFUNCTION_CLASS) != null;
    boolean canBeImplementedExternally =
        (type instanceof JInterfaceType && (isJsType || isJsFunction))
        || (type instanceof JClassType && isJsNative);
    type.setJsTypeInfo(isJsType, isJsNative, isJsFunction, namespace, exportName, isClassWideExport,
        canBeImplementedExternally);
  }

  public static void maybeSetJsInteropPropertiesNew(JDeclaredType type, Annotation[] annotations) {
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
    boolean canBeImplementedExternally = isJsNative || isJsFunction;
    type.setJsTypeInfo(isJsType, isJsNative, isJsFunction, namespace, name, isJsType,
        canBeImplementedExternally);
  }

  public static void maybeSetJsInteropProperties(JMethod method, Annotation... annotations) {
    boolean isPropertyAccessor = JdtUtil.getAnnotation(annotations, JSPROPERTY_CLASS) != null;
    setJsInteropProperties(method, annotations, isPropertyAccessor);
  }

  public static void maybeSetJsInteropPropertiesNew(
      JMethod method, boolean generateExport, Annotation... annotations) {
    AnnotationBinding annotation = getInteropAnnotation(annotations, "JsMethod");
    if (annotation == null) {
      annotation = getInteropAnnotation(annotations, "JsConstructor");
    }
    if (annotation == null) {
      annotation = getInteropAnnotation(annotations, "JsProperty");
    }

    boolean isPropertyAccessor = getInteropAnnotation(annotations, "JsProperty") != null;
    setJsInteropPropertiesNew(method, annotations, annotation, isPropertyAccessor, generateExport);
  }

  public static void maybeSetJsInteropProperties(JField field, Annotation... annotations) {
    if (field.getEnclosingType().isJsNative() && field.isCompileTimeConstant()) {
      field.setJsOverlay();
    }
    setJsInteropProperties(field, annotations, false);
  }

  public static void maybeSetJsInteropPropertiesNew(
      JField field, boolean generateExport, Annotation... annotations) {
    AnnotationBinding annotation = getInteropAnnotation(annotations, "JsProperty");
    setJsInteropPropertiesNew(field, annotations, annotation, false, generateExport);
  }

  private static void setJsInteropProperties(
      JMember member, Annotation[] annotations, boolean isPropertyAccessor) {
    boolean hasExport = JdtUtil.getAnnotation(annotations, JSEXPORT_CLASS) != null;
    JsMemberType memberType = getJsMemberType(member, isPropertyAccessor);
    String namespace = maybeGetJsNamespace(annotations);
    String exportName = maybeGetJsExportName(annotations);
    if (hasExport) {
      member.setJsMemberInfo(memberType, namespace, exportName, true);
      return;
    }

    /* Apply class wide JsInterop annotations */

    boolean ignore = JdtUtil.getAnnotation(annotations, JSNOEXPORT_CLASS) != null;
    if (ignore || (!member.isPublic() && !member.getEnclosingType().isJsNative())) {
      return;
    }

    JDeclaredType enclosingType = member.getEnclosingType();

    if (enclosingType.isJsType() && member.needsDynamicDispatch()) {
      member.setJsMemberInfo(memberType, namespace, exportName, true);
    }

    if (enclosingType.isClassWideExport() && !member.needsDynamicDispatch()) {
      member.setJsMemberInfo(memberType, namespace, exportName, true);
    }
  }

  private static void setJsInteropPropertiesNew(JMember member, Annotation[] annotations,
      AnnotationBinding memberAnnotation, boolean isAccessor, boolean generateExport) {
    if (getInteropAnnotation(annotations, "JsOverlay") != null) {
      member.setJsOverlay();
    }

    if (getInteropAnnotation(annotations, "JsIgnore") != null) {
      return;
    }

    boolean isPublicMemberForJsType = member.getEnclosingType().isJsType() && member.isPublic();
    boolean memberForNativeType = member.getEnclosingType().isJsNative();
    if (!isPublicMemberForJsType && !memberForNativeType && memberAnnotation == null) {
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

  private static String maybeGetJsNamespace(Annotation[] annotations) {
    AnnotationBinding jsNamespace = JdtUtil.getAnnotation(annotations, JSNAMESPACE_CLASS);
    return JdtUtil.getAnnotationParameterString(jsNamespace, "value");
  }

  private static String maybeGetJsExportName(Annotation[] annotations) {
    AnnotationBinding annotation = JdtUtil.getAnnotation(annotations, JSEXPORT_CLASS);
    return JdtUtil.getAnnotationParameterString(annotation, "value");
  }
}
