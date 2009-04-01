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
package com.google.gwt.i18n.rebind;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.core.ext.typeinfo.TypeOracleException;
import com.google.gwt.i18n.rebind.AbstractResource.ResourceList;
import com.google.gwt.i18n.shared.GwtLocale;
import com.google.gwt.user.rebind.SourceWriter;

import java.util.Map;

/**
 * Creates the class implementation for a given resource bundle using the
 * standard <code>AbstractGeneratorClassCreator</code>.
 */
class ConstantsImplCreator extends AbstractLocalizableImplCreator {
  /**
   * Does a Map need to be generated in order to store complex results?
   */
  private boolean needCache = false;

  /**
   * Constructor for <code>ConstantsImplCreator</code>.
   * 
   * @param logger logger to print errors
   * @param writer <code>Writer</code> to print to
   * @param localizableClass class/interface to conform to
   * @param resourceList resource bundle used to generate the class
   * @param oracle types
   * @throws UnableToCompleteException
   */
  public ConstantsImplCreator(TreeLogger logger, SourceWriter writer,
      JClassType localizableClass, ResourceList resourceList, TypeOracle oracle)
      throws UnableToCompleteException {
    super(logger, writer, localizableClass, resourceList, true);
    try {
      JClassType stringClass = oracle.getType(String.class.getName());
      JClassType mapClass = oracle.getType(Map.class.getName());
      JType stringArrayClass = oracle.getArrayType(stringClass);
      JType intClass = oracle.parse(int.class.getName());
      JType doubleClass = oracle.parse(double.class.getName());
      JType floatClass = oracle.parse(float.class.getName());
      JType booleanClass = oracle.parse(boolean.class.getName());
      register(stringClass, new SimpleValueMethodCreator(this,
          SimpleValueMethodCreator.STRING));
      register(mapClass, new ConstantsMapMethodCreator(this));
      register(intClass, new SimpleValueMethodCreator(this,
          SimpleValueMethodCreator.INT));
      register(doubleClass, new SimpleValueMethodCreator(this,
          SimpleValueMethodCreator.DOUBLE));
      register(floatClass, new SimpleValueMethodCreator(this,
          SimpleValueMethodCreator.FLOAT));
      register(booleanClass, new SimpleValueMethodCreator(this,
          SimpleValueMethodCreator.BOOLEAN));

      register(stringArrayClass, new ConstantsStringArrayMethodCreator(this));
    } catch (NotFoundException e) {
      throw error(logger, e);
    } catch (TypeOracleException e) {
      throw error(logger, e);
    }
  }

  /**
   * Checks that the method has the right structure to implement
   * <code>Constant</code>.
   * 
   * @param method method to check
   */
  protected void checkConstantMethod(TreeLogger logger, JMethod method)
      throws UnableToCompleteException {
    if (method.getParameters().length > 0) {
      throw error(logger,
          "Methods in interfaces extending Constant must have no parameters");
    }
    checkReturnType(logger, method);
  }

  /**
   * @param logger
   * @param method
   * @throws UnableToCompleteException
   */
  protected void checkReturnType(TreeLogger logger, JMethod method)
      throws UnableToCompleteException {
    JType returnType = method.getReturnType();
    JPrimitiveType primitive = returnType.isPrimitive();
    if (primitive != null
        && (primitive == JPrimitiveType.BOOLEAN
            || primitive == JPrimitiveType.DOUBLE
            || primitive == JPrimitiveType.FLOAT || primitive == JPrimitiveType.INT)) {
      return;
    }
    JArrayType arrayType = returnType.isArray();
    if (arrayType != null) {
      String arrayComponent = arrayType.getComponentType().getQualifiedSourceName();
      if (!arrayComponent.equals("java.lang.String")) {
        throw error(logger,
            "Methods in interfaces extending Constant only support arrays of Strings");
      }
      return;
    }
    String returnTypeName = returnType.getQualifiedSourceName();
    if (returnTypeName.equals("java.lang.String")) {
      return;
    }
    if (returnTypeName.equals("java.util.Map")) {
      JParameterizedType paramType = returnType.isParameterized();
      if (paramType != null) {
        JClassType[] typeArgs = paramType.getTypeArgs();
        if (typeArgs.length != 2
            || !typeArgs[0].getQualifiedSourceName().equals("java.lang.String")
            || !typeArgs[1].getQualifiedSourceName().equals("java.lang.String")) {
          throw error(logger,
              "Map Methods in interfaces extending Constant must be raw or <String, String>");
        }
      }
      return;
    }
    throw error(logger,
        "Methods in interfaces extending Constant must have a return type of "
            + "String/int/float/boolean/double/String[]/Map");
  }

  @Override
  protected void classEpilog() {
    if (isNeedCache()) {
      getWriter().println("java.util.Map cache = new java.util.HashMap();");
    }
  }

  /**
   * Create the method body associated with the given method. Arguments are
   * arg0...argN.
   */
  @Override
  protected void emitMethodBody(TreeLogger logger, JMethod method,
      GwtLocale locale) throws UnableToCompleteException {
    checkConstantMethod(logger, method);
    delegateToCreator(logger, method, locale);
  }

  boolean isNeedCache() {
    return needCache;
  }

  void setNeedCache(boolean needCache) {
    this.needCache = needCache;
  }
}
