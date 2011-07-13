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
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.FieldSetElement;
import com.google.gwt.dom.client.FormElement;
import com.google.gwt.dom.client.FrameElement;
import com.google.gwt.dom.client.FrameSetElement;
import com.google.gwt.dom.client.HRElement;
import com.google.gwt.dom.client.HeadElement;
import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.dom.client.ImageElement;
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
import com.google.gwt.dom.client.TitleElement;
import com.google.gwt.dom.client.UListElement;
import com.google.gwt.dom.client.VideoElement;
import com.google.gwt.safecss.shared.SafeStyles;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;

/**
 * Implementation of methods in {@link ElementBuilderBase} used to render HTML
 * as a string, using innerHtml to generate an element.
 */
class HtmlBuilderImpl extends ElementBuilderImpl {

  /*
   * Common element builders, and those most likely to appear in a loop, are
   * created on initialization to avoid null checks. Less common element
   * builders are created lazily to avoid unnecessary object creation.
   */
  private HtmlAnchorBuilder anchorBuilder;
  private HtmlAreaBuilder areaBuilder;
  private HtmlAudioBuilder audioBuilder;
  private HtmlBaseBuilder baseBuilder;
  private HtmlBodyBuilder bodyBuilder;
  private HtmlBRBuilder brBuilder;
  private HtmlButtonBuilder buttonBuilder;
  private HtmlCanvasBuilder canvasBuilder;
  private final HtmlDivBuilder divBuilder = new HtmlDivBuilder(this);
  private HtmlDListBuilder dListBuilder;
  private final HtmlElementBuilder elementBuilder = new HtmlElementBuilder(this);
  private HtmlFieldSetBuilder fieldSetBuilder;
  private HtmlFormBuilder formBuilder;
  private HtmlFrameBuilder frameBuilder;
  private HtmlFrameSetBuilder frameSetBuilder;
  private HtmlHeadBuilder headBuilder;
  private HtmlHeadingBuilder headingBuilder;
  private HtmlHRBuilder hrBuilder;
  private HtmlIFrameBuilder iFrameBuilder;
  private HtmlImageBuilder imageBuilder;
  private final HtmlInputBuilder inputBuilder = new HtmlInputBuilder(this);
  private HtmlLabelBuilder labelBuilder;
  private HtmlLegendBuilder legendBuilder;
  private final HtmlLIBuilder liBuilder = new HtmlLIBuilder(this);
  private HtmlLinkBuilder linkBuilder;
  private HtmlMapBuilder mapBuilder;
  private HtmlMetaBuilder metaBuilder;
  private HtmlOListBuilder oListBuilder;
  private final HtmlOptionBuilder optionBuilder = new HtmlOptionBuilder(this);
  private HtmlOptGroupBuilder optGroupBuilder;
  private HtmlParagraphBuilder paragraphBuilder;
  private HtmlParamBuilder paramBuilder;
  private HtmlPreBuilder preBuilder;
  private HtmlQuoteBuilder quoteBuilder;
  private HtmlScriptBuilder scriptBuilder;
  private HtmlSelectBuilder selectBuilder;
  private HtmlSourceBuilder sourceBuilder;
  private final HtmlSpanBuilder spanBuilder = new HtmlSpanBuilder(this);
  private HtmlStyleBuilder styleBuilder;
  private final StylesBuilder stylesBuilder = new HtmlStylesBuilder(this);
  private HtmlTableBuilder tableBuilder;
  private final HtmlTableCellBuilder tableCellBuilder = new HtmlTableCellBuilder(this);
  private HtmlTableCaptionBuilder tableCaptionBuilder;
  private HtmlTableColBuilder tableColBuilder;
  private final HtmlTableRowBuilder tableRowBuilder = new HtmlTableRowBuilder(this);
  private HtmlTableSectionBuilder tableSectionBuilder;
  private HtmlTextAreaBuilder textAreaBuilder;
  private HtmlTitleBuilder titleBuilder;
  private HtmlUListBuilder uListBuilder;
  private HtmlVideoBuilder videoBuilder;

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

