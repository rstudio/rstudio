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
package com.google.gwt.dom.client;

import com.google.gwt.core.client.GWT;

/**
 * A Document is the root of the HTML hierarchy and holds the entire content.
 * Besides providing access to the hierarchy, it also provides some convenience
 * methods for accessing certain sets of information from the document.
 */
public class Document extends Node {

  /**
   * We cache Document.nativeGet() in DevMode, because crossing the JSNI
   * boundary thousands of times just to read a constant value is slow. 
   */
  private static Document doc;
  
  /**
   * Gets the default document. This is the document in which the module is
   * running.
   * 
   * @return the default document
   */
  public static Document get() {
    if (GWT.isScript()) {
      return nativeGet();
    }
    
    // No need to be MT-safe. Single-threaded JS code.
    if (doc == null) {
      doc = nativeGet();
    }
    return doc;
  }

  private static native Document nativeGet() /*-{
    return $doc;
  }-*/;

  protected Document() {
  }

  /**
   * Creates an &lt;a&gt; element.
   * 
   * @return the newly created element
   */
  public final AnchorElement createAnchorElement() {
    return (AnchorElement) DOMImpl.impl.createElement(this, AnchorElement.TAG);
  }

  /**
   * Creates an &lt;area&gt; element.
   * 
   * @return the newly created element
   */
  public final AreaElement createAreaElement() {
    return (AreaElement) DOMImpl.impl.createElement(this, AreaElement.TAG);
  }

  /**
   * Creates an &lt;audio&gt; element.
   * 
   * @return the newly created element
   */
  public final AudioElement createAudioElement() {
    return (AudioElement) DOMImpl.impl.createElement(this, AudioElement.TAG);
  }

  /**
   * Creates a &lt;base&gt; element.
   * 
   * @return the newly created element
   */
  public final BaseElement createBaseElement() {
    return (BaseElement) DOMImpl.impl.createElement(this, BaseElement.TAG);
  }

  /**
   * Creates a &lt;blockquote&gt; element.
   * 
   * @return the newly created element
   */
  public final QuoteElement createBlockQuoteElement() {
    return (QuoteElement) DOMImpl.impl.createElement(this,
        QuoteElement.TAG_BLOCKQUOTE);
  }

  /**
   * Creates a 'blur' event.
   */
  public final NativeEvent createBlurEvent() {
    return createHtmlEvent(BrowserEvents.BLUR, false, false);
  }

  /**
   * Creates a &lt;br&gt; element.
   * 
   * @return the newly created element
   */
  public final BRElement createBRElement() {
    return (BRElement) DOMImpl.impl.createElement(this, BRElement.TAG);
  }

  /**
   * Creates a &lt;button&gt; element.
   * <p>
   * <b>Warning!</b> The button type is actually implementation-dependent and is
   * read-only.
   * 
   * @return the newly created element
   * @deprecated use {@link #createPushButtonElement()},
   *             {@link #createResetButtonElement()} or
   *             {@link #createSubmitButtonElement()} instead.
   */
  @Deprecated
  public final ButtonElement createButtonElement() {
    return (ButtonElement) DOMImpl.impl.createElement(this, ButtonElement.TAG);
  }

  /**
   * Creates an &lt;input type='button'&gt; element.
   * 
   * @return the newly created element
   */
  public final InputElement createButtonInputElement() {
    return DOMImpl.impl.createInputElement(this, "button");
  }

  /**
   * Creates a &lt;canvas&gt; element.
   * 
   * @return the newly created element
   */
  public final CanvasElement createCanvasElement() {
    return (CanvasElement) DOMImpl.impl.createElement(this, CanvasElement.TAG);
  }

  /**
   * Creates a &lt;caption&gt; element.
   * 
   * @return the newly created element
   */
  public final TableCaptionElement createCaptionElement() {
    return (TableCaptionElement) DOMImpl.impl.createElement(this,
        TableCaptionElement.TAG);
  }

  /**
   * Creates a 'change' event.
   */
  public final NativeEvent createChangeEvent() {
    return createHtmlEvent(BrowserEvents.CHANGE, false, true);
  }

  /**
   * Creates an &lt;input type='checkbox'&gt; element.
   * 
   * @return the newly created element
   */
  public final InputElement createCheckInputElement() {
    return DOMImpl.impl.createCheckInputElement(this);
  }

  /**
   * Creates a 'click' event.
   * 
   * <p>
   * Note that this method does not allow the event's 'button' field to be
   * specified, because not all browsers support it reliably for click events.
   * </p>
   * 
   * @param detail the event's detail property
   * @param screenX the event's screen-relative x-position
   * @param screenY the event's screen-relative y-position
   * @param clientX the event's client-relative x-position
   * @param clientY the event's client-relative y-position
   * @param ctrlKey <code>true</code> if the ctrl key is depressed
   * @param altKey <code>true</code> if the alt key is depressed
   * @param shiftKey <code>true</code> if the shift key is depressed
   * @param metaKey <code>true</code> if the meta key is depressed
   * @return the event object
   */
  public final NativeEvent createClickEvent(int detail, int screenX,
      int screenY, int clientX, int clientY, boolean ctrlKey, boolean altKey,
      boolean shiftKey, boolean metaKey) {
    // We disallow setting the button here, because IE doesn't provide the
    // button property for click events.
    return createMouseEvent(BrowserEvents.CLICK, true, true, detail, screenX, screenY,
        clientX, clientY, ctrlKey, altKey, shiftKey, metaKey,
        NativeEvent.BUTTON_LEFT, null);
  }

  /**
   * Creates a &lt;col&gt; element.
   * 
   * @return the newly created element
   */
  public final TableColElement createColElement() {
    return (TableColElement) DOMImpl.impl.createElement(this,
        TableColElement.TAG_COL);
  }

  /**
   * Creates a &lt;colgroup&gt; element.
   * 
   * @return the newly created element
   */
  public final TableColElement createColGroupElement() {
    return (TableColElement) DOMImpl.impl.createElement(this,
        TableColElement.TAG_COLGROUP);
  }

