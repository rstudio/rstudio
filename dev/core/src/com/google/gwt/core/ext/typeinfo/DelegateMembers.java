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
package com.google.gwt.core.ext.typeinfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Class that initializes the different members of a
 * {@link JDelegatingClassType} from its corresponding base type on demand.
 */
class DelegateMembers extends AbstractMembers {
  private final JClassType baseType;
  private List<JConstructor> lazyConstructors;
  private Map<String, JField> lazyFields;
  private Map<String, List<JMethod>> lazyMethods;
  private final Substitution substitution;

  /**
   */
  public DelegateMembers(JDelegatingClassType enclosingType,
      JClassType baseType, Substitution substitution) {
    super(enclosingType);
    this.baseType = baseType;
    this.substitution = substitution;
  }

  @Override
  protected List<JConstructor> doGetConstructors() {
    if (lazyConstructors != null) {
      /*
       * Return if the constructors are being initialized or have been
       * initialized.
       */
      return lazyConstructors;
    }
    lazyConstructors = new ArrayList<JConstructor>();

    JConstructor[] baseCtors = baseType.getConstructors();
    for (JConstructor baseCtor : baseCtors) {
      JConstructor newCtor = new JConstructor(getParentType(), baseCtor);
      initializeParams(baseCtor, newCtor);
      addConstructor(newCtor);
    }

    return lazyConstructors;
  }

  @Override
  protected Map<String, JField> doGetFields() {
    if (lazyFields != null) {
      /*
       * Return if the fields are being initialized or have been initialized.
       */
      return lazyFields;
    }
    lazyFields = new LinkedHashMap<String, JField>();

    JField[] baseFields = baseType.getFields();
    for (JField baseField : baseFields) {
      JField newField = new JField(getParentType(), baseField);
      newField.setType(substitution.getSubstitution(baseField.getType()));
      addField(newField);
    }

    return lazyFields;
  }

  @Override
  protected Map<String, List<JMethod>> doGetMethods() {
    if (lazyMethods != null) {
      /*
       * Return if the methods are being initialized or have been initialized.
       */
      return lazyMethods;
    }
    lazyMethods = new LinkedHashMap<String, List<JMethod>>();

    JMethod[] baseMethods = baseType.getMethods();
    for (JMethod baseMethod : baseMethods) {
      JMethod newMethod = new JMethod(getParentType(), baseMethod);
      initializeParams(baseMethod, newMethod);
      newMethod.setReturnType(substitution.getSubstitution(baseMethod.getReturnType()));
      initializeExceptions(baseMethod, newMethod);
      addMethod(newMethod);
    }

    return lazyMethods;
  }

  private void initializeExceptions(JAbstractMethod srcMethod,
      JAbstractMethod newMethod) {
    for (JType thrown : srcMethod.getThrows()) {
      // exceptions cannot be parameterized; just copy them over
      newMethod.addThrows(thrown);
    }
  }

  private void initializeParams(JAbstractMethod srcMethod,
      JAbstractMethod newMethod) {
    for (JParameter srcParam : srcMethod.getParameters()) {
      JParameter newParam = new JParameter(newMethod, srcParam);
      newParam.setType(substitution.getSubstitution(srcParam.getType()));
      newMethod.addParameter(newParam);
    }
  }
}
