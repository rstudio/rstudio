/*
 * Copyright 2011 Google Inc.
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

package com.google.gwt.uibinder.rebind.model;

import com.google.gwt.uibinder.rebind.FieldManager;
import com.google.gwt.uibinder.rebind.IndentedWriter;
import com.google.gwt.uibinder.rebind.MortalLogger;
import com.google.gwt.uibinder.rebind.Tokenator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Model class for SafeHtml templates used in generated UiBinder rendering
 * implementation.
 */

public class HtmlTemplatesWriter {
  private final List<HtmlTemplateMethodWriter> htmlTemplates = new ArrayList<HtmlTemplateMethodWriter>();
  private final Set<String> safeConstantExpressions = new HashSet<String>();
  private final Set<String> uriExpressions = new HashSet<String>();
  private final FieldManager fieldManager;
  private final MortalLogger logger;

  public HtmlTemplatesWriter(FieldManager fieldManager, MortalLogger logger) {
    this.fieldManager = fieldManager;
    this.logger = logger;
  }

  /**
   * Add a SafeHtml template and an instance method for invoking the template to
   * the generated BinderImpl class. These templates are declared at the
   * beginning of the class and instantiated with a GWT.create(Template.class
   * call.
   * <p>
   * Note that the UiBinder#tokenator is used to determine the arguments to the
   * generated SafeHtml template.
   * 
   * @return the object that models this template method
   */
  public HtmlTemplateMethodWriter addSafeHtmlTemplate(String html, Tokenator t) throws IllegalArgumentException {
    if (html == null) {
      throw new IllegalArgumentException("Template html cannot be null");
    }
    if (t == null) {
      throw new IllegalArgumentException("Template tokenator cannot be null");
    }

    HtmlTemplateMethodWriter method = new HtmlTemplateMethodWriter(html, t, this);
    htmlTemplates.add(method);

    return method;
  }

  public int getNumTemplates() {
    return htmlTemplates.size();
  }

  public List<HtmlTemplateMethodWriter> getTemplates() {
    return htmlTemplates;
  }

  public boolean isEmpty() {
    return htmlTemplates.isEmpty();
  }

  public boolean isSafeConstant(String expression) {
    return safeConstantExpressions.contains(expression);
  }

  public boolean isUri(String expression) {
    return uriExpressions.contains(expression);
  }

  public void noteSafeConstant(String expression) {
    safeConstantExpressions.add(expression);
  }

  public void noteUri(String expression) {
    uriExpressions.add(expression);
  }

  /**
   * Write the SafeHtmlTemplates interface and its GWT.create() call.
   */
  public void writeInterface(IndentedWriter w) {
    w.write("interface Template extends SafeHtmlTemplates {");
    w.indent();
    for (HtmlTemplateMethodWriter t : htmlTemplates) {
      t.writeTemplateMethod(w);
    }
    w.outdent();
    w.write("}");
    w.newline();
    w.write("Template template = GWT.create(Template.class);");
  }

  /**
   * Write the no-arg methods that that call each template method. These are
   * called from HTMLPanelParser constructors and such.
   */
  public void writeTemplateCallers(IndentedWriter w) {
    for (HtmlTemplateMethodWriter t1 : htmlTemplates) {
      t1.writeTemplateCaller(w);
    }
  }

  FieldManager getFieldManager() {
    return fieldManager;
  }

  MortalLogger getLogger() {
    return logger;
  }

  /**
   * Increment the total number of templates.
   */
  int nextTemplateId() {
    return htmlTemplates.size() + 1;
  }
}
