/*
 * Copyright 2006 Google Inc.
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
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.core.ext.typeinfo.TypeOracleException;
import com.google.gwt.i18n.rebind.util.AbstractResource;
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
   * @param messageBindings resource bundle used to generate the class
   * @param oracle types
   * @throws UnableToCompleteException
   */
  public ConstantsImplCreator(TreeLogger logger, SourceWriter writer,
      JClassType localizableClass, AbstractResource messageBindings,
      TypeOracle oracle) throws UnableToCompleteException {
    super(writer, localizableClass, messageBindings);
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

  protected void classEpilog() {
    if (isNeedCache()) {
      getWriter().println(
          "java.util.Map cache = new java.util.HashMap();".toString());
    }
  }

  /**
   * Create the method body associated with the given method. Arguments are
   * arg0...argN.
   */
  protected void emitMethodBody(TreeLogger logger, JMethod method)
      throws UnableToCompleteException {
    checkConstantMethod(logger, method);
    delegateToCreator(logger, method);
  }

  boolean isNeedCache() {
    return needCache;
  }

  void setNeedCache(boolean needCache) {
    this.needCache = needCache;
  }

  /**
   * Checks that the method has the right structure to implement
   * <code>Constant</code>.
   * 
   * @param method method to check
   */
  private void checkConstantMethod(TreeLogger logger, JMethod method)
      throws UnableToCompleteException {
    if (method.getParameters().length > 0) {
      String s = "Methods in interfaces extending Constant must have no parameters and a return type of  String/int/float/boolean/double/String[]/Map";
      throw error(logger, s);
    }
  }
}
