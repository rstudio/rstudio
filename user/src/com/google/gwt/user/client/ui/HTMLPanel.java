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

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.safehtml.shared.SafeHtml;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A panel that contains HTML, and which can attach child widgets to identified
 * elements within that HTML.
 */
public class HTMLPanel extends ComplexPanel {

  private static Element hiddenDiv;

  /**
   * A helper method for creating unique IDs for elements within dynamically-
   * generated HTML. This is important because no two elements in a document
   * should have the same id.
   *
   * @return a new unique identifier
   */
  public static String createUniqueId() {
    return Document.get().createUniqueId();
  }

  /**
   * Creates an HTML panel that wraps an existing element.
   *
   * This element must already be attached to the document. If the element is
   * removed from the document, you must call
   * {@link RootPanel#detachNow(Widget)}.
   *
   * @param element the element to be wrapped
   */
  public static HTMLPanel wrap(Element element) {
    // Assert that the element is attached.
    assert Document.get().getBody().isOrHasChild(element);

    HTMLPanel html = new HTMLPanel(element);

    // Mark it attached and remember it for cleanup.
    html.onAttach();
    RootPanel.detachOnWindowClose(html);

    return html;
  }

  /**
   * Creates an HTML panel with the specified HTML contents inside a DIV
   * element. Any element within this HTML that has a specified id can contain a
   * child widget.
   *
   * @param html the panel's HTML
   */
  public HTMLPanel(String html) {
    /*
     * Normally would call this("div", html), but that method
     * has some slightly expensive IE defensiveness that we just
     * don't need for a div
     */
    setElement(Document.get().createDivElement());
    getElement().setInnerHTML(html);
  }

  /**
   * Initializes the panel's HTML from a given {@link SafeHtml} object.
   *
   * Similar to {@link #HTMLPanel(String)}
   *
   * @param safeHtml the html to set.
   */
  public HTMLPanel(SafeHtml safeHtml) {
    this(safeHtml.asString());
  }

  /**
   * Creates an HTML panel whose root element has the given tag, and with the
   * specified HTML contents. Any element within this HTML that has a specified
   * id can contain a child widget.
   *
   * @param tag the tag of the root element
   * @param html the panel's HTML
   */
  public HTMLPanel(String tag, String html) {
    // Optimization for when the HTML is empty.
    if ("".equals(html)) {
      setElement(Document.get().createElement(tag));
      return;
    }

    /*
     * IE has very arbitrary rules about what will and will not accept
     * innerHTML. <table> and <tbody> simply won't, the property is read only.
     * <p> will explode if you incorrectly try to put another <p> inside of it.
     * And who knows what else.
     *
     * However, if you cram a complete, possibly incorrect structure inside a
     * div, IE will swallow it gladly. So that's what we do here in the name of
     * IE robustification.
     */
    StringBuilder b = new StringBuilder();
    b.append('<').append(tag).append('>').append(html);
    b.append("</").append(tag).append('>');

    // We could use the static hiddenDiv, but that thing is attached
    // to the document. The caller might not want that.

    DivElement scratchDiv = Document.get().createDivElement();
    scratchDiv.setInnerHTML(b.toString());
    setElement(scratchDiv.getFirstChildElement());
    getElement().removeFromParent();
  }

  /**
   * Construct a new {@link HTMLPanel} with the specified element.
   *
   * @param elem the element at the root of the panel
   */
  private HTMLPanel(Element elem) {
    setElement(elem);
  }

  /**
   * Adds a child widget to the panel.
   *
   * @param widget the widget to be added
   */
  @Override
  public void add(Widget widget) {
    add(widget, getElement());
  }

  /**
   * Adds a child widget to the panel, contained within the HTML element
   * specified by a given id.
   *
   * @param widget the widget to be added
   * @param id the id of the element within which it will be contained
   */
  public void add(Widget widget, String id) {
    final Element elem = getElementById(id);

    if (elem == null) {
      throw new NoSuchElementException(id);
    }

    add(widget, elem);
  }

  /**
   * Adds a child widget to the panel, contained within an HTML
   * element.  It is up to the caller to ensure that the given element
   * is a child of this panel's root element.
   *
   * @param widget the widget to be added
   * @param elem the element within which it will be contained
   */
  public void add(Widget widget, Element elem) {
    com.google.gwt.user.client.Element clientElem = elem.cast();
    super.add(widget, clientElem);
  }

  /**
   * Adds a child widget to the panel, replacing the HTML element.
   *
   * @param widget the widget to be added
   * @param toReplace the element to be replaced by the widget
   */
  public final void addAndReplaceElement(Widget widget, Element toReplace) {
    com.google.gwt.user.client.Element clientElem = toReplace.cast();
    addAndReplaceElement(widget, clientElem);
  }

