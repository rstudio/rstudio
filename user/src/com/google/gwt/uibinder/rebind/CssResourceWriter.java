/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.uibinder.rebind;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.attributeparsers.CssNameConverter;
import com.google.gwt.uibinder.rebind.model.ImplicitCssResource;

import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

/**
 * Writes the source to implement an {@link ImplicitCssResource} interface.
 */
public class CssResourceWriter {
  private static final JType[] NO_PARAMS = new JType[0];
  private final ImplicitCssResource css;
  private final IndentedWriter writer;
  private final JClassType cssResourceType;
  private final JClassType stringType;
  private final CssNameConverter nameConverter;
  private final MortalLogger logger;

  public CssResourceWriter(ImplicitCssResource css, TypeOracle types,
      PrintWriter writer, MortalLogger logger) {
    this.css = css;
    this.writer = new IndentedWriter(writer);
    this.cssResourceType = types.findType(CssResource.class.getName());
    this.stringType = types.findType(String.class.getName());
    this.nameConverter = new CssNameConverter();
    this.logger = logger;
  }

  public void write() throws UnableToCompleteException {
    // Package declaration
    String packageName = css.getPackageName();
    if (packageName.length() > 0) {
      writer.write("package %1$s;", packageName);
      writer.newline();
    }

    JClassType superType = css.getExtendedInterface();
    if (superType == null) {
      superType = cssResourceType;
    }

    writer.write("import %s;", superType.getQualifiedSourceName());
    writer.newline();

    // Open interface
    writer.write("public interface %s extends %s {", css.getClassName(),
        superType.getSimpleSourceName());
    writer.indent();

    writeCssMethods(superType);

    // Close interface.
    writer.outdent();
    writer.write("}");
  }

  private boolean isOverride(String methodName, JClassType superType) {
    JMethod method = superType.findMethod(methodName, NO_PARAMS);
    if (method != null && stringType.equals(method.getReturnType())) {
      return true;
    }
    return false;
  }

  private void writeCssMethods(JClassType superType)
      throws UnableToCompleteException {
    Set<String> rawClassNames = css.getCssClassNames();
    Map<String, String> convertedClassNames = null;

    try {
      convertedClassNames = nameConverter.convertSet(rawClassNames);
    } catch (CssNameConverter.Failure e) {
      logger.die(e.getMessage());
    }

    for (Map.Entry<String, String> entry : convertedClassNames.entrySet()) {
      String className = entry.getValue();
      /*
       * Only write names that we are not overriding from super, or else we'll
       * re-obfuscate any @Shared ones
       */
      if (!isOverride(className, superType)) {
        if (!rawClassNames.contains(className)) {
          writer.write("@ClassName(\"%s\")", entry.getKey());
        }
        writer.write("String %s();", className);
      }
    }
  }
}