  public void attribute(String name, int value) {
    trustedAttribute(escape(name), value);
  }

  public void attribute(String name, String value) {
    trustedAttribute(escape(name), value);
  }

  public HtmlAnchorBuilder startAnchor() {
    if (anchorBuilder == null) {
      anchorBuilder = new HtmlAnchorBuilder(this);
    }
    trustedStart(AnchorElement.TAG, anchorBuilder);
    return anchorBuilder;
  }

  public HtmlAreaBuilder startArea() {
    if (areaBuilder == null) {
      areaBuilder = new HtmlAreaBuilder(this);
    }
    trustedStart(AreaElement.TAG, areaBuilder);
    return areaBuilder;
  }

  public HtmlAudioBuilder startAudio() {
    if (audioBuilder == null) {
      audioBuilder = new HtmlAudioBuilder(this);
    }
    trustedStart(AudioElement.TAG, audioBuilder);
    return audioBuilder;
  }

  public HtmlBaseBuilder startBase() {
    if (baseBuilder == null) {
      baseBuilder = new HtmlBaseBuilder(this);
    }
    trustedStart(BaseElement.TAG, baseBuilder);
    return baseBuilder;
  }

  public HtmlQuoteBuilder startBlockQuote() {
    return startQuote(QuoteElement.TAG_BLOCKQUOTE);
  }

  public HtmlBodyBuilder startBody() {
    if (bodyBuilder == null) {
      bodyBuilder = new HtmlBodyBuilder(this);
    }
    trustedStart(BodyElement.TAG, bodyBuilder);
    return bodyBuilder;
  }

  public HtmlBRBuilder startBR() {
    if (brBuilder == null) {
      brBuilder = new HtmlBRBuilder(this);
    }
    trustedStart(BRElement.TAG, brBuilder);
    return brBuilder;
  }

  public InputBuilder startButtonInput() {
    return startInput(ButtonElement.TAG);
  }

  public HtmlCanvasBuilder startCanvas() {
    if (canvasBuilder == null) {
      canvasBuilder = new HtmlCanvasBuilder(this);
    }
    trustedStart(CanvasElement.TAG, canvasBuilder);
    return canvasBuilder;
  }

  public InputBuilder startCheckboxInput() {
    return startInput("checkbox");
  }

  public HtmlTableColBuilder startCol() {
    return startTableCol(TableColElement.TAG_COL);
  }

  public HtmlTableColBuilder startColGroup() {
    return startTableCol(TableColElement.TAG_COLGROUP);
  }

  public HtmlDivBuilder startDiv() {
    trustedStart(DivElement.TAG, divBuilder);
    return divBuilder;
  }

  public HtmlDListBuilder startDList() {
    if (dListBuilder == null) {
      dListBuilder = new HtmlDListBuilder(this);
    }
    trustedStart(DListElement.TAG, dListBuilder);
    return dListBuilder;
  }

  public HtmlFieldSetBuilder startFieldSet() {
    if (fieldSetBuilder == null) {
      fieldSetBuilder = new HtmlFieldSetBuilder(this);
    }
    trustedStart(FieldSetElement.TAG, fieldSetBuilder);
    return fieldSetBuilder;
  }

  public InputBuilder startFileInput() {
    return startInput("file");
  }

  public HtmlFormBuilder startForm() {
    if (formBuilder == null) {
      formBuilder = new HtmlFormBuilder(this);
    }
    trustedStart(FormElement.TAG, formBuilder);
    return formBuilder;
  }

  public HtmlFrameBuilder startFrame() {
    if (frameBuilder == null) {
      frameBuilder = new HtmlFrameBuilder(this);
    }
    trustedStart(FrameElement.TAG, frameBuilder);
    return frameBuilder;
  }

  public HtmlFrameSetBuilder startFrameSet() {
    if (frameSetBuilder == null) {
      frameSetBuilder = new HtmlFrameSetBuilder(this);
    }
    trustedStart(FrameSetElement.TAG, frameSetBuilder);
    return frameSetBuilder;
  }

