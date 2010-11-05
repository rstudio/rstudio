/*
 * Copyright 2010 Google Inc.
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

import com.google.gwt.dom.client.Element;
import com.google.gwt.i18n.client.BidiUtils;
import com.google.gwt.i18n.client.HasDirection.Direction;
import com.google.gwt.i18n.shared.BidiFormatter;
import com.google.gwt.i18n.shared.DirectionEstimator;
import com.google.gwt.i18n.shared.HasDirectionEstimator;
import com.google.gwt.i18n.shared.WordCountDirectionEstimator;

/**
 * A helper class for displaying bidi (i.e. potentially opposite-direction) text 
 * or HTML in an element.
 * Note: this class assumes that callers perform all their text/html and
 * direction manipulations through it alone.
 */
public class DirectionalTextHelper implements HasDirectionEstimator {

  /**
   * A default direction estimator instance.
   */
  public static final DirectionEstimator DEFAULT_DIRECTION_ESTIMATOR =
      WordCountDirectionEstimator.get();

  /**
   * The DirectionEstimator object.
   */
  private DirectionEstimator directionEstimator;

  /**
   * The target element.
   */
  private final Element element;

  /**
   * The initial direction of the element.
   */
  private Direction initialElementDir;

  /**
   * Whether direction was explicitly set on the last {@code setTextOrHtml}
   * call. If so, {@link #setDirectionEstimator} will refrain from modifying the
   * direction until {@link #setTextOrHtml} is called without specifying an
   * explicit direction.
   */
  private boolean isDirectionExplicitlySet;

  /**
   * Whether the element is inline (e.g. a &lt;span&gt; element, but not a block
   * element like &lt;div&gt;).
   * This is needed because direction is handled differently for inline elements
   * and for non-inline elements.
   */
  private final boolean isElementInline;

  /**
   * Whether the element contains a nested &lt;span&gt; element used to
   * indicate the content's direction.
   * <p>
   * The element itself is used for this purpose when it is a block element
   * (i.e. !isElementInline), but doing so on an inline element often results in
   * garbling what follows it. Thus, when the element is inline, a nested
   * &lt;span&gt; must be used to carry the content's direction, with an LRM or
   * RLM character afterwards to prevent the garbling.
   */
  private boolean isSpanWrapped;

  /**
   * The direction of the element's content.
   * Note: this may not match the direction attribute of the element itself.
   * See
   * {@link #setTextOrHtml(String, com.google.gwt.i18n.client.HasDirection.Direction, boolean) setTextOrHtml(String, Direction, boolean)}
   * for details.
   */
  private Direction textDir;

  /**
   * @param element The widget's element holding text.
   * @param isElementInline Whether the element is an inline element.
   */
  public DirectionalTextHelper(Element element, boolean isElementInline) {
    this.element = element;
    this.isElementInline = isElementInline;
    isSpanWrapped = false;
    this.initialElementDir = BidiUtils.getDirectionOnElement(element);
    textDir = initialElementDir;
    // setDirectionEstimator shouldn't refresh appearance of initial empty text.
    isDirectionExplicitlySet = true;
  }

  public DirectionEstimator getDirectionEstimator() {
    return directionEstimator;
  }

  public Direction getTextDirection() {
    return textDir;
  }

  /**
   * Get the inner text or html of the element, taking the inner span wrap into
   * consideration, if needed.
   * 
   * @param isHtml true to get the inner html, false to get the inner text
   * @return the text or html
   */
  public String getTextOrHtml(boolean isHtml) {
    Element elem = isSpanWrapped ? element.getFirstChildElement() : element;
    return isHtml ? elem.getInnerHTML() : elem.getInnerText();
  }

