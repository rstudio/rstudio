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
package com.google.gwt.dom.builder.client;

import com.google.gwt.dom.builder.shared.ElementBuilderBase;
import com.google.gwt.dom.builder.shared.ElementBuilderImpl;
import com.google.gwt.dom.builder.shared.StylesBuilder;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.shared.SafeHtml;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of methods in
 * {@link com.google.gwt.dom.builder.shared.ElementBuilderBase} used to render
 * Elements using DOM manipulation.
 */
class DomBuilderImpl extends ElementBuilderImpl {

  /*
   * Common element builders are created on initialization to avoid null checks.
   * Less common element builders are created lazily to avoid unnecessary object
   * creation.
   */
  private final DomDivBuilder divElementBuilder = new DomDivBuilder(this);
  private final DomElementBuilder elementBuilder = new DomElementBuilder(this);
  private DomOptionBuilder optionElementBuilder;
  private DomSelectBuilder selectElementBuilder;
  private final StylesBuilder styleBuilder = new DomStylesBuilder(this);

  private Element root;

  /**
   * The element at the top of the stack.
   * 
   * With normal usage, the current element will be accessed repeatedly to add
   * attributes and styles. We maintain the current element outside of the stack
   * to avoid a list access on each operation.
   */
  private Element currentElement;
  private final List<Element> stackElements = new ArrayList<Element>();

  @Override
  public ElementBuilderBase<?> end() {
    ElementBuilderBase<?> builder = super.end();
    popElement();
    return builder;
  }

  public DomDivBuilder startDiv() {
    return start(Document.get().createDivElement(), divElementBuilder);
  }

  public DomOptionBuilder startOption() {
    if (optionElementBuilder == null) {
      optionElementBuilder = new DomOptionBuilder(this);
    }
    return start(Document.get().createOptionElement(), optionElementBuilder);
  }

  public DomSelectBuilder startSelect() {
    if (selectElementBuilder == null) {
      selectElementBuilder = new DomSelectBuilder(this);
    }
    return start(Document.get().createSelectElement(), selectElementBuilder);
  }

  @Override
  public StylesBuilder style() {
    return styleBuilder;
  }

  public DomElementBuilder trustedStart(String tagName) {
    /*
     * Validate the tag before trying to create the element, or the browser may
     * throw a JS error and prevent us from triggering an
     * IllegalArgumentException.
     */
    assertValidTagName(tagName);
    return start(Document.get().createElement(tagName), elementBuilder);
  }

  @Override
  protected void doCloseStartTagImpl() {
    // No-op.
  }

  @Override
  protected void doCloseStyleAttributeImpl() {
    // No-op.
  }

  @Override
  protected void doEndTagImpl(String tagName) {
    // No-op.
  }

  @Override
  protected Element doFinishImpl() {
    return root;
  }

  @Override
  protected void doHtmlImpl(SafeHtml html) {
    getCurrentElement().setInnerHTML(html.asString());
  }

  @Override
  protected void doOpenStyleImpl() {
    // No-op.
  }

  @Override
  protected void doTextImpl(String text) {
    getCurrentElement().setInnerText(text);
  }

  /**
   * Assert that the builder is in a state where an attribute can be added.
   * 
   * @return the element on which the attribute can be set
   * @throw {@link IllegalStateException} if the start tag is closed
   */
  Element assertCanAddAttribute() {
    assertCanAddAttributeImpl();
    return getCurrentElement();
  }

  /**
   * Assert that the builder is in a state where a style property can be added.
   * 
   * @return the {@link Style} on which the property can be set
   * @throw {@link IllegalStateException} if the style is not accessible
   */
  Style assertCanAddStyleProperty() {
    assertCanAddStylePropertyImpl();
    return getCurrentElement().getStyle();
  }

  /**
   * Get the element current being built.
   */
  Element getCurrentElement() {
    if (currentElement == null) {
      throw new IllegalStateException("There are no elements on the stack.");
    }
    return currentElement;
  }

  private void popElement() {
    Element toRet = getCurrentElement();
    int itemCount = stackElements.size(); // Greater than or equal to one.
    stackElements.remove(itemCount - 1);
    if (itemCount == 1) {
      currentElement = null;
    } else {
      currentElement = stackElements.get(itemCount - 2);
    }
  }

  private void pushElement(Element e) {
    stackElements.add(e);
    currentElement = e;
  }

  /**
   * Start a child element.
   * 
   * @param element the element to start
   * @param builder the builder used to builder the new element
   */
  private <B extends ElementBuilderBase<?>> B start(Element element, B builder) {
    onStart(element.getTagName(), builder);

    // Set the root element.
    if (root == null) {
      // This is the new root element.
      root = element;
    } else {
      // Appending to the current element.
      getCurrentElement().appendChild(element);
    }

    // Add the element to the stack.
    pushElement(element);

    return builder;
  }
}