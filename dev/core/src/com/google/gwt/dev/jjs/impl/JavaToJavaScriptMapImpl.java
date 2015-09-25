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

import com.google.gwt.dev.jjs.ast.HasName;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.js.ast.JsExprStmt;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.thirdparty.guava.common.collect.Maps;

import java.util.List;
import java.util.Map;

/**
 * The default implementation of a JavaToJavaScriptMap.
 */
public class JavaToJavaScriptMapImpl implements JavaToJavaScriptMap {

  private final Map<HasName, JsName> names;
  private final Map<JsName, JField> fieldForName;
  private final Map<JsName, JMethod> methodForName;
  private final Map<JsName, JClassType> typeForConstructorName;

  private final Map<JsStatement, JDeclaredType> typeForStatement;
  private final Map<JsStatement, JMethod> methodForStatement;

  public JavaToJavaScriptMapImpl(List<JDeclaredType> types,
      Map<HasName, JsName> names,
      Map<JsStatement, JDeclaredType> typeForStatement,
      Map<JsStatement, JMethod> methodForStatement) {

    // Generate reverse indexes for names.
    Map<JsName, JMethod> methodsByJsName = Maps.newHashMap();
    Map<JsName, JField> staticFieldsByJsName = Maps.newHashMap();
    Map<JsName, JClassType> typesByContructorJsName = Maps.newHashMap();
    for (JDeclaredType type : types) {
      JsName typeName = names.get(type);
      if (type instanceof JClassType && typeName != null) {
        typesByContructorJsName.put(typeName, (JClassType) type);
      }
      for (JField field : type.getFields()) {
        if (field.isStatic()) {
          JsName fieldName = names.get(field);
          if (fieldName != null) {
            staticFieldsByJsName.put(fieldName, field);
          }
        }
      }
      for (JMethod method : type.getMethods()) {
        JsName methodName = names.get(method);
        if (methodName != null) {
          methodsByJsName.put(methodName, method);
        }
      }
    }

    this.names = names;
    this.fieldForName = staticFieldsByJsName;
    this.methodForName = methodsByJsName;
    this.typeForConstructorName = typesByContructorJsName;

    this.typeForStatement = typeForStatement;
    this.methodForStatement = methodForStatement;
  }

  @Override
  public JsName nameForField(JField field) {
    return names.get(field);
  }

  @Override
  public JsName nameForMethod(JMethod method) {
    return names.get(method);
  }

  @Override
  public JsName nameForType(JDeclaredType type) {
    return names.get(type);
  }

  @Override
  public JField nameToField(JsName name) {
    return fieldForName.get(name);
  }

  @Override
  public JMethod nameToMethod(JsName name) {
    return methodForName.get(name);
  }

  @Override
  public JClassType nameToType(JsName name) {
    return typeForConstructorName.get(name);
  }

  @Override
  public JDeclaredType typeForStatement(JsStatement statement) {
    return typeForStatement.get(statement);
  }

  @Override
  public JMethod methodForStatement(JsStatement statement) {
    JMethod method = methodForStatement.get(statement);
    if (JavaToJavaScriptMapImpl.class.desiredAssertionStatus()
        && method == null && statement instanceof JsExprStmt
        && ((JsExprStmt) statement).getExpression() instanceof JsFunction) {
      JsFunction function = (JsFunction) ((JsExprStmt) statement).getExpression();
      assert nameToMethod(function.getName()) == null;
    }
    return method;
  }
}
