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
package com.google.gwt.dev.js;

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.ConfigurationProperty;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsModVisitor;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsVars;
import com.google.gwt.dev.js.ast.JsVars.JsVar;

import java.util.ArrayList;
import java.util.List;

/**
 * Divides large var statements into smaller ones. Very long var statements have
 * trouble on some browsers, including the initial Safari 4 beta. See Issue
 * 3455.
 */
public class JsBreakUpLargeVarStatements extends JsModVisitor {
  private static final String CONFIG_PROP_MAX_VARS = "compiler.max.vars.per.var";

  public static void exec(JsProgram program, PropertyOracle[] propertyOracles) {
    (new JsBreakUpLargeVarStatements(propertyOracles)).accept(program);
  }

  private static JsVars last(List<JsVars> list) {
    return list.get(list.size() - 1);
  }

  private final int maxVarsPerStatement;

  private JsBreakUpLargeVarStatements(PropertyOracle[] propertyOracles) {
    maxVarsPerStatement = getMaxVarsPerStatement(propertyOracles[0]);
  }

  @Override
  public void endVisit(JsVars x, JsContext context) {
    if (maxVarsPerStatement < 0) {
      return;
    }

    if (x.getNumVars() > maxVarsPerStatement) {
      // compute a list of smaller JsVars statements
      List<JsVars> smallerVars = new ArrayList<JsVars>();
      smallerVars.add(makeNewChildVars(x));

      for (JsVar var : x) {
        if (last(smallerVars).getNumVars() >= maxVarsPerStatement) {
          // Previous statement is full; start a new one
          smallerVars.add(makeNewChildVars(x));
        }
        last(smallerVars).add(var);
      }

      // replace x by the sequence smallerVars
      for (JsVars sv : smallerVars) {
        context.insertBefore(sv);
      }
      context.removeMe();
    }
  }

  /**
   * Look up in the specified property oracle the maximum number of variables to
   * allow per var statement.
   */
  private int getMaxVarsPerStatement(PropertyOracle propertyOracle)
      throws InternalCompilerException, NumberFormatException {
    ConfigurationProperty prop;
    try {
      prop = propertyOracle.getConfigurationProperty(CONFIG_PROP_MAX_VARS);
    } catch (BadPropertyValueException e) {
      throw new InternalCompilerException("Could not find property "
          + CONFIG_PROP_MAX_VARS, e);
    }
    int t = Integer.parseInt(prop.getValues().get(0));
    return t;
  }

  /**
   * Make a new, empty {@link JsVars} that is a child of x.
   */
  private JsVars makeNewChildVars(JsVars x) {
    return new JsVars(x.getSourceInfo());
  }
}
