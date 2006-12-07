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
package com.google.gwt.user.rebind;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JMethod;

/**
 * Creates method factories depending upon the method type. Includes the core
 * concrete implementations as package-protected classes to avoid cluttering up
 * the package space.
 */
public abstract class AbstractMethodCreator extends AbstractSourceCreator {
  /**
   * AbstractGeneratorClassCreator associated with the method currently in
   * process.
   */
  protected AbstractGeneratorClassCreator currentCreator;

  /**
   * Constructor for <code>AbstractMethodCreator</code>.
   * 
   * @param classCreator
   */
  public AbstractMethodCreator(AbstractGeneratorClassCreator classCreator) {
    this.currentCreator = classCreator;
  }

  /**
   * Generate the method body for the target method.
   * 
   * @param logger
   * @param targetMethod Method
   * @param value Arbitrary value
   * @throws UnableToCompleteException
   */
  public abstract void createMethodFor(TreeLogger logger, JMethod targetMethod,
      String value) throws UnableToCompleteException;

  /**
   * Prints to the current <code>AbstractGeneratorClassCreator</code>.
   * 
   * @param printMe <code>Object</code> to print
   */
  public void println(Object printMe) {
    currentCreator.getWriter().println(printMe.toString());
  }

  /**
   * Indent subsequent lines.
   */
  protected void indent() {
    currentCreator.getWriter().indent();
  }

  /**
   * Outdent subsequent lines.
   */
  protected void outdent() {
    currentCreator.getWriter().outdent();
  }

  /**
   * Prints to the current <code>AbstractGeneratorClassCreator</code>.
   * 
   * @param printMe
   */
  protected void print(String printMe) {
    currentCreator.getWriter().print(printMe);
  }
}
