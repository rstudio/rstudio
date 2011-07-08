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

import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.AreaElement;
import com.google.gwt.dom.client.AudioElement;
import com.google.gwt.dom.client.BRElement;
import com.google.gwt.dom.client.BaseElement;
import com.google.gwt.dom.client.BodyElement;
import com.google.gwt.dom.client.ButtonElement;
import com.google.gwt.dom.client.CanvasElement;
import com.google.gwt.dom.client.DListElement;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.FieldSetElement;
import com.google.gwt.dom.client.FormElement;
import com.google.gwt.dom.client.FrameElement;
import com.google.gwt.dom.client.FrameSetElement;
import com.google.gwt.dom.client.HRElement;
import com.google.gwt.dom.client.HeadElement;
import com.google.gwt.dom.client.HeadingElement;
import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.LIElement;
import com.google.gwt.dom.client.LabelElement;
import com.google.gwt.dom.client.LegendElement;
import com.google.gwt.dom.client.LinkElement;
import com.google.gwt.dom.client.MapElement;
import com.google.gwt.dom.client.MetaElement;
import com.google.gwt.dom.client.OListElement;
import com.google.gwt.dom.client.OptGroupElement;
import com.google.gwt.dom.client.OptionElement;
import com.google.gwt.dom.client.ParagraphElement;
import com.google.gwt.dom.client.ParamElement;
import com.google.gwt.dom.client.PreElement;
import com.google.gwt.dom.client.QuoteElement;
import com.google.gwt.dom.client.ScriptElement;
import com.google.gwt.dom.client.SelectElement;
import com.google.gwt.dom.client.SourceElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.StyleElement;
import com.google.gwt.dom.client.TableCaptionElement;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableColElement;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.dom.client.TableSectionElement;
import com.google.gwt.dom.client.TextAreaElement;
import com.google.gwt.dom.client.UListElement;
import com.google.gwt.dom.client.VideoElement;
import com.google.gwt.safehtml.shared.SafeHtml;

/**
 * Abstract base class for implementations of {@link ElementBuilderBase}.
 * 
 * <p>
 * Subclasses of {@link AbstractElementBuilderBase} act as typed wrappers around
 * a shared implementation that handles the actual building. The wrappers merely
 * delegate to the shared implementation, so wrapper instances can be reused,
 * avoiding object creation. This approach is necessary so that the return value
 * of common methods, such as {@link #id(String)}, return a typed builder
 * instead of the generic {@link ElementBuilderBase}.
 * </p>
 * 
 * @param <R> the builder type returned from build methods
 */
public abstract class AbstractElementBuilderBase<R extends ElementBuilderBase<?>> implements
    ElementBuilderBase<R> {

  private final ElementBuilderImpl delegate;
  private final boolean isEndTagForbidden;

  protected AbstractElementBuilderBase(ElementBuilderImpl delegate, boolean isEndTagForbidden) {
    this.delegate = delegate;
    this.isEndTagForbidden = isEndTagForbidden;
  }

  @Override
  public R attribute(String name, int value) {
    return attribute(name, String.valueOf(value));
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B end() {
    // An explicit cast is required to satisfy some javac compilers.
    return (B) delegate.end();
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B end(String tagName) {
    return (B) delegate.end(tagName);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endAnchor() {
    return (B) end(AnchorElement.TAG);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endArea() {
    return (B) end(AreaElement.TAG);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endAudio() {
    return (B) end(AudioElement.TAG);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endBase() {
    return (B) end(BaseElement.TAG);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endBlockQuote() {
    return (B) end(QuoteElement.TAG_BLOCKQUOTE);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endBody() {
    return (B) end(BodyElement.TAG);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endBR() {
    return (B) end(BRElement.TAG);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endButton() {
    return (B) end(ButtonElement.TAG);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endCanvas() {
    return (B) end(CanvasElement.TAG);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endCol() {
    return (B) end(TableColElement.TAG_COL);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endColGroup() {
    return (B) end(TableColElement.TAG_COLGROUP);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endDiv() {
    return (B) end(DivElement.TAG);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endDList() {
    return (B) end(DListElement.TAG);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endFieldSet() {
    return (B) end(FieldSetElement.TAG);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endForm() {
    return (B) end(FormElement.TAG);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endFrame() {
    return (B) end(FrameElement.TAG);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endFrameSet() {
    return (B) end(FrameSetElement.TAG);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endH1() {
    return (B) end(HeadingElement.TAG_H1);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endH2() {
    return (B) end(HeadingElement.TAG_H2);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endH3() {
    return (B) end(HeadingElement.TAG_H3);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endH4() {
    return (B) end(HeadingElement.TAG_H4);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endH5() {
    return (B) end(HeadingElement.TAG_H5);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endH6() {
    return (B) end(HeadingElement.TAG_H6);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endHead() {
    return (B) end(HeadElement.TAG);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endHR() {
    return (B) end(HRElement.TAG);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endIFrame() {
    return (B) end(IFrameElement.TAG);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endImage() {
    return (B) end(ImageElement.TAG);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endInput() {
    return (B) end(InputElement.TAG);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endLabel() {
    return (B) end(LabelElement.TAG);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endLegend() {
    return (B) end(LegendElement.TAG);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endLI() {
    return (B) end(LIElement.TAG);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endLink() {
    return (B) end(LinkElement.TAG);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endMap() {
    return (B) end(MapElement.TAG);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endMeta() {
    return (B) end(MetaElement.TAG);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endOList() {
    return (B) end(OListElement.TAG);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endOptGroup() {
    return (B) end(OptGroupElement.TAG);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endOption() {
    return (B) end(OptionElement.TAG);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endParagraph() {
    return (B) end(ParagraphElement.TAG);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endParam() {
    return (B) end(ParamElement.TAG);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endPre() {
    return (B) end(PreElement.TAG);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endQuote() {
    return (B) end(QuoteElement.TAG_Q);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endScript() {
    return (B) end(ScriptElement.TAG);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endSelect() {
    return (B) end(SelectElement.TAG);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endSource() {
    return (B) end(SourceElement.TAG);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endSpan() {
    return (B) end(SpanElement.TAG);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endStyle() {
    return (B) end(StyleElement.TAG);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endTable() {
    return (B) end(TableElement.TAG);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endTableCaption() {
    return (B) end(TableCaptionElement.TAG);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endTBody() {
    return (B) end(TableSectionElement.TAG_TBODY);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endTD() {
    return (B) end(TableCellElement.TAG_TD);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endTextArea() {
    return (B) end(TextAreaElement.TAG);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endTFoot() {
    return (B) end(TableSectionElement.TAG_TFOOT);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endTH() {
    return (B) end(TableCellElement.TAG_TH);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endTHead() {
    return (B) end(TableSectionElement.TAG_THEAD);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endTR() {
    return (B) end(TableRowElement.TAG);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endUList() {
    return (B) end(UListElement.TAG);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends ElementBuilderBase<?>> B endVideo() {
    return (B) end(VideoElement.TAG);
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
  public boolean isChildElementSupported() {
    return !isEndTagForbidden;
  }

  @Override
  public boolean isEndTagForbidden() {
    return isEndTagForbidden;
  }

  @Override
  public StylesBuilder style() {
    return delegate.style();
  }

  @Override
  public R text(String text) {
    delegate.text(text);
    return getReturnBuilder();
  }

  /**
   * Get the builder to return from build methods.
   * 
   * @return the return builder
   */
  @SuppressWarnings("unchecked")
  protected R getReturnBuilder() {
    return (R) this;
  }
}
