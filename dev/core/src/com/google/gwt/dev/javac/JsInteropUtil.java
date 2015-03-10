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

import com.google.gwt.dev.jjs.ast.JConstructor;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMember;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethod.JsPropertyType;

import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
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
  public static final String JSTYPEPROTOTYPE_CLASS =
      "com.google.gwt.core.client.js.impl.PrototypeOfJsType";

  public static void maybeSetJsInteropProperties(JMethod method, Annotation... annotations) {
    setJsInteropProperties(method, annotations);
    if (JdtUtil.getAnnotation(annotations, JSPROPERTY_CLASS) != null) {
      setJsPropertyProperties(method);
    }
  }

  public static void maybeSetJsInteropProperties(JField field, Annotation... annotations) {
    setJsInteropProperties(field, annotations);
  }

  private static void setJsInteropProperties(JMember member, Annotation... annotations) {
    AnnotationBinding jsExport = JdtUtil.getAnnotation(annotations, JSEXPORT_CLASS);
    if (jsExport != null) {
      String value = JdtUtil.getAnnotationParameterString(jsExport, "value");
      setExportInfo(member, value == null ? "" : value);
    }

    /* Apply class wide JsInterop annotations */

    boolean ignore = JdtUtil.getAnnotation(annotations, JSNOEXPORT_CLASS) != null;
    if (ignore || !member.isPublic()) {
      return;
    }

    JDeclaredType enclosingType = member.getEnclosingType();

    if (enclosingType.isJsType() && member.needsVtable()) {
      member.setJsMemberName(member.getName());
    }

    if (enclosingType.isClassWideExport() && !member.needsVtable() && jsExport == null) {
      setExportInfo(member, "");
    }
  }

  private static void setJsPropertyProperties(JMethod method) {
    String methodName = method.getName();
    if (method.getParams().isEmpty()) {
      if (startsWithCamelCase(method.getName(), "has")) {
        // Has
        method.setJsPropertyType(JsPropertyType.HAS);
        method.setJsMemberName(Introspector.decapitalize(methodName.substring(3)));
      } else {
        // Getter
        method.setJsPropertyType(JsPropertyType.GET);
        if (startsWithCamelCase(methodName, "is")) {
          method.setJsMemberName(Introspector.decapitalize(methodName.substring(2)));
        } else if (startsWithCamelCase(methodName, "get")) {
          method.setJsMemberName(Introspector.decapitalize(methodName.substring(3)));
        } else {
          method.setJsMemberName(methodName);
        }
      }
    } else {
      // Setter
      method.setJsPropertyType(JsPropertyType.SET);
      if (startsWithCamelCase(methodName, "set")) {
        method.setJsMemberName(Introspector.decapitalize(methodName.substring(3)));
      } else {
        method.setJsMemberName(methodName);
      }
    }
  }

  // TODO(goktug): Move other namespace logic to here as well after we get access to package
  // annotations in GwtAstBuilder.
  private static void setExportInfo(JMember member, String exportName) {
    if (exportName.isEmpty()) {
      member.setExportInfo(null, computeExportName(member));
    } else {
      int split = exportName.lastIndexOf('.');
      if (split == -1) {
        member.setExportInfo("", exportName);
      } else {
        member.setExportInfo(exportName.substring(0, split), exportName.substring(split + 1));
      }
    }
  }

  private static String computeExportName(JMember member) {
    // Constructors for nested class might have different name in the AST than the original source
    // so use the simple name of the enclosing type instead that always matches the source name.
    return member instanceof JConstructor ? member.getEnclosingType().getSimpleName()
        : member.getName();
  }

  public static boolean isJsType(TypeDeclaration x) {
    return JdtUtil.getAnnotation(x.annotations, JSTYPE_CLASS) != null;
  }

  public static boolean isJsPrototypeFlag(TypeDeclaration x) {
    return JdtUtil.getAnnotation(x.annotations, JSTYPEPROTOTYPE_CLASS) != null;
  }

  public static boolean isClassWideJsExport(TypeDeclaration x) {
    return JdtUtil.getAnnotation(x.annotations, JSEXPORT_CLASS) != null;
  }

  public static String maybeGetJsNamespace(TypeDeclaration x) {
    AnnotationBinding jsNamespace = JdtUtil.getAnnotation(x.annotations, JSNAMESPACE_CLASS);
    return JdtUtil.getAnnotationParameterString(jsNamespace, "value");
  }

  public static String maybeGetJsTypePrototype(TypeDeclaration x) {
    AnnotationBinding jsType = JdtUtil.getAnnotation(x.annotations, JSTYPE_CLASS);
    return JdtUtil.getAnnotationParameterString(jsType, "prototype");
  }

  public static boolean isJsFunction(TypeDeclaration x) {
    return JdtUtil.getAnnotation(x.annotations, JSFUNCTION_CLASS) != null;
  }

  private static boolean startsWithCamelCase(String string, String prefix) {
    return string.length() > prefix.length() && string.startsWith(prefix)
        && Character.isUpperCase(string.charAt(prefix.length()));
  }
}
