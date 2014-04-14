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
package com.google.gwt.dev.javac.typemodel;

import com.google.gwt.dev.util.collect.Lists;
import com.google.gwt.dev.util.collect.Maps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A container for methods and fields.
 */
class Members extends AbstractMembers {
  /**
   * Implementation note: cannot be lazily computed because unlike
   * {@link DelegateMembers}, this serves as the real internal container for
   * real classes.
   */

  private List<JConstructor> constructors = Lists.create();
  private Map<String, JField> fieldMap = Maps.create();
  private List<JField> fields = Lists.create();
  private Map<String, Object> methodMap = Maps.create();
  private List<String> methodOrder = Lists.create();
  private Map<String, JClassType> nestedTypes = Maps.create();

  public Members(JClassType classType) {
    super(classType);
  }

  @Override
  public JField findField(String name) {
    return fieldMap.get(name);
  }

  @Override
  public JField[] getFields() {
    return fields.toArray(TypeOracle.NO_JFIELDS);
  }

  @SuppressWarnings("unchecked")
  @Override
  public JMethod[] getMethods() {
    List<JMethod> result = new ArrayList<JMethod>();
    for (String methodName : methodOrder) {
      Object object = methodMap.get(methodName);
      if (object instanceof JMethod) {
        result.add((JMethod) object);
      } else {
        result.addAll((List<JMethod>) object);
      }
    }
    return result.toArray(TypeOracle.NO_JMETHODS);
  }

  @SuppressWarnings("unchecked")
  @Override
  public JMethod[] getOverloads(String name) {
    Object object = methodMap.get(name);
    if (object == null) {
      return TypeOracle.NO_JMETHODS;
    } else if (object instanceof JMethod) {
      return new JMethod[]{(JMethod) object};
    } else {
      List<JMethod> overloads = (List<JMethod>) object;
      return overloads.toArray(TypeOracle.NO_JMETHODS);
    }
  }

  @Override
  protected void addConstructor(JConstructor ctor) {
    assert (!constructors.contains(ctor));
    constructors = Lists.add(constructors, ctor);
  }

  @Override
  protected void addField(JField field) {
    assert !fieldMap.containsKey(field.getName());
    fieldMap = Maps.put(fieldMap, field.getName(), field);
    fields = Lists.add(fields, field);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void addMethod(JMethod method) {
    String methodName = method.getName();
    Object object = methodMap.get(methodName);
    if (object == null) {
      methodMap = Maps.put(methodMap, methodName, method);
      methodOrder = Lists.add(methodOrder, methodName);
    } else if (object instanceof JMethod) {
      List<JMethod> overloads = new ArrayList<JMethod>(2);
      overloads.add((JMethod) object);
      overloads.add(method);
      methodMap = Maps.put(methodMap, methodName, overloads);
    } else {
      List<JMethod> overloads = (List<JMethod>) object;
      overloads.add(method);
    }
  }

  @Override
  protected List<JConstructor> doGetConstructors() {
    return constructors;
  }

  @Override
  protected Map<String, JClassType> doGetNestedTypes() {
    return nestedTypes;
  }

  void addNestedType(JClassType type) {
    nestedTypes = Maps.put(nestedTypes, type.getSimpleSourceName(), type);
  }

}