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
package com.google.gwt.dev.jjs;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.jjs.test.UnstableGeneratorTest;

import java.io.PrintWriter;

/**
 * Generates unstable results than change on each invocation.
 */
public class UnstableGenerator extends Generator {

  private static int counter = 0;

  @Override
  public String generate(TreeLogger logger, GeneratorContext context,
      String typeName) throws UnableToCompleteException {
    try {
      ++counter;
      TypeOracle typeOracle = context.getTypeOracle();
      JClassType inputType = typeOracle.getType(typeName);
      JClassType intfType = typeOracle.getType(UnstableGeneratorTest.UnstableResult.class.getName().replace(
          '$', '.'));
      String packageName = inputType.getPackage().getName();
      String className = inputType.getName().replace('.', '_') + "Impl"
          + counter;
      PrintWriter writer = context.tryCreate(logger, packageName, className);
      if (writer == null) {
        return null;
      }
      writer.println("package " + packageName + ";");
      writer.println("public class " + className + " implements "
          + intfType.getQualifiedSourceName() + " {");
      writer.println("  public String get() { return \"foo" + counter + "\"; }");
      writer.println("}");
      context.commit(logger, writer);
      return (packageName.length() == 0) ? className
          : (packageName + "." + className);
    } catch (NotFoundException e) {
      logger.log(TreeLogger.ERROR, "Unable to find required client type", e);
      throw new UnableToCompleteException();
    }
  }
}
