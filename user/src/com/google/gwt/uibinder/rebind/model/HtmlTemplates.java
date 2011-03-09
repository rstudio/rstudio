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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Model class for SafeHtml templates used in generated 
 * UiBinder rendering implementation.
 */

public class HtmlTemplates {
  private final List<HtmlTemplate> htmlTemplates = new ArrayList<HtmlTemplate>();
  private final Set<String> safeConstantTokens = new HashSet<String>();
  
  public HtmlTemplates() {
  }
  
  /**
   * Add a SafeHtml template and an instance method for invoking the template 
   * to the generated BinderImpl class.  These templates are declared at the 
   * beginning of the class and instantiated with a GWT.create(Template.class 
   * call.
   * <p>
   * Note that the UiBinder#tokenator is used to determine the arguments to
   * the generated SafeHtml template.
   * 
   * @return String the function to call this template
   */
  public String addSafeHtmlTemplate(String html, Tokenator t)
    throws IllegalArgumentException {
    if (html == null) {
      throw new IllegalArgumentException("Template html cannot be null");
    }
    if (t == null) {
      throw new IllegalArgumentException("Template tokenator cannot be null");
    }
    
    HtmlTemplate ht = new HtmlTemplate(html, t, this);
    htmlTemplates.add(ht);
  
    return ht.writeTemplateCall();
  }
    
  public int getNumTemplates() {
    return htmlTemplates.size();
  }
  
  public List<HtmlTemplate> getTemplates() {
    return htmlTemplates;
  }
  
  public boolean isEmpty() {
    return htmlTemplates.isEmpty();
  }
  
  public boolean isSafeConstant(String token) {
    return safeConstantTokens.contains(token);
  }
  
  public void noteSafeConstant(String token) {
    safeConstantTokens.add(token);
  }

  public void writeTemplates(IndentedWriter w) {
    for (HtmlTemplate t : htmlTemplates) {
      t.writeTemplate(w);
    }
  }
  
  /**
   * Increment the total number of templates.
   */
  protected int nextTemplateId() {
   return htmlTemplates.size() + 1; 
  }
}