  /**
   * Provides implementation for HasDirection's method setDirection (normally
   * deprecated), dealing with backwards compatibility issues.
   * @deprecated
   */
  @Deprecated
  public void setDirection(Direction direction) {
    BidiUtils.setDirectionOnElement(element, direction);
    initialElementDir = direction;

    /* 
     * For backwards compatibility, assure there's no span wrap, and update the
     * content direction.
     */
    setInnerTextOrHtml(getTextOrHtml(true), true);
    isSpanWrapped = false;
    textDir = initialElementDir;
    isDirectionExplicitlySet = true;
  }

  /**
   * See note at
   * {@link #setDirectionEstimator(com.google.gwt.i18n.shared.DirectionEstimator)}.
   */
  public void setDirectionEstimator(boolean enabled) {
    setDirectionEstimator(enabled ? DEFAULT_DIRECTION_ESTIMATOR : null);
  }

  /**
   * Note: if the element already has non-empty content, this will update
   * its direction according to the new estimator's result. This may cause
   * flicker, and thus should be avoided; DirectionEstimator should be set
   * before the element has any content.
   */
  public void setDirectionEstimator(DirectionEstimator directionEstimator) {
    this.directionEstimator = directionEstimator;
    /* 
     * Refresh appearance unless direction was explicitly set on last
     * setTextOrHtml call.
     */
    if (!isDirectionExplicitlySet) {
      setTextOrHtml(getTextOrHtml(true), true);
    }
  }

  /**
   * Sets the element's content to the given value (either plain text or HTML).
   * If direction estimation is off, the direction is verified to match the
   * element's initial direction. Otherwise, the direction is affected as
   * described at
   * {@link #setTextOrHtml(String, com.google.gwt.i18n.client.HasDirection.Direction, boolean) setTextOrHtml(String, Direction, boolean)}.
   *
   * @param content the element's new content
   * @param isHtml whether the content is HTML
   */
  public void setTextOrHtml(String content, boolean isHtml) {
    if (directionEstimator == null) {
      isSpanWrapped = false;
      setInnerTextOrHtml(content, isHtml);

      /*
       * Preserves the initial direction of the element. This is different from
       * passing the direction parameter explicitly as DEFAULT, which forces the
       * element to inherit the direction from its parent.
       */
      if (textDir != initialElementDir) {
        textDir = initialElementDir;
        BidiUtils.setDirectionOnElement(element, initialElementDir);
      }
    } else {
      setTextOrHtml(content, directionEstimator.estimateDirection(content,
          isHtml), isHtml);
    }
    isDirectionExplicitlySet = false;
  }

  /**
   * Sets the element's content to the given value (either plain text or HTML),
   * applying the given direction.
   * <p>
   * Implementation details:
   * <ul>
   * <li> If the element is a block element, sets its dir attribute according
   * to the given direction.
   * <li> Otherwise (i.e. the element is inline), the direction is set using a
   * nested &lt;span dir=...&gt; element which holds the content of the element.
   * This nested span may be followed by a zero-width Unicode direction
   * character (LRM or RLM). This manipulation is necessary to prevent garbling
   * in case the direction of the element is opposite to the direction of its
   * context. See {@link com.google.gwt.i18n.shared.BidiFormatter} for more
   * details.
   * </ul>
   *
   * @param content the element's new content
   * @param dir the content's direction
   * @param isHtml whether the content is HTML
   */
  public void setTextOrHtml(String content, Direction dir, boolean isHtml) {
    textDir = dir;
    // Set the text and the direction.
    if (isElementInline) {
      isSpanWrapped = true;
      element.setInnerHTML(BidiFormatter.getInstanceForCurrentLocale(
          true /* alwaysSpan */).spanWrapWithKnownDir(dir, content, isHtml));
    } else {
      isSpanWrapped = false;
      BidiUtils.setDirectionOnElement(element, dir);
      setInnerTextOrHtml(content, isHtml);
    }
    isDirectionExplicitlySet = true;
  }

  private void setInnerTextOrHtml(String content, boolean isHtml) {
    if (isHtml) {
      element.setInnerHTML(content);
    } else {
      element.setInnerText(content);
    }
  }
}
