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
import com.google.gwt.dom.client.Style.WhiteSpace;
import com.google.gwt.i18n.client.BidiUtils;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.i18n.shared.DirectionEstimator;
import com.google.gwt.i18n.shared.HasDirectionEstimator;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.annotations.IsSafeHtml;
import com.google.gwt.safehtml.shared.annotations.IsSafeUri;
import com.google.gwt.safehtml.shared.annotations.SuppressIsSafeHtmlCastCheck;

/**
 * A widget that represents a simple &lt;a&gt; element.
 * 
 * <p>
 * If you want use this anchor only for changing history states, use
 * {@link Hyperlink} instead.
 * </p>
 *
 * <p>
 * <h3>Built-in Bidi Text Support</h3>
 * This widget is capable of automatically adjusting its direction according to
 * its content. This feature is controlled by {@link #setDirectionEstimator} or
 * passing a DirectionEstimator parameter to the constructor, and is off by
 * default.
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
    HasName, HasHTML, HasWordWrap, HasDirection,
    HasDirectionEstimator, HasDirectionalSafeHtml {

  public static final DirectionEstimator DEFAULT_DIRECTION_ESTIMATOR =
      DirectionalTextHelper.DEFAULT_DIRECTION_ESTIMATOR;

  /**
   * The default HREF is a no-op javascript statement. We need an href to ensure
   * that the browser renders the anchor with native styles, such as underline
   * and font color.
   */
  private static final String DEFAULT_HREF = "javascript:;";

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

  private final DirectionalTextHelper directionalTextHelper;

  private HorizontalAlignmentConstant horzAlign;

  /**
   * Creates an empty anchor.
   * 
   * <p>
   * The anchor's href is <em>not</em> set, which means that the widget will not
   * not be styled with the browser's native link styles (such as underline and
   * font color). Use {@link #Anchor(boolean)} to add a default no-op href that
   * does not open a link but ensures the native link styles are applied.
   * </p>
   * 
   * @see #Anchor(boolean)
   */
  public Anchor() {
    this(false);
  }

  /**
   * Creates an anchor.
   * 
   * The anchor's href is optionally set to <code>javascript:;</code>, based on
   * the expectation that listeners will be added to the anchor.
   * 
   * @param useDefaultHref true to set the default href to
   *          <code>javascript:;</code>, false to leave it blank
   */
  public Anchor(boolean useDefaultHref) {
    setElement(Document.get().createAnchorElement());
    setStyleName("gwt-Anchor");
    directionalTextHelper = new DirectionalTextHelper(getAnchorElement(),
        /* is inline */true);
    if (useDefaultHref) {
      setHref(DEFAULT_HREF);
    }
  }

  /**
   * Creates an anchor for scripting.
   *
   * @param html the anchor's html
   */
  public Anchor(SafeHtml html) {
    this(html.asString(), true);
  }

  /**
   * Creates an anchor for scripting.
   *
   * The anchor's href is set to <code>javascript : ;</code>, based on the
   * expectation that listeners will be added to the anchor.
   *
   * @param html the anchor's html
   * @param dir the html's direction
   */
  public Anchor(SafeHtml html, Direction dir) {
    this(html.asString(), true, dir, DEFAULT_HREF);
  }

  /**
   * Creates an anchor for scripting.
   *
   * The anchor's href is set to <code>javascript : ;</code>, based on the
   * expectation that listeners will be added to the anchor.
   *
   * @param html the anchor's html
   * @param directionEstimator A DirectionEstimator object used for automatic
   *          direction adjustment. For convenience,
   *          {@link #DEFAULT_DIRECTION_ESTIMATOR} can be used.
   */
  public Anchor(SafeHtml html, DirectionEstimator directionEstimator) {
    this(html.asString(), true, directionEstimator, DEFAULT_HREF);
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
    this(text, DEFAULT_HREF);
  }

  /**
   * Creates an anchor for scripting.
   *
   * The anchor's href is set to <code>javascript : ;</code>, based on the
   * expectation that listeners will be added to the anchor.
   *
   * @param text the anchor's text
   * @param dir the text's direction
   */
  public Anchor(String text, Direction dir) {
    this(text, dir, DEFAULT_HREF);
  }

  /**
   * Creates an anchor for scripting.
   *
   * The anchor's href is set to <code>javascript : ;</code>, based on the
   * expectation that listeners will be added to the anchor.
   *
   * @param text the anchor's text
   * @param directionEstimator A DirectionEstimator object used for automatic
   *          direction adjustment. For convenience,
   *          {@link #DEFAULT_DIRECTION_ESTIMATOR} can be used.
   */
  public Anchor(String text, DirectionEstimator directionEstimator) {
    this(text, directionEstimator, DEFAULT_HREF);
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
  public Anchor(@IsSafeHtml String text, boolean asHtml) {
    this(text, asHtml, DEFAULT_HREF);
  }

  /**
   * Creates an anchor with its html and href (target URL) specified.
   *
   * @param html the anchor's html
   * @param href the url to which it will link
   */
  public Anchor(SafeHtml html, @IsSafeUri String href) {
    this(html.asString(), true, href);
  }

  /**
   * Creates an anchor with its html and href (target URL) specified.
   *
   * @param html the anchor's html
   * @param href the url to which it will link
   */
  public Anchor(SafeHtml html, SafeUri href) {
    this(html.asString(), true, href.asString());
  }

  /**
   *  Creates an anchor with its html and href (target URL) specified.
   *
   * @param html the anchor's html
   * @param dir the html's direction
   * @param href the url to which it will link
   */
  public Anchor(SafeHtml html, Direction dir, @IsSafeUri String href) {
    this(html.asString(), true, dir, href);
  }

  /**
   *  Creates an anchor with its html and href (target URL) specified.
   *
   * @param html the anchor's html
   * @param dir the html's direction
   * @param href the url to which it will link
   */
  public Anchor(SafeHtml html, Direction dir, SafeUri href) {
    this(html.asString(), true, dir, href.asString());
  }

  /**
   *  Creates an anchor with its html and href (target URL) specified.
   *
   * @param html the anchor's html
   * @param directionEstimator A DirectionEstimator object used for automatic
   *          direction adjustment. For convenience,
   *          {@link #DEFAULT_DIRECTION_ESTIMATOR} can be used.
   * @param href the url to which it will link
   */
  public Anchor(SafeHtml html, DirectionEstimator directionEstimator, @IsSafeUri String href) {
    this(html.asString(), true, directionEstimator, href);
  }

  /**
   *  Creates an anchor with its html and href (target URL) specified.
   *
   * @param html the anchor's html
   * @param directionEstimator A DirectionEstimator object used for automatic
   *          direction adjustment. For convenience,
   *          {@link #DEFAULT_DIRECTION_ESTIMATOR} can be used.
   * @param href the url to which it will link
   */
  public Anchor(SafeHtml html, DirectionEstimator directionEstimator,
      SafeUri href) {
    this(html.asString(), true, directionEstimator, href.asString());
  }

  /**
   * Creates an anchor with its text and href (target URL) specified.
   *
   * @param text the anchor's text
   * @param href the url to which it will link
   */
  @SuppressIsSafeHtmlCastCheck
  public Anchor(String text, @IsSafeUri String href) {
    this(text, false, href);
  }

  /**
   * Creates an anchor with its text and href (target URL) specified.
   *
   * @param text the anchor's text
   * @param dir the text's direction
   * @param href the url to which it will link
   */
  @SuppressIsSafeHtmlCastCheck
  public Anchor(String text, Direction dir, @IsSafeUri String href) {
    this(text, false, dir, href);
  }

  /**
   * Creates an anchor with its text and href (target URL) specified.
   *
   * @param text the anchor's text
   * @param directionEstimator A DirectionEstimator object used for automatic
   *          direction adjustment. For convenience,
   *          {@link #DEFAULT_DIRECTION_ESTIMATOR} can be used.
   * @param href the url to which it will link
   */
  @SuppressIsSafeHtmlCastCheck
  public Anchor(String text, DirectionEstimator directionEstimator, @IsSafeUri String href) {
    this(text, false, directionEstimator, href);
  }

  /**
   * Creates an anchor with its text and href (target URL) specified.
   *
   * @param text the anchor's text
   * @param asHTML <code>true</code> to treat the specified text as html
   * @param href the url to which it will link
   */
  public Anchor(@IsSafeHtml String text, boolean asHTML, @IsSafeUri String href) {
    this();
    directionalTextHelper.setTextOrHtml(text, asHTML);
    setHref(href);
  }

  /**
   * Creates a source anchor (link to URI).
   *
   * That is, an anchor with an href attribute specifying the destination URI.
   *
   * @param html the anchor's html
   * @param href the url to which it will link
   * @param target the target frame (e.g. "_blank" to open the link in a new
   *          window)
   */
  public Anchor(SafeHtml html, @IsSafeUri String href, String target) {
    this(html.asString(), true, href, target);
  }

  /**
   * Creates a source anchor (link to URI).
   *
   * That is, an anchor with an href attribute specifying the destination URI.
   *
   * @param html the anchor's html
   * @param href the url to which it will link
   * @param target the target frame (e.g. "_blank" to open the link in a new
   *          window)
   */
  public Anchor(SafeHtml html, SafeUri href, String target) {
    this(html.asString(), true, href.asString(), target);
  }

  /**
   * Creates a source anchor with a frame target.
   *
   * @param text the anchor's text
   * @param href the url to which it will link
   * @param target the target frame (e.g. "_blank" to open the link in a new
   *          window)
   */
  @SuppressIsSafeHtmlCastCheck
  public Anchor(String text, @IsSafeUri String href, String target) {
    this(text, false, href, target);
  }

  /**
   * Creates a source anchor (link to URI).
   *
   * That is, an anchor with an href attribute specifying the destination URI.
   *
   * @param text the anchor's text
   * @param asHtml asHTML <code>true</code> to treat the specified text as html
   * @param href the url to which it will link
   * @param target the target frame (e.g. "_blank" to open the link in a new
   *          window)
   */
  public Anchor(@IsSafeHtml String text, boolean asHtml, @IsSafeUri String href, String target) {
    this(text, asHtml, href);
    setTarget(target);
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
    directionalTextHelper = new DirectionalTextHelper(getAnchorElement(),
        /* is inline */ true);
  }

  /**
   * Creates an anchor with its text, direction and href (target URL) specified.
   *
   * @param text the anchor's text
   * @param asHTML <code>true</code> to treat the specified text as html
   * @param dir the text's direction
   * @param href the url to which it will link
   */
  private Anchor(@IsSafeHtml String text, boolean asHTML, Direction dir, @IsSafeUri String href) {
    this();
    directionalTextHelper.setTextOrHtml(text, dir, asHTML);
    setHref(href);
  }

  /**
   * Creates an anchor with its text, direction and href (target URL) specified.
   *
   * @param text the anchor's text
   * @param asHTML <code>true</code> to treat the specified text as html
   * @param directionEstimator A DirectionEstimator object used for automatic
   *          direction adjustment. For convenience,
   *          {@link #DEFAULT_DIRECTION_ESTIMATOR} can be used.
   * @param href the url to which it will link
   */
  private Anchor(
      @IsSafeHtml String text,
      boolean asHTML,
      DirectionEstimator directionEstimator,
      @IsSafeUri String href) {
    this();
    directionalTextHelper.setDirectionEstimator(directionEstimator);
    directionalTextHelper.setTextOrHtml(text, asHTML);
    setHref(href);
  }

  @Override
  public Direction getDirection() {
    return BidiUtils.getDirectionOnElement(getElement());
  }

  @Override
  public DirectionEstimator getDirectionEstimator() {
    return directionalTextHelper.getDirectionEstimator();
  }

  @Override
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

  @Override
  public String getHTML() {
    return getElement().getInnerHTML();
  }

  @Override
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

  @Override
  public String getText() {
    return directionalTextHelper.getText();
  }

  @Override
  public Direction getTextDirection() {
    return directionalTextHelper.getTextDirection();
  }

  @Override
  public boolean getWordWrap() {
    return !WhiteSpace.NOWRAP.getCssName().equals(getElement().getStyle().getWhiteSpace());
  }

  @Override
  public void setAccessKey(char key) {
    getAnchorElement().setAccessKey(Character.toString(key));
  }

  /**
   * @deprecated Use {@link #setDirectionEstimator} and / or pass explicit
   * direction to {@link #setText}, {@link #setHTML} instead
   */
  @Override
  @Deprecated
  public void setDirection(Direction direction) {
    directionalTextHelper.setDirection(direction);
  }

  /**
   * {@inheritDoc}
   * <p>
   * See note at {@link #setDirectionEstimator(DirectionEstimator)}.
   */
  @Override
  public void setDirectionEstimator(boolean enabled) {
    directionalTextHelper.setDirectionEstimator(enabled);
  }

  /**
   * {@inheritDoc}
   * <p>
   * Note: DirectionEstimator should be set before the widget has any content;
   * it's highly recommended to set it using a constructor. Reason: if the
   * widget already has non-empty content, this will update its direction
   * according to the new estimator's result. This may cause flicker, and thus
   * should be avoided.
   */
  @Override
  public void setDirectionEstimator(DirectionEstimator directionEstimator) {
    directionalTextHelper.setDirectionEstimator(directionEstimator);
  }

  @Override
  public void setFocus(boolean focused) {
    if (focused) {
      getAnchorElement().focus();
    } else {
      getAnchorElement().blur();
    }
  }

  @Override
  public void setHorizontalAlignment(HorizontalAlignmentConstant align) {
    horzAlign = align;
    getElement().getStyle().setProperty("textAlign", align.getTextAlignString());
  }

  /**
   * Sets the anchor's href (the url to which it links).
   *
   * @param href the anchor's href
   */
  public void setHref(SafeUri href) {
    getAnchorElement().setHref(href);
  }

  /**
   * Sets the anchor's href (the url to which it links).
   *
   * @param href the anchor's href
   */
  public void setHref(@IsSafeUri String href) {
    getAnchorElement().setHref(href);
  }

  @Override
  public void setHTML(SafeHtml html) {
    directionalTextHelper.setHtml(html);
  }

  @Override
  public void setHTML(@IsSafeHtml String html) {
    directionalTextHelper.setHtml(html);
  }

  @Override
  public void setHTML(SafeHtml html, Direction dir) {
    directionalTextHelper.setHtml(html, dir);
  }

  @Override
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

  @Override
  public void setText(String text) {
    directionalTextHelper.setText(text);
  }

  @Override
  public void setText(String text, Direction dir) {
    directionalTextHelper.setText(text, dir);
  }

  @Override
  public void setWordWrap(boolean wrap) {
    getElement().getStyle().setWhiteSpace(wrap ? WhiteSpace.NORMAL : WhiteSpace.NOWRAP);
  }

  private AnchorElement getAnchorElement() {
    return AnchorElement.as(getElement());
  }
}
