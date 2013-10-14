/*
 * Copyright 2013 Google Inc.
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

package com.google.gwt.resources.rg;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.resources.ext.AbstractResourceGenerator;
import com.google.gwt.resources.ext.ResourceContext;
import com.google.gwt.user.rebind.SourceWriter;

/**
 * Base class for CSS resource generators.
 */
public abstract class AbstractCssResourceGenerator extends AbstractResourceGenerator {

  /**
   * Returns the java expression that contains the compiled CSS.
   *
   * @throws UnableToCompleteException
   */
  protected abstract String getCssExpression(TreeLogger logger, ResourceContext context,
      JMethod method) throws UnableToCompleteException;

  protected boolean isReturnTypeString(JClassType classReturnType) {
    return (classReturnType != null
        && String.class.getName().equals(classReturnType.getQualifiedSourceName()));
  }

  protected void writeEnsureInjected(SourceWriter sw) {
    sw.println("private boolean injected;");
    sw.println("public boolean ensureInjected() {");
    sw.indent();
    sw.println("if (!injected) {");
    sw.indentln("injected = true;");
    sw.indentln(StyleInjector.class.getName() + ".inject(getText());");
    sw.indentln("return true;");
    sw.println("}");
    sw.println("return false;");
    sw.outdent();
    sw.println("}");
  }

  protected void writeGetName(JMethod method, SourceWriter sw) {
    sw.println("public String getName() {");
    sw.indentln("return \"" + method.getName() + "\";");
    sw.println("}");
  }

  protected void writeGetText(TreeLogger logger, ResourceContext context, JMethod method,
      SourceWriter sw) throws UnableToCompleteException {
    String cssExpression = getCssExpression(logger, context, method);

    sw.println("public String getText() {");
    sw.indentln("return " + cssExpression + ";");
    sw.println("}");
  }

  protected void writeSimpleGetter(JMethod methodToImplement, String toReturn, SourceWriter sw) {
    sw.print(methodToImplement.getReadableDeclaration(false, true, true, true, true));
    sw.println(" {");
    sw.indentln("return " + toReturn + ";");
    sw.println("}");
  }
}
