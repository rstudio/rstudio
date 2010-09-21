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

package com.google.gwt.user.client.ui;

import com.google.gwt.safehtml.shared.SafeHtml;

/**
 * A widget that serves as an "internal" hyperlink. That is, it is a link to
 * another state of the running application. It should behave exactly like
 * {@link com.google.gwt.user.client.ui.Hyperlink}, save that it lays out
 * as an inline element, not block.
 * 
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-InlineHyperlink { }</li>
 * </ul>
 */
public class InlineHyperlink extends Hyperlink {

  /**
   * Creates an empty hyperlink.
   */
  public InlineHyperlink() {
    super(null);

    setStyleName("gwt-InlineHyperlink");
  }

  /**
   * Creates a hyperlink with its html and target history token specified.
   *
   * @param html the hyperlink's html
   * @param targetHistoryToken the history token to which it will link
   * @see #setTargetHistoryToken
   */
  public InlineHyperlink(SafeHtml html, String targetHistoryToken) {
    this(html.asString(), true, targetHistoryToken);
  }

  /**
   * Creates a hyperlink with its text and target history token specified.
   *
   * @param text the hyperlink's text
   * @param asHTML <code>true</code> to treat the specified text as html
   * @param targetHistoryToken the history token to which it will link
   * @see #setTargetHistoryToken
   */
  public InlineHyperlink(String text, boolean asHTML, String targetHistoryToken) {
    this();

    if (asHTML) {
      setHTML(text);
    } else {
      setText(text);
    }
    setTargetHistoryToken(targetHistoryToken);
  }

  /**
   * Creates a hyperlink with its text and target history token specified.
   * 
   * @param text the hyperlink's text
   * @param targetHistoryToken the history token to which it will link
   */
  public InlineHyperlink(String text, String targetHistoryToken) {
    this(text, false, targetHistoryToken);
  }
}
