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
package com.google.gwt.dom.builder.shared;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.safecss.shared.SafeStyles;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;

/**
 * Implementation of methods in {@link ElementBuilderBase} used to render HTML
 * as a string, using innerHtml to generate an element.
 */
class HtmlBuilderImpl extends ElementBuilderImpl {

  /*
   * Common element builders are created on initialization to avoid null checks.
   * Less common element builders are created lazily to avoid unnecessary object
   * creation.
   */
  private final HtmlDivBuilder divElementBuilder = new HtmlDivBuilder(this);
  private final HtmlElementBuilder elementBuilder = new HtmlElementBuilder(this);
  private HtmlOptionBuilder optionElementBuilder;
  private HtmlSelectBuilder selectElementBuilder;
  private final StylesBuilder styleBuilder = new HtmlStylesBuilder(this);

  /**
   * Used to builder the HTML string. We cannot use
   * {@link com.google.gwt.safehtml.shared.SafeHtmlBuilder} because it does some
   * rudimentary checks that the HTML tags are complete. Instead, we escape
   * values before appending them.
   */
  private final StringBuilder sb = new StringBuilder();

  /**
   * Return the HTML as a {@link SafeHtml} string.
   */
  public SafeHtml asSafeHtml() {
    // End all open tags.
    endAllTags();

    /*
     * sb is trusted because we only append trusted strings or escaped strings
     * to it.
     */
    return SafeHtmlUtils.fromTrustedString(sb.toString());
  }

  public void attribute(String name, String value) {
    assertCanAddAttributeImpl();
    sb.append(" ").append(escape(name)).append("=\"").append(escape(value)).append("\"");
  }

  public HtmlDivBuilder startDiv() {
    return trustedStart("div", divElementBuilder);
  }

  public HtmlOptionBuilder startOption() {
    if (optionElementBuilder == null) {
      optionElementBuilder = new HtmlOptionBuilder(this);
    }
    return trustedStart("option", optionElementBuilder);
  }

  public HtmlSelectBuilder startSelect() {
    if (selectElementBuilder == null) {
      selectElementBuilder = new HtmlSelectBuilder(this);
    }
    return trustedStart("select", selectElementBuilder);
  }

  @Override
  public StylesBuilder style() {
    return styleBuilder;
  }

  public StylesBuilder styleProperty(SafeStyles style) {
    assertCanAddStylePropertyImpl();
    sb.append(style.asString());
    return style();
  }

  public HtmlElementBuilder trustedStart(String tagName) {
    return trustedStart(tagName, elementBuilder);
  }

  @Override
  protected void doCloseStartTagImpl() {
    sb.append(">");
  }

  @Override
  protected void doCloseStyleAttributeImpl() {
    sb.append("\"");
  }

  @Override
  protected void doEndTagImpl(String tagName) {
    /*
     * Add an end tag.
     * 
     * Some browsers do not behave correctly if you self close (ex <select />)
     * certain tags, so we always add the end tag.
     * 
     * The tag name is safe because it comes from the stack, and tag names are
     * checked before they are added to the stack.
     */
    sb.append("</").append(tagName).append(">");
  }

  @Override
  protected Element doFinishImpl() {
    Element tmp = Document.get().createDivElement();
    tmp.setInnerHTML(asSafeHtml().asString());
    return tmp.getFirstChildElement();
  }

  @Override
  protected void doHtmlImpl(SafeHtml html) {
    sb.append(html.asString());
  }

  @Override
  protected void doOpenStyleImpl() {
    sb.append(" style=\"");
  }

  @Override
  protected void doTextImpl(String text) {
    sb.append(escape(text));
  }

  /**
   * Escape a string.
   * 
   * @param s the string to escape
   */
  private String escape(String s) {
    return SafeHtmlUtils.htmlEscape(s);
  }

  /**
   * Start a tag using the specified builder. The tagName is not checked or
   * escaped.
   * 
   * @return the builder
   */
  private <B extends ElementBuilderBase<?>> B trustedStart(String tagName, B builder) {
    onStart(tagName, builder);
    sb.append("<").append(tagName);
    return builder;
  }
}
