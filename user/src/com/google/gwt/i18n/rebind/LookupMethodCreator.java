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
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.user.rebind.AbstractGeneratorClassCreator;
import com.google.gwt.user.rebind.AbstractMethodCreator;
import com.google.gwt.user.rebind.AbstractSourceCreator;

import java.text.MessageFormat;

/**
 * Method creator to call the correct Map for the given Dictionary.
 */
class LookupMethodCreator extends AbstractMethodCreator {
  private JType returnType;

  /**
   * Constructor for <code>LookupMethodCreator</code>.
   * 
   * @param classCreator parent class creator
   * @param returnType associated return type
   */
  public LookupMethodCreator(AbstractGeneratorClassCreator classCreator,
      JType returnType) {
    super(classCreator);
    this.returnType = returnType;
  }

  public void createMethodFor(TreeLogger logger, JMethod targetMethod,
      String value) {
    createMethodFor(targetMethod);
  }

  void createMethodFor(JMethod targetMethod) {
    String template = "{0} target = ({0})cache.get(arg0);";
    String type;
    JPrimitiveType s = returnType.isPrimitive();
    if (s != null) {
      type = AbstractSourceCreator.getJavaObjectTypeFor(s);
    } else {
      type = returnType.getQualifiedSourceName();
    }
    Object[] args = {type};
    String lookup = MessageFormat.format(template, args);
    println(lookup);
    println("if (target != null)");
    printReturnTarget();
    JMethod[] methods = ((ConstantsWithLookupImplCreator) currentCreator).allInterfaceMethods;
    for (int i = 0; i < methods.length; i++) {
      if (methods[i].getReturnType().equals(returnType)
          && methods[i] != targetMethod) {
        String methodName = methods[i].getName();
        String body = "if(arg0.equals(" + wrap(methodName) + ")){";
        println(body);
        printFound(methodName);
        println("}");
      }
    }
    String format = "throw new java.util.MissingResourceException(\"Cannot find constant ''\" + {0} + \"''; expecting a method name\", \"{1}\", {0});";
    String[] throwArgs = {
        "arg0", this.currentCreator.getTarget().getQualifiedSourceName()};
    String result = MessageFormat.format(format, throwArgs);
    println(result);
  }

  void printFound(String methodName) {
    Object[] args2 = {methodName};
    println(MessageFormat.format(returnTemplate(), args2));
  }

  void printReturnTarget() {
    println("return target;");
  }

  String returnTemplate() {
    return "return {0}();";
  }
}
