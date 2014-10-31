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
package com.google.gwt.resources.converter;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.resources.css.ast.Context;
import com.google.gwt.resources.css.ast.CssProperty;
import com.google.gwt.resources.css.ast.CssProperty.FunctionValue;
import com.google.gwt.resources.css.ast.CssProperty.IdentValue;
import com.google.gwt.resources.css.ast.CssProperty.ListValue;
import com.google.gwt.resources.css.ast.CssProperty.Value;
import com.google.gwt.resources.css.ast.CssRule;
import com.google.gwt.resources.css.ast.CssVisitor;
import com.google.gwt.thirdparty.guava.common.base.Preconditions;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts upper case strings used in properties to lowercase if they are not defined
 * with a proper @def statement.
 */
public class UndefinedConstantVisitor extends CssVisitor {

  private final Set<String> gssContantNames;
  private final Pattern pattern = Pattern.compile("^[A-Z_][A-Z_0-9]+$");
  private final Set<String> propertyNamesToSkip =
      Sets.newHashSet("filter", "-ms-filter", "font-family");
  private final boolean lenient;
  private final TreeLogger treeLogger;

  public UndefinedConstantVisitor(Set<String> gssContantNames, boolean lenient,
      TreeLogger treeLogger) {
    this.gssContantNames = gssContantNames;
    this.lenient = lenient;
    this.treeLogger = treeLogger;
  }

  @Override
  public boolean visit(CssRule x, Context ctx) {
    List<CssProperty> properties = x.getProperties();
    for (CssProperty cssProperty : properties) {
      String cssPropertyName = cssProperty.getName();
      if (propertyNamesToSkip.contains(cssPropertyName)) {
        continue;
      }

      // for logging purpose
      String selector = x.getSelectors().toString();

      ListValue listValue = visitListValue(cssProperty.getValues(), cssPropertyName, selector);

      cssProperty.setValue(listValue);
    }

    return false;
  }

  private ListValue visitListValue(ListValue values, String cssPropertyName, String selector) {
    Preconditions.checkNotNull(values, "values cannot be null");

    List<Value> cssPropertyValues = values.getValues();
    List<Value> newValues = new ArrayList<Value>(cssPropertyValues.size());

    for (Value value : cssPropertyValues) {
      if (value.isListValue() != null) {
        newValues.add(visitListValue(value.isListValue(), cssPropertyName, selector));

      } else if (value.isFunctionValue() != null) {
        FunctionValue functionValue = value.isFunctionValue();
        ListValue listValue = visitListValue(functionValue.getValues(), cssPropertyName,
            selector);

        newValues.add(new FunctionValue(functionValue.getName(), listValue));

      } else if (value.isIdentValue() != null) {
        newValues.add(visitIdentValue(value.isIdentValue(), cssPropertyName, selector));

      } else {
        newValues.add(value);
      }
    }

    return new ListValue(newValues);
  }

  private Value visitIdentValue(IdentValue identValue, String cssPropertyName, String selector) {
    Matcher matcher = pattern.matcher(identValue.getIdent());

    if (matcher.matches()) {
      String upperCaseString = matcher.group();
      if (!gssContantNames.contains(upperCaseString)) {
        treeLogger.log(Type.WARN, "Property '" + cssPropertyName + "' from rule '"
            + selector + "' uses an undefined constant: " + upperCaseString);
        if (lenient) {
          treeLogger.log(Type.WARN, "turning '" + upperCaseString +
              "' to lower case. This is probably not what you wanted here in the " +
              "first place!");
          return new IdentValue(upperCaseString.toLowerCase(Locale.US));
        } else {
          throw new Css2GssConversionException("Found undefined constant in input. "
              + cssPropertyName + "' from rule '" + selector + "' undefined constant: " +
              upperCaseString);
        }
      }
    }

    return identValue;
  }
}
