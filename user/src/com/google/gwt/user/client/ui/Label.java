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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.editor.client.IsEditor;
import com.google.gwt.editor.client.LeafValueEditor;
import com.google.gwt.editor.client.adapters.HasTextEditor;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.HasAllMouseHandlers;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.HasDoubleClickHandlers;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.dom.client.MouseWheelEvent;
import com.google.gwt.event.dom.client.MouseWheelHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.BidiUtils;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.i18n.shared.BidiFormatter;
import com.google.gwt.i18n.shared.DirectionEstimator;
import com.google.gwt.i18n.shared.HasDirectionEstimator;
import com.google.gwt.i18n.shared.WordCountDirectionEstimator;
import com.google.gwt.safehtml.shared.SafeHtml;

/**
 * A widget that contains arbitrary text, <i>not</i> interpreted as HTML.
 *
 * This widget uses a &lt;div&gt; element, causing it to be displayed with block
 * layout.
 *
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-Label { }</li>
 * </ul>
 *
 * <p>
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.HTMLExample}
 * </p>
 */
@SuppressWarnings("deprecation")
public class Label extends Widget implements HasDirectionalText, HasWordWrap,
    HasDirection, HasClickHandlers, HasDoubleClickHandlers, SourcesClickEvents,
    SourcesMouseEvents, HasAllMouseHandlers, HasDirectionEstimator,
    HasAutoHorizontalAlignment, IsEditor<LeafValueEditor<String>> {

  /**
   * Creates a Label widget that wraps an existing &lt;div&gt; or &lt;span&gt;
   * element.
   *
   * This element must already be attached to the document. If the element is
   * removed from the document, you must call
   * {@link RootPanel#detachNow(Widget)}.
   *
   * @param element the element to be wrapped
   */
  public static Label wrap(Element element) {
    // Assert that the element is attached.
    assert Document.get().getBody().isOrHasChild(element);

    Label label = new Label(element);

    // Mark it attached and remember it for cleanup.
    label.onAttach();
    RootPanel.detachOnWindowClose(label);

    return label;
  }

  /**
   * The widget's auto horizontal alignment policy.
   * @see HasAutoHorizontalAlignment
   */
  private AutoHorizontalAlignmentConstant autoHorizontalAlignment;

  /**
   * The direction of the widget's content.
   * Note: this may not match the direction of the widget's top DOM element
   * ({@code getElement()}).
   * See {@link #setTextOrHtml(String, Direction, boolean)} for details.
   */
  private Direction textDir;

  /**
   * The widget's DirectionEstimator object.
   */
  private DirectionEstimator directionEstimator;

  /**
   * The widget's horizontal alignment.
   */
  private HorizontalAlignmentConstant horzAlign;

  /**
   * The initial direction of the widget's element.
   */
  private Direction initialElementDir;

  /**
   * Whether the widget is inline (a &lt;span&gt; element).
   * This is needed because direction is handled differently for inline elements
   * and for non-inline elements.
   * <p>
   * In case Label supports types of elements other than span and div, this
   * should get true for any element that is inline by default. Another approach
   * could be calculating the element's display property, but this may have some
   * overhead, and is problematic when the element is yet unattached.
   */
  private boolean isElementInline;

  /**
   * Whether the widget contains a nested &lt;span&gt; element used to
   * indicate the content's direction.
   * <p>
   * The widget's top element is used for this purpose when it is a &lt;div&gt;,
   * but doing so on an inline element often results in garbling what follows
   * it. Thus, when the widget's top element is a &lt;span&gt;, a nested
   * &lt;span&gt; must be used to carry the content's direction, with an LRM or
   * RLM character afterwards to prevent the garbling.
   */
  private boolean isSpanWrapped;

  /**
   * Creates an empty label.
   */
  public Label() {
    setElement(Document.get().createDivElement());
    setStyleName("gwt-Label");
    isElementInline = false;
    isSpanWrapped = false;
    textDir = Direction.DEFAULT;
    initialElementDir = Direction.DEFAULT;
  }

  /**
   * Creates a label with the specified text.
   *
   * @param text the new label's text
   */
  public Label(String text) {
    this();
    setText(text);
  }

  /**
   * Creates a label with the specified text and direction.
   *
   * @param text the new label's text
   * @param dir the text's direction. Note that {@code DEFAULT} means direction
   *        should be inherited from the widget's parent element.
   */
  public Label(String text, Direction dir) {
    this();
    setText(text, dir);
  }

  /**
   * Creates a label with the specified text.
   *
   * @param text the new label's text
   * @param wordWrap <code>false</code> to disable word wrapping
   */
  public Label(String text, boolean wordWrap) {
    this(text);
    setWordWrap(wordWrap);
  }

  /**
   * This constructor may be used by subclasses to explicitly use an existing
   * element. This element must be either a &lt;div&gt; or &lt;span&gt; element.
   *
   * @param element the element to be used
   */
  protected Label(Element element) {
    setElement(element);
    String tagName = element.getTagName();
    isElementInline = tagName.equalsIgnoreCase("span");
    assert isElementInline || tagName.equalsIgnoreCase("div");
    isSpanWrapped = false;
    initialElementDir = BidiUtils.getDirectionOnElement(element);
    textDir = initialElementDir;
  }

  public HandlerRegistration addClickHandler(ClickHandler handler) {
    return addDomHandler(handler, ClickEvent.getType());
  }

  /**
   * @deprecated Use {@link #addClickHandler} instead
   */
  @Deprecated
  public void addClickListener(ClickListener listener) {
    ListenerWrapper.WrappedClickListener.add(this, listener);
  }

  public HandlerRegistration addDoubleClickHandler(DoubleClickHandler handler) {
    return addDomHandler(handler, DoubleClickEvent.getType());
  }

  public HandlerRegistration addMouseDownHandler(MouseDownHandler handler) {
    return addDomHandler(handler, MouseDownEvent.getType());
  }

  /**
   * @deprecated Use {@link #addMouseOverHandler},
   * {@link #addMouseMoveHandler}, {@link #addMouseDownHandler},
   * {@link #addMouseUpHandler} and {@link #addMouseOutHandler} instead
   */
  @Deprecated
  public void addMouseListener(MouseListener listener) {
    ListenerWrapper.WrappedMouseListener.add(this, listener);
  }

  public HandlerRegistration addMouseMoveHandler(MouseMoveHandler handler) {
    return addDomHandler(handler, MouseMoveEvent.getType());
  }

  public HandlerRegistration addMouseOutHandler(MouseOutHandler handler) {
    return addDomHandler(handler, MouseOutEvent.getType());
  }

  public HandlerRegistration addMouseOverHandler(MouseOverHandler handler) {
    return addDomHandler(handler, MouseOverEvent.getType());
  }

  public HandlerRegistration addMouseUpHandler(MouseUpHandler handler) {
    return addDomHandler(handler, MouseUpEvent.getType());
  }

  public HandlerRegistration addMouseWheelHandler(MouseWheelHandler handler) {
    return addDomHandler(handler, MouseWheelEvent.getType());
  }

  /**
   * @deprecated Use {@link #addMouseWheelHandler} instead
   */
  @Deprecated
  public void addMouseWheelListener(MouseWheelListener listener) {
    ListenerWrapper.WrappedMouseWheelListener.add(this, listener);
  }

  public LeafValueEditor<String> asEditor() {
    return HasTextEditor.of(this);
  }

  /**
   * {@inheritDoc}
   */
  public AutoHorizontalAlignmentConstant getAutoHorizontalAlignment() {
    return autoHorizontalAlignment;
  }

  /**
   * Gets the widget element's direction.
   * @deprecated Use {@link #getTextDirection} instead
   */
  @Deprecated
  public Direction getDirection() {
    return BidiUtils.getDirectionOnElement(getElement());
  }

  public DirectionEstimator getDirectionEstimator() {
    return directionEstimator;
  }

  /**
   * {@inheritDoc}
   */
  public HorizontalAlignmentConstant getHorizontalAlignment() {
    return horzAlign;
  }

  public String getText() {
    return getTextOrHtml(false);
  }

  public Direction getTextDirection() {
    return textDir;
  }

  public boolean getWordWrap() {
    return !getElement().getStyle().getProperty("whiteSpace").equals("nowrap");
  }

  /**
   * @deprecated Use the {@link HandlerRegistration#removeHandler} method on
   * the object returned by {@link #addClickHandler} instead
   */
  @Deprecated
  public void removeClickListener(ClickListener listener) {
    ListenerWrapper.WrappedClickListener.remove(this, listener);
  }

  /**
   * @deprecated Use the {@link HandlerRegistration#removeHandler}
   * method on the object returned by an add*Handler method instead
   */
  @Deprecated
  public void removeMouseListener(MouseListener listener) {
    ListenerWrapper.WrappedMouseListener.remove(this, listener);
  }

  /**
   * @deprecated Use the {@link HandlerRegistration#removeHandler}
   * method on the object returned by {@link #addMouseWheelHandler} instead
   */
  @Deprecated
  public void removeMouseWheelListener(MouseWheelListener listener) {
    ListenerWrapper.WrappedMouseWheelListener.remove(this, listener);
  }

  /**
   * {@inheritDoc}
   */
  public void setAutoHorizontalAlignment(AutoHorizontalAlignmentConstant
      autoAlignment) {
    autoHorizontalAlignment = autoAlignment;
    updateHorizontalAlignment();
  }

  /**
   * Sets the widget element's direction.
   * @deprecated Use {@link #setDirectionEstimator} and / or pass explicit
   * direction to {@link #setText} instead
   */
  @Deprecated
  public void setDirection(Direction direction) {
    BidiUtils.setDirectionOnElement(getElement(), direction);
    initialElementDir = direction;

    // For backwards compatibility, assure there's no span wrap, and update the
    // content direction.
    setInnerTextOrHtml(getTextOrHtml(true), true);
    isSpanWrapped = false;
    textDir = initialElementDir;
    updateHorizontalAlignment();
  }

  /**
   * {@inheritDoc}
   * <p>
   * See note at {@link #setDirectionEstimator(DirectionEstimator)}.
   */
  public void setDirectionEstimator(boolean enabled) {
    setDirectionEstimator(enabled ? WordCountDirectionEstimator.get() : null);
  }

  /**
   * {@inheritDoc}
   * <p>
   * Note: if the widget already has non-empty content, this will update
   * its direction according to the new estimator's result. This may cause
   * flicker, and thus should be avoided; DirectionEstimator should be set
   * before the widget has any content.
   */
  public void setDirectionEstimator(DirectionEstimator directionEstimator) {
    this.directionEstimator = directionEstimator;
    // Refresh appearance
    setTextOrHtml(getTextOrHtml(true), true);
  }

  /**
   * {@inheritDoc}
   *
   * <p> Note: A subsequent call to {@link #setAutoHorizontalAlignment} may
   * override the horizontal alignment set by this method.
   * <p> Note: For {@code null}, the horizontal alignment is cleared, allowing
   * it to be determined by the standard HTML mechanisms such as inheritance and
   * CSS rules.
   * @see #setAutoHorizontalAlignment
   */
  public void setHorizontalAlignment(HorizontalAlignmentConstant align) {
    setAutoHorizontalAlignment(align);
  }

  /**
   * Sets the label's content to the given text.
   * <p>
   * Doesn't change the widget's direction or horizontal alignment if {@code
   * directionEstimator} is null. Otherwise, the widget's direction is set using
   * the estimator, and its alignment may therefore change as described in
   * {@link #setText(String, com.google.gwt.i18n.client.HasDirection.Direction)
   * setText(String, Direction)}.
   * 
   * @param text the widget's new text
   */
  public void setText(String text) {
    setTextOrHtml(text, false);
  }

  /**
   * Sets the label's content to the given text, applying the given direction.
   * <p>
   * This will have the following effect on the horizontal alignment:
   * <ul>
   * <li> If the automatic alignment setting is ALIGN_CONTENT_START or
   * ALIGN_CONTENT_END, the horizontal alignment will be set to match the start
   * or end edge, respectively, of the new direction (the {@code dir}
   * parameter). If that is DEFAULT, the locale direction is used.
   * <li> Otherwise, the horizontal alignment value is not changed, but the
   * effective alignment may nevertheless change according to the usual HTML
   * rules, i.e. it will match the start edge of the new direction if the widget
   * element is a &lt;div&gt; and has no explicit alignment value even by
   * inheritance.
   * </ul>
   *
   * @param text the widget's new text
   * @param dir the text's direction. Note: {@code Direction.DEFAULT} means
   *        direction should be inherited from the widget's parent element.
   */
  public void setText(String text, Direction dir) {
    setTextOrHtml(text, dir, false);
  }

  public void setWordWrap(boolean wrap) {
    getElement().getStyle().setProperty("whiteSpace",
        wrap ? "normal" : "nowrap");
  }

  protected String getTextOrHtml(boolean isHtml) {
    Element element = isSpanWrapped ? getElement().getFirstChildElement()
        : getElement();
    return isHtml ? element.getInnerHTML() : element.getInnerText();
  }

  /**
   * Sets the label's content to the given safe html. See
   * {@link #setText(String)} for details on potential effects on direction and
   * alignment.
   *
   * @param html the widget's new safe html
   */
  protected void setHTML(SafeHtml html) {
    setTextOrHtml(html.asString(), true);
  }

  /**
   * Sets the label's content to the given safe html. See
   * {@link #setText(String)} for details on potential effects on direction and
   * alignment.
   *
   * @param html the widget's new safe html
   * @param dir the content's direction
   */
  protected void setHTML(SafeHtml html, Direction dir) {
    setTextOrHtml(html.asString(), dir, true);
  }

  /**
   * Sets the label's content to the given value (either plain text or HTML).
   * See {@link #setText(String)} for details on potential effects on direction
   * and alignment.
   *
   * @param content the widget's new content
   * @param isHtml whether the content is HTML
   */
  protected void setTextOrHtml(String content, boolean isHtml) {
    if (directionEstimator == null) {
      isSpanWrapped = false;
      setInnerTextOrHtml(content, isHtml);

      // Preserves the initial direction of the widget. This is different from
      // passing the direction parameter explicitly as DEFAULT, which forces the
      // widget to inherit the direction from its parent.
      if (textDir != initialElementDir) {
        textDir = initialElementDir;
        BidiUtils.setDirectionOnElement(getElement(), initialElementDir);
        updateHorizontalAlignment();
      }
    } else {
      setTextOrHtml(content, directionEstimator.estimateDirection(content,
          isHtml), isHtml);
    }
  }

  /**
   * Sets the label's content to the given value (either plain text or HTML),
   * applying the given direction. See
   * {@link #setText(String, com.google.gwt.i18n.client.HasDirection.Direction)
   * setText(String, Direction)} for details on potential effects on alignment.
   * <p>
   * Implementation details:
   * <ul>
   * <li>If the widget's element is a &lt;div&gt;, sets its dir attribute
   * according to the given direction.
   * <li>Otherwise (i.e. the widget's element is a &lt;span&gt;), the direction
   * is set using a nested &lt;span dir=...&gt; element which holds the content
   * of the widget. This nested span may be followed by a zero-width Unicode
   * direction character (LRM or RLM). This manipulation is necessary to prevent
   * garbling in case the direction of the widget is opposite to the direction
   * of its context. See {@link com.google.gwt.i18n.shared.BidiFormatter} for
   * more details.
   * </ul>
   * 
   * @param content the widget's new content
   * @param dir the content's direction
   * @param isHtml whether the content is HTML
   */
  protected void setTextOrHtml(String content, Direction dir, boolean isHtml) {
    textDir = dir;

    // Set the text and the direction.
    if (isElementInline) {
      isSpanWrapped = true;
      getElement().setInnerHTML(BidiFormatter.getInstanceForCurrentLocale(
          true /* alwaysSpan */).spanWrapWithKnownDir(dir, content, isHtml));
    } else {
      isSpanWrapped = false;
      BidiUtils.setDirectionOnElement(getElement(), dir);
      setInnerTextOrHtml(content, isHtml);
    }

    // Update the horizontal alignment if needed.
    updateHorizontalAlignment();
  }

  private void setInnerTextOrHtml(String content, boolean isHtml) {
    if (isHtml) {
      getElement().setInnerHTML(content);
    } else {
      getElement().setInnerText(content);
    }
  }

  /**
   * Sets the horizontal alignment of the widget according to the current
   * AutoHorizontalAlignment setting.
   */
  private void updateHorizontalAlignment() {
    HorizontalAlignmentConstant align;
    if (autoHorizontalAlignment == null) {
      align = null;
    } else if (autoHorizontalAlignment instanceof HorizontalAlignmentConstant) {
      align = (HorizontalAlignmentConstant) autoHorizontalAlignment;
    } else {
      /* autoHorizontalAlignment is a truly automatic policy, i.e. either
      ALIGN_CONTENT_START or ALIGN_CONTENT_END */
      align = autoHorizontalAlignment == ALIGN_CONTENT_START ?
          HorizontalAlignmentConstant.startOf(textDir) :
          HorizontalAlignmentConstant.endOf(textDir);
    }

    if (align != horzAlign) {
      horzAlign = align;
      getElement().getStyle().setProperty("textAlign", horzAlign == null ? ""
          : horzAlign.getTextAlignString());
    }
  }
}
