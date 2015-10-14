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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.PrecompileTaskOptions;
import com.google.gwt.dev.cfg.ConfigurationProperties;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JConstructor;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JNewInstance;
import com.google.gwt.dev.jjs.ast.JNullLiteral;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.RuntimeConstants;
import com.google.gwt.thirdparty.guava.common.collect.Lists;

import java.util.Collection;
import java.util.List;

/**
 * Performs optimizations on Enums.
 */
public class EnumNameObfuscator {

  private static final String ENUM_NAME_OBFUSCATION_PROPERTY =
      "compiler.enum.obfuscate.names";
  private static final String ENUM_NAME_OBFUSCATION_BLACKLIST_PROPERTY =
      "compiler.enum.obfuscate.names.blacklist";

  private static class EnumNameCallChecker extends JVisitor {

    private final JDeclaredType classType;
    private final JMethod enumNameMethod;
    private final JMethod enumToStringMethod;
    private final JDeclaredType enumType;
    private final JMethod enumValueOfMethod;
    private final TreeLogger logger;
    private final List<String> blacklistedEnums;
    private final JDeclaredType stringType;

    private EnumNameCallChecker(JProgram jprogram, TreeLogger logger,
        List<String> blacklistedEnums) {
      this.logger = logger;
      this.blacklistedEnums = blacklistedEnums;
      this.enumNameMethod = jprogram.getIndexedMethod(RuntimeConstants.ENUM_NAME);
      this.enumToStringMethod = jprogram.getIndexedMethod(RuntimeConstants.ENUM_TO_STRING);
      this.classType = jprogram.getFromTypeMap("java.lang.Class");
      this.enumType = jprogram.getFromTypeMap("java.lang.Enum");
      this.stringType = jprogram.getFromTypeMap("java.lang.String");

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
        List<JParameter> params = enumMethod.getParams();
        if (enumMethod.getName().equals("valueOf") &&
            params.size() == 2 && params.get(0).getType() == classType
            && params.get(1).getType() == stringType) {
          foundMethod = enumMethod;
          break;
        }
      }
      this.enumValueOfMethod = foundMethod;
    }

    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      JMethod target = x.getTarget();
      JDeclaredType type = target.getEnclosingType();

      if (!(type instanceof JClassType)) {
        return;
      }
      JClassType cType = (JClassType) type;
      if (typeInBlacklist(blacklistedEnums, cType)) {
        return;
      }
      if (target == enumNameMethod || target == enumToStringMethod || target == enumValueOfMethod) {
        warn(x);
        return;
      }
      if (cType.isEnumOrSubclass() != null) {
        return;
      }
      /*
       * Check for calls to the auto-generated
       * EnumSubType.valueOf(String). Note, the check of the signature for
       * the single String arg version is to avoid flagging user-defined
       * overloaded versions of the method, which are presumably ok.
       */
      List<JParameter> params = target.getParams();
      if (target.getName().equals("valueOf") &&
          params.size() == 1 && params.get(0).getType() == stringType) {
        warn(x);
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

    private final JProgram jprogram;
    private final JMethod makeEnumName;
    private List<String> blacklistedEnums;
    private boolean closureMode;
    private final TreeLogger logger;

    private EnumNameReplacer(JProgram jprogram, TreeLogger logger,
        List<String> blacklistedEnums, boolean closureMode) {
      this.logger = logger;
      this.jprogram = jprogram;
      this.blacklistedEnums = blacklistedEnums;
      this.closureMode = closureMode;
      this.makeEnumName = jprogram.getIndexedMethod(RuntimeConstants.UTIL_MAKE_ENUM_NAME);
    }

    private void exec() {
      accept(jprogram);
    }

    @Override
    public boolean visit(JClassType x, Context ctx) {
      if (x.isEnumOrSubclass() == null || typeInBlacklist(blacklistedEnums, x)) {
        return false;
      }
      trace(x);
      return true;
    }

    @Override
    public void endVisit(JNewInstance x, Context ctx) {
      JConstructor target = x.getTarget();
      JClassType enclosingType = target.getEnclosingType();
      if (enclosingType.isEnumOrSubclass() == null) {
        return;
      }

      if (typeInBlacklist(blacklistedEnums, enclosingType)) {
        return;
      }

      JNewInstance newEnum = new JNewInstance(x);
      List<JExpression> args = Lists.newArrayList(x.getArgs());
      // replace first argument with null or the closure obfuscation function.
      args.set(0, closureMode ?
            new JMethodCall(x.getSourceInfo(), null, makeEnumName, args.get(0)) :
            JNullLiteral.INSTANCE);
      newEnum.addArgs(args);
      ctx.replaceMe(newEnum);
    }

    private void trace(JClassType x) {
      if (logger.isLoggable(TreeLogger.TRACE)) {
        logger.log(TreeLogger.TRACE, "Obfuscating enum " + x + x.getSourceInfo().getFileName() + ":"
            + x.getSourceInfo().getStartLine());
      }
    }
  }

  private static boolean typeInBlacklist(Collection<String> blacklistedEnums, JClassType cType) {
    return blacklistedEnums.contains(cType.getName().replace('$', '.'));
  }

  public static void exec(JProgram jprogram, TreeLogger logger, ConfigurationProperties configurationProperties,
      PrecompileTaskOptions options) {
    if (!configurationProperties.getBoolean(ENUM_NAME_OBFUSCATION_PROPERTY, false)) {
      return;
    }
    boolean closureMode = options.isClosureCompilerFormatEnabled();
    List<String> blacklistedEnums =
        configurationProperties.getCommaSeparatedStrings(ENUM_NAME_OBFUSCATION_BLACKLIST_PROPERTY);
    new EnumNameCallChecker(jprogram, logger, blacklistedEnums).accept(jprogram);
    new EnumNameReplacer(jprogram, logger, blacklistedEnums, closureMode).exec();
  }
}
