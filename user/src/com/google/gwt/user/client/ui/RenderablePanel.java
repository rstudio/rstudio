/*
 * Copyright 2011 Google Inc.
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

import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.builder.shared.HtmlElementBuilder;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

/**
 * EXPERIMENTAL and subject to change. Do not use this in production code.
 * <p>
 * An {@link IsRenderable} version of {@link HTMLPanel}. This class is a stepping
 * in our transition to the Renderable strategy. Eventually this functionality
 * should be merged into {@link HTMLPanel}.
 * The only reason this class doesn't extend {@link HTMLPanel} is because it
 * doesn't provide any way to build the panel lazily (which is needed here).
 */
public class RenderablePanel extends ComplexPanel implements IsRenderable {

  private static Element hiddenDiv;

  private static String TAG_NAME = "div";

  private static void ensureHiddenDiv() {
    // If it's already been created, don't do anything.
    if (hiddenDiv != null) {
      return;
    }

    hiddenDiv = Document.get().createDivElement();
    UIObject.setVisible(hiddenDiv, false);
    RootPanel.getBodyElement().appendChild(hiddenDiv);
  }

  // TODO(rdcastro): Add setters for these, or maybe have a list instead of a
  // single callback.
  public ScheduledCommand wrapInitializationCallback = null;
  public ScheduledCommand detachedInitializationCallback = null;

  protected SafeHtml html = null;

  /**
   * Creates an HTML panel with the specified HTML contents inside a DIV
   * element. Any element within this HTML that has a specified id can contain a
   * child widget.
   * The actual element that will hold this HTML isn't initialized until it is
   * needed.
   *
   * @param html the panel's HTML
   */
  public RenderablePanel(String html) {
    this(new SafeHtmlBuilder().appendHtmlConstant(html).toSafeHtml());
  }

  /**
   * Initializes the panel's HTML from a given {@link SafeHtml} object.
   *
   * Similar to {@link #HTMLPanel(String)}
   *
   * @param safeHtml the html to set.
   */
  public RenderablePanel(SafeHtml safeHtml) {
    this.html = safeHtml;
    setElement(PotentialElement.build(this, TAG_NAME));
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
  public void addAndReplaceElement(Widget widget,
      com.google.gwt.user.client.Element toReplace) {
    // Logic pulled from super.add(), replacing the element rather than adding.
    widget.removeFromParent();
    getChildren().add(widget);
    toReplace.getParentNode().replaceChild(widget.getElement(), toReplace);
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

  @Override
  public void claimElement(Element element) {
    if (isFullyInitialized()) {
      /*
       * If claimElement is being called after the widget is fully initialized,
       * that's probably a programming error, as it's much more efficient to
       * build the whole tree at once.
       */
      throw new IllegalStateException(
          "claimElement() cannot be called twice, or after forcing the widget to"
          + " render itself (e.g. after adding it to a panel)");
    }

    setElement(element);
    html = null;
    if (wrapInitializationCallback != null) {
      wrapInitializationCallback.execute();
      wrapInitializationCallback = null;
    }
  }

  @Override
  public void initializeClaimedElement() {
    if (detachedInitializationCallback != null) {
      detachedInitializationCallback.execute();
      detachedInitializationCallback = null;
    }
  }

  /**
   * Adopts the given, but doesn't change anything about its DOM element.
   * Should only be used for widgets with elements that are children of this
   * panel's element.
   * No-op if called with an {@link IsRenderable} that isn't also IsWidget,
   * but safe to call with such as a convenience.
   */
  public void logicalAdd(IsRenderable renderable) {
    if (!(renderable instanceof IsWidget)) {
      // Nothing to do if not a Widget.
      return;
    }
    Widget widget = ((IsWidget) renderable).asWidget();
    getChildren().add(widget);
    adopt(widget);
  }

  @Override
  public SafeHtml render(RenderableStamper stamper) {
    HtmlElementBuilder builder = PotentialElement.createBuilderFor(getElement());
    stamper.stamp(builder);
    builder.html(getInnerHtml()).end();

    SafeHtml returnValue = builder.asSafeHtml();
    return returnValue;
  }

  @Override
  public void render(RenderableStamper stamper, SafeHtmlBuilder builder) {
    builder.append(render(stamper));
  }

  @Override
  public Element resolvePotentialElement() {
    buildAndInitDivContainer();
    html = null;
    return getElement();
  }

  /**
   * Returns the HTML to be set as the innerHTML of the container.
   */
  protected SafeHtml getInnerHtml() {
    return html;
  }

  /**
   * Whether the initilization of the panel is finished (i.e., the corresponding
   * DOM element has been built).
   */
  protected boolean isFullyInitialized() {
    return html == null;
  }

  /**
   * Method that finishes the initialization of HTMLPanel instances built from
   * HTML. This will create a div to wrap the given HTML and call any callbacks
   * that may have been added to the panel.
   */
  private void buildAndInitDivContainer() {
    // TODO(rdcastro): Use the same technique as in render() above.

    // Build the div that'll container the panel's HTML.
    Element element = Document.get().createDivElement();
    element.setInnerHTML(getInnerHtml().asString());

    // TODO(rdcastro): Implement something like
    // element.mergeFrom(getElement());
    String styleName = getStyleName();
    if (styleName != null) {
      element.setClassName(styleName);
    }

    setElement(element);

    // If there's any wrap callback to call, we have to attach the div before
    // calling it, and then detach again.
    if (wrapInitializationCallback != null) {
      ensureHiddenDiv();
      hiddenDiv.appendChild(element);
      wrapInitializationCallback.execute();
      element.getParentNode().removeChild(element);
    }

    // Call any detached init callbacks we might have.
    if (detachedInitializationCallback != null) {
      detachedInitializationCallback.execute();
    }
  }
}
