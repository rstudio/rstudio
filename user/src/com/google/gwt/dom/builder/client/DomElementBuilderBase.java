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

import com.google.gwt.dom.builder.shared.DivBuilder;
import com.google.gwt.dom.builder.shared.ElementBuilder;
import com.google.gwt.dom.builder.shared.ElementBuilderBase;
import com.google.gwt.dom.builder.shared.OptionBuilder;
import com.google.gwt.dom.builder.shared.SelectBuilder;
import com.google.gwt.dom.builder.shared.StylesBuilder;
import com.google.gwt.dom.client.Element;
import com.google.gwt.safehtml.shared.SafeHtml;

/**
 * Implementation of {@link ElementBuilderBase} that delegates to a
 * {@link DomBuilderImpl}.
 * 
 * <p>
 * Subclasses of {@link DomElementBuilderBase} operate directly on the
 * {@link Element} being built.
 * </p>
 * 
 * @param <T> the builder type returned from build methods
 * @param <E> the {@link Element} type
 */
public class DomElementBuilderBase<T extends ElementBuilderBase<?>, E extends Element> implements
    ElementBuilderBase<T> {

  private final DomBuilderImpl delegate;

  /**
   * Construct a new {@link DomElementBuilderBase}.
   * 
   * @param delegate the delegate that builds the element
   */
  DomElementBuilderBase(DomBuilderImpl delegate) {
    this.delegate = delegate;
  }

  @Override
  public T attribute(String name, int value) {
    return attribute(name, String.valueOf(value));
  }

  @Override
  public T attribute(String name, String value) {
    assertCanAddAttribute().setAttribute(name, value);
    return getReturnBuilder();
  }

  @Override
  public T className(String className) {
    assertCanAddAttribute().setClassName(className);
    return getReturnBuilder();
  }

  @Override
  public T dir(String dir) {
    assertCanAddAttribute().setDir(dir);
    return getReturnBuilder();
  }

  @Override
  public T draggable(String draggable) {
    assertCanAddAttribute().setDraggable(draggable);
    return getReturnBuilder();
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
  public T html(SafeHtml html) {
    delegate.html(html);
    return getReturnBuilder();
  }

  @Override
  public T id(String id) {
    assertCanAddAttribute().setId(id);
    return getReturnBuilder();
  }

  @Override
  public T lang(String lang) {
    assertCanAddAttribute().setLang(lang);
    return getReturnBuilder();
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
  public T tabIndex(int tabIndex) {
    assertCanAddAttribute().setTabIndex(tabIndex);
    return getReturnBuilder();
  }

  @Override
  public T text(String text) {
    delegate.text(text);
    return getReturnBuilder();
  }

  @Override
  public T title(String title) {
    assertCanAddAttribute().setTitle(title);
    return getReturnBuilder();
  }

  @Override
  public ElementBuilder trustedStart(String tagName) {
    return delegate.trustedStart(tagName);
  }

  /**
   * Assert that the builder is in a state where an attribute can be added.
   * 
   * @return the element on which the attribute can be set
   */
  protected E assertCanAddAttribute() {
    return delegate.assertCanAddAttribute().cast();
  }

  /**
   * Get the builder to return from build methods.
   * 
   * @return the return builder
   */
  @SuppressWarnings("unchecked")
  private T getReturnBuilder() {
    return (T) this;
  }
}
