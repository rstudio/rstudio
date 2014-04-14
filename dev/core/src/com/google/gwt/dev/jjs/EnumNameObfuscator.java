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
package com.google.gwt.dev.jjs;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReturnStatement;
import com.google.gwt.dev.jjs.ast.JThisRef;

import java.util.List;

/**
 * Performs optimizations on Enums.
 */
public class EnumNameObfuscator {

  private static class EnumNameCallChecker extends JModVisitor {

    private final JDeclaredType classType;
    private final JMethod enumNameMethod;
    private final JMethod enumToStringMethod;
    private final JDeclaredType enumType;
    private final JMethod enumValueOfMethod;
    private final TreeLogger logger;
    private final JDeclaredType stringType;

    public EnumNameCallChecker(JProgram jprogram, TreeLogger logger) {
      this.logger = logger;
      this.enumNameMethod = jprogram.getIndexedMethod("Enum.name");
      this.enumToStringMethod = jprogram.getIndexedMethod("Enum.toString");
      this.classType = jprogram.getIndexedType("Class");
      this.enumType = jprogram.getIndexedType("Enum");
      this.stringType = jprogram.getIndexedType("String");

      /*
       * Find the correct version of enumValueOfMethod.
       *
       * Note: it doesn't work to check against a ref returned by
       * jprogram.getIndexedMethod("Enum.valueOf"), since there are 2 different
       * versions of Enum.valueOf in our jre emulation library, and the indexed
       * ref won't reliably flag the public instance, which is the one we want
       * here (i.e. Enum.valueOf(Class<T>,String)). The other version is
       * protected, but is called by the generated constructors for sub-classes
       * of Enum, and we don't want to warn for those cases.
       */
      JMethod foundMethod = null;
      List<JMethod> enumMethods = enumType.getMethods();
      for (JMethod enumMethod : enumMethods) {
        if ("valueOf".equals(enumMethod.getName())) {
          List<JParameter> jParameters = enumMethod.getParams();
          if (jParameters.size() == 2 && jParameters.get(0).getType() == classType
              && jParameters.get(1).getType() == stringType) {
            foundMethod = enumMethod;
            break;
          }
        }
      }
      this.enumValueOfMethod = foundMethod;
    }

    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      JMethod target = x.getTarget();
      JDeclaredType type = target.getEnclosingType();

      if (type instanceof JClassType) {
        JClassType cType = (JClassType) type;

        if (target == enumNameMethod || target == enumToStringMethod || target == enumValueOfMethod) {
          warn(x);
        } else if (cType.isEnumOrSubclass() != null) {
          if ("valueOf".equals(target.getName())) {
            /*
             * Check for calls to the auto-generated
             * EnumSubType.valueOf(String). Note, the check of the signature for
             * the single String arg version is to avoid flagging user-defined
             * overloaded versions of the method, which are presumably ok.
             */
            List<JParameter> jParameters = target.getParams();
            if (jParameters.size() == 1 && jParameters.get(0).getType() == stringType) {
              warn(x);
            }
          }
        }
      }
    }

    @Override
    public boolean visit(JClassType x, Context ctx) {
      if (x == enumType) {
        // don't traverse into Enum class itself, don't warn on internal method
        // calls
        return false;
      }
      return true;
    }

    private void warn(JMethodCall x) {
      /*
       * TODO: add a way to suppress warning with annotation if you know what
       * you're doing.
       */
      logger.log(TreeLogger.WARN, "Call to Enum method " + x.getTarget().getName()
          + " when enum obfuscation is enabled:  " + x.getSourceInfo().getFileName() + ":"
          + x.getSourceInfo().getStartLine());
    }
  }

  private static class EnumNameReplacer extends JModVisitor {

    private final JMethod enumObfuscatedName;
    private final JClassType enumType;
    private final JProgram jprogram;
    private final TreeLogger logger;

    public EnumNameReplacer(JProgram jprogram, TreeLogger logger) {
      this.logger = logger;
      this.jprogram = jprogram;
      this.enumType = (JClassType) jprogram.getIndexedType("Enum");
      this.enumObfuscatedName = jprogram.getIndexedMethod("Enum.obfuscatedName");
    }

    @Override
    public void endVisit(JReturnStatement x, Context ctx) {
      info(x);
      JReturnStatement toReturn =
          new JReturnStatement(x.getSourceInfo(), new JMethodCall(x.getSourceInfo(), new JThisRef(x
              .getSourceInfo(), enumType), enumObfuscatedName));
      ctx.replaceMe(toReturn);
    }

    public void exec() {
      accept(jprogram.getIndexedMethod("Enum.name"));
    }

    private void info(JReturnStatement x) {
      if (logger.isLoggable(TreeLogger.INFO)) {
        logger.log(TreeLogger.INFO, "Replacing Enum.name method :  "
            + x.getSourceInfo().getFileName() + ":"
            + x.getSourceInfo().getStartLine());
      }
    }
  }

  public static void exec(JProgram jprogram, TreeLogger logger) {
    new EnumNameCallChecker(jprogram, logger).accept(jprogram);
    new EnumNameReplacer(jprogram, logger).exec();
  }
}