  /**
   * Creates a 'contextmenu' event.
   * 
   * Note: Contextmenu events will not dispatch properly on Firefox 2 and
   * earlier.
   * 
   * @return the event object
   */
  public final NativeEvent createContextMenuEvent() {
    return createHtmlEvent(BrowserEvents.CONTEXTMENU, true, true);
  }

  /**
   * Creates a 'dblclick' event.
   * 
   * <p>
   * Note that this method does not allow the event's 'button' field to be
   * specified, because not all browsers support it reliably for click events.
   * </p>
   * 
   * <p>
   * Note that on some browsers, this may cause 'click' events to be synthesized
   * as well.
   * </p>
   * 
   * @param detail the event's detail property
   * @param screenX the event's screen-relative x-position
   * @param screenY the event's screen-relative y-position
   * @param clientX the event's client-relative x-position
   * @param clientY the event's client-relative y-position
   * @param ctrlKey <code>true</code> if the ctrl key is depressed
   * @param altKey <code>true</code> if the alt key is depressed
   * @param shiftKey <code>true</code> if the shift key is depressed
   * @param metaKey <code>true</code> if the meta key is depressed
   * @return the event object
   */
  public final NativeEvent createDblClickEvent(int detail, int screenX,
      int screenY, int clientX, int clientY, boolean ctrlKey, boolean altKey,
      boolean shiftKey, boolean metaKey) {
    // We disallow setting the button here, because IE doesn't provide the
    // button property for click events.
    return createMouseEvent(BrowserEvents.DBLCLICK, true, true, detail, screenX, screenY,
        clientX, clientY, ctrlKey, altKey, shiftKey, metaKey,
        NativeEvent.BUTTON_LEFT, null);
  }

  /**
   * Creates a &lt;del&gt; element.
   * 
   * @return the newly created element
   */
  public final ModElement createDelElement() {
    return (ModElement) DOMImpl.impl.createElement(this, ModElement.TAG_DEL);
  }

  /**
   * Creates a &lt;div&gt; element.
   * 
   * @return the newly created element
   */
  public final DivElement createDivElement() {
    return (DivElement) DOMImpl.impl.createElement(this, DivElement.TAG);
  }

  /**
   * Creates a &lt;dl&gt; element.
   * 
   * @return the newly created element
   */
  public final DListElement createDLElement() {
    return (DListElement) DOMImpl.impl.createElement(this, DListElement.TAG);
  }

  /**
   * Creates a new element.
   * 
   * @param tagName the tag name of the element to be created
   * @return the newly created element
   */
  public final Element createElement(String tagName) {
    return DOMImpl.impl.createElement(this, tagName);
  }

  /**
   * Creates an 'error' event.
   * 
   * @return the event object
   */
  public final NativeEvent createErrorEvent() {
    return createHtmlEvent(BrowserEvents.ERROR, false, false);
  }

  /**
   * Creates a &lt;fieldset&gt; element.
   * 
   * @return the newly created element
   */
  public final FieldSetElement createFieldSetElement() {
    return (FieldSetElement) DOMImpl.impl.createElement(this,
        FieldSetElement.TAG);
  }

  /**
   * Creates an &lt;input type='file'&gt; element.
   * 
   * @return the newly created element
   */
  public final InputElement createFileInputElement() {
    return DOMImpl.impl.createInputElement(this, "file");
  }

  /**
   * Creates a 'focus' event.
   * 
   * @return the event object
   */
  public final NativeEvent createFocusEvent() {
    return createHtmlEvent(BrowserEvents.FOCUS, false, false);
  }

  /**
   * Creates a &lt;form&gt; element.
   * 
   * @return the newly created element
   */
  public final FormElement createFormElement() {
    return (FormElement) DOMImpl.impl.createElement(this, FormElement.TAG);
  }

  /**
   * Creates a &lt;frame&gt; element.
   * 
   * @return the newly created element
   */
  public final FrameElement createFrameElement() {
    return (FrameElement) DOMImpl.impl.createElement(this, FrameElement.TAG);
  }

  /**
   * Creates a &lt;frameset&gt; element.
   * 
   * @return the newly created element
   */
  public final FrameSetElement createFrameSetElement() {
    return (FrameSetElement) DOMImpl.impl.createElement(this,
        FrameSetElement.TAG);
  }

  /**
   * Creates a &lt;head&gt; element.
   * 
   * @return the newly created element
   */
  public final HeadElement createHeadElement() {
    return (HeadElement) DOMImpl.impl.createElement(this, HeadElement.TAG);
  }

  /**
   * Creates an &lt;h(n)&gt; element.
   * 
   * @param n the type of heading, from 1 to 6 inclusive
   * @return the newly created element
   */
  public final HeadingElement createHElement(int n) {
    assert (n >= 1) && (n <= 6);
    return (HeadingElement) DOMImpl.impl.createElement(this, "h" + n);
  }

  /**
   * Creates an &lt;input type='hidden'&gt; element.
   * 
   * @return the newly created element
   */
  public final InputElement createHiddenInputElement() {
    return DOMImpl.impl.createInputElement(this, "hidden");
  }

  /**
   * Creates an &lt;hr&gt; element.
   * 
   * @return the newly created element
   */
  public final HRElement createHRElement() {
    return (HRElement) DOMImpl.impl.createElement(this, HRElement.TAG);
  }

  /**
   * Creates an event.
   * 
   * <p>
   * While this method may be used to create events directly, it is generally
   * preferable to use existing helper methods such as
   * {@link #createFocusEvent()}.
   * </p>
   * 
   * <p>
   * Also, note that on Internet Explorer the 'canBubble' and 'cancelable'
   * arguments will be ignored (the event's behavior is inferred by the browser
   * based upon its type).
   * </p>
   * 
   * @param type the type of event (e.g., BrowserEvents.FOCUS, BrowserEvents.LOAD, etc)
   * @param canBubble <code>true</code> if the event should bubble
   * @param cancelable <code>true</code> if the event should be cancelable
   * @return the event object
   */
  public final NativeEvent createHtmlEvent(String type, boolean canBubble,
      boolean cancelable) {
    return DOMImpl.impl.createHtmlEvent(this, type, canBubble, cancelable);
  }

