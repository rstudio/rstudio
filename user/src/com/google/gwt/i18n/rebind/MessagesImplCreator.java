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
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.i18n.rebind.util.AbstractResource;
import com.google.gwt.user.rebind.SourceWriter;

/**
 * Creates the class implementation for a given resource bundle using the
 * standard <code>AbstractGeneratorClassCreator</code>.
 */
class MessagesImplCreator extends AbstractLocalizableImplCreator {
  /**
   * Constructor for <code>ConstantsImplCreator</code>.
   * 
   * @param writer <code>Writer</code> to print to
   * @param localizableClass Class/Interface to conform to
   * @param messageBindings resource bundle used to generate the class
   * @param oracle types
   * @param logger logger to print errors
   * @throws UnableToCompleteException
   */
  public MessagesImplCreator(TreeLogger logger, SourceWriter writer,
      JClassType localizableClass, AbstractResource messageBindings,
      TypeOracle oracle) throws UnableToCompleteException {
    super(writer, localizableClass, messageBindings);
    try {
      JClassType stringClass = oracle.getType(String.class.getName());
      register(stringClass, new MessagesMethodCreator(this));
    } catch (NotFoundException e) {
      // never expect this error in practice
      throw error(logger, e);
    }
  }

  /**
   * Checks that the method has the right structure to implement
   * <code>Messages</code>.
   * 
   * @param method
   * @throws UnableToCompleteException
   */
  private void checkMessagesMethod(TreeLogger logger, JMethod method)
      throws UnableToCompleteException {
    if (!method.getReturnType().getQualifiedSourceName().equals(
      "java.lang.String")) {
      throw error(
        logger,
        "All methods in interfaces extending Messages must have a return type of String.");
    }
  }

  /**
   * Create the method body associated with the given method. Arguments are
   * arg0...argN.
   * 
   * @param m method to emit
   * @throws UnableToCompleteException
   */
  protected void emitMethodBody(TreeLogger logger, JMethod m)
      throws UnableToCompleteException {
    checkMessagesMethod(logger, m);
    delegateToCreator(logger, m);
  }
}