  public HtmlHeadingBuilder startH1() {
    return startHeading(1);
  }

  public HtmlHeadingBuilder startH2() {
    return startHeading(2);
  }

  public HtmlHeadingBuilder startH3() {
    return startHeading(3);
  }

  public HtmlHeadingBuilder startH4() {
    return startHeading(4);
  }

  public HtmlHeadingBuilder startH5() {
    return startHeading(5);
  }

  public HtmlHeadingBuilder startH6() {
    return startHeading(6);
  }

  public HtmlHeadBuilder startHead() {
    if (headBuilder == null) {
      headBuilder = new HtmlHeadBuilder(this);
    }
    trustedStart(HeadElement.TAG, headBuilder);
    return headBuilder;
  }

  public InputBuilder startHiddenInput() {
    return startInput("hidden");
  }

  public HtmlHRBuilder startHR() {
    if (hrBuilder == null) {
      hrBuilder = new HtmlHRBuilder(this);
    }
    trustedStart(HRElement.TAG, hrBuilder);
    return hrBuilder;
  }

  public HtmlIFrameBuilder startIFrame() {
    if (iFrameBuilder == null) {
      iFrameBuilder = new HtmlIFrameBuilder(this);
    }
    trustedStart(IFrameElement.TAG, iFrameBuilder);
    return iFrameBuilder;
  }

  public HtmlImageBuilder startImage() {
    if (imageBuilder == null) {
      imageBuilder = new HtmlImageBuilder(this);
    }
    trustedStart(ImageElement.TAG, imageBuilder);
    return imageBuilder;
  }

  public InputBuilder startImageInput() {
    return startInput("image");
  }

  public HtmlLabelBuilder startLabel() {
    if (labelBuilder == null) {
      labelBuilder = new HtmlLabelBuilder(this);
    }
    trustedStart(LabelElement.TAG, labelBuilder);
    return labelBuilder;
  }

  public HtmlLegendBuilder startLegend() {
    if (legendBuilder == null) {
      legendBuilder = new HtmlLegendBuilder(this);
    }
    trustedStart(LegendElement.TAG, legendBuilder);
    return legendBuilder;
  }

  public HtmlLIBuilder startLI() {
    trustedStart(LIElement.TAG, liBuilder);
    return liBuilder;
  }

  public HtmlLinkBuilder startLink() {
    if (linkBuilder == null) {
      linkBuilder = new HtmlLinkBuilder(this);
    }
    trustedStart(LinkElement.TAG, linkBuilder);
    return linkBuilder;
  }

  public HtmlMapBuilder startMap() {
    if (mapBuilder == null) {
      mapBuilder = new HtmlMapBuilder(this);
    }
    trustedStart(MapElement.TAG, mapBuilder);
    return mapBuilder;
  }

  public HtmlMetaBuilder startMeta() {
    if (metaBuilder == null) {
      metaBuilder = new HtmlMetaBuilder(this);
    }
    trustedStart(MetaElement.TAG, metaBuilder);
    return metaBuilder;
  }

  public HtmlOListBuilder startOList() {
    if (oListBuilder == null) {
      oListBuilder = new HtmlOListBuilder(this);
    }
    trustedStart(OListElement.TAG, oListBuilder);
    return oListBuilder;
  }

  public HtmlOptGroupBuilder startOptGroup() {
    if (optGroupBuilder == null) {
      optGroupBuilder = new HtmlOptGroupBuilder(this);
    }
    trustedStart(OptGroupElement.TAG, optGroupBuilder);
    return optGroupBuilder;
  }

  public HtmlOptionBuilder startOption() {
    trustedStart(OptionElement.TAG, optionBuilder);
    return optionBuilder;
  }

  public HtmlParagraphBuilder startParagraph() {
    if (paragraphBuilder == null) {
      paragraphBuilder = new HtmlParagraphBuilder(this);
    }
    trustedStart(ParagraphElement.TAG, paragraphBuilder);
    return paragraphBuilder;
  }

  public HtmlParamBuilder startParam() {
    if (paramBuilder == null) {
      paramBuilder = new HtmlParamBuilder(this);
    }
    trustedStart(ParamElement.TAG, paramBuilder);
    return paramBuilder;
  }

