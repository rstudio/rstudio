/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.dev;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.EmittedArtifact.Visibility;
import com.google.gwt.dev.util.Util;
import com.google.gwt.thirdparty.guava.common.base.Charsets;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * A Generator that creates a bar.txt resource and a class whose name depends on an input resource.
 */
public class FooResourceGenerator extends Generator {

  public static int runCount = 0;

  @Override
  public String generate(TreeLogger logger, GeneratorContext context, String typeName)
      throws UnableToCompleteException {
    runCount++;

    // On first run generate a bar.txt resource that always has the same contents.
    OutputStream barStream = context.tryCreateResource(logger, "bar.txt");
    if (barStream != null) {
      try {
        barStream.write("here's some text.".getBytes(Charsets.UTF_8));
        context.commitResource(logger, barStream).setVisibility(Visibility.Public);
      } catch (IOException e) {
        return null;
      }
    }

    // On first run generate a class whose name depends on the contents of a read input resource and
    // another class whose name is stable but whose content changes based on the read input
    // resource.
    String generatedClassName;
    try {
      generatedClassName = Util.readStreamAsString(context.getResourcesOracle().getResource(
          "com/foo/generatedClassName.txt").openContents()).trim();

      // Custom class name with stable content.
      PrintWriter customNameClassPw = context.tryCreate(logger, "com.foo", generatedClassName);
      if (customNameClassPw != null) {
        customNameClassPw.write("package com.foo;");
        customNameClassPw.write("public class " + generatedClassName + " {");
        customNameClassPw.write("  HasCustomContent hasCustomContent = new HasCustomContent();");
        customNameClassPw.write("}");

        customNameClassPw.close();
        context.commit(logger, customNameClassPw);
      }

      // Stable class name with custom content.
      PrintWriter customContentClassPw = context.tryCreate(logger, "com.foo", "HasCustomContent");
      if (customContentClassPw != null) {
        String generatedFunctionName = generatedClassName;
        customContentClassPw.write("package com.foo;");
        customContentClassPw.write("public class HasCustomContent {");
        customContentClassPw.write("  public void " + generatedFunctionName + "() {}");
        customContentClassPw.write("}");

        customContentClassPw.close();
        context.commit(logger, customContentClassPw);
      }

      return "com.foo." + generatedClassName;
    } catch (IOException e) {
      return "";
    }
  }
}