  /**
   * Creates an &lt;iframe&gt; element.
   * 
   * @return the newly created element
   */
  public final IFrameElement createIFrameElement() {
    return (IFrameElement) DOMImpl.impl.createElement(this, IFrameElement.TAG);
  }

  /**
   * Creates an &lt;img&gt; element.
   * 
   * @return the newly created element
   */
  public final ImageElement createImageElement() {
    return (ImageElement) DOMImpl.impl.createElement(this, ImageElement.TAG);
  }

  /**
   * Creates an &lt;input type='image'&gt; element.
   * 
   * @return the newly created element
   */
  public final InputElement createImageInputElement() {
    return DOMImpl.impl.createInputElement(this, "image");
  }

  /**
   * Creates an &lt;ins&gt; element.
   * 
   * @return the newly created element
   */
  public final ModElement createInsElement() {
    return (ModElement) DOMImpl.impl.createElement(this, ModElement.TAG_INS);
  }

  /**
   * Creates a key-code event ('keydown' or 'keyup').
   * 
   * <p>
   * While this method may be used to create events directly, it is generally
   * preferable to use existing helper methods such as
   * {@link #createKeyDownEvent(boolean, boolean, boolean, boolean, int)} or
   * {@link #createKeyUpEvent(boolean, boolean, boolean, boolean, int)}.
   * </p>
   * 
   * @param type the type of event (e.g., BrowserEvents.KEYDOWN, BrowserEvents.KEYPRESS, etc)
   * @param ctrlKey <code>true</code> if the ctrl key is depressed
   * @param altKey <code>true</code> if the alt key is depressed
   * @param shiftKey <code>true</code> if the shift key is depressed
   * @param metaKey <code>true</code> if the meta key is depressed
   * @param keyCode the key-code to be set on the event
   * @return the event object
   */
  public final NativeEvent createKeyCodeEvent(String type, boolean ctrlKey,
      boolean altKey, boolean shiftKey, boolean metaKey, int keyCode) {
    return DOMImpl.impl.createKeyCodeEvent(this, type, ctrlKey, altKey,
        shiftKey, metaKey, keyCode);
  }

  /**
   * Creates a 'keydown' event.
   * 
   * @param ctrlKey <code>true</code> if the ctrl key is depressed
   * @param altKey <code>true</code> if the alt key is depressed
   * @param shiftKey <code>true</code> if the shift key is depressed
   * @param metaKey <code>true</code> if the meta key is depressed
   * @param keyCode the key-code to be set on the event
   * @return the event object
   */
  public final NativeEvent createKeyDownEvent(boolean ctrlKey, boolean altKey,
      boolean shiftKey, boolean metaKey, int keyCode) {
    return createKeyCodeEvent(BrowserEvents.KEYDOWN, ctrlKey, altKey, shiftKey, metaKey,
        keyCode);
  }

  /**
   * Creates a 'keydown' event.
   * 
   * @param ctrlKey <code>true</code> if the ctrl key is depressed
   * @param altKey <code>true</code> if the alt key is depressed
   * @param shiftKey <code>true</code> if the shift key is depressed
   * @param metaKey <code>true</code> if the meta key is depressed
   * @param keyCode the key-code to be set on the event
   * @param charCode the char-code to be set on the event
   * @return the event object
   * 
   * @deprecated as of GWT2.1 (keydown events don't have a charCode), use
   *             {@link #createKeyDownEvent(boolean, boolean, boolean, boolean, int)}
   */
  @Deprecated
  public final NativeEvent createKeyDownEvent(boolean ctrlKey, boolean altKey,
      boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
    return createKeyEvent(BrowserEvents.KEYDOWN, true, true, ctrlKey, altKey, shiftKey,
        metaKey, keyCode, charCode);
  }

  /**
   * Creates a key event.
   * 
   * <p>
   * While this method may be used to create events directly, it is generally
   * preferable to use existing helper methods such as
   * {@link #createKeyPressEvent(boolean, boolean, boolean, boolean, int, int)}
   * .
   * </p>
   * 
   * <p>
   * Also, note that on Internet Explorer the 'canBubble' and 'cancelable'
   * arguments will be ignored (the event's behavior is inferred by the browser
   * based upon its type).
   * </p>
   * 
   * @param type the type of event (e.g., BrowserEvents.KEYDOWN, BrowserEvents.KEYPRESS, etc)
   * @param canBubble <code>true</code> if the event should bubble
   * @param cancelable <code>true</code> if the event should be cancelable
   * @param ctrlKey <code>true</code> if the ctrl key is depressed
   * @param altKey <code>true</code> if the alt key is depressed
   * @param shiftKey <code>true</code> if the shift key is depressed
   * @param metaKey <code>true</code> if the meta key is depressed
   * @param keyCode the key-code to be set on the event
   * @param charCode the char-code to be set on the event
   * @return the event object
   * 
   * @deprecated use
   *             {@link #createKeyCodeEvent(String, boolean, boolean, boolean, boolean, int)}
   *             or
   *             {@link #createKeyPressEvent(boolean, boolean, boolean, boolean, int)}
   */
  @Deprecated
  public final NativeEvent createKeyEvent(String type, boolean canBubble,
      boolean cancelable, boolean ctrlKey, boolean altKey, boolean shiftKey,
      boolean metaKey, int keyCode, int charCode) {
    return DOMImpl.impl.createKeyEvent(this, type, canBubble, cancelable,
        ctrlKey, altKey, shiftKey, metaKey, keyCode, charCode);
  }

