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

import com.google.gwt.dom.client.Element;
import com.google.gwt.safehtml.shared.SafeHtml;

/**
 * Implementation of {@link ElementBuilderBase} that delegates to an
 * {@link HtmlBuilderImpl}.
 * 
 * <p>
 * Subclasses of {@link HtmlElementBuilderBase} act as typed wrappers around a
 * shared {@link ElementBuilderBase} implementation that handles the actual
 * building. The wrappers merely delegate to the shared implementation, so
 * wrapper instances can be reused, avoiding object creation. This approach is
 * necessary so that the return value of common methods, such as
 * {@link #id(String)}, return a typed builder instead of the generic
 * {@link ElementBuilderBase}.
 * </p>
 * 
 * @param <R> the builder type returned from build methods
 */
public class HtmlElementBuilderBase<R extends ElementBuilderBase<?>> implements
    ElementBuilderBase<R> {

  private final HtmlBuilderImpl delegate;

  /**
   * Construct a new {@link HtmlElementBuilderBase}.
   * 
   * @param delegate the delegate that builds the element
   */
  HtmlElementBuilderBase(HtmlBuilderImpl delegate) {
    this.delegate = delegate;
  }

  /**
   * Return the HTML as a {@link SafeHtml} string.
   */
  public SafeHtml asSafeHtml() {
    return delegate.asSafeHtml();
  }

  @Override
  public R attribute(String name, int value) {
    return attribute(name, String.valueOf(value));
  }

  @Override
  public R attribute(String name, String value) {
    delegate.attribute(name, value);
    return getReturnBuilder();
  }

  @Override
  public R className(String className) {
    return attribute("class", className);
  }

  @Override
  public R dir(String dir) {
    return attribute("dir", dir);
  }

  @Override
  public R draggable(String draggable) {
    return attribute("draggable", draggable);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B end() {
    return (B) delegate.end();
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B end(String tagName) {
    return (B) delegate.end(tagName);
  }

  @Override
  public <B extends ElementBuilderBase<?>> B endDiv() {
    return end("div");
  }

  @Override
  public <B extends ElementBuilderBase<?>> B endOption() {
    return end("option");
  }

  @Override
  public <B extends ElementBuilderBase<?>> B endSelect() {
    return end("select");
  }

  @Override
  public Element finish() {
    return delegate.finish();
  }

  @Override
  public R html(SafeHtml html) {
    delegate.html(html);
    return getReturnBuilder();
  }

  @Override
  public R id(String id) {
    return attribute("id", id);
  }

  @Override
  public R lang(String lang) {
    return attribute("lang", lang);
  }

  @Override
  public DivBuilder startDiv() {
    return delegate.startDiv();
  }

  @Override
  public OptionBuilder startOption() {
    return delegate.startOption();
  }

  @Override
  public SelectBuilder startSelect() {
    return delegate.startSelect();
  }

  @Override
  public StylesBuilder style() {
    return delegate.style();
  }

  @Override
  public R tabIndex(int tabIndex) {
    return attribute("tabIndex", tabIndex);
  }

  @Override
  public R text(String text) {
    delegate.text(text);
    return getReturnBuilder();
  }

  @Override
  public R title(String title) {
    return attribute("title", title);
  }

  @Override
  public ElementBuilder trustedStart(String tagName) {
    return delegate.trustedStart(tagName);
  }

  /**
   * Get the builder to return from build methods.
   * 
   * @return the return builder
   */
  @SuppressWarnings("unchecked")
  private R getReturnBuilder() {
    return (R) this;
  }
}
