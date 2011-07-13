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
import com.google.gwt.dom.builder.shared.InputBuilder;
import com.google.gwt.dom.builder.shared.StylesBuilder;
import com.google.gwt.dom.client.ButtonElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.QuoteElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.TableColElement;
import com.google.gwt.dom.client.TableSectionElement;
import com.google.gwt.safehtml.shared.SafeHtml;

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
  private DomAnchorBuilder anchorBuilder;
  private DomAreaBuilder areaBuilder;
  private DomAudioBuilder audioBuilder;
  private DomBaseBuilder baseBuilder;
  private DomBodyBuilder bodyBuilder;
  private DomBRBuilder brBuilder;
  private DomButtonBuilder buttonBuilder;
  private DomCanvasBuilder canvasBuilder;
  private final DomDivBuilder divBuilder = new DomDivBuilder(this);
  private DomDListBuilder dListBuilder;
  private final DomElementBuilder elementBuilder = new DomElementBuilder(this);
  private DomFieldSetBuilder fieldSetBuilder;
  private DomFormBuilder formBuilder;
  private DomFrameBuilder frameBuilder;
  private DomFrameSetBuilder frameSetBuilder;
  private DomHeadBuilder headBuilder;
  private DomHeadingBuilder headingBuilder;
  private DomHRBuilder hrBuilder;
  private DomIFrameBuilder iFrameBuilder;
  private DomImageBuilder imageBuilder;
  private final DomInputBuilder inputBuilder = new DomInputBuilder(this);
  private DomLabelBuilder labelBuilder;
  private DomLegendBuilder legendBuilder;
  private final DomLIBuilder liBuilder = new DomLIBuilder(this);
  private DomLinkBuilder linkBuilder;
  private DomMapBuilder mapBuilder;
  private DomMetaBuilder metaBuilder;
  private DomOListBuilder oListBuilder;
  private final DomOptionBuilder optionBuilder = new DomOptionBuilder(this);
  private DomOptGroupBuilder optGroupBuilder;
  private DomParagraphBuilder paragraphBuilder;
  private DomParamBuilder paramBuilder;
  private DomPreBuilder preBuilder;
  private DomQuoteBuilder quoteBuilder;
  private DomScriptBuilder scriptBuilder;
  private DomSelectBuilder selectBuilder;
  private DomSourceBuilder sourceBuilder;
  private final DomSpanBuilder spanBuilder = new DomSpanBuilder(this);
  private final StylesBuilder stylesBuilder = new DomStylesBuilder(this);
  private DomStyleBuilder styleBuilder;
  private DomTableBuilder tableBuilder;
  private final DomTableCellBuilder tableCellBuilder = new DomTableCellBuilder(this);
  private DomTableCaptionBuilder tableCaptionBuilder;
  private DomTableColBuilder tableColBuilder;
  private final DomTableRowBuilder tableRowBuilder = new DomTableRowBuilder(this);
  private DomTableSectionBuilder tableSectionBuilder;
  private DomTextAreaBuilder textAreaBuilder;
  private DomUListBuilder uListBuilder;
  private DomVideoBuilder videoBuilder;

  /**
   * The root element of the DOM structure being built.
   */
  private Element rootElement;

  /**
   * The element at the top of the stack. We use DOM manipulation to move up and
   * down the stack.
   */
  private Element currentElement;

  public DomAnchorBuilder startAnchor() {
    if (anchorBuilder == null) {
      anchorBuilder = new DomAnchorBuilder(this);
    }
    start(Document.get().createAnchorElement(), anchorBuilder);
    return anchorBuilder;
  }

  public DomAreaBuilder startArea() {
    if (areaBuilder == null) {
      areaBuilder = new DomAreaBuilder(this);
    }
    start(Document.get().createAreaElement(), areaBuilder);
    return areaBuilder;
  }

  public DomAudioBuilder startAudio() {
    if (audioBuilder == null) {
      audioBuilder = new DomAudioBuilder(this);
    }
    start(Document.get().createAudioElement(), audioBuilder);
    return audioBuilder;
  }

  public DomBaseBuilder startBase() {
    if (baseBuilder == null) {
      baseBuilder = new DomBaseBuilder(this);
    }
    start(Document.get().createBaseElement(), baseBuilder);
    return baseBuilder;
  }

  public DomQuoteBuilder startBlockQuote() {
    return startQuote(Document.get().createBlockQuoteElement());
  }

  public DomBodyBuilder startBody() {
    if (bodyBuilder == null) {
      bodyBuilder = new DomBodyBuilder(this);
    }
    start(Document.get().createElement("body"), bodyBuilder);
    return bodyBuilder;
  }

  public DomBRBuilder startBR() {
    if (brBuilder == null) {
      brBuilder = new DomBRBuilder(this);
    }
    start(Document.get().createBRElement(), brBuilder);
    return brBuilder;
  }

  public InputBuilder startButtonInput() {
    return startInput(Document.get().createButtonInputElement());
  }

  public DomCanvasBuilder startCanvas() {
    if (canvasBuilder == null) {
      canvasBuilder = new DomCanvasBuilder(this);
    }
    start(Document.get().createCanvasElement(), canvasBuilder);
    return canvasBuilder;
  }

  public InputBuilder startCheckboxInput() {
    return startInput(Document.get().createCheckInputElement());
  }

  public DomTableColBuilder startCol() {
    return startTableCol(Document.get().createColElement());
  }

  public DomTableColBuilder startColGroup() {
    return startTableCol(Document.get().createColGroupElement());
  }

  public DomDivBuilder startDiv() {
    start(Document.get().createDivElement(), divBuilder);
    return divBuilder;
  }

  public DomDListBuilder startDList() {
    if (dListBuilder == null) {
      dListBuilder = new DomDListBuilder(this);
    }
    start(Document.get().createDLElement(), dListBuilder);
    return dListBuilder;
  }

  public DomFieldSetBuilder startFieldSet() {
    if (fieldSetBuilder == null) {
      fieldSetBuilder = new DomFieldSetBuilder(this);
    }
    start(Document.get().createFieldSetElement(), fieldSetBuilder);
    return fieldSetBuilder;
  }

  public InputBuilder startFileInput() {
    return startInput(Document.get().createFileInputElement());
  }

  public DomFormBuilder startForm() {
    if (formBuilder == null) {
      formBuilder = new DomFormBuilder(this);
    }
    start(Document.get().createFormElement(), formBuilder);
    return formBuilder;
  }

  public DomFrameBuilder startFrame() {
    if (frameBuilder == null) {
      frameBuilder = new DomFrameBuilder(this);
    }
    start(Document.get().createFrameElement(), frameBuilder);
    return frameBuilder;
  }

  public DomFrameSetBuilder startFrameSet() {
    if (frameSetBuilder == null) {
      frameSetBuilder = new DomFrameSetBuilder(this);
    }
    start(Document.get().createFrameSetElement(), frameSetBuilder);
    return frameSetBuilder;
  }

  public DomHeadingBuilder startH1() {
    return startHeading(1);
  }

  public DomHeadingBuilder startH2() {
    return startHeading(2);
  }

  public DomHeadingBuilder startH3() {
    return startHeading(3);
  }

  public DomHeadingBuilder startH4() {
    return startHeading(4);
  }

  public DomHeadingBuilder startH5() {
    return startHeading(5);
  }

  public DomHeadingBuilder startH6() {
    return startHeading(6);
  }

  public DomHeadBuilder startHead() {
    if (headBuilder == null) {
      headBuilder = new DomHeadBuilder(this);
    }
    start(Document.get().createHeadElement(), headBuilder);
    return headBuilder;
  }

  public InputBuilder startHiddenInput() {
    return startInput(Document.get().createHiddenInputElement());
  }

  public DomHRBuilder startHR() {
    if (hrBuilder == null) {
      hrBuilder = new DomHRBuilder(this);
    }
    start(Document.get().createHRElement(), hrBuilder);
    return hrBuilder;
  }

  public DomIFrameBuilder startIFrame() {
    if (iFrameBuilder == null) {
      iFrameBuilder = new DomIFrameBuilder(this);
    }
    start(Document.get().createIFrameElement(), iFrameBuilder);
    return iFrameBuilder;
  }

  public DomImageBuilder startImage() {
    if (imageBuilder == null) {
      imageBuilder = new DomImageBuilder(this);
    }
    start(Document.get().createImageElement(), imageBuilder);
    return imageBuilder;
  }

  public InputBuilder startImageInput() {
    return startInput(Document.get().createImageInputElement());
  }

  /**
   * Start an input using the specified InputElement.
   */
  public DomInputBuilder startInput(InputElement input) {
    start(input, inputBuilder);
    return inputBuilder;
  }

  public DomLabelBuilder startLabel() {
    if (labelBuilder == null) {
      labelBuilder = new DomLabelBuilder(this);
    }
    start(Document.get().createLabelElement(), labelBuilder);
    return labelBuilder;
  }

  public DomLegendBuilder startLegend() {
    if (legendBuilder == null) {
      legendBuilder = new DomLegendBuilder(this);
    }
    start(Document.get().createLegendElement(), legendBuilder);
    return legendBuilder;
  }

  public DomLIBuilder startLI() {
    start(Document.get().createLIElement(), liBuilder);
    return liBuilder;
  }

  public DomLinkBuilder startLink() {
    if (linkBuilder == null) {
      linkBuilder = new DomLinkBuilder(this);
    }
    start(Document.get().createLinkElement(), linkBuilder);
    return linkBuilder;
  }

  public DomMapBuilder startMap() {
    if (mapBuilder == null) {
      mapBuilder = new DomMapBuilder(this);
    }
    start(Document.get().createMapElement(), mapBuilder);
    return mapBuilder;
  }

  public DomMetaBuilder startMeta() {
    if (metaBuilder == null) {
      metaBuilder = new DomMetaBuilder(this);
    }
    start(Document.get().createMetaElement(), metaBuilder);
    return metaBuilder;
  }

  public DomOListBuilder startOList() {
    if (oListBuilder == null) {
      oListBuilder = new DomOListBuilder(this);
    }
    start(Document.get().createOLElement(), oListBuilder);
    return oListBuilder;
  }

  public DomOptGroupBuilder startOptGroup() {
    if (optGroupBuilder == null) {
      optGroupBuilder = new DomOptGroupBuilder(this);
    }
    start(Document.get().createOptGroupElement(), optGroupBuilder);
    return optGroupBuilder;
  }

  public DomOptionBuilder startOption() {
    start(Document.get().createOptionElement(), optionBuilder);
    return optionBuilder;
  }

  public DomParagraphBuilder startParagraph() {
    if (paragraphBuilder == null) {
      paragraphBuilder = new DomParagraphBuilder(this);
    }
    start(Document.get().createPElement(), paragraphBuilder);
    return paragraphBuilder;
  }

  public DomParamBuilder startParam() {
    if (paramBuilder == null) {
      paramBuilder = new DomParamBuilder(this);
    }
    start(Document.get().createParamElement(), paramBuilder);
    return paramBuilder;
  }

  public InputBuilder startPasswordInput() {
    return startInput(Document.get().createPasswordInputElement());
  }

  public DomPreBuilder startPre() {
    if (preBuilder == null) {
      preBuilder = new DomPreBuilder(this);
    }
    start(Document.get().createPreElement(), preBuilder);
    return preBuilder;
  }

  public DomButtonBuilder startPushButton() {
    return startButton(Document.get().createPushButtonElement());
  }

  public DomQuoteBuilder startQuote() {
    return startQuote(Document.get().createQElement());
  }

  public InputBuilder startRadioInput(String name) {
    return startInput(Document.get().createRadioInputElement(name));
  }

  public DomButtonBuilder startResetButton() {
    return startButton(Document.get().createResetButtonElement());
  }

  public InputBuilder startResetInput() {
    return startInput(Document.get().createSubmitInputElement());
  }

  public DomScriptBuilder startScript() {
    if (scriptBuilder == null) {
      scriptBuilder = new DomScriptBuilder(this);
    }
    start(Document.get().createScriptElement(), scriptBuilder);
    return scriptBuilder;
  }

  public DomSelectBuilder startSelect() {
    if (selectBuilder == null) {
      selectBuilder = new DomSelectBuilder(this);
    }
    start(Document.get().createSelectElement(), selectBuilder);
    return selectBuilder;
  }

  public DomSourceBuilder startSource() {
    if (sourceBuilder == null) {
      sourceBuilder = new DomSourceBuilder(this);
    }
    start(Document.get().createSourceElement(), sourceBuilder);
    return sourceBuilder;
  }

  public DomSpanBuilder startSpan() {
    start(Document.get().createSpanElement(), spanBuilder);
    return spanBuilder;
  }

  public DomStyleBuilder startStyle() {
    if (styleBuilder == null) {
      styleBuilder = new DomStyleBuilder(this);
    }
    start(Document.get().createStyleElement(), styleBuilder);
    return styleBuilder;
  }

  public DomButtonBuilder startSubmitButton() {
    return startButton(Document.get().createSubmitButtonElement());
  }

  public InputBuilder startSubmitInput() {
    return startInput(Document.get().createSubmitInputElement());
  }

  public DomTableBuilder startTable() {
    if (tableBuilder == null) {
      tableBuilder = new DomTableBuilder(this);
    }
    start(Document.get().createTableElement(), tableBuilder);
    return tableBuilder;
  }

  public DomTableCaptionBuilder startTableCaption() {
    if (tableCaptionBuilder == null) {
      tableCaptionBuilder = new DomTableCaptionBuilder(this);
    }
    start(Document.get().createCaptionElement(), tableCaptionBuilder);
    return tableCaptionBuilder;
  }

  public DomTableSectionBuilder startTBody() {
    return startTableSection(Document.get().createTBodyElement());
  }

  public DomTableCellBuilder startTD() {
    start(Document.get().createTDElement(), tableCellBuilder);
    return tableCellBuilder;
  }

  public DomTextAreaBuilder startTextArea() {
    if (textAreaBuilder == null) {
      textAreaBuilder = new DomTextAreaBuilder(this);
    }
    start(Document.get().createTextAreaElement(), textAreaBuilder);
    return textAreaBuilder;
  }

  public DomTableSectionBuilder startTFoot() {
    return startTableSection(Document.get().createTFootElement());
  }

  public DomTableCellBuilder startTH() {
    start(Document.get().createTHElement(), tableCellBuilder);
    return tableCellBuilder;
  }

  public DomTableSectionBuilder startTHead() {
    return startTableSection(Document.get().createTHeadElement());
  }

  public DomTableRowBuilder startTR() {
    start(Document.get().createTRElement(), tableRowBuilder);
    return tableRowBuilder;
  }

  public DomUListBuilder startUList() {
    if (uListBuilder == null) {
      uListBuilder = new DomUListBuilder(this);
    }
    start(Document.get().createULElement(), uListBuilder);
    return uListBuilder;
  }

  public DomVideoBuilder startVideo() {
    if (videoBuilder == null) {
      videoBuilder = new DomVideoBuilder(this);
    }
    start(Document.get().createVideoElement(), videoBuilder);
    return videoBuilder;
  }

  @Override
  public StylesBuilder style() {
    return stylesBuilder;
  }

  public DomElementBuilder trustedStart(String tagName) {
    /*
     * Validate the tag before trying to create the element, or the browser may
     * throw a JS error and prevent us from triggering an
     * IllegalArgumentException.
     */
    assertValidTagName(tagName);
    start(Document.get().createElement(tagName), elementBuilder);
    return elementBuilder;
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
  protected void doEndStartTagImpl() {
    popElement();
  }

  @Override
  protected void doEndTagImpl(String tagName) {
    popElement();
  }

  @Override
  protected Element doFinishImpl() {
    return rootElement;
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

  @Override
  protected void lockCurrentElement() {
    // Overridden for visibility.
    super.lockCurrentElement();
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

  InputBuilder startTextInput() {
    return startInput(Document.get().createTextInputElement());
  }

  /**
   * Pop to the previous element in the stack.
   */
  private void popElement() {
    currentElement = getCurrentElement().getParentElement();
  }

  /**
   * Start a child element.
   * 
   * @param element the element to start
   * @param builder the builder used to builder the new element
   */
  private void start(Element element, ElementBuilderBase<?> builder) {
    onStart(element.getTagName(), builder);

    // Set the root element.
    if (rootElement == null) {
      // This is the new root element.
      rootElement = element;
    } else {
      // Appending to the current element.
      getCurrentElement().appendChild(element);
    }

    // Add the element to the stack.
    currentElement = element;
  }

  /**
   * Start a button using the specified {@link ButtonElement}.
   */
  private DomButtonBuilder startButton(ButtonElement button) {
    if (buttonBuilder == null) {
      buttonBuilder = new DomButtonBuilder(this);
    }
    start(button, buttonBuilder);
    return buttonBuilder;
  }

  /**
   * Start one of the many headers.
   */
  private DomHeadingBuilder startHeading(int level) {
    if (headingBuilder == null) {
      headingBuilder = new DomHeadingBuilder(this);
    }
    start(Document.get().createHElement(level), headingBuilder);
    return headingBuilder;
  }

  /**
   * Start a quote or blockquote.
   */
  private DomQuoteBuilder startQuote(QuoteElement quote) {
    if (quoteBuilder == null) {
      quoteBuilder = new DomQuoteBuilder(this);
    }
    start(quote, quoteBuilder);
    return quoteBuilder;
  }

  /**
   * Start a table col or colgroup.
   */
  private DomTableColBuilder startTableCol(TableColElement element) {
    if (tableColBuilder == null) {
      tableColBuilder = new DomTableColBuilder(this);
    }
    start(element, tableColBuilder);
    return tableColBuilder;
  }

  /**
   * Start a table section using the specified {@link TableSectionElement}.
   */
  private DomTableSectionBuilder startTableSection(TableSectionElement section) {
    if (tableSectionBuilder == null) {
      tableSectionBuilder = new DomTableSectionBuilder(this);
    }
    start(section, tableSectionBuilder);
    return tableSectionBuilder;
  }
}