  /**
   * Creates a 'keypress' event.
   * 
   * @param ctrlKey <code>true</code> if the ctrl key is depressed
   * @param altKey <code>true</code> if the alt key is depressed
   * @param shiftKey <code>true</code> if the shift key is depressed
   * @param metaKey <code>true</code> if the meta key is depressed
   * @param charCode the char-code to be set on the event
   * @return the event object
   */
  public final NativeEvent createKeyPressEvent(boolean ctrlKey, boolean altKey,
      boolean shiftKey, boolean metaKey, int charCode) {
    return DOMImpl.impl.createKeyPressEvent(this, ctrlKey, altKey, shiftKey,
        metaKey, charCode);
  }

  /**
   * Creates a 'keypress' event.
   * 
   * @param ctrlKey <code>true</code> if the ctrl key is depressed
   * @param altKey <code>true</code> if the alt key is depressed
   * @param shiftKey <code>true</code> if the shift key is depressed
   * @param metaKey <code>true</code> if the meta key is depressed
   * @param keyCode the key-code to be set on the event
   * @param charCode the char-code to be set on the event
   * @return the event object
   * 
   * @deprecated as of GWT 2.1 (keypress events don't have a keyCode), use
   *             {@link #createKeyPressEvent(boolean, boolean, boolean, boolean, int)}
   */
  @Deprecated
  public final NativeEvent createKeyPressEvent(boolean ctrlKey, boolean altKey,
      boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
    return createKeyEvent(BrowserEvents.KEYPRESS, true, true, ctrlKey, altKey, shiftKey,
        metaKey, keyCode, charCode);
  }

  /**
   * Creates a 'keyup' event.
   * 
   * @param ctrlKey <code>true</code> if the ctrl key is depressed
   * @param altKey <code>true</code> if the alt key is depressed
   * @param shiftKey <code>true</code> if the shift key is depressed
   * @param metaKey <code>true</code> if the meta key is depressed
   * @param keyCode the key-code to be set on the event
   * @return the event object
   */
  public final NativeEvent createKeyUpEvent(boolean ctrlKey, boolean altKey,
      boolean shiftKey, boolean metaKey, int keyCode) {
    return createKeyCodeEvent(BrowserEvents.KEYUP, ctrlKey, altKey, shiftKey, metaKey,
        keyCode);
  }

  /**
   * Creates a 'keyup' event.
   * 
   * @param ctrlKey <code>true</code> if the ctrl key is depressed
   * @param altKey <code>true</code> if the alt key is depressed
   * @param shiftKey <code>true</code> if the shift key is depressed
   * @param metaKey <code>true</code> if the meta key is depressed
   * @param keyCode the key-code to be set on the event
   * @param charCode the char-code to be set on the event
   * @return the event object
   * 
   * @deprecated as of GWT 2.1 (keyup events don't have a charCode), use
   *             {@link #createKeyUpEvent(boolean, boolean, boolean, boolean, int)}
   */
  @Deprecated
  public final NativeEvent createKeyUpEvent(boolean ctrlKey, boolean altKey,
      boolean shiftKey, boolean metaKey, int keyCode, int charCode) {
    return createKeyEvent(BrowserEvents.KEYUP, true, true, ctrlKey, altKey, shiftKey,
        metaKey, keyCode, charCode);
  }

  /**
   * Creates a &lt;label&gt; element.
   * 
   * @return the newly created element
   */
  public final LabelElement createLabelElement() {
    return (LabelElement) DOMImpl.impl.createElement(this, LabelElement.TAG);
  }

  /**
   * Creates a &lt;legend&gt; element.
   * 
   * @return the newly created element
   */
  public final LegendElement createLegendElement() {
    return (LegendElement) DOMImpl.impl.createElement(this, LegendElement.TAG);
  }

  /**
   * Creates a &lt;li&gt; element.
   * 
   * @return the newly created element
   */
  public final LIElement createLIElement() {
    return (LIElement) DOMImpl.impl.createElement(this, LIElement.TAG);
  }

  /**
   * Creates a &lt;link&gt; element.
   * 
   * @return the newly created element
   */
  public final LinkElement createLinkElement() {
    return (LinkElement) DOMImpl.impl.createElement(this, LinkElement.TAG);
  }

  /**
   * Creates a 'load' event.
   * 
   * @return the event object
   */
  public final NativeEvent createLoadEvent() {
    return createHtmlEvent(BrowserEvents.LOAD, false, false);
  }

  /**
   * Creates a &lt;map&gt; element.
   * 
   * @return the newly created element
   */
  public final MapElement createMapElement() {
    return (MapElement) DOMImpl.impl.createElement(this, MapElement.TAG);
  }

  /**
   * Creates a &lt;meta&gt; element.
   * 
   * @return the newly created element
   */
  public final MetaElement createMetaElement() {
    return (MetaElement) DOMImpl.impl.createElement(this, MetaElement.TAG);
  }

  /**
   * Creates a 'mousedown' event.
   * 
   * @param detail the event's detail property
   * @param screenX the event's screen-relative x-position
   * @param screenY the event's screen-relative y-position
   * @param clientX the event's client-relative x-position
   * @param clientY the event's client-relative y-position
   * @param ctrlKey <code>true</code> if the ctrl key is depressed
   * @param altKey <code>true</code> if the alt key is depressed
   * @param shiftKey <code>true</code> if the shift key is depressed
   * @param metaKey <code>true</code> if the meta key is depressed
   * @param button the event's button property (values from
   *          {@link NativeEvent#BUTTON_LEFT} et al)
   * @return the event object
   */
  public final NativeEvent createMouseDownEvent(int detail, int screenX,
      int screenY, int clientX, int clientY, boolean ctrlKey, boolean altKey,
      boolean shiftKey, boolean metaKey, int button) {
    return createMouseEvent(BrowserEvents.MOUSEDOWN, true, true, detail, screenX, screenY,
        clientX, clientY, ctrlKey, altKey, shiftKey, metaKey, button, null);
  }

