/*
 * Copyright 2014 Google Inc.
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

import com.google.gwt.dev.PrecompileTaskOptions;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.SourceOrigin;
import com.google.gwt.dev.jjs.ast.JBooleanLiteral;
import com.google.gwt.dev.jjs.ast.JCharLiteral;
import com.google.gwt.dev.jjs.ast.JDoubleLiteral;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JFloatLiteral;
import com.google.gwt.dev.jjs.ast.JIntLiteral;
import com.google.gwt.dev.jjs.ast.JLiteral;
import com.google.gwt.dev.jjs.ast.JLongLiteral;
import com.google.gwt.dev.jjs.ast.JNullLiteral;
import com.google.gwt.dev.jjs.ast.JStringLiteral;
import com.google.gwt.dev.jjs.ast.js.JMultiExpression;
import com.google.gwt.dev.js.ast.JsBooleanLiteral;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsLiteral;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsNullLiteral;
import com.google.gwt.dev.js.ast.JsNumberLiteral;
import com.google.gwt.dev.js.ast.JsObjectLiteral;
import com.google.gwt.dev.js.ast.JsStringLiteral;
import com.google.gwt.dev.util.arg.OptionJsInteropMode.Mode;
import com.google.gwt.lang.LongLib;
import com.google.gwt.thirdparty.guava.common.base.Predicate;
import com.google.gwt.thirdparty.guava.common.base.Predicates;
import com.google.gwt.thirdparty.guava.common.collect.Collections2;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableMap;
import com.google.gwt.thirdparty.guava.common.collect.Lists;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * General utilities related to Java AST manipulation.
 */
public class JjsUtils {

  /**
   * Creates a multi expression from a list of expressions, removing expressions that do
   * not have side effects if possible.
   */
  public static JExpression createOptimizedMultiExpression(JExpression... expressions) {
    return createOptimizedMultiExpression(false, Arrays.asList(expressions));
  }

  /**
   * Creates a multi expression from a list of expressions, removing expressions that do
   * not have side effects if possible.
   */
  public static JExpression createOptimizedMultiExpression(boolean ignoringResult,
      List<JExpression> expressions) {

    int numberOfExpressions = expressions.size();
    JExpression result = expressions.get(numberOfExpressions - 1);

    numberOfExpressions = expressions.size();
    if (numberOfExpressions == 0) {
      return new JMultiExpression(SourceOrigin.UNKNOWN);
    }

    expressions =  Lists.newArrayList(Collections2.filter(
        expressions.subList(0, numberOfExpressions - 1),
        Predicates.and(
            Predicates.notNull(),
            new Predicate<JExpression>() {
              @Override
              public boolean apply(JExpression expression) {
                return expression.hasSideEffects();
              }
            })));

    if (result != null && (!ignoringResult || result.hasSideEffects())) {
      expressions.add(result);
    }

    if (expressions.size() == 1) {
      // Do not create a multi expression if it consists only of the result.
      return expressions.iterator().next();
    }

    SourceInfo info = expressions.size() > 0 ? expressions.get(0).getSourceInfo() :
        SourceOrigin.UNKNOWN;
    return new JMultiExpression(info, expressions);
  }

  /**
   * Translates a Java literal into a JavaScript literal.
   */
  public static JsLiteral translateLiteral(JLiteral literal) {
    return translatorByLiteralClass.get(literal.getClass()).translate(literal);
  }

  public static boolean closureStyleLiteralsNeeded(PrecompileTaskOptions options) {
    return closureStyleLiteralsNeeded(options.isIncrementalCompileEnabled(),
        options.getJsInteropMode(), options.isClosureCompilerFormatEnabled());
  }

  public static boolean closureStyleLiteralsNeeded(boolean incremental, Mode jsInteropMode,
      boolean closureOutputFormat) {
    return !incremental && jsInteropMode == Mode.JS && closureOutputFormat;
  }

