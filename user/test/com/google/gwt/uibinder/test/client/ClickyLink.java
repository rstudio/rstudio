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
package com.google.gwt.uibinder.test.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Widget;

/**
 * A silly little widget to make sure that silly custom widgets
 * actually work.
 */

public class ClickyLink extends Widget implements HasText {

  /**
   * This static method exists purely to ensure BeanParser handles overloaded
   * methods correctly.
   */
  public static void setPopupText(HasText t, String text) {
    t.setText(text);
  }

  private String popupText;

  /**
   * Creates an empty hyperlink.
   */
  public ClickyLink() {
    setElement(DOM.createAnchor());
    DOM.setElementAttribute(getElement(), "href", "#");
    sinkEvents(Event.ONCLICK);

    addDomHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        if (popupText != null) {
          Window.alert(popupText);
        }
        event.preventDefault();
      }
    }, ClickEvent.getType());
  }

  /**
   * Creates a hyperlink with its text specified.
   *
   * @param text the hyperlink's text
   */
  public ClickyLink(String text) {
    this();
    setText(text);
  }

  public String getPopupText() {
    return popupText;
  }

  public String getText() {
    return DOM.getInnerText(getElement());
  }

  public void setPopupText(String text) {
    popupText = text;
  }

  public void setText(String text) {
    DOM.setInnerText(getElement(), text);
  }
}
