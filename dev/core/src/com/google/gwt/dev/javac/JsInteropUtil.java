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
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JMethod;

import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding;

/**
 * Utility functions to interact with JDT classes for JsInterop.
 */
public final class JsInteropUtil {

  public static final String JSEXPORT_CLASS = "com.google.gwt.core.client.js.JsExport";
  public static final String JSPROPERTY_CLASS = "com.google.gwt.core.client.js.JsProperty";
  public static final String JSINTERFACE_CLASS = "com.google.gwt.core.client.js.JsInterface";
  public static final String JSINTERFACEPROTOTYPE_CLASS =
      "com.google.gwt.core.client.js.impl.PrototypeOfJsInterface";

  public static void maybeSetExportedField(FieldDeclaration x, JField field) {
    if (x.annotations != null) {
      AnnotationBinding jsExport = JdtUtil.getAnnotation(x.binding, JSEXPORT_CLASS);
      if (jsExport != null) {
        field.setExportName(JdtUtil.getAnnotationParameterString(jsExport, "value"));
      }
    }
  }

  public static void maybeSetJsinteropMethodProperties(AbstractMethodDeclaration x,
      JMethod method) {
    if (x.annotations != null) {
      AnnotationBinding jsExport = JdtUtil.getAnnotation(x.binding, JSEXPORT_CLASS);
      AnnotationBinding jsProperty = JdtUtil.getAnnotation(x.binding, JSPROPERTY_CLASS);
      if (jsExport != null) {
        method.setExportName(JdtUtil.getAnnotationParameterString(jsExport, "value"));
      }
      if (jsProperty != null) {
        method.setJsProperty(true);
      }
    }
  }

  public static JInterfaceType.JsInteropType maybeGetJsInterfaceType(TypeDeclaration x,
      String jsPrototype, JInterfaceType.JsInteropType interopType) {
    if (x.annotations != null) {
      AnnotationBinding jsInterface = JdtUtil.getAnnotation(x.binding, JSINTERFACE_CLASS);
      if (jsInterface != null) {
        boolean isNative = JdtUtil.getAnnotationParameterBoolean(jsInterface, "isNative");
        interopType = jsPrototype != null ?
            (isNative ? JInterfaceType.JsInteropType.NATIVE_PROTOTYPE :
            JInterfaceType.JsInteropType.JS_PROTOTYPE) : JInterfaceType.JsInteropType.NO_PROTOTYPE;
      }
    }
    return interopType;
  }

  public static String maybeGetJsInterfacePrototype(TypeDeclaration x, String jsPrototype) {
    if (x.annotations != null) {
      AnnotationBinding jsInterface = JdtUtil.getAnnotation(x.binding, JSINTERFACE_CLASS);
      if (jsInterface != null) {
        jsPrototype = JdtUtil.getAnnotationParameterString(jsInterface, "prototype");
      }
    }
    return jsPrototype;
  }

  public static void maybeSetJsPrototypeFlag(TypeDeclaration x, JClassType type) {
    if (JdtUtil.getAnnotation(x.binding, JSINTERFACEPROTOTYPE_CLASS) != null) {
      ((JClassType) type).setJsPrototypeStub(true);
    }
  }
}
