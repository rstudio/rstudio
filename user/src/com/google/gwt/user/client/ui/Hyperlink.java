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

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.HasDirection.Direction;
import com.google.gwt.i18n.shared.DirectionEstimator;
import com.google.gwt.i18n.shared.HasDirectionEstimator;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.impl.HyperlinkImpl;

/**
 * A widget that serves as an "internal" hyperlink. That is, it is a link to
 * another state of the running application. When clicked, it will create a new
 * history frame using {@link com.google.gwt.user.client.History#newItem}, but
 * without reloading the page.
 * 
 * <p>
 * If you want an HTML hyperlink (&lt;a&gt; tag) without interacting with the
 * history system, use {@link Anchor} instead.
 * </p>
 * 
 * <p>
 * Being a true hyperlink, it is also possible for the user to "right-click,
 * open link in new window", which will cause the application to be loaded in a
 * new window at the state specified by the hyperlink.
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
 * <p>
 * <img class='gallery' src='doc-files/Hyperlink.png'/>
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
 * 
 * @see Anchor
 */
@SuppressWarnings("deprecation")
public class Hyperlink extends Widget implements HasHTML, SourcesClickEvents,
    HasClickHandlers, HasDirectionEstimator, HasDirectionalSafeHtml {

  public static final DirectionEstimator DEFAULT_DIRECTION_ESTIMATOR =
      DirectionalTextHelper.DEFAULT_DIRECTION_ESTIMATOR;

  private static HyperlinkImpl impl = GWT.create(HyperlinkImpl.class);

  protected final DirectionalTextHelper directionalTextHelper;
  private final Element anchorElem = DOM.createAnchor();
  private String targetHistoryToken;

  /**
   * Creates an empty hyperlink.
   */
  public Hyperlink() {
    this(DOM.createDiv());
  }

  /**
   * Creates a hyperlink with its html and target history token specified.
   *
   * @param html the hyperlink's safe html
   * @param targetHistoryToken the history token to which it will link
   * @see #setTargetHistoryToken
   */
  public Hyperlink(SafeHtml html, String targetHistoryToken) {
    this(html.asString(), true, targetHistoryToken);
  }

  /**
   * Creates a hyperlink with its html and target history token specified.
   *
   * @param html the hyperlink's safe html
   * @param dir the html's direction
   * @param targetHistoryToken the history token to which it will link
   * @see #setTargetHistoryToken
   */
  public Hyperlink(SafeHtml html, Direction dir, String targetHistoryToken) {
    this(html.asString(), true, dir, targetHistoryToken);
  }
  
  /**
   * Creates a hyperlink with its html and target history token specified.
   *
   * @param html the hyperlink's safe html
   * @param directionEstimator A DirectionEstimator object used for automatic
   *          direction adjustment. For convenience,
   *          {@link #DEFAULT_DIRECTION_ESTIMATOR} can be used.
   * @param targetHistoryToken the history token to which it will link
   * @see #setTargetHistoryToken
   */
  public Hyperlink(SafeHtml html, DirectionEstimator directionEstimator,
      String targetHistoryToken) {
    this(html.asString(), true, directionEstimator, targetHistoryToken);
  }

  /**
   * Creates a hyperlink with its text and target history token specified.
   *
   * @param text the hyperlink's text
   * @param targetHistoryToken the history token to which it will link, which
   *          may not be null (use {@link Anchor} instead if you don't need
   *          history processing)
   */
  public Hyperlink(String text, String targetHistoryToken) {
    this(text, false, targetHistoryToken);
  }

  /**
   * Creates a hyperlink with its text and target history token specified.
   * 
   * @param text the hyperlink's text
   * @param dir the text's direction
   * @param targetHistoryToken the history token to which it will link, which
   *          may not be null (use {@link Anchor} instead if you don't need
   *          history processing)
   */
  public Hyperlink(String text, Direction dir, String targetHistoryToken) {
    this(text, false, dir, targetHistoryToken);
  }

  /**
   * Creates a hyperlink with its text and target history token specified.
   * 
   * @param text the hyperlink's text
   * @param directionEstimator A DirectionEstimator object used for automatic
   *          direction adjustment. For convenience,
   *          {@link #DEFAULT_DIRECTION_ESTIMATOR} can be used.
   * @param targetHistoryToken the history token to which it will link, which
   *          may not be null (use {@link Anchor} instead if you don't need
   *          history processing)
   */
  public Hyperlink(String text, DirectionEstimator directionEstimator,
      String targetHistoryToken) {
    this(text, false, directionEstimator, targetHistoryToken);
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
    directionalTextHelper.setTextOrHtml(text, asHTML);
    setTargetHistoryToken(targetHistoryToken);
  }
  
  protected Hyperlink(Element elem) {
    if (elem == null) {
      setElement(anchorElem);
    } else {
      setElement(elem);
      DOM.appendChild(getElement(), anchorElem);
    }

    sinkEvents(Event.ONCLICK);
    setStyleName("gwt-Hyperlink");
    directionalTextHelper = new DirectionalTextHelper(anchorElem,
        /* is inline */ true);
  }

  /**
   * Creates a hyperlink with its text target history token specified.
   *
   * @param text the hyperlink's text
   * @param asHTML <code>true</code> to treat the specified text as html
   * @param dir the text's direction
   * @param targetHistoryToken the history token to which it will link
   * @see #setTargetHistoryToken
   */
  private Hyperlink(String text, boolean asHTML, Direction dir,
      String targetHistoryToken) {
    this();
    directionalTextHelper.setTextOrHtml(text, dir, asHTML);
    setTargetHistoryToken(targetHistoryToken);
  }

  /**
   * Creates a hyperlink with its text and target history token specified.
   *
   * @param text the hyperlink's text
   * @param asHTML <code>true</code> to treat the specified text as html
   * @param directionEstimator A DirectionEstimator object used for automatic
   *          direction adjustment. For convenience,
   *          {@link #DEFAULT_DIRECTION_ESTIMATOR} can be used.
   * @param targetHistoryToken the history token to which it will link
   * @see #setTargetHistoryToken
   */
  private Hyperlink(String text, boolean asHTML,
      DirectionEstimator directionEstimator, String targetHistoryToken) {
    this();
    directionalTextHelper.setDirectionEstimator(directionEstimator);
    directionalTextHelper.setTextOrHtml(text, asHTML);
    setTargetHistoryToken(targetHistoryToken);
  }

  /**
   * @deprecated Use {@link Anchor#addClickHandler} instead and call
   *     History.newItem from the handler if you need to process the
   *     click before the history token is set.
   */
  @Deprecated
  public HandlerRegistration addClickHandler(ClickHandler handler) {
    return addHandler(handler, ClickEvent.getType());
  }

  /**
   * @deprecated Use {@link Anchor#addClickHandler} instead and call
   *     History.newItem from the handler if you need to process the
   *     click before the history token is set.
   */
  @Deprecated
  public void addClickListener(ClickListener listener) {
    ListenerWrapper.WrappedClickListener.add(this, listener);
  }

  public DirectionEstimator getDirectionEstimator() {
    return directionalTextHelper.getDirectionEstimator();
  }

  public String getHTML() {
    return directionalTextHelper.getTextOrHtml(true);
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
    return directionalTextHelper.getTextOrHtml(false);
  }

  public Direction getTextDirection() {
    return directionalTextHelper.getTextDirection();
  }

  @Override
  public void onBrowserEvent(Event event) {
    super.onBrowserEvent(event);
    if (DOM.eventGetType(event) == Event.ONCLICK && impl.handleAsClick(event)) {
      History.newItem(getTargetHistoryToken());
      DOM.eventPreventDefault(event);
    }
  }

  /**
   * @deprecated Use the {@link HandlerRegistration#removeHandler}
   * method on the object returned by an add*Handler method instead
   */
  @Deprecated
  public void removeClickListener(ClickListener listener) {
    ListenerWrapper.WrappedClickListener.remove(this, listener);
  }

  /**
   * {@inheritDoc}
   * <p>
   * See note at {@link #setDirectionEstimator(DirectionEstimator)}.
   */
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
  public void setDirectionEstimator(DirectionEstimator directionEstimator) {
    directionalTextHelper.setDirectionEstimator(directionEstimator);
  }

  public void setHTML(SafeHtml html) {
    setHTML(html.asString());
  }

  public void setHTML(String html) {
    directionalTextHelper.setTextOrHtml(html, true);
  }

  public void setHTML(SafeHtml html, Direction dir) {
    directionalTextHelper.setTextOrHtml(html.asString(), dir, true);
  }

  /**
   * Sets the history token referenced by this hyperlink. This is the history
   * token that will be passed to {@link History#newItem} when this link is
   * clicked.
   *
   * @param targetHistoryToken the new history token, which may not be null (use
   *          {@link Anchor} instead if you don't need history processing)
   */
  public void setTargetHistoryToken(String targetHistoryToken) {
    assert targetHistoryToken != null
      : "targetHistoryToken must not be null, consider using Anchor instead";
    this.targetHistoryToken = targetHistoryToken;
    String hash = History.encodeHistoryToken(targetHistoryToken);
    DOM.setElementProperty(anchorElem, "href", "#" + hash);
  }

  public void setText(String text) {
    directionalTextHelper.setTextOrHtml(text, false);
  }

  public void setText(String text, Direction dir) {
    directionalTextHelper.setTextOrHtml(text, dir, false);
  }

  /**
   * <b>Affected Elements:</b>
   * <ul>
   * <li>-wrapper = the div around the link.</li>
   * </ul>
   * 
   * @see UIObject#onEnsureDebugId(String)
   */
  @Override
  protected void onEnsureDebugId(String baseID) {
    ensureDebugId(anchorElem, "", baseID);
    ensureDebugId(getElement(), baseID, "wrapper");
  }
}
