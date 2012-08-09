/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.validation.rebind;

import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JRawType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.thirdparty.guava.common.base.Function;

import javax.validation.metadata.BeanDescriptor;
import javax.validation.metadata.PropertyDescriptor;

/**
 * <strong>EXPERIMENTAL</strong> and subject to change. Do not use this in
 * production code.
 * <p>
 * A simple struct for the various values associated with a Bean that can be
 * validated.
 */
public final class BeanHelper {

  public static final Function<BeanHelper, Class<?>> TO_CLAZZ = 
      new Function<BeanHelper, Class<?>>() {
    public Class<?> apply(BeanHelper helper) {
      return helper.getClazz();
    }
  };

  private final BeanDescriptor beanDescriptor;

  private final JClassType jClass;

  private final Class<?> clazz;

  /**
   * Shouldn't be created directly; instead use BeanHelperCache.
   */
  BeanHelper(JClassType jClass, Class<?> clazz, BeanDescriptor beanDescriptor) {
    this.beanDescriptor = beanDescriptor;
    this.jClass = jClass;
    this.clazz = clazz;
  }

  public JClassType getAssociationType(PropertyDescriptor p, boolean useField) {
    JType type = this.getElementType(p, useField);
    JArrayType jArray = type.isArray();
    if (jArray != null) {
      return jArray.getComponentType().isClassOrInterface();
    }
    JParameterizedType pType = type.isParameterized();
    JClassType[] typeArgs;
    if (pType == null) {
      JRawType rType = type.isRawType();
      typeArgs = rType.getGenericType().getTypeParameters();
    } else {
      typeArgs = pType.getTypeArgs();
    }
    // it is either a Iterable or a Map use the last type arg.
    return typeArgs[typeArgs.length - 1].isClassOrInterface();
  }

  public BeanDescriptor getBeanDescriptor() {
    return beanDescriptor;
  }

  /*
   * The server side validator needs an actual class.
   */
  public Class<?> getClazz() {
    return clazz;
  }

  public String getFullyQualifiedValidatorName() {
    return getPackage() + "." + getValidatorName();
  }

  public JClassType getJClass() {
    return jClass;
  }

  public String getPackage() {
    return jClass.getPackage().getName();
  }

  public String getTypeCanonicalName() {
    return jClass.getQualifiedSourceName();
  }

  public String getValidatorInstanceName() {
    return getFullyQualifiedValidatorName() + ".INSTANCE";
  }

  public String getValidatorName() {
    return makeJavaSafe("_" + jClass.getName() + "Validator");
  }

  @Override
  public String toString() {
    return getTypeCanonicalName();
  }

  JType getElementType(PropertyDescriptor p, boolean useField) {
    if (useField) {
      return jClass.findField(p.getPropertyName()).getType();
    } else {
      return jClass.findMethod(GwtSpecificValidatorCreator.asGetter(p),
          GwtSpecificValidatorCreator.NO_ARGS).getReturnType();
    }
  }

  boolean hasField(PropertyDescriptor p) {
    JField field = jClass.findField(p.getPropertyName());
    return field != null;
  }

  boolean hasGetter(PropertyDescriptor p) {
    JType[] paramTypes = new JType[]{};
    try {
      jClass.getMethod(GwtSpecificValidatorCreator.asGetter(p), paramTypes);
      return true;
    } catch (NotFoundException e) {
      return false;
    }
  }

  private String makeJavaSafe(String in) {
    return in.replaceAll("\\.", "_");
  }

}