  private static Map<Class<? extends JLiteral>, LiteralTranslators> translatorByLiteralClass =
      new ImmutableMap.Builder()
          .put(JBooleanLiteral.class, LiteralTranslators.BOOLEAN_LITERAL_TRANSLATOR)
          .put(JCharLiteral.class, LiteralTranslators.CHAR_LITERAL_TRANSLATOR)
          .put(JFloatLiteral.class, LiteralTranslators.FLOAT_LITERAL_TRANSLATOR)
          .put(JDoubleLiteral.class, LiteralTranslators.DOUBLE_LITERAL_TRANSLATOR)
          .put(JIntLiteral.class, LiteralTranslators.INT_LITERAL_TRANSLATOR)
          .put(JLongLiteral.class, LiteralTranslators.LONG_LITERAL_TRANSLATOR)
          .put(JNullLiteral.class, LiteralTranslators.NULL_LITERAL_TRANSLATOR)
          .put(JStringLiteral.class, LiteralTranslators.STRING_LITERAL_TRANSLATOR)
          .build();

  private enum LiteralTranslators {
    BOOLEAN_LITERAL_TRANSLATOR() {
      @Override
      JsLiteral translate(JExpression literal) {
        return JsBooleanLiteral.get(((JBooleanLiteral) literal).getValue());
      }
    },
    CHAR_LITERAL_TRANSLATOR() {
      @Override
      JsLiteral translate(JExpression literal) {
        return new JsNumberLiteral(literal.getSourceInfo(), ((JCharLiteral) literal).getValue());
      }
    },
    FLOAT_LITERAL_TRANSLATOR() {
      @Override
      JsLiteral translate(JExpression literal) {
        return new JsNumberLiteral(literal.getSourceInfo(), ((JFloatLiteral) literal).getValue());
      }
    },
    DOUBLE_LITERAL_TRANSLATOR() {
      @Override
      JsLiteral translate(JExpression literal) {
        return new JsNumberLiteral(literal.getSourceInfo(), ((JDoubleLiteral) literal).getValue());
      }
    },
    INT_LITERAL_TRANSLATOR() {
      @Override
      JsLiteral translate(JExpression literal) {
        return new JsNumberLiteral(literal.getSourceInfo(), ((JIntLiteral) literal).getValue());
      }
    },
    LONG_LITERAL_TRANSLATOR() {
      @Override
      JsLiteral translate(JExpression literal) {
        SourceInfo sourceInfo = literal.getSourceInfo();
        int[] values = LongLib.getAsIntArray(((JLongLiteral) literal).getValue());
        JsObjectLiteral objectLiteral = new JsObjectLiteral(sourceInfo);
        objectLiteral.setInternable();

        assert values.length == names.length;
        for (int i = 0; i < names.length; i++) {
          addPropertyToObject(sourceInfo, names[i], values[i], objectLiteral);
        }
        return objectLiteral;
      }
    },
    STRING_LITERAL_TRANSLATOR() {
      @Override
      JsLiteral translate(JExpression literal) {
        return new JsStringLiteral(literal.getSourceInfo(), ((JStringLiteral) literal).getValue());
      }
    },
    NULL_LITERAL_TRANSLATOR() {
      @Override
      JsLiteral translate(JExpression literal) {
        return JsNullLiteral.INSTANCE;
      }
    };

    private static final JsName[] names;

    static {
      // The names of the components in an emulated long ('l', 'm', and 'h') are accessed directly
      // through JSNI in LongLib (the implementor of emulated long operations), hence it is
      // important that they don't get renamed hence the corresponding JsNames are created
      // unscoped (null scope) and unobfuscatable.
      String[] stringNames = {"l","m","h"};
      names = new JsName[stringNames.length];
      for (int i = 0; i < stringNames.length; i++) {
        names[i] = new JsName(null, stringNames[i], stringNames[i]);
        names[i].setObfuscatable(false);
      }
    }

    abstract JsLiteral translate(JExpression literal);
  }

  private static void addPropertyToObject(SourceInfo sourceInfo, JsName propertyName,
      int propertyValue, JsObjectLiteral objectLiteral) {
    JsExpression label = propertyName.makeRef(sourceInfo);
    JsExpression value = new JsNumberLiteral(sourceInfo, propertyValue);
    objectLiteral.addProperty(sourceInfo, label, value);
  }

  private JjsUtils() {
  }
}