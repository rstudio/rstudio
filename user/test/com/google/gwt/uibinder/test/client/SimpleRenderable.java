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
package com.google.gwt.uibinder.test.client;

import com.google.gwt.dom.builder.client.DomBuilderFactory;
import com.google.gwt.dom.builder.shared.DivBuilder;
import com.google.gwt.dom.builder.shared.ElementBuilderBase;
import com.google.gwt.dom.builder.shared.ElementBuilderFactory;
import com.google.gwt.dom.builder.shared.HtmlBuilderFactory;
import com.google.gwt.dom.builder.shared.HtmlElementBuilderBase;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.IsRenderable;
import com.google.gwt.user.client.ui.PotentialElement;
import com.google.gwt.user.client.ui.RenderableStamper;
import com.google.gwt.user.client.ui.Widget;

/**
 * Used by {@link IsRenderableIntegrationTest}. A simple implementation of
 * IsRenderable. Note that the actual building for both string based and dom
 * based versions happens in one place, {@link #build(ElementBuilderFactory)}.
 * <p>
 * Note also that that the widget's contents are simply re-rendered when the
 * widget's state changes, in this case in {@link #setText}. This is a bad idea
 * for large widgets, but for little leaf widgets like this one is probably just
 * fine.
 * <p>
 * It would probably be a good refactor a production super class out of this
 * once the IsRenderable interface settles down.
 * <p>
 * It's HasValue aspect is just meant for unit testing, not anything to
 * generalize.
 */
public class SimpleRenderable extends Widget implements IsRenderable, HasText, HasValue<Object> {

  private String text = "";
  private Object value;

  public SimpleRenderable() {
    setElement(PotentialElement.build(this));
  }

  @Override
  public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Object> handler) {
    return addHandler(handler, ValueChangeEvent.getType());
  }

  @Override
  public void claimElement(Element element) {
    setElement(element);
  }

  public String getText() {
    return text;
  }

  @Override
  public Object getValue() {
    return value;
  }

  @Override
  public void initializeClaimedElement() {
  }

  @Override
  public SafeHtml render(RenderableStamper stamper) {
    HtmlBuilderFactory factory = HtmlBuilderFactory.get();
    String realText = text;
    text = "[string built]" + text;
    HtmlElementBuilderBase<?> builder = (HtmlElementBuilderBase<?>) build(factory);
    text = realText;
    return stamper.stamp(builder.asSafeHtml());
  }

  @Override
  public void render(RenderableStamper stamper, SafeHtmlBuilder safeHtmlBuilder) {
    safeHtmlBuilder.append(render(stamper));
  }

  @Override
  public Element resolvePotentialElement() {
    String realText = text;
    text = "[dom built]" + text;
    ElementBuilderBase<?> builder = build(DomBuilderFactory.get());
    text = realText;
    setElement(builder.finish());
    return getElement();
  }

  @Override
  public void setText(String text) {
    this.text = text;
    updateElement();
  }

  @Override
  public void setValue(Object value) {
    setValue(value, false);
  }

  @Override
  public void setValue(Object newValue, boolean fireEvents) {
    Object oldValue = value;
    value = newValue;
    ValueChangeEvent.fireIfNotEqual(this, oldValue, newValue);
  }

  private ElementBuilderBase<?> build(ElementBuilderFactory factory) {
    DivBuilder builder = factory.createDivBuilder();
    builder.text(text).end();
    return builder;
  }

  private boolean domIsReal() {
    return !PotentialElement.isPotential(getElement());
  }

  private void updateElement() {
    if (domIsReal()) {
      String realText = text;
      text = "[updated]" + text;
      /*
       * Sleazey. If this is to get real, should split rendering into the outer
       * builder and the inner builder. Update can just call the inner builder.
       * But we'd still have to make a bunch of dom calls to tear down the old
       * guts, something like while (el.firstChild) {
       * el.removeChild(el.firstChild); }
       * 
       * Have to wonder if that actually would be any faster than rebuilding the
       * guts as string
       */
      ElementBuilderBase<?> builder = build(DomBuilderFactory.get());
      getElement().setInnerHTML(builder.finish().getInnerHTML());
      text = realText;
    }
  }
}