  /**
   * Creates an mouse event.
   * 
   * <p>
   * While this method may be used to create events directly, it is generally
   * preferable to use existing helper methods such as
   * {@link #createClickEvent(int, int, int, int, int, boolean, boolean, boolean, boolean)}
   * .
   * </p>
   * 
   * <p>
   * Also, note that on Internet Explorer the 'canBubble' and 'cancelable'
   * arguments will be ignored (the event's behavior is inferred by the browser
   * based upon its type).
   * </p>
   * 
   * @param type the type of event (e.g., BrowserEvents.FOCUS, BrowserEvents.LOAD, etc)
   * @param canBubble <code>true</code> if the event should bubble
   * @param cancelable <code>true</code> if the event should be cancelable
   * @param detail the event's detail property
   * @param screenX the event's screen-relative x-position
   * @param screenY the event's screen-relative y-position
   * @param clientX the event's client-relative x-position
   * @param clientY the event's client-relative y-position
   * @param ctrlKey <code>true</code> if the ctrl key is depressed
   * @param altKey <code>true</code> if the alt key is depressed
   * @param shiftKey <code>true</code> if the shift key is depressed
   * @param metaKey <code>true</code> if the meta key is depressed
   * @param button the event's button property (values from
   *          {@link NativeEvent#BUTTON_LEFT} et al)
   * @param relatedTarget the event's related target (only relevant for
   *          mouseover and mouseout events)
   * @return the event object
   */
  public final NativeEvent createMouseEvent(String type, boolean canBubble,
      boolean cancelable, int detail, int screenX, int screenY, int clientX,
      int clientY, boolean ctrlKey, boolean altKey, boolean shiftKey,
      boolean metaKey, int button, Element relatedTarget) {
    return DOMImpl.impl.createMouseEvent(this, type, canBubble, cancelable,
        detail, screenX, screenY, clientX, clientY, ctrlKey, altKey, shiftKey,
        metaKey, button, relatedTarget);
  }

  /**
   * Creates a 'mousemove' event.
   * 
   * @param detail the event's detail property
   * @param screenX the event's screen-relative x-position
   * @param screenY the event's screen-relative y-position
   * @param clientX the event's client-relative x-position
   * @param clientY the event's client-relative y-position
   * @param ctrlKey <code>true</code> if the ctrl key is depressed
   * @param altKey <code>true</code> if the alt key is depressed
   * @param shiftKey <code>true</code> if the shift key is depressed
   * @param metaKey <code>true</code> if the meta key is depressed
   * @param button the event's button property (values from
   *          {@link NativeEvent#BUTTON_LEFT} et al)
   * @return the event object
   */
  public final NativeEvent createMouseMoveEvent(int detail, int screenX,
      int screenY, int clientX, int clientY, boolean ctrlKey, boolean altKey,
      boolean shiftKey, boolean metaKey, int button) {
    return createMouseEvent(BrowserEvents.MOUSEMOVE, true, true, detail, screenX, screenY,
        clientX, clientY, ctrlKey, altKey, shiftKey, metaKey, button, null);
  }

  /**
   * Creates a 'mouseout' event.
   * 
   * Note: The 'relatedTarget' parameter will be ignored on Firefox 2 and
   * earlier.
   * 
   * @param detail the event's detail property
   * @param screenX the event's screen-relative x-position
   * @param screenY the event's screen-relative y-position
   * @param clientX the event's client-relative x-position
   * @param clientY the event's client-relative y-position
   * @param ctrlKey <code>true</code> if the ctrl key is depressed
   * @param altKey <code>true</code> if the alt key is depressed
   * @param shiftKey <code>true</code> if the shift key is depressed
   * @param metaKey <code>true</code> if the meta key is depressed
   * @param button the event's button property (values from
   *          {@link NativeEvent#BUTTON_LEFT} et al)
   * @param relatedTarget the event's related target
   * @return the event object
   */
  public final NativeEvent createMouseOutEvent(int detail, int screenX,
      int screenY, int clientX, int clientY, boolean ctrlKey, boolean altKey,
      boolean shiftKey, boolean metaKey, int button, Element relatedTarget) {
    return createMouseEvent(BrowserEvents.MOUSEOUT, true, true, detail, screenX, screenY,
        clientX, clientY, ctrlKey, altKey, shiftKey, metaKey, button,
        relatedTarget);
  }

  /**
   * Creates a 'mouseover' event.
   * 
   * Note: The 'relatedTarget' parameter will be ignored on Firefox 2 and
   * earlier.
   * 
   * @param detail the event's detail property
   * @param screenX the event's screen-relative x-position
   * @param screenY the event's screen-relative y-position
   * @param clientX the event's client-relative x-position
   * @param clientY the event's client-relative y-position
   * @param ctrlKey <code>true</code> if the ctrl key is depressed
   * @param altKey <code>true</code> if the alt key is depressed
   * @param shiftKey <code>true</code> if the shift key is depressed
   * @param metaKey <code>true</code> if the meta key is depressed
   * @param button the event's button property (values from
   *          {@link NativeEvent#BUTTON_LEFT} et al)
   * @param relatedTarget the event's related target
   * @return the event object
   */
  public final NativeEvent createMouseOverEvent(int detail, int screenX,
      int screenY, int clientX, int clientY, boolean ctrlKey, boolean altKey,
      boolean shiftKey, boolean metaKey, int button, Element relatedTarget) {
    return createMouseEvent(BrowserEvents.MOUSEOVER, true, true, detail, screenX, screenY,
        clientX, clientY, ctrlKey, altKey, shiftKey, metaKey, button,
        relatedTarget);
  }

