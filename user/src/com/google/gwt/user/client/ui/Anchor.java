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

import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.i18n.client.BidiUtils;
import com.google.gwt.i18n.client.HasDirection;

/**
 * A widget that represents a simple &lt;a&gt; element.
 * 
 * <p>
 * If you want use this anchor only for changing history states, use
 * {@link Hyperlink} instead.
 * </p>
 * 
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-Anchor { }</li>
 * </ul>
 * 
 * @see Hyperlink
 */
public class Anchor extends FocusWidget implements HasHorizontalAlignment,
    HasName, HasText, HasHTML, HasWordWrap, HasDirection {

  /**
   * Creates an Anchor widget that wraps an existing &lt;a&gt; element.
   * 
   * This element must already be attached to the document. If the element is
   * removed from the document, you must call
   * {@link RootPanel#detachNow(Widget)}.
   * 
   * @param element the element to be wrapped
   */
  public static Anchor wrap(Element element) {
    // Assert that the element is attached.
    assert Document.get().getBody().isOrHasChild(element);

    Anchor anchor = new Anchor(element);

    // Mark it attached and remember it for cleanup.
    anchor.onAttach();
    RootPanel.detachOnWindowClose(anchor);

    return anchor;
  }

  private HorizontalAlignmentConstant horzAlign;

  /**
   * Creates an empty anchor.
   */
  public Anchor() {
    setElement(Document.get().createAnchorElement());
    setStyleName("gwt-Anchor");
  }

  /**
   * Creates an anchor for scripting.
   * 
   * The anchor's href is set to <code>javascript:;</code>, based on the
   * expectation that listeners will be added to the anchor.
   * 
   * @param text the anchor's text
   */
  public Anchor(String text) {
    this(text, "javascript:;");
  }

  /**
   * Creates an anchor for scripting.
   * 
   * The anchor's href is set to <code>javascript:;</code>, based on the
   * expectation that listeners will be added to the anchor.
   * 
   * @param text the anchor's text
   * @param asHtml <code>true</code> to treat the specified text as html
   */
  public Anchor(String text, boolean asHtml) {
    this(text, asHtml, "javascript:;");
  }

  /**
   * Creates an anchor with its text and href (target URL) specified.
   * 
   * @param text the anchor's text
   * @param asHTML <code>true</code> to treat the specified text as html
   * @param href the url to which it will link
   */
  public Anchor(String text, boolean asHTML, String href) {
    this();
    if (asHTML) {
      setHTML(text);
    } else {
      setText(text);
    }
    setHref(href);
  }

  /**
   * Creates a source anchor (link to URI).
   * 
   * That is, an anchor with an href attribute specifying the destination URI.
   * 
   * @param text the anchor's text
   * @param asHtml asHTML <code>true</code> to treat the specified text as
   *          html
   * @param href the url to which it will link
   * @param target the target frame (e.g. "_blank" to open the link in a new
   *          window)
   */
  public Anchor(String text, boolean asHtml, String href, String target) {
    this(text, asHtml, href);
    setTarget(target);
  }

  /**
   * Creates an anchor with its text and href (target URL) specified.
   * 
   * @param text the anchor's text
   * @param href the url to which it will link
   */
  public Anchor(String text, String href) {
    this();
    setText(text);
    setHref(href);
  }

  /**
   * Creates a source anchor with a frame target.
   * 
   * @param text the anchor's text
   * @param href the url to which it will link
   * @param target the target frame (e.g. "_blank" to open the link in a new
   *          window)
   */
  public Anchor(String text, String href, String target) {
    this(text, false, href, target);
  }

  /**
   * This constructor may be used by subclasses to explicitly use an existing
   * element. This element must be an &lt;a&gt; element.
   * 
   * @param element the element to be used
   */
  protected Anchor(Element element) {
    AnchorElement.as(element);
    setElement(element);
  }

  public Direction getDirection() {
    return BidiUtils.getDirectionOnElement(getElement());
  }

  public HorizontalAlignmentConstant getHorizontalAlignment() {
    return horzAlign;
  }

  /**
   * Gets the anchor's href (the url to which it links).
   * 
   * @return the anchor's href
   */
  public String getHref() {
    return getAnchorElement().getHref();
  }

  public String getHTML() {
    return getElement().getInnerHTML();
  }

  public String getName() {
    return getAnchorElement().getName();
  }

  @Override
  public int getTabIndex() {
    return getAnchorElement().getTabIndex();
  }

  /**
   * Gets the anchor's target frame (the frame in which navigation will occur
   * when the link is selected).
   * 
   * @return the target frame
   */
  public String getTarget() {
    return getAnchorElement().getTarget();
  }

  public String getText() {
    return getElement().getInnerText();
  }

  public boolean getWordWrap() {
    return !getElement().getStyle().getProperty("whiteSpace").equals("nowrap");
  }

  @Override
  public void setAccessKey(char key) {
    getAnchorElement().setAccessKey(Character.toString(key));
  }

  public void setDirection(Direction direction) {
    BidiUtils.setDirectionOnElement(getElement(), direction);
  }

  @Override
  public void setFocus(boolean focused) {
    if (focused) {
      getAnchorElement().focus();
    } else {
      getAnchorElement().blur();
    }
  }

  public void setHorizontalAlignment(HorizontalAlignmentConstant align) {
    horzAlign = align;
    getElement().getStyle().setProperty("textAlign", align.getTextAlignString());
  }

  /**
   * Sets the anchor's href (the url to which it links).
   * 
   * @param href the anchor's href
   */
  public void setHref(String href) {
    getAnchorElement().setHref(href);
  }

  public void setHTML(String html) {
    getElement().setInnerHTML(html);
  }

  public void setName(String name) {
    getAnchorElement().setName(name);
  }

  @Override
  public void setTabIndex(int index) {
    getAnchorElement().setTabIndex(index);
  }

  /**
   * Sets the anchor's target frame (the frame in which navigation will occur
   * when the link is selected).
   * 
   * @param target the target frame
   */
  public void setTarget(String target) {
    getAnchorElement().setTarget(target);
  }

  public void setText(String text) {
    getElement().setInnerText(text);
  }

  public void setWordWrap(boolean wrap) {
    getElement().getStyle().setProperty("whiteSpace",
        wrap ? "normal" : "nowrap");
  }

  private AnchorElement getAnchorElement() {
    return AnchorElement.as(getElement());
  }
}
