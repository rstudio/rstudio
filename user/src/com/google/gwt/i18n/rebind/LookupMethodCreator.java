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
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.i18n.rebind.AbstractResource.ResourceList;
import com.google.gwt.i18n.shared.GwtLocale;
import com.google.gwt.user.rebind.AbstractMethodCreator;
import com.google.gwt.user.rebind.AbstractSourceCreator;

import java.text.MessageFormat;
import java.util.List;

/**
 * Method creator to call the correct Map for the given Dictionary.
 */
class LookupMethodCreator extends AbstractMethodCreator {

  private final JType returnType;

  /**
   * Constructor for <code>LookupMethodCreator</code>.
   *
   * @param classCreator parent class creator
   * @param returnType associated return type
   */
  public LookupMethodCreator(ConstantsWithLookupImplCreator classCreator, JType returnType) {
    super(classCreator);
    this.returnType = returnType;
  }

  @Override
  public void createMethodFor(TreeLogger logger, JMethod targetMethod, String key,
      ResourceList resourceList, GwtLocale locale) {
    getConstantsWithLookupCreator().createMethodWithPartitionCheckFor(this, targetMethod);
  }
  
  /**
   * Returns a {@code String} containing the return type name.
   */
  protected String getReturnTypeName() {
    String type;
    JPrimitiveType s = returnType.isPrimitive();
    if (s != null) {
      type = AbstractSourceCreator.getJavaObjectTypeFor(s);
    } else {
      type = returnType.getParameterizedQualifiedSourceName();
    }
    return type;
  }

  void createCacheLookupFor() {
    String template = "{0} target = ({0}) cache.get(arg0);";
    String returnTypeName = getReturnTypeName();
    String lookup = MessageFormat.format(template, new Object[] {returnTypeName});
    println(lookup);
    println("if (target != null) {");
    indent();
    printReturnTarget();
    outdent();
    println("}");
  }

  /**
   * 
   * @param targetMethod the method to create the lookups for
   * @param methodsToCreate the looked up methods
   * @param nextPartitionMethod the name of next method is called or null if end this generated
   *          method with throwing an exception
   */
  void createMethodFor(JMethod targetMethod, List<JMethod> methodsToCreate,
      String nextPartitionMethod) {
    createMethodLookups(methodsToCreate);
    final String result;
    boolean isLastPartition = nextPartitionMethod == null;
    if (isLastPartition) {
      result = createThrowMissingResourceException();
    } else {
      String callPartitionMethodTemplate = "return {0}(arg0);";
      result = MessageFormat.format(callPartitionMethodTemplate, new Object[] {
          nextPartitionMethod});
    }
    println(result);
  }

  void createMethodLookups(List<JMethod> methodsToCreate) {
    for (JMethod methodToCreate : methodsToCreate) {
      String methodName = methodToCreate.getName();
      String body = "if (arg0.equals(" + wrap(methodName) + ")) {";
      println(body);
      indent();
      printFound(methodName);
      outdent();
      println("}");
    }
  }

  void createPartitionLookup(String partitionMethodName, JMethod targetMethod,
      List<JMethod> methodsToCreate, String nextPartitionMethod) {
    println("");
    final String templatePartitionMethodName = "private {0} {1}({2} arg0) '{";
    final String argument0Type = targetMethod.getParameterTypes()[0].getQualifiedSourceName();
    String partitionMethodSignature = MessageFormat.format(templatePartitionMethodName,
        new Object[] {getReturnTypeName(), partitionMethodName, argument0Type});
    println(partitionMethodSignature);
    indent();
    createMethodFor(targetMethod, methodsToCreate, nextPartitionMethod);
    outdent();
    println("}");
  }
  
  ConstantsWithLookupImplCreator getConstantsWithLookupCreator() {
    return ((ConstantsWithLookupImplCreator) currentCreator);
  }

  JType getReturnType() {
    return returnType;
  }

  void printFound(String methodName) {
    println(MessageFormat.format(returnTemplate(), new Object[] {methodName}));
  }

  void printReturnTarget() {
    println("return target;");
  }

  String returnTemplate() {
    return "return {0}();";
  }

  private String createThrowMissingResourceException() {
    final String result;
    String format = "throw new java.util.MissingResourceException(\"Cannot find constant ''\" +"
        + "{0} + \"''; expecting a method name\", \"{1}\", {0});";
    result = MessageFormat.format(format, "arg0", this.currentCreator.getTarget()
        .getQualifiedSourceName());
    return result;
  }
}
