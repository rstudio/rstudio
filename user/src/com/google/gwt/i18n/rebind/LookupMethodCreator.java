/*
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.i18n.rebind;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.i18n.rebind.AbstractResource.ResourceList;
import com.google.gwt.i18n.shared.GwtLocale;
import com.google.gwt.user.rebind.AbstractGeneratorClassCreator;
import com.google.gwt.user.rebind.AbstractMethodCreator;
import com.google.gwt.user.rebind.AbstractSourceCreator;

import java.text.MessageFormat;

/**
 * Method creator to call the correct Map for the given Dictionary.
 */
class LookupMethodCreator extends AbstractMethodCreator {

  /**
   * Used partition size if no one is specified.
   * 
   * Used in constructor without a partition size.
   */
  private static final int DEFAULT_PARTITIONS_SIZE = 500;

  private JType returnType;

  private final int partitionsSize;

  /**
   * Constructor for <code>LookupMethodCreator</code>. The default partition size of 500 is used.
   *
   * @param classCreator parent class creator
   * @param returnType associated return type
   * 
   * @see LookupMethodCreator#DEFAULT_PARTITIONS_SIZE
   */
  public LookupMethodCreator(AbstractGeneratorClassCreator classCreator, JType returnType) {
    this(classCreator, returnType, DEFAULT_PARTITIONS_SIZE);
    this.returnType = returnType;
  }

  /**
   * Constructor for <code>LookupMethodCreator</code>.
   *
   * @param classCreator parent class creator
   * @param returnType associated return type
   * @param partitionsSize max numbers of lookups per method.
   */
  public LookupMethodCreator(AbstractGeneratorClassCreator classCreator, JType returnType,
      int partitionsSize) {
    super(classCreator);
    this.returnType = returnType;
    this.partitionsSize = partitionsSize;
  }

  @Override
  public void createMethodFor(TreeLogger logger, JMethod targetMethod, String key,
      ResourceList resourceList, GwtLocale locale) {
    createMethodFor(targetMethod);
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

  void printLookup(String methodName) {
    String body = "if(arg0.equals(" + wrap(methodName) + ")) {";
    println(body);
    indent();
    printFound(methodName);
    outdent();
    println("}");
  }

  void createMethodFor(JMethod targetMethod) {
    String template = "{0} target = ({0}) cache.get(arg0);";
    String returnTypeName = getReturnTypeName();
    String lookup = MessageFormat.format(template, new Object[] {returnTypeName});
    println(lookup);
    println("if (target != null) {");
    indent();
    printReturnTarget();
    outdent();
    println("}");
    JMethod[] methods = ((ConstantsWithLookupImplCreator) currentCreator).allInterfaceMethods;

    final int partitions = (methods.length / partitionsSize) + 1;
    println(returnTypeName + " tmp;");
    for (int i = 0; i < partitions; i++) {
      println("tmp = " + targetMethod.getName() + i + "(arg0);");
      println("if (tmp != null) {");
      indent();
      println("return tmp;");
      outdent();
      println("}");
    }

    String format = "throw new java.util.MissingResourceException(\"Cannot find constant ''\" +"
        + "{0} + \"''; expecting a method name\", \"{1}\", {0});";
    String result = MessageFormat.format(format, "arg0", this.currentCreator.getTarget()
        .getQualifiedSourceName());
    println(result);
    outdent();
    println("}");

    println("");

    final String argument0Type = targetMethod.getParameterTypes()[0].getQualifiedSourceName();
    for (int p = 0; p < partitions; p++) {
      final String templateNewMethod = "private {0} {1}{2}({3} arg0) '{";
      final String header = MessageFormat.format(templateNewMethod, new Object[] {
          returnTypeName, targetMethod.getName(), p, argument0Type});
      println(header);
      indent();
      final JType erasedType = returnType.getErasedType();
      for (int i = 0 + p * partitionsSize; i < methods.length && i < (p + 1)
          * partitionsSize; i++) {
        final JMethod method = methods[i];
        if (method.getReturnType().getErasedType().equals(erasedType) && method != targetMethod) {
          String methodName = method.getName();
          printLookup(methodName);
        }
      }

      println("return null;");
      if (p < partitions - 1) {
        outdent();
        println("}");
        println("");
      }
    }
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
}
