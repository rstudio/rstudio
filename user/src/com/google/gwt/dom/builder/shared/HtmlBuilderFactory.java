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

/**
 * Factory for creating element builders that use string concatenation to
 * generate HTML.
 */
public class HtmlBuilderFactory extends ElementBuilderFactory {

  private static HtmlBuilderFactory instance;

  /**
   * Get the instance of the {@link HtmlBuilderFactory}.
   * 
   * <p>
   * Use {@link ElementBuilderFactory#get()} to fetch a factory optimized for
   * the browser client. However, you can use this factory directly if you want
   * to force the builders to builder elements using HTML string concatenation
   * and innerHTML. You can also use this factory if you want access to the HTML
   * string, such as when you are building HTML on a server.
   * </p>
   * 
   * @return the {@link ElementBuilderFactory}
   */
  public static HtmlBuilderFactory get() {
    if (instance == null) {
      instance = new HtmlBuilderFactory();
    }
    return instance;
  }

  /**
   * Created from static factory method.
   */
  protected HtmlBuilderFactory() {
  }

  @Override
  public HtmlAnchorBuilder createAnchorBuilder() {
    return impl().startAnchor();
  }

  @Override
  public HtmlAreaBuilder createAreaBuilder() {
    return impl().startArea();
  }

  @Override
  public HtmlAudioBuilder createAudioBuilder() {
    return impl().startAudio();
  }

  @Override
  public HtmlBaseBuilder createBaseBuilder() {
    return impl().startBase();
  }

  @Override
  public HtmlQuoteBuilder createBlockQuoteBuilder() {
    return impl().startBlockQuote();
  }

  @Override
  public HtmlBodyBuilder createBodyBuilder() {
    return impl().startBody();
  }

  @Override
  public HtmlBRBuilder createBRBuilder() {
    return impl().startBR();
  }

  @Override
  public HtmlInputBuilder createButtonInputBuilder() {
    return impl().startButtonInput();
  }

  @Override
  public HtmlCanvasBuilder createCanvasBuilder() {
    return impl().startCanvas();
  }

  @Override
  public HtmlInputBuilder createCheckboxInputBuilder() {
    return impl().startCheckboxInput();
  }

  @Override
  public HtmlTableColBuilder createColBuilder() {
    return impl().startCol();
  }

  @Override
  public HtmlTableColBuilder createColGroupBuilder() {
    return impl().startColGroup();
  }

  @Override
  public HtmlDivBuilder createDivBuilder() {
    return impl().startDiv();
  }

  @Override
  public HtmlDListBuilder createDListBuilder() {
    return impl().startDList();
  }

  @Override
  public HtmlFieldSetBuilder createFieldSetBuilder() {
    return impl().startFieldSet();
  }

  @Override
  public HtmlInputBuilder createFileInputBuilder() {
    return impl().startFileInput();
  }

  @Override
  public HtmlFormBuilder createFormBuilder() {
    return impl().startForm();
  }

  @Override
  public HtmlFrameBuilder createFrameBuilder() {
    return impl().startFrame();
  }

  @Override
  public HtmlFrameSetBuilder createFrameSetBuilder() {
    return impl().startFrameSet();
  }

  @Override
  public HtmlHeadingBuilder createH1Builder() {
    return impl().startH1();
  }

  @Override
  public HtmlHeadingBuilder createH2Builder() {
    return impl().startH2();
  }

  @Override
  public HtmlHeadingBuilder createH3Builder() {
    return impl().startH3();
  }

  @Override
  public HtmlHeadingBuilder createH4Builder() {
    return impl().startH4();
  }

  @Override
  public HtmlHeadingBuilder createH5Builder() {
    return impl().startH5();
  }

  @Override
  public HtmlHeadingBuilder createH6Builder() {
    return impl().startH6();
  }

  @Override
  public HtmlHeadBuilder createHeadBuilder() {
    return impl().startHead();
  }

  @Override
  public HtmlInputBuilder createHiddenInputBuilder() {
    return impl().startHiddenInput();
  }

  @Override
  public HtmlHRBuilder createHRBuilder() {
    return impl().startHR();
  }

  @Override
  public HtmlIFrameBuilder createIFrameBuilder() {
    return impl().startIFrame();
  }

  @Override
  public HtmlImageBuilder createImageBuilder() {
    return impl().startImage();
  }

  @Override
  public HtmlInputBuilder createImageInputBuilder() {
    return impl().startImageInput();
  }

  @Override
  public HtmlLabelBuilder createLabelBuilder() {
    return impl().startLabel();
  }

  @Override
  public HtmlLegendBuilder createLegendBuilder() {
    return impl().startLegend();
  }

  @Override
  public HtmlLIBuilder createLIBuilder() {
    return impl().startLI();
  }

  @Override
  public HtmlLinkBuilder createLinkBuilder() {
    return impl().startLink();
  }

  @Override
  public HtmlMapBuilder createMapBuilder() {
    return impl().startMap();
  }

  @Override
  public HtmlMetaBuilder createMetaBuilder() {
    return impl().startMeta();
  }

  @Override
  public HtmlOListBuilder createOListBuilder() {
    return impl().startOList();
  }

  @Override
  public HtmlOptGroupBuilder createOptGroupBuilder() {
    return impl().startOptGroup();
  }

  @Override
  public HtmlOptionBuilder createOptionBuilder() {
    return impl().startOption();
  }

  @Override
  public HtmlParagraphBuilder createParagraphBuilder() {
    return impl().startParagraph();
  }

  @Override
  public HtmlParamBuilder createParamBuilder() {
    return impl().startParam();
  }

  @Override
  public HtmlInputBuilder createPasswordInputBuilder() {
    return impl().startPasswordInput();
  }

  @Override
  public HtmlPreBuilder createPreBuilder() {
    return impl().startPre();
  }

  @Override
  public HtmlButtonBuilder createPushButtonBuilder() {
    return impl().startPushButton();
  }

  @Override
  public HtmlQuoteBuilder createQuoteBuilder() {
    return impl().startQuote();
  }

  @Override
  public HtmlInputBuilder createRadioInputBuilder(String name) {
    return impl().startRadioInput(name);
  }

  @Override
  public HtmlButtonBuilder createResetButtonBuilder() {
    return impl().startResetButton();
  }

  @Override
  public HtmlInputBuilder createResetInputBuilder() {
    return impl().startResetInput();
  }

  @Override
  public HtmlScriptBuilder createScriptBuilder() {
    return impl().startScript();
  }

  @Override
  public HtmlSelectBuilder createSelectBuilder() {
    return impl().startSelect();
  }

  @Override
  public HtmlSourceBuilder createSourceBuilder() {
    return impl().startSource();
  }

  @Override
  public HtmlSpanBuilder createSpanBuilder() {
    return impl().startSpan();
  }

  @Override
  public HtmlStyleBuilder createStyleBuilder() {
    return impl().startStyle();
  }

  @Override
  public HtmlButtonBuilder createSubmitButtonBuilder() {
    return impl().startSubmitButton();
  }

  @Override
  public HtmlInputBuilder createSubmitInputBuilder() {
    return impl().startSubmitInput();
  }

  @Override
  public HtmlTableBuilder createTableBuilder() {
    return impl().startTable();
  }

  @Override
  public HtmlTableCaptionBuilder createTableCaptionBuilder() {
    return impl().startTableCaption();
  }

  @Override
  public HtmlTableSectionBuilder createTBodyBuilder() {
    return impl().startTBody();
  }

  @Override
  public HtmlTableCellBuilder createTDBuilder() {
    return impl().startTD();
  }

  @Override
  public HtmlTextAreaBuilder createTextAreaBuilder() {
    return impl().startTextArea();
  }

  @Override
  public HtmlInputBuilder createTextInputBuilder() {
    return impl().startTextInput();
  }

  @Override
  public HtmlTableSectionBuilder createTFootBuilder() {
    return impl().startTFoot();
  }

  @Override
  public HtmlTableCellBuilder createTHBuilder() {
    return impl().startTH();
  }

  @Override
  public HtmlTableSectionBuilder createTHeadBuilder() {
    return impl().startTHead();
  }

  public HtmlTitleBuilder createTitleBuilder() {
    return impl().startTitle();
  }

  @Override
  public HtmlTableRowBuilder createTRBuilder() {
    return impl().startTR();
  }

  @Override
  public HtmlUListBuilder createUListBuilder() {
    return impl().startUList();
  }

  @Override
  public HtmlVideoBuilder createVideoBuilder() {
    return impl().startVideo();
  }

  @Override
  public HtmlElementBuilder trustedCreate(String tagName) {
    return impl().trustedStart(tagName);
  }

  private HtmlBuilderImpl impl() {
    return new HtmlBuilderImpl();
  }
}