  /**
   * Creates a 'mouseup' event.
   * 
   * @param detail the event's detail property
   * @param screenX the event's screen-relative x-position
   * @param screenY the event's screen-relative y-position
   * @param clientX the event's client-relative x-position
   * @param clientY the event's client-relative y-position
   * @param ctrlKey <code>true</code> if the ctrl key is depressed
   * @param altKey <code>true</code> if the alt key is depressed
   * @param shiftKey <code>true</code> if the shift key is depressed
   * @param metaKey <code>true</code> if the meta key is depressed
   * @param button the event's button property (values from
   *          {@link NativeEvent#BUTTON_LEFT} et al)
   * @return the event object
   */
  public final NativeEvent createMouseUpEvent(int detail, int screenX,
      int screenY, int clientX, int clientY, boolean ctrlKey, boolean altKey,
      boolean shiftKey, boolean metaKey, int button) {
    return createMouseEvent(BrowserEvents.MOUSEUP, true, true, detail, screenX, screenY,
        clientX, clientY, ctrlKey, altKey, shiftKey, metaKey, button, null);
  }

  /**
   * Creates a &lt;object&gt; element.
   * 
   * @return the newly created element
   */
  public final ObjectElement createObjectElement() {
    return (ObjectElement) DOMImpl.impl.createElement(this, ObjectElement.TAG);
  }

  /**
   * Creates an &lt;ol&gt; element.
   * 
   * @return the newly created element
   */
  public final OListElement createOLElement() {
    return (OListElement) DOMImpl.impl.createElement(this, OListElement.TAG);
  }

  /**
   * Creates an &lt;optgroup&gt; element.
   * 
   * @return the newly created element
   */
  public final OptGroupElement createOptGroupElement() {
    return (OptGroupElement) DOMImpl.impl.createElement(this,
        OptGroupElement.TAG);
  }

  /**
   * Creates an &lt;option&gt; element.
   * 
   * @return the newly created element
   */
  public final OptionElement createOptionElement() {
    return (OptionElement) DOMImpl.impl.createElement(this, OptionElement.TAG);
  }

  /**
   * Creates a &lt;param&gt; element.
   * 
   * @return the newly created element
   */
  public final ParamElement createParamElement() {
    return (ParamElement) DOMImpl.impl.createElement(this, ParamElement.TAG);
  }

  /**
   * Creates an &lt;input type='password'&gt; element.
   * 
   * @return the newly created element
   */
  public final InputElement createPasswordInputElement() {
    return DOMImpl.impl.createInputElement(this, "password");
  }

  /**
   * Creates a &lt;p&gt; element.
   * 
   * @return the newly created element
   */
  public final ParagraphElement createPElement() {
    return (ParagraphElement) DOMImpl.impl.createElement(this,
        ParagraphElement.TAG);
  }

  /**
   * Creates a &lt;pre&gt; element.
   * 
   * @return the newly created element
   */
  public final PreElement createPreElement() {
    return (PreElement) DOMImpl.impl.createElement(this, PreElement.TAG);
  }

  /**
   * Creates a &lt;button type='button'&gt; element.
   * 
   * @return the newly created element
   */
  public final ButtonElement createPushButtonElement() {
    return DOMImpl.impl.createButtonElement(this, "button");
  }

  /**
   * Creates a &lt;q&gt; element.
   * 
   * @return the newly created element
   */
  public final QuoteElement createQElement() {
    return (QuoteElement) DOMImpl.impl.createElement(this, QuoteElement.TAG_Q);
  }

  /**
   * Creates an &lt;input type='radio'&gt; element.
   * 
   * @param name the name of the radio input (used for grouping)
   * @return the newly created element
   */
  public final InputElement createRadioInputElement(String name) {
    return DOMImpl.impl.createInputRadioElement(this, name);
  }

  /**
   * Creates a &lt;button type='reset'&gt; element.
   * 
   * @return the newly created element
   */
  public final ButtonElement createResetButtonElement() {
    return DOMImpl.impl.createButtonElement(this, "reset");
  }

  /**
   * Creates an &lt;input type='reset'&gt; element.
   * 
   * @return the newly created element
   */
  public final InputElement createResetInputElement() {
    return DOMImpl.impl.createInputElement(this, "reset");
  }

  /**
   * Creates a &lt;script&gt; element.
   * 
   * @return the newly created element
   */
  public final ScriptElement createScriptElement() {
    return (ScriptElement) DOMImpl.impl.createElement(this, ScriptElement.TAG);
  }

  /**
   * Creates a &lt;script&gt; element.
   * 
   * @param source the source code to set inside the element
   * @return the newly created element
   */
  public final ScriptElement createScriptElement(String source) {
    return DOMImpl.impl.createScriptElement(this, source);
  }

  /**
   * Creates a 'scroll' event.
   * 
   * Note: Contextmenu events will not dispatch properly on Firefox 2 and
   * earlier.
   * 
   * @return the event object
   */
  public final NativeEvent createScrollEvent() {
    return createHtmlEvent(BrowserEvents.SCROLL, false, false);
  }

  /**
   * Creates a &lt;select&gt; element.
   * 
   * @return the newly created element
   */
  public final SelectElement createSelectElement() {
    return DOMImpl.impl.createSelectElement(this, false);
  }

  /**
   * Creates a &lt;select&gt; element.
   * 
   * @param multiple <code>true</code> to allow multiple-selection
   * @return the newly created element
   */
  public final SelectElement createSelectElement(boolean multiple) {
    return DOMImpl.impl.createSelectElement(this, multiple);
  }

  /**
   * Creates an &lt;source&gt; element.
   * 
   * @return the newly created element
   */
  public final SourceElement createSourceElement() {
    return (SourceElement) DOMImpl.impl.createElement(this, SourceElement.TAG);
  }

  /**
   * Creates a &lt;span&gt; element.
   * 
   * @return the newly created element
   */
  public final SpanElement createSpanElement() {
    return (SpanElement) DOMImpl.impl.createElement(this, SpanElement.TAG);
  }

  /**
   * Creates a &lt;style&gt; element.
   * 
   * @return the newly created element
   */
  public final StyleElement createStyleElement() {
    return (StyleElement) DOMImpl.impl.createElement(this, StyleElement.TAG);
  }

  /**
   * Creates a &lt;button type='submit'&gt; element.
   * 
   * @return the newly created element
   */
  public final ButtonElement createSubmitButtonElement() {
    return DOMImpl.impl.createButtonElement(this, "submit");
  }

