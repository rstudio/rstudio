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

import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JDeclaredType.JsInteropType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.js.ast.JsNameRef;

import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding;

/**
 * Utility functions to interact with JDT classes for JsInterop.
 */
public final class JsInteropUtil {

  public static final String JSEXPORT_CLASS = "com.google.gwt.core.client.js.JsExport";
  public static final String JSNAMESPACE_CLASS = "com.google.gwt.core.client.js.JsNamespace";
  public static final String JSNOEXPORT_CLASS = "com.google.gwt.core.client.js.JsNoExport";
  public static final String JSPROPERTY_CLASS = "com.google.gwt.core.client.js.JsProperty";
  public static final String JSTYPE_CLASS = "com.google.gwt.core.client.js.JsType";
  public static final String JSTYPEPROTOTYPE_CLASS =
      "com.google.gwt.core.client.js.impl.PrototypeOfJsType";

  public static void maybeSetExportedField(FieldDeclaration x, JField field) {
    if (x.annotations != null) {
      AnnotationBinding jsExport = JdtUtil.getAnnotation(x.binding, JSEXPORT_CLASS);
      if (jsExport != null) {
        String value = JdtUtil.getAnnotationParameterString(jsExport, "value");
        if (value == null) {
          value = ""; // JDT bug? returns null sometimes instead of "" for default value
        }
        field.setExportName(value);
      }
      if (JdtUtil.getAnnotation(x.binding, JSNOEXPORT_CLASS) != null) {
        field.setNoExport(true);
      }
    }
  }

  public static void maybeSetJsinteropMethodProperties(AbstractMethodDeclaration x,
      JMethod method) {
    if (x.annotations != null) {
      AnnotationBinding jsExport = JdtUtil.getAnnotation(x.binding, JSEXPORT_CLASS);
      AnnotationBinding jsProperty = JdtUtil.getAnnotation(x.binding, JSPROPERTY_CLASS);
      if (jsExport != null) {
        String value = JdtUtil.getAnnotationParameterString(jsExport, "value");
        if (value == null) {
          // JDT bug? returns null instead of "" sometimes for default
          value = "";
        }
        method.setExportName(value);
      }
      if (jsProperty != null) {
        method.setJsProperty(true);
      }
      if (JdtUtil.getAnnotation(x.binding, JSNOEXPORT_CLASS) != null) {
        method.setNoExport(true);
      }
    }
  }

  public static JsInteropType maybeGetJsInteropType(TypeDeclaration x, String jsPrototype) {
    if (x.annotations != null) {
      AnnotationBinding jsInterface = JdtUtil.getAnnotation(x.binding, JSTYPE_CLASS);
      if (jsInterface != null) {
        return jsPrototype.isEmpty() ? JsInteropType.NO_PROTOTYPE : JsInteropType.JS_PROTOTYPE;
      }
    }
    return JsInteropType.NONE;
  }

  public static String maybeGetJsTypePrototype(TypeDeclaration x) {
    if (x.annotations != null) {
      AnnotationBinding jsType = JdtUtil.getAnnotation(x.binding, JSTYPE_CLASS);
      if (jsType != null) {
        String value = JdtUtil.getAnnotationParameterString(jsType, "prototype");
        if (value != null) {
          return value;
        }
      }
    }
    return "";
  }

  public static void maybeSetJsPrototypeFlag(TypeDeclaration x, JClassType type) {
    if (JdtUtil.getAnnotation(x.binding, JSTYPEPROTOTYPE_CLASS) != null) {
      type.setJsPrototypeStub(true);
    }
  }

  public static boolean isClassWideJsExport(TypeDeclaration x) {
    if (x.annotations != null) {
      AnnotationBinding jsExport = JdtUtil.getAnnotation(x.binding, JSEXPORT_CLASS);
      if (jsExport != null) {
        return true;
      }
    }
    return false;
  }

  public static void maybeSetJsNamespace(JDeclaredType type, TypeDeclaration x) {
    if (x.annotations != null) {
      AnnotationBinding jsNamespace = JdtUtil.getAnnotation(x.binding, JSNAMESPACE_CLASS);
      if (jsNamespace != null) {
        type.setJsNamespace(JdtUtil.getAnnotationParameterString(jsNamespace, "value"));
      }
    }
  }

  public static JsNameRef convertQualifiedPrototypeToNameRef(SourceInfo sourceInfo, String jsPrototype) {
    JsNameRef ref = new JsNameRef(sourceInfo, "$wnd");
    for (String part : jsPrototype.split("\\.")) {
      JsNameRef newRef = new JsNameRef(sourceInfo, part);
      newRef.setQualifier(ref);
      ref = newRef;
    }
    return ref;
  }
}
