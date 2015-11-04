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

import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JConstructor;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JMember;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethod.JsPropertyAccessorType;
import com.google.gwt.thirdparty.guava.common.base.Strings;

import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding;

import java.beans.Introspector;

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
    String exportName = maybeGetJsExportName(annotations, "");
    String jsPrototype = JdtUtil.getAnnotationParameterString(jsType, "prototype");
    boolean isJsNative = jsPrototype != null;
    if (isJsNative) {
      int indexOf = jsPrototype.lastIndexOf(".");
      namespace = indexOf == -1 ? "" : jsPrototype.substring(0, indexOf);
      exportName = jsPrototype.substring(indexOf + 1);
    }
    boolean isJsType = jsType != null;
    boolean isClassWideExport = exportName != null;
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
    setJsInteropProperties(method, annotations);
    if (JdtUtil.getAnnotation(annotations, JSPROPERTY_CLASS) != null) {
      setJsPropertyProperties(method);
    }
  }

  public static void maybeSetJsInteropPropertiesNew(JMethod method, Annotation... annotations) {
    AnnotationBinding annotation = getInteropAnnotation(annotations, "JsMethod");
    if (getInteropAnnotation(annotations, "JsOverlay") != null) {
      method.setJsOverlay();
    }

    if (annotation == null) {
      annotation = getInteropAnnotation(annotations, "JsConstructor");
    }
    if (annotation == null) {
      annotation = getInteropAnnotation(annotations, "JsProperty");
    }
    setJsInteropPropertiesNew(method, annotations, annotation);
    if (getInteropAnnotation(annotations, "JsProperty") != null) {
      setJsPropertyProperties(method);
    }
  }

  public static void maybeSetJsInteropProperties(JField field, Annotation... annotations) {
    setJsInteropProperties(field, annotations);
  }

  public static void maybeSetJsInteropPropertiesNew(JField field, Annotation... annotations) {
    AnnotationBinding annotation = getInteropAnnotation(annotations, "JsProperty");
    setJsInteropPropertiesNew(field, annotations, annotation);
  }

  private static void setJsInteropProperties(JMember member, Annotation... annotations) {
    String namespace = maybeGetJsNamespace(annotations);
    String exportName = maybeGetJsExportName(annotations, computeName(member));
    member.setJsMemberInfo(namespace, exportName, exportName != null);

    /* Apply class wide JsInterop annotations */

    boolean ignore = JdtUtil.getAnnotation(annotations, JSNOEXPORT_CLASS) != null;
    if (ignore || (!member.isPublic() && !isNativeConstructor(member)) || exportName != null) {
      return;
    }

    JDeclaredType enclosingType = member.getEnclosingType();

    if (enclosingType.isJsType() && member.needsDynamicDispatch()) {
      member.setJsMemberInfo(namespace, computeName(member), true);
    }

    if (enclosingType.isClassWideExport() && !member.needsDynamicDispatch()) {
      member.setJsMemberInfo(namespace, computeName(member), true);
    }
  }

  private static boolean isNativeConstructor(JMember member) {
    return member instanceof JConstructor && member.getEnclosingType().isJsNative();
  }

  private static void setJsInteropPropertiesNew(
      JMember member, Annotation[] annotations, AnnotationBinding memberAnnotation) {
    if (getInteropAnnotation(annotations, "JsIgnore") != null) {
      return;
    }

    boolean isPublicMemberForJsType = member.getEnclosingType().isJsType() && member.isPublic();
    if (!isPublicMemberForJsType && !isNativeConstructor(member) && memberAnnotation == null) {
      return;
    }

    String namespace = JdtUtil.getAnnotationParameterString(memberAnnotation, "namespace");
    String name = JdtUtil.getAnnotationParameterString(memberAnnotation, "name");
    member.setJsMemberInfo(namespace, name == null ? computeName(member) : name, true);
  }

  private static void setJsPropertyProperties(JMethod method) {
    String methodName = method.getName();
    if (startsWithCamelCase(methodName, "set")) {
      String jsName = Introspector.decapitalize(methodName.substring(3));
      method.setJsPropertyInfo(jsName, JsPropertyAccessorType.SETTER);
    } else if (startsWithCamelCase(methodName, "get")) {
      String jsName = Introspector.decapitalize(methodName.substring(3));
      method.setJsPropertyInfo(jsName, JsPropertyAccessorType.GETTER);
    } else if (startsWithCamelCase(methodName, "is")) {
      String jsName = Introspector.decapitalize(methodName.substring(2));
      method.setJsPropertyInfo(jsName, JsPropertyAccessorType.GETTER);
    } else {
      method.setJsPropertyInfo(INVALID_JSNAME, JsPropertyAccessorType.UNDEFINED);
    }
  }

  private static AnnotationBinding getInteropAnnotation(Annotation[] annotations, String name) {
    return JdtUtil.getAnnotation(annotations, "jsinterop.annotations." + name);
  }

  private static String computeName(JMember member) {
    return member instanceof JConstructor ? "" : member.getName();
  }

  private static String maybeGetJsNamespace(Annotation[] annotations) {
    AnnotationBinding jsNamespace = JdtUtil.getAnnotation(annotations, JSNAMESPACE_CLASS);
    return JdtUtil.getAnnotationParameterString(jsNamespace, "value");
  }

  private static String maybeGetJsExportName(Annotation[] annotations, String calculatedName) {
    AnnotationBinding jsExport = JdtUtil.getAnnotation(annotations, JSEXPORT_CLASS);
    if (jsExport == null) {
      return null;
    }
    String value = JdtUtil.getAnnotationParameterString(jsExport, "value");
    return Strings.isNullOrEmpty(value) ? calculatedName : value;
  }

  private static boolean startsWithCamelCase(String string, String prefix) {
    return string.length() > prefix.length() && string.startsWith(prefix)
        && Character.isUpperCase(string.charAt(prefix.length()));
  }
}
