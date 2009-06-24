/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.museum.client.common;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.LinkElement;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * An abstract issue that can be used in the code museum. Each
 * {@link AbstractIssue} should address a single issue. If at all possible, that
 * issue should be obvious from the initial ui state.
 */
public abstract class AbstractIssue implements EntryPoint {
  /**
   * Headline for this issue.
   */
  private String headline;

  /**
   * Creates the css associated with this issue.
   * 
   * @return link with css
   */
  public LinkElement createCSS() {
    String cssName;
    if (hasCSS()) {
      // Fetch the associated style sheet using an HTTP request
      cssName = getClass().getName();
      cssName = cssName.substring(cssName.lastIndexOf(".") + 1);
    } else {
      cssName = "Default";
    }

    String baseUrl = GWT.getModuleBaseURL();
    LinkElement issueLinkElement = Document.get().createLinkElement();
    issueLinkElement.setRel("stylesheet");
    issueLinkElement.setType("text/css");
    issueLinkElement.setHref(baseUrl + "issues/" + cssName + ".css");
    return issueLinkElement;
  }

  /**
   * <p>
   * Create a widget that illustrates the issue. Each issue should include a
   * detailed description of the expected results and the observed results
   * before the issue was fixed.
   * </p>
   * <p>
   * Note that createIssue will may be called multiple times if the user
   * refreshes the issue. If you save state within the instance, you must clear
   * it out and reset the issue when createIssue is called again.
   * </p>
   * 
   * @return a widget that can reproduce the issue
   */
  public abstract Widget createIssue();

  /**
   * Returns the "<i>classname</i>: summary".
   * 
   * @return a short summary of the issue, including the class name
   */
  public final String getHeadline() {
    if (headline == null) {
      String className = getClass().getName();
      headline = className.substring(className.lastIndexOf(".") + 1) + ": "
          + getSummary();
    }
    return headline;
  }

  /**
   * Return a detailed description of what the user should expect to see. The
   * description will be added above the example. You can also include
   * instructions to reproduce the issue.
   * 
   * @return instructions explaining what the user should see
   */
  public abstract String getInstructions();

  /**
   * Gets the summary for this test. All tests should include a summary so users
   * can scan through them quickly.
   */
  public abstract String getSummary();

  /**
   * Does the test have css?
   * 
   * @return true to load a CSS file of the same name, placed in the issues
   *         directory
   */
  public abstract boolean hasCSS();

  /**
   * Called immediately after the widget is attached.
   */
  public void onAttached() {
    // By default do nothing.
  }

  public void onModuleLoad() {
    Utility.getHeadElement().appendChild(createCSS());
    Window.setTitle(getHeadline());
    RootPanel.get().add(new HTML(getInstructions()));
    RootPanel.get().add(createIssue());
    onAttached();
  }

}
