/*
 * Copyright 2006 Google Inc.
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

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;

/**
 * A widget that can contain arbitrary HTML.
 * 
 * <p>
 * If you only need a simple label (text, but not HTML), then the
 * {@link com.google.gwt.user.client.ui.Label} widget is more appropriate, as it
 * disallows the use of HTML, which can lead to potential security issues if not
 * used properly.
 * </p>
 * 
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-HTML { }</li>
 * </ul>
 * 
 * <p>
 * <h3>Example</h3> {@example com.google.gwt.examples.HTMLExample}
 * </p>
 */
public class HTML extends Label implements HasHTML {

  /**
   * Creates an empty HTML widget.
   */
  public HTML() {
    setElement(DOM.createDiv());
    sinkEvents(Event.ONCLICK | Event.MOUSEEVENTS);
    setStyleName("gwt-HTML");
  }

  /**
   * Creates an HTML widget with the specified HTML contents.
   * 
   * @param html the new widget's HTML contents
   */
  public HTML(String html) {
    this();
    setHTML(html);
  }

  /**
   * Creates an HTML widget with the specified contents, optionally treating it
   * as HTML, and optionally disabling word wrapping.
   * 
   * @param html the widget's contents
   * @param wordWrap <code>false</code> to disable word wrapping
   */
  public HTML(String html, boolean wordWrap) {
    this(html);
    setWordWrap(wordWrap);
  }

  public String getHTML() {
    return DOM.getInnerHTML(getElement());
  }

  public void setHTML(String html) {
    DOM.setInnerHTML(getElement(), html);
  }
}
