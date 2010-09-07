/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.dev;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.JAbstractMethod;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import java.io.PrintWriter;

/**
 * Entry point that outputs the GWT JRE support.
 */
public class GetJreEmulation {

  /**
   * Only print the publicly visible API of the JRE.
   */
  private static final class FilterImplementation implements
      SignatureDumper.Filter {
    public boolean shouldPrint(JAbstractMethod method) {
      return method.isPublic() || method.isProtected();
    }

    public boolean shouldPrint(JClassType type) {
      if (type.isMemberType()) {
        if (!shouldPrint(type.getEnclosingType())) {
          return false;
        }
      }
      return type.getQualifiedSourceName().startsWith("java.")
          && (type.isPublic() || type.isProtected());
    }

    public boolean shouldPrint(JField field) {
      return field.isPublic() || field.isProtected();
    }
  }

  /**
   * @param args unused
   */
  public static void main(String[] args) {
    try {
      PrintWriterTreeLogger logger = new PrintWriterTreeLogger(new PrintWriter(
          System.err, true));
      logger.setMaxDetail(TreeLogger.WARN);
      ModuleDef module = ModuleDefLoader.loadFromClassPath(logger,
          "com.google.gwt.core.Core");
      CompilationState compilationState = module.getCompilationState(logger);
      TypeOracle typeOracle = compilationState.getTypeOracle();
      SignatureDumper.dumpSignatures(typeOracle, System.out,
          new FilterImplementation());
    } catch (Throwable e) {
      System.err.println("Unexpected error");
      e.printStackTrace();
      System.exit(1);
    }
  }
}
