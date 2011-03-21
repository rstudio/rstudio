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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.editor.client.IsEditor;
import com.google.gwt.editor.client.LeafValueEditor;
import com.google.gwt.editor.client.adapters.TakesValueEditor;
import com.google.gwt.text.shared.Parser;
import com.google.gwt.text.shared.Renderer;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.user.client.TakesValue;

import java.text.ParseException;

/**
 * A label displaying its value through a renderer.
 * 
 * @param <T> the value type.
 */
public class ValueLabel<T> extends LabelBase<T> implements TakesValue<T>,
    IsEditor<LeafValueEditor<T>> {

  /**
   * Creates a ValueLabel widget that wraps an existing &lt;span&gt; element.
   * <p>
   * The ValueLabel's value will be <code>null</code>, whether the element being
   * wrapped has content or not. Use {@link #wrap(Element, Renderer, Parser)} to
   * parse the initial element's content to initialize the ValueLabel's value.
   * <p>
   * This element must already be attached to the document. If the element is
   * removed from the document, you must call
   * {@link RootPanel#detachNow(Widget)}.
   * 
   * @param element the element to be wrapped
   * @param renderer the renderer used to render values into the element
   */
  public static <T> ValueLabel<T> wrap(Element element,
      Renderer<? super T> renderer) {
    // Assert that the element is attached.
    assert Document.get().getBody().isOrHasChild(element);

    ValueLabel<T> label = new ValueLabel<T>(element, renderer);

    // Mark it attached and remember it for cleanup.
    label.onAttach();
    RootPanel.detachOnWindowClose(label);

    return label;
  }

  /**
   * Creates a ValueLabel widget that wraps an existing &lt;span&gt; element.
   * <p>
   * The ValueLabel's value will be initialized with the element's content,
   * passed through the <code>parser</code>.
   * <p>
   * This element must already be attached to the document. If the element is
   * removed from the document, you must call
   * {@link RootPanel#detachNow(Widget)}.
   * 
   * @param element the element to be wrapped
   * @param renderer the renderer used to render values into the element
   * @param parser the parser used to initialize the ValueLabel's value from the
   *          element's content
   */
  public static <T> ValueLabel<T> wrap(Element element,
      Renderer<? super T> renderer, Parser<? extends T> parser)
      throws ParseException {
    ValueLabel<T> label = wrap(element, renderer);

    label.setValue(parser.parse(element.getInnerText()));

    // Mark it attached and remember it for cleanup.
    label.onAttach();
    RootPanel.detachOnWindowClose(label);

    return label;
  }

  private final Renderer<? super T> renderer;
  private T value;
  private LeafValueEditor<T> editor;

  /**
   * Creates an empty value label.
   * 
   * @param renderer
   */
  @UiConstructor
  public ValueLabel(Renderer<? super T> renderer) {
    super(true);
    this.renderer = renderer;
  }

  /**
   * This constructor may be used by subclasses to explicitly use an existing
   * element. This element must be either a &lt;span&gt; or a &lt;div&gt;
   * element.
   * 
   * @param element the element to be used
   */
  protected ValueLabel(Element element, Renderer<? super T> renderer) {
    super(element);
    this.renderer = renderer;
  }

  public LeafValueEditor<T> asEditor() {
    if (editor == null) {
      editor = TakesValueEditor.of(this);
    }
    return editor;
  }

  public T getValue() {
    return value;
  }

  public void setValue(T value) {
    this.value = value;
    directionalTextHelper.setTextOrHtml(renderer.render(value), false);
    updateHorizontalAlignment();
  }
}