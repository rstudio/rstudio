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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.resources.client.DataResource;
import com.google.gwt.resources.css.ast.Context;
import com.google.gwt.resources.css.ast.CssCompilerException;
import com.google.gwt.resources.css.ast.CssDef;
import com.google.gwt.resources.css.ast.CssProperty;
import com.google.gwt.resources.css.ast.CssUrl;
import com.google.gwt.resources.css.ast.CssVisitor;
import com.google.gwt.resources.css.ast.CssProperty.ExpressionValue;
import com.google.gwt.resources.css.ast.CssProperty.IdentValue;
import com.google.gwt.resources.css.ast.CssProperty.ListValue;
import com.google.gwt.resources.css.ast.CssProperty.Value;
import com.google.gwt.resources.ext.ResourceContext;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * Substitute symbolic replacements into string values.
 */
public class SubstitutionReplacer extends CssVisitor {
  private final ResourceContext context;
  private final TreeLogger logger;
  private final Map<String, CssDef> substitutions;

  public SubstitutionReplacer(TreeLogger logger, ResourceContext context,
      Map<String, CssDef> substitutions) {
    this.context = context;
    this.logger = logger;
    this.substitutions = substitutions;
  }

  @Override
  public void endVisit(CssProperty x, Context ctx) {
    if (x.getValues() == null) {
      // Nothing to do
      return;
    }

    List<Value> values = new ArrayList<Value>(x.getValues().getValues());

    for (ListIterator<Value> i = values.listIterator(); i.hasNext();) {
      IdentValue v = i.next().isIdentValue();

      if (v == null) {
        // Don't try to substitute into anything other than idents
        continue;
      }

      String value = v.getIdent();
      CssDef def = substitutions.get(value);

      if (def == null) {
        continue;
      } else if (def instanceof CssUrl) {
        assert def.getValues().size() == 1;
        assert def.getValues().get(0).isIdentValue() != null;
        String functionName = def.getValues().get(0).isIdentValue().getIdent();

        // Find the method
        JMethod methods[] = context.getClientBundleType().getOverridableMethods();
        boolean foundMethod = false;
        if (methods != null) {
          for (JMethod method : methods) {
            if (method.getName().equals(functionName)) {
              foundMethod = true;
              break;
            }
          }
        }

        if (!foundMethod) {
          logger.log(TreeLogger.ERROR, "Unable to find DataResource method "
              + functionName + " in "
              + context.getClientBundleType().getQualifiedSourceName());
          throw new CssCompilerException("Cannot find data function");
        }

        String instance = "((" + DataResource.class.getName() + ")("
            + context.getImplementationSimpleSourceName() + ".this."
            + functionName + "()))";

        StringBuilder expression = new StringBuilder();
        expression.append("\"url('\" + ");
        expression.append(instance).append(".getUrl()");
        expression.append(" + \"')\"");
        i.set(new ExpressionValue(expression.toString()));

      } else {
        i.remove();
        for (Value defValue : def.getValues()) {
          i.add(defValue);
        }
      }
    }

    x.setValue(new ListValue(values));
  }
}
