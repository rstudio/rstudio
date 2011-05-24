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

import com.google.gwt.uibinder.rebind.IndentedWriter;
import com.google.gwt.uibinder.rebind.Tokenator;
import com.google.gwt.uibinder.rebind.Tokenator.Resolver;

import java.util.ArrayList;
import java.util.List;

/**
 * An individual SafeHtml template to be written by UiBinder.
 */
public class HtmlTemplate {
  private final List<String> template = new ArrayList<String>();
  private final String methodName;
  private final ArrayList<HtmlTemplateArgument> methodArgs = 
    new ArrayList<HtmlTemplateArgument>();
  private final HtmlTemplates templates;
  
  public HtmlTemplate(String html, Tokenator tokenator, HtmlTemplates templates)
    throws IllegalArgumentException {
    if (html == null) {
      throw new IllegalArgumentException("Template html cannot be null");
    }
    if (tokenator == null) {
      throw new IllegalArgumentException("Template tokenator cannot be null");
    }
    if (templates == null) { 
      throw new IllegalArgumentException("HtmlTemplates container cannot be null");
    }
    
    this.templates = templates;
    methodName = "html" + this.templates.nextTemplateId();
    
    populateArgMap(html,tokenator);
    
    template.add("@Template(\"" + addTemplatePlaceholders(html) + "\")");
    template.add("SafeHtml " + methodName + "(" + getTemplateArgs() + ");");
    template.add(" ");
  }
  
  public List<String> getTemplate() {
    return template;
  }
    
  /**
   * Writes all templates to the provided {@link IndentedWriter}.
   * 
   * @param w the writer to write the template to
   */
  public void writeTemplate(IndentedWriter w) {
    for (String s : template) {
      w.write(s);
    }
  }

  /**
   * Creates the template method invocation.
   * 
   * @return String the template method call with parameters
   */
  public String writeTemplateCall() {
    return "template." + methodName + "(" + getSafeHtmlArgs() 
      + ").asString()";
  }

  /**
   *  Replaces string tokens with {} placeholders for SafeHtml templating.
   *  
   * @return the rendering string, with tokens replaced by {} placeholders
   */
  private String addTemplatePlaceholders(String html) {
    String rtn = Tokenator.detokenate(html, new Resolver() {
      int tokenId = 0;
      public String resolveToken(String token) {
        return "{" + tokenId++ + "}";
      }
    });
    return rtn;
  }

  /**
   * Retrieves the arguments for SafeHtml template function call from 
   * the {@link Tokenator}.
   */
  private String getSafeHtmlArgs() {
    StringBuilder b = new StringBuilder();
    
    for (HtmlTemplateArgument arg : methodArgs) {
      if (b.length() > 0) {
        b.append(", ");
      }
      b.append(arg.getArg());  
    }
      
    return b.toString();
  }
  
  /**
   * Creates the argument string for the generated SafeHtmlTemplate function.
   */
  private String getTemplateArgs() {
    StringBuilder b = new StringBuilder();
    int i = 0;
    
       for (HtmlTemplateArgument arg : methodArgs) {
         if (b.length() > 0) {
           b.append(", ");
         }
        b.append(arg.getType() + " arg" + i);
        i++;
      }
    
    return b.toString();
  }
  
  private void populateArgMap(String s, Tokenator t) {
    if (t != null) {
      List<String> args = t.getOrderedValues(s);
      
      for (String arg : args) {
        if (templates.isSafeConstant(arg)) {
          methodArgs.add(HtmlTemplateArgument.forHtml(arg));
        } else {
          methodArgs.add(HtmlTemplateArgument.forString(arg.substring(4, 
              arg.length() - 4)));
        }
      }
    }
  }
}