  /**
   * Creates an &lt;input type='submit'&gt; element.
   * 
   * @return the newly created element
   */
  public final InputElement createSubmitInputElement() {
    return DOMImpl.impl.createInputElement(this, "submit");
  }

  /**
   * Creates a &lt;table&gt; element.
   * 
   * @return the newly created element
   */
  public final TableElement createTableElement() {
    return (TableElement) DOMImpl.impl.createElement(this, TableElement.TAG);
  }

  /**
   * Creates a &lt;tbody&gt; element.
   * 
   * @return the newly created element
   */
  public final TableSectionElement createTBodyElement() {
    return (TableSectionElement) DOMImpl.impl.createElement(this,
        TableSectionElement.TAG_TBODY);
  }

  /**
   * Creates a &lt;td&gt; element.
   * 
   * @return the newly created element
   */
  public final TableCellElement createTDElement() {
    return (TableCellElement) DOMImpl.impl.createElement(this,
        TableCellElement.TAG_TD);
  }

  /**
   * Creates a &lt;textarea&gt; element.
   * 
   * @return the newly created element
   */
  public final TextAreaElement createTextAreaElement() {
    return (TextAreaElement) DOMImpl.impl.createElement(this,
        TextAreaElement.TAG);
  }

  /**
   * Creates an &lt;input type='text'&gt; element.
   * 
   * @return the newly created element
   */
  public final InputElement createTextInputElement() {
    return DOMImpl.impl.createInputElement(this, "text");
  }

  /**
   * Creates a text node.
   * 
   * @param data the text node's initial text
   * @return the newly created element
   */
  public final native Text createTextNode(String data) /*-{
    return this.createTextNode(data);
  }-*/;

  /**
   * Creates a &lt;tfoot&gt; element.
   * 
   * @return the newly created element
   */
  public final TableSectionElement createTFootElement() {
    return (TableSectionElement) DOMImpl.impl.createElement(this,
        TableSectionElement.TAG_TFOOT);
  }

  /**
   * Creates a &lt;thead&gt; element.
   * 
   * @return the newly created element
   */
  public final TableSectionElement createTHeadElement() {
    return (TableSectionElement) DOMImpl.impl.createElement(this,
        TableSectionElement.TAG_THEAD);
  }

  /**
   * Creates a &lt;th&gt; element.
   * 
   * @return the newly created element
   */
  public final TableCellElement createTHElement() {
    return (TableCellElement) DOMImpl.impl.createElement(this,
        TableCellElement.TAG_TH);
  }

  /**
   * Creates a &lt;title&gt; element.
   * 
   * @return the newly created element
   */
  public final TitleElement createTitleElement() {
    return (TitleElement) DOMImpl.impl.createElement(this, TitleElement.TAG);
  }

  /**
   * Creates a &lt;tr&gt; element.
   * 
   * @return the newly created element
   */
  public final TableRowElement createTRElement() {
    return (TableRowElement) DOMImpl.impl.createElement(this,
        TableRowElement.TAG);
  }

  /**
   * Creates a &lt;ul&gt; element.
   * 
   * @return the newly created element
   */
  public final UListElement createULElement() {
    return (UListElement) DOMImpl.impl.createElement(this, UListElement.TAG);
  }

  /**
   * Creates an identifier guaranteed to be unique within this document.
   * 
   * This is useful for allocating element id's.
   * 
   * @return a unique identifier
   */
  public final native String createUniqueId() /*-{
    // In order to force uid's to be document-unique across multiple modules,
    // we hang a counter from the document.
    if (!this.gwt_uid) {
      this.gwt_uid = 1;
    }

    return "gwt-uid-" + this.gwt_uid++;
  }-*/;

  /**
   * Creates a &lt;video&gt; element.
   * 
   * @return the newly created element
   */
  public final VideoElement createVideoElement() {
    return (VideoElement) DOMImpl.impl.createElement(this, VideoElement.TAG);
  }

  /**
   * Enables or disables scrolling of the document.
   * 
   * @param enable whether scrolling should be enabled or disabled
   */
  public final void enableScrolling(boolean enable) {
    getViewportElement().getStyle().setProperty("overflow",
        enable ? "auto" : "hidden");
  }

  /**
   * The element that contains the content for the document. In documents with
   * BODY contents, returns the BODY element.
   * 
   * @return the document's body
   */
  public final native BodyElement getBody() /*-{
    return this.body;
  }-*/;

  /**
   * Returns the left offset between the absolute coordinate system and the
   * body's positioning context. This method is useful for positioning children
   * of the body element in absolute coordinates.
   * 
   * <p>
   * For example, to position an element directly under the mouse cursor
   * (assuming you are handling a mouse event), do the following:
   * </p>
   * 
   * <pre>
   * Event event;
   * Document doc;
   * DivElement child;  // assume absolutely-positioned child of the body
   * 
   * // Get the event location in absolute coordinates.
   * int absX = event.getClientX() + Window.getScrollLeft();
   * int absY = event.getClientY() + Window.getScrollTop();
   * 
   * // Position the child element, adjusting for the difference between the
   * // absolute coordinate system and the body's positioning coordinates.
   * child.getStyle().setPropertyPx("left", absX - doc.getBodyOffsetLeft());
   * child.getStyle().setPropertyPx("top", absY - doc.getBodyOffsetTop());
   * </pre>
   * 
   * @return the left offset of the body's positioning coordinate system
   */
  public final int getBodyOffsetLeft() {
    return DOMImpl.impl.getBodyOffsetLeft(this);
  }

  /**
   * Returns the top offset between the absolute coordinate system and the
   * body's positioning context. This method is useful for positioning children
   * of the body element in absolute coordinates.
   * 
   * @return the top offset of the body's positioning coordinate system
   * @see #getBodyOffsetLeft()
   */
  public final int getBodyOffsetTop() {
    return DOMImpl.impl.getBodyOffsetTop(this);
  }

