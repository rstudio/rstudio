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
  private final R returnBuilder;

  @SuppressWarnings("unchecked")
  protected AbstractElementBuilderBase(ElementBuilderImpl delegate, boolean isEndTagForbidden) {
    this.delegate = delegate;
    this.isEndTagForbidden = isEndTagForbidden;

    // Cache the return builder to avoid repeated cast checks.
    this.returnBuilder = (R) this;
  }

  @Override
  public void end() {
    delegate.end();
  }

  @Override
  public void end(String tagName) {
    delegate.end(tagName);
  }

  @Override
  public void endAnchor() {
    end(AnchorElement.TAG);
  }

  @Override
  public void endArea() {
    end(AreaElement.TAG);
  }

  @Override
  public void endAudio() {
    end(AudioElement.TAG);
  }

  @Override
  public void endBase() {
    end(BaseElement.TAG);
  }

  @Override
  public void endBlockQuote() {
    end(QuoteElement.TAG_BLOCKQUOTE);
  }

  @Override
  public void endBody() {
    end(BodyElement.TAG);
  }

  @Override
  public void endBR() {
    end(BRElement.TAG);
  }

  @Override
  public void endButton() {
    end(ButtonElement.TAG);
  }

  @Override
  public void endCanvas() {
    end(CanvasElement.TAG);
  }

  @Override
  public void endCol() {
    end(TableColElement.TAG_COL);
  }

  @Override
  public void endColGroup() {
    end(TableColElement.TAG_COLGROUP);
  }

  @Override
  public void endDiv() {
    end(DivElement.TAG);
  }

  @Override
  public void endDList() {
    end(DListElement.TAG);
  }

  @Override
  public void endFieldSet() {
    end(FieldSetElement.TAG);
  }

  @Override
  public void endForm() {
    end(FormElement.TAG);
  }

  @Override
  public void endFrame() {
    end(FrameElement.TAG);
  }

  @Override
  public void endFrameSet() {
    end(FrameSetElement.TAG);
  }

  @Override
  public void endH1() {
    end(HeadingElement.TAG_H1);
  }

  @Override
  public void endH2() {
    end(HeadingElement.TAG_H2);
  }

  @Override
  public void endH3() {
    end(HeadingElement.TAG_H3);
  }

  @Override
  public void endH4() {
    end(HeadingElement.TAG_H4);
  }

  @Override
  public void endH5() {
    end(HeadingElement.TAG_H5);
  }

  @Override
  public void endH6() {
    end(HeadingElement.TAG_H6);
  }

  @Override
  public void endHead() {
    end(HeadElement.TAG);
  }

  @Override
  public void endHR() {
    end(HRElement.TAG);
  }

  @Override
  public void endIFrame() {
    end(IFrameElement.TAG);
  }

  @Override
  public void endImage() {
    end(ImageElement.TAG);
  }

  @Override
  public void endInput() {
    end(InputElement.TAG);
  }

  @Override
  public void endLabel() {
    end(LabelElement.TAG);
  }

  @Override
  public void endLegend() {
    end(LegendElement.TAG);
  }

  @Override
  public void endLI() {
    end(LIElement.TAG);
  }

  @Override
  public void endLink() {
    end(LinkElement.TAG);
  }

  @Override
  public void endMap() {
    end(MapElement.TAG);
  }

  @Override
  public void endMeta() {
    end(MetaElement.TAG);
  }

  @Override
  public void endOList() {
    end(OListElement.TAG);
  }

  @Override
  public void endOptGroup() {
    end(OptGroupElement.TAG);
  }

  @Override
  public void endOption() {
    end(OptionElement.TAG);
  }

  @Override
  public void endParagraph() {
    end(ParagraphElement.TAG);
  }

  @Override
  public void endParam() {
    end(ParamElement.TAG);
  }

  @Override
  public void endPre() {
    end(PreElement.TAG);
  }

  @Override
  public void endQuote() {
    end(QuoteElement.TAG_Q);
  }

  @Override
  public void endScript() {
    end(ScriptElement.TAG);
  }

  @Override
  public void endSelect() {
    end(SelectElement.TAG);
  }

  @Override
  public void endSource() {
    end(SourceElement.TAG);
  }

  @Override
  public void endSpan() {
    end(SpanElement.TAG);
  }

  @Override
  public void endStyle() {
    end(StyleElement.TAG);
  }

  @Override
  public void endTable() {
    end(TableElement.TAG);
  }

  @Override
  public void endTableCaption() {
    end(TableCaptionElement.TAG);
  }

  @Override
  public void endTBody() {
    end(TableSectionElement.TAG_TBODY);
  }

  @Override
  public void endTD() {
    end(TableCellElement.TAG_TD);
  }

  @Override
  public void endTextArea() {
    end(TextAreaElement.TAG);
  }

  @Override
  public void endTFoot() {
    end(TableSectionElement.TAG_TFOOT);
  }

  @Override
  public void endTH() {
    end(TableCellElement.TAG_TH);
  }

  @Override
  public void endTHead() {
    end(TableSectionElement.TAG_THEAD);
  }

  @Override
  public void endTR() {
    end(TableRowElement.TAG);
  }

  @Override
  public void endUList() {
    end(UListElement.TAG);
  }

  @Override
  public void endVideo() {
    end(VideoElement.TAG);
  }

  @Override
  public Element finish() {
    return delegate.finish();
  }

  @Override
  public int getDepth() {
    return delegate.getDepth();
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
  protected R getReturnBuilder() {
    return returnBuilder;
  }
}
