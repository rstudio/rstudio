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
package com.google.gwt.resources.css;

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.ConfigurationProperty;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.SelectionProperty;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.resources.css.ast.Context;
import com.google.gwt.resources.css.ast.CssCompilerException;
import com.google.gwt.resources.css.ast.CssIf;
import com.google.gwt.resources.css.ast.CssModVisitor;
import com.google.gwt.resources.css.ast.CssNode;

import java.util.Arrays;

/**
 * Statically evaluates {@literal @if} rules.
 */
public class IfEvaluator extends CssModVisitor {
  private final TreeLogger logger;
  private final PropertyOracle oracle;

  public IfEvaluator(TreeLogger logger, PropertyOracle oracle) {
    this.logger = logger.branch(TreeLogger.DEBUG,
        "Replacing property-based @if blocks");
    this.oracle = oracle;
  }

  @Override
  public void endVisit(CssIf x, Context ctx) {
    if (x.getExpression() != null) {
      // This gets taken care of by the runtime substitution visitor
    } else {
      try {
        String propertyName = x.getPropertyName();
        String propValue = null;
        try {
          SelectionProperty selProp = oracle.getSelectionProperty(logger,
              propertyName);
          propValue = selProp.getCurrentValue();
        } catch (BadPropertyValueException e) {
          ConfigurationProperty confProp = oracle.getConfigurationProperty(propertyName);
          propValue = confProp.getValues().get(0);
        }

        /*
         * If the deferred binding property's value is in the list of values in
         * the @if rule, move the rules into the @if's context.
         */
        if (Arrays.asList(x.getPropertyValues()).contains(propValue)
            ^ x.isNegated()) {
          for (CssNode n : x.getNodes()) {
            ctx.insertBefore(n);
          }
        } else {
          // Otherwise, move the else block into the if statement's position
          for (CssNode n : x.getElseNodes()) {
            ctx.insertBefore(n);
          }
        }

        // Always delete @if rules that we can statically evaluate
        ctx.removeMe();
      } catch (BadPropertyValueException e) {
        logger.log(TreeLogger.ERROR, "Unable to evaluate @if block", e);
        throw new CssCompilerException("Unable to parse CSS", e);
      }
    }
  }
}
