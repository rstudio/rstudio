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

import com.google.gwt.i18n.client.HasDirection.Direction;
import com.google.gwt.i18n.shared.DirectionEstimator;
import com.google.gwt.safehtml.shared.SafeHtml;

/**
 * A widget that serves as an "internal" hyperlink. That is, it is a link to
 * another state of the running application. It should behave exactly like
 * {@link com.google.gwt.user.client.ui.Hyperlink}, save that it lays out
 * as an inline element, not block.
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
   * Creates a hyperlink with its html and target history token specified.
   * 
   * @param html the hyperlink's html
   * @param dir the html's direction
   * @param targetHistoryToken the history token to which it will link
   * @see #setTargetHistoryToken
   */
  public InlineHyperlink(SafeHtml html, Direction dir,
      String targetHistoryToken) {
    this(html.asString(), true, dir, targetHistoryToken);
  }
  
  /**
   * Creates a hyperlink with its html and target history token specified.
   *
   * @param html the hyperlink's html
   * @param directionEstimator A DirectionEstimator object used for automatic
   *          direction adjustment. For convenience,
   *          {@link Hyperlink#DEFAULT_DIRECTION_ESTIMATOR} can be used.
   * @param targetHistoryToken the history token to which it will link
   * @see #setTargetHistoryToken
   */
  public InlineHyperlink(SafeHtml html, DirectionEstimator directionEstimator,
      String targetHistoryToken) {
    this(html.asString(), true, directionEstimator, targetHistoryToken);
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

  /**
   * Creates a hyperlink with its text and target history token specified.
   * 
   * @param text the hyperlink's text
   * @param dir the text's direction
   * @param targetHistoryToken the history token to which it will link
   */
  public InlineHyperlink(String text, Direction dir,
      String targetHistoryToken) {
    this(text, false, dir, targetHistoryToken);
  }

  /**
   * Creates a hyperlink with its text and target history token specified.
   * 
   * @param text the hyperlink's text
   * @param directionEstimator A DirectionEstimator object used for automatic
   *          direction adjustment. For convenience,
   *          {@link Hyperlink#DEFAULT_DIRECTION_ESTIMATOR} can be used.
   * @param targetHistoryToken the history token to which it will link
   */
  public InlineHyperlink(String text, DirectionEstimator directionEstimator,
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
  public InlineHyperlink(String text, boolean asHTML,
      String targetHistoryToken) {
    this();
    directionalTextHelper.setTextOrHtml(text, asHTML);
    setTargetHistoryToken(targetHistoryToken);
  }

  /**
   * Creates a hyperlink with its text and target history token specified.
   *
   * @param text the hyperlink's text
   * @param asHTML <code>true</code> to treat the specified text as html
   * @param dir the text's direction
   * @param targetHistoryToken the history token to which it will link
   * @see #setTargetHistoryToken
   */
  private InlineHyperlink(String text, boolean asHTML, Direction dir,
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
   *          {@link Hyperlink#DEFAULT_DIRECTION_ESTIMATOR} can be used.
   * @param targetHistoryToken the history token to which it will link
   * @see #setTargetHistoryToken
   */
  private InlineHyperlink(String text, boolean asHTML,
      DirectionEstimator directionEstimator, String targetHistoryToken) {
    this();
    directionalTextHelper.setDirectionEstimator(directionEstimator);
    directionalTextHelper.setTextOrHtml(text, asHTML);
    setTargetHistoryToken(targetHistoryToken);
  }
}