  public InputBuilder startPasswordInput() {
    return startInput("password");
  }

  public HtmlPreBuilder startPre() {
    if (preBuilder == null) {
      preBuilder = new HtmlPreBuilder(this);
    }
    trustedStart(PreElement.TAG, preBuilder);
    return preBuilder;
  }

  public HtmlButtonBuilder startPushButton() {
    return startButton("button");
  }

  public HtmlQuoteBuilder startQuote() {
    return startQuote(QuoteElement.TAG_Q);
  }

  public InputBuilder startRadioInput(String name) {
    InputBuilder builder = startInput("radio");
    attribute("name", name);
    return builder;
  }

  public HtmlButtonBuilder startResetButton() {
    return startButton("reset");
  }

  public InputBuilder startResetInput() {
    return startInput("reset");
  }

  public HtmlScriptBuilder startScript() {
    if (scriptBuilder == null) {
      scriptBuilder = new HtmlScriptBuilder(this);
    }
    trustedStart(ScriptElement.TAG, scriptBuilder);
    return scriptBuilder;
  }

  public HtmlSelectBuilder startSelect() {
    if (selectBuilder == null) {
      selectBuilder = new HtmlSelectBuilder(this);
    }
    trustedStart(SelectElement.TAG, selectBuilder);
    return selectBuilder;
  }

  public HtmlSourceBuilder startSource() {
    if (sourceBuilder == null) {
      sourceBuilder = new HtmlSourceBuilder(this);
    }
    trustedStart(SourceElement.TAG, sourceBuilder);
    return sourceBuilder;
  }

  public HtmlSpanBuilder startSpan() {
    trustedStart(SpanElement.TAG, spanBuilder);
    return spanBuilder;
  }

  public HtmlStyleBuilder startStyle() {
    if (styleBuilder == null) {
      styleBuilder = new HtmlStyleBuilder(this);
    }
    trustedStart(StyleElement.TAG, styleBuilder);
    return styleBuilder;
  }

  public HtmlButtonBuilder startSubmitButton() {
    return startButton("submit");
  }

  public InputBuilder startSubmitInput() {
    return startInput("submit");
  }

  public HtmlTableBuilder startTable() {
    if (tableBuilder == null) {
      tableBuilder = new HtmlTableBuilder(this);
    }
    trustedStart(TableElement.TAG, tableBuilder);
    return tableBuilder;
  }

  public HtmlTableCaptionBuilder startTableCaption() {
    if (tableCaptionBuilder == null) {
      tableCaptionBuilder = new HtmlTableCaptionBuilder(this);
    }
    trustedStart(TableCaptionElement.TAG, tableCaptionBuilder);
    return tableCaptionBuilder;
  }

  public HtmlTableSectionBuilder startTBody() {
    return startTableSection(TableSectionElement.TAG_TBODY);
  }

  public HtmlTableCellBuilder startTD() {
    trustedStart(TableCellElement.TAG_TD, tableCellBuilder);
    return tableCellBuilder;
  }

  public HtmlTextAreaBuilder startTextArea() {
    if (textAreaBuilder == null) {
      textAreaBuilder = new HtmlTextAreaBuilder(this);
    }
    trustedStart(TextAreaElement.TAG, textAreaBuilder);
    return textAreaBuilder;
  }

  public InputBuilder startTextInput() {
    return startInput("text");
  }

  public HtmlTableSectionBuilder startTFoot() {
    return startTableSection(TableSectionElement.TAG_TFOOT);
  }

  public HtmlTableCellBuilder startTH() {
    trustedStart(TableCellElement.TAG_TH, tableCellBuilder);
    return tableCellBuilder;
  }

  public HtmlTableSectionBuilder startTHead() {
    return startTableSection(TableSectionElement.TAG_THEAD);
  }

  public HtmlTitleBuilder startTitle() {
    if (titleBuilder == null) {
      titleBuilder = new HtmlTitleBuilder(this);
    }
    trustedStart(TitleElement.TAG, titleBuilder);
    return titleBuilder;
  }

