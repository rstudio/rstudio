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
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.History;

/**
 * A widget that serves as an "internal" hyperlink. That is, it is a link to
 * another state of the running application. When clicked, it will create a new
 * history frame using {@link com.google.gwt.user.client.History#newItem}, but
 * without reloading the page.
 * 
 * <p>
 * Being a true hyperlink, it is also possible for the user to "right-click,
 * open link in new window", which will cause the application to be loaded in a
 * new window at the state specified by the hyperlink.
 * </p>
 * 
 * <p>
 * <img class='gallery' src='Hyperlink.png'/>
 * </p>
 * 
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-Hyperlink { }</li>
 * </ul>
 * 
 * <p>
 * <h3>Example</h3> {@example com.google.gwt.examples.HistoryExample}
 * </p>
 */
public class Hyperlink extends Widget implements HasHTML, SourcesClickEvents {

  private Element anchorElem;
  private ClickListenerCollection clickListeners;
  private String targetHistoryToken;

  /**
   * Creates an empty hyperlink.
   */
  public Hyperlink() {
    setElement(DOM.createDiv());
    DOM.appendChild(getElement(), anchorElem = DOM.createAnchor());
    sinkEvents(Event.ONCLICK);
    setStyleName("gwt-Hyperlink");
  }

  /**
   * Creates a hyperlink with its text and target history token specified.
   * 
   * @param text the hyperlink's text
   * @param asHTML <code>true</code> to treat the specified text as html
   * @param targetHistoryToken the history token to which it will link
   * @see #setTargetHistoryToken
   */
  public Hyperlink(String text, boolean asHTML, String targetHistoryToken) {
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
  public Hyperlink(String text, String targetHistoryToken) {
    this();
    setText(text);
    setTargetHistoryToken(targetHistoryToken);
  }

  public void addClickListener(ClickListener listener) {
    if (clickListeners == null) {
      clickListeners = new ClickListenerCollection();
    }
    clickListeners.add(listener);
  }

  public String getHTML() {
    return DOM.getInnerHTML(anchorElem);
  }

  /**
   * Gets the history token referenced by this hyperlink.
   * 
   * @return the target history token
   * @see #setTargetHistoryToken
   */
  public String getTargetHistoryToken() {
    return targetHistoryToken;
  }

  public String getText() {
    return DOM.getInnerText(anchorElem);
  }

  public void onBrowserEvent(Event event) {
    if (DOM.eventGetType(event) == Event.ONCLICK) {
      if (clickListeners != null) {
        clickListeners.fireClick(this);
      }
      History.newItem(targetHistoryToken);
      DOM.eventPreventDefault(event);
    }
  }

  public void removeClickListener(ClickListener listener) {
    if (clickListeners != null) {
      clickListeners.remove(listener);
    }
  }

  public void setHTML(String html) {
    DOM.setInnerHTML(anchorElem, html);
  }

  /**
   * Sets the history token referenced by this hyperlink. This is the history
   * token that will be passed to {@link History#newItem} when this link is
   * clicked.
   * 
   * @param targetHistoryToken the new target history token
   */
  public void setTargetHistoryToken(String targetHistoryToken) {
    this.targetHistoryToken = targetHistoryToken;
    DOM.setAttribute(anchorElem, "href", "#" + targetHistoryToken);
  }

  public void setText(String text) {
    DOM.setInnerHTML(anchorElem, text);
  }
}