  /**
   * Adds a child widget to the panel, replacing the HTML element.
   *
   * @param widget the widget to be added
   * @param toReplace the element to be replaced by the widget
   * @deprecated use {@link #addAndReplaceElement(Widget, Element)}
   */
  @Deprecated
  public void addAndReplaceElement(Widget widget, com.google.gwt.user.client.Element toReplace) {
    /*
     * Early exit if the element to replace and the replacement are the same. If
     * we remove the new widget, we would also remove the element to replace.
     */
    if (toReplace == widget.getElement()) {
      return;
    }

    // Logic pulled from super.add(), replacing the element rather than adding.

    // Detach new child. Okay if its a child of the element to replace.
    widget.removeFromParent();

    // Logical detach of all children of the element to replace.
    Widget toRemove = null;
    Iterator<Widget> children = getChildren().iterator();
    while (children.hasNext()) {
      Widget next = children.next();
      if (toReplace.isOrHasChild(next.getElement())) {
        if (next.getElement() == toReplace) {
          /*
           * If the element that we are replacing is itself a widget, then we
           * cannot remove it until the new widget has been inserted, or we lose
           * the location of the element to replace. Save the widget to remove
           * for now, and remove it after inserting the new widget.
           */
          toRemove = next;
          break;
        }
        children.remove();
      }
    }

    // Logical attach.
    getChildren().add(widget);

    // Physical attach.
    if (toRemove == null) {
      toReplace.getParentNode().replaceChild(widget.getElement(), toReplace);
    } else {
      /*
       * The element being replaced is a widget, which needs to be removed.
       * First insert the new widget at the same location, then remove the old
       * widget.
       */
      toReplace.getParentNode().insertBefore(widget.getElement(), toReplace);
      remove(toRemove);
    }

    // Adopt.
    adopt(widget);
  }

  /**
   * Overloaded version for IsWidget.
   *
   * @see #addAndReplaceElement(Widget,Element)
   *
   * @deprecated use {@link #addAndReplaceElement(IsWidget, Element)}
   */
  @Deprecated
  public void addAndReplaceElement(IsWidget widget,
      com.google.gwt.user.client.Element toReplace) {
    this.addAndReplaceElement(widget.asWidget(), toReplace);
  }

  /**
   * Overloaded version for IsWidget.
   *
   * @see #addAndReplaceElement(Widget,Element)
   */
  public void addAndReplaceElement(IsWidget widget, Element toReplace) {
    this.addAndReplaceElement(widget.asWidget(), toReplace);
  }

  /**
   * Adds a child widget to the panel, replacing the HTML element specified by a
   * given id.
   *
   * @param widget the widget to be added
   * @param id the id of the element to be replaced by the widget
   */
  public void addAndReplaceElement(Widget widget, String id) {
    final Element toReplace = getElementById(id);

    if (toReplace == null) {
      throw new NoSuchElementException(id);
    }

    addAndReplaceElement(widget, toReplace);
  }

  /**
   * Overloaded version for IsWidget.
   *
   * @see #addAndReplaceElement(Widget,String)
   */
  public void addAndReplaceElement(IsWidget widget, String id) {
    this.addAndReplaceElement(widget.asWidget(), id);
  }

  /**
   * Finds an {@link Element element} within this panel by its id.
   *
   * This method uses
   * {@link com.google.gwt.dom.client.Document#getElementById(String)}, so the
   * id must still be unique within the document.
   *
   * @param id the id of the element to be found
   * @return the element with the given id, or <code>null</code> if none is found
   */
  public com.google.gwt.user.client.Element getElementById(String id) {
    Element elem = isAttached() ? Document.get().getElementById(id) : attachToDomAndGetElement(id);
    return elem.cast();
  }

  /**
   * Performs a {@link Document#getElementById(String)} after attaching the panel's
   * element into a hidden DIV in the document's body. Attachment is necessary
   * to be able to use the native getElementById. The panel's element will be
   * re-attached to its original parent (if any) after the method returns.
   *
   * @param id the id whose associated element is to be retrieved
   * @return the associated element, or <code>null</code> if none is found
   */
  private Element attachToDomAndGetElement(String id) {
    // If the hidden DIV has not been created, create it.
    if (hiddenDiv == null) {
      hiddenDiv = Document.get().createDivElement();
      UIObject.setVisible(hiddenDiv, false);
      RootPanel.getBodyElement().appendChild(hiddenDiv);
    }

    // Hang on to the panel's original parent and sibling elements so that it
    // can be replaced.
    Element origParent = getElement().getParentElement();
    Element origSibling = getElement().getNextSiblingElement();

    // Attach the panel's element to the hidden div.
    hiddenDiv.appendChild(getElement());

    // Now that we're attached to the DOM, we can use getElementById.
    Element child = Document.get().getElementById(id);

    // Put the panel's element back where it was.
    if (origParent != null) {
      origParent.insertBefore(getElement(), origSibling);
    } else {
      hiddenDiv.removeChild(getElement());
    }

    return child;
  }
}