  public HtmlTableRowBuilder startTR() {
    trustedStart(TableRowElement.TAG, tableRowBuilder);
    return tableRowBuilder;
  }

  public HtmlUListBuilder startUList() {
    if (uListBuilder == null) {
      uListBuilder = new HtmlUListBuilder(this);
    }
    trustedStart(UListElement.TAG, uListBuilder);
    return uListBuilder;
  }

  public HtmlVideoBuilder startVideo() {
    if (videoBuilder == null) {
      videoBuilder = new HtmlVideoBuilder(this);
    }
    trustedStart(VideoElement.TAG, videoBuilder);
    return videoBuilder;
  }

  @Override
  public StylesBuilder style() {
    return stylesBuilder;
  }

  public StylesBuilder styleProperty(SafeStyles style) {
    assertCanAddStylePropertyImpl();
    sb.append(style.asString());
    return style();
  }

  /**
   * Add a trusted attribute without escaping the name.
   */
  public void trustedAttribute(String name, int value) {
    assertCanAddAttributeImpl();
    sb.append(" ").append(name).append("=\"").append(value).append("\"");
  }

  /**
   * Add a trusted attribute without escaping the name. The value is still
   * escaped.
   */
  public void trustedAttribute(String name, String value) {
    assertCanAddAttributeImpl();
    sb.append(" ").append(name).append("=\"").append(escape(value)).append("\"");
  }

  public HtmlElementBuilder trustedStart(String tagName) {
    trustedStart(tagName, elementBuilder);
    return elementBuilder;
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
  protected void doEndStartTagImpl() {
    sb.append(" />");
  }

  @Override
  protected void doEndTagImpl(String tagName) {
    /*
     * Add an end tag.
     * 
     * Some browsers do not behave correctly if you self close (ex <select />)
     * certain tags, so we always add the end tag unless the element
     * specifically forbids an end tag (see doEndStartTagImpl()).
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
   * Start a button with the specified type.
   */
  private HtmlButtonBuilder startButton(String type) {
    if (buttonBuilder == null) {
      buttonBuilder = new HtmlButtonBuilder(this);
    }
    trustedStart("button", buttonBuilder);
    buttonBuilder.attribute("type", type);
    return buttonBuilder;
  }

  /**
   * Start one of the many heading elements.
   */
  private HtmlHeadingBuilder startHeading(int level) {
    if (headingBuilder == null) {
      headingBuilder = new HtmlHeadingBuilder(this);
    }
    trustedStart("h" + level, headingBuilder);
    return headingBuilder;
  }

  /**
   * Start an input with the specified type.
   */
  private HtmlInputBuilder startInput(String type) {
    trustedStart("input", inputBuilder);
    attribute("type", type);
    return inputBuilder;
  }

  /**
   * Start a quote or blockquote.
   */
  private HtmlQuoteBuilder startQuote(String tagName) {
    if (quoteBuilder == null) {
      quoteBuilder = new HtmlQuoteBuilder(this);
    }
    trustedStart(tagName, quoteBuilder);
    return quoteBuilder;
  }

  /**
   * Start a table col or colgroup.
   */
  private HtmlTableColBuilder startTableCol(String tagName) {
    if (tableColBuilder == null) {
      tableColBuilder = new HtmlTableColBuilder(this);
    }
    trustedStart(tagName, tableColBuilder);
    return tableColBuilder;
  }

  /**
   * Start a table section of the specified tag name.
   */
  private HtmlTableSectionBuilder startTableSection(String tagName) {
    if (tableSectionBuilder == null) {
      tableSectionBuilder = new HtmlTableSectionBuilder(this);
    }
    trustedStart(tagName, tableSectionBuilder);
    return tableSectionBuilder;
  }

  /**
   * Start a tag using the specified builder. The tagName is not checked or
   * escaped.
   */
  private void trustedStart(String tagName, ElementBuilderBase<?> builder) {
    onStart(tagName, builder);
    sb.append("<").append(tagName);
  }
}