  /**
   * The height of the document's client area.
   * 
   * @return the document's client height
   */
  public final int getClientHeight() {
    return getViewportElement().getClientHeight();
  }

  /**
   * The width of the document's client area.
   * 
   * @return the document's client width
   */
  public final int getClientWidth() {
    return getViewportElement().getClientWidth();
  }

  /**
   * Gets the document's "compatibility mode", typically used for determining
   * whether the document is in "quirks" or "strict" mode.
   * 
   * @return one of "BackCompat" or "CSS1Compat"
   */
  public final native String getCompatMode() /*-{
    return this.compatMode;
  }-*/;

  /**
   * Gets the document's element. This is typically the &lt;html&gt; element.
   * 
   * @return the document element
   */
  public final native Element getDocumentElement() /*-{
    return this.documentElement;
  }-*/;

  /**
   * The domain name of the server that served the document, or null if the
   * server cannot be identified by a domain name.
   * 
   * @return the document's domain, or <code>null</code> if none exists
   */
  public final native String getDomain() /*-{
    return this.domain;
  }-*/;

  /**
   * Returns the {@link Element} whose id is given by elementId. If no such
   * element exists, returns null. Behavior is not defined if more than one
   * element has this id.
   * 
   * @param elementId the unique id value for an element
   * @return the matching element
   */
  public final native Element getElementById(String elementId) /*-{
    return this.getElementById(elementId);
  }-*/;

  /**
   * Returns a {@link NodeList} of all the {@link Element Elements} with a given
   * tag name in the order in which they are encountered in a preorder traversal
   * of the document tree.
   * 
   * @param tagName the name of the tag to match on (the special value
   *          <code>"*"</code> matches all tags)
   * @return a list containing all the matched elements
   */
  public final native NodeList<Element> getElementsByTagName(String tagName) /*-{
    return this.getElementsByTagName(tagName);
  }-*/;

  /**
   * Returns the URI of the page that linked to this page. The value is an empty
   * string if the user navigated to the page directly (not through a link, but,
   * for example, via a bookmark).
   * 
   * @return the referrer URI
   */
  public final native String getReferrer() /*-{
    return this.referrer;
  }-*/;

  /**
   * The height of the scrollable area of the document.
   * 
   * @return the height of the document's scrollable area
   */
  public final int getScrollHeight() {
    return getViewportElement().getScrollHeight();
  }

  /**
   * The number of pixels that the document's content is scrolled from the left.
   * 
   * <p>
   * If the document is in RTL mode, this method will return a negative value of
   * the number of pixels scrolled from the right.
   * </p>
   * 
   * @return the document's left scroll position
   */
  public final int getScrollLeft() {
    return DOMImpl.impl.getScrollLeft(this);
  }

  /**
   * The number of pixels that the document's content is scrolled from the top.
   * 
   * @return the document's top scroll position
   */
  public final int getScrollTop() {
    return DOMImpl.impl.getScrollTop(this);
  }

  /**
   * The width of the scrollable area of the document.
   * 
   * @return the width of the document's scrollable area
   */
  public final int getScrollWidth() {
    return getViewportElement().getScrollWidth();
  }

  /**
   * Gets the title of a document as specified by the TITLE element in the head
   * of the document.
   * 
   * @return the document's title
   */
  public final native String getTitle() /*-{
    return this.title;
  }-*/;

  /**
   * Gets the absolute URI of this document.
   * 
   * @return the document URI
   */
  public final native String getURL() /*-{
    return this.URL;
  }-*/;

  /**
   * Imports a node from another document to this document.
   * 
   * The returned node has no parent; ({@link Node#getParentNode()} is null).
   * The source node is not altered or removed from the original document; this
   * method creates a new copy of the source node.
   * 
   * For all nodes, importing a node creates a node object owned by the
   * importing document, with attribute values identical to the source node's
   * nodeName and nodeType, plus the attributes related to namespaces (prefix,
   * localName, and namespaceURI). As in the cloneNode operation on a Node, the
   * source node is not altered. Additional information is copied as appropriate
   * to the nodeType, attempting to mirror the behavior expected if a fragment
   * of XML or HTML source was copied from one document to another, recognizing
   * that the two documents may have different DTDs in the XML case.
   * 
   * @param node the node to import
   * @param deep If <code>true</code>, recursively import the subtree under the
   *          specified node; if <code>false</code>, import only the node
   *          itself, as explained above
   */
  public final native void importNode(Node node, boolean deep) /*-{
    this.importNode(node, deep);
  }-*/;

  /**
   * Determines whether the document's "compatMode" is "CSS1Compat". This is
   * normally described as "strict" mode.
   * 
   * @return <code>true</code> if the document is in CSS1Compat mode
   */
  public final boolean isCSS1Compat() {
    return getCompatMode().equals("CSS1Compat");
  }

  /**
   * Sets the number of pixels that the document's content is scrolled from the
   * left.
   * 
   * @param left the document's left scroll position
   */
  public final void setScrollLeft(int left) {
    DOMImpl.impl.setScrollLeft(this, left);
  }

  /**
   * Sets the number of pixels that the document's content is scrolled from the
   * top.
   * 
   * @param top the document's top scroll position
   */
  public final void setScrollTop(int top) {
    DOMImpl.impl.setScrollTop(this, top);
  }

  /**
   * Sets the title of a document as specified by the TITLE element in the head
   * of the document.
   * 
   * @param title the document's new title
   */
  public final native void setTitle(String title) /*-{
    this.title = title;
  }-*/;

  /**
   * Gets the document's viewport element. This is the element that should be
   * used to for scrolling and client-area measurement. In quirks-mode it is the
   * &lt;body&gt; element, while in standards-mode it is the &lt;html&gt;
   * element.
   * 
   * This is package-protected because the viewport is
   * 
   * @return the document's viewport element
   */
  final Element getViewportElement() {
    return isCSS1Compat() ? getDocumentElement() : getBody();
  }
}
