/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.dom.client;

/**
 * A Document is the root of the HTML hierarchy and holds the entire
 * content. Besides providing access to the hierarchy, it also provides some
 * convenience methods for accessing certain sets of information from the
 * document.
 */
public class Document extends Node {

  private static int curUidIndex = 0;

  public static native Document get() /*-{
     return $doc;
   }-*/;

  protected Document() {
  }

  public final AnchorElement createAnchorElement() {
    return (AnchorElement) DOMImpl.impl.createElement("a");
  }

  public final AreaElement createAreaElement() {
    return (AreaElement) DOMImpl.impl.createElement("area");
  }

  public final BaseElement createBaseElement() {
    return (BaseElement) DOMImpl.impl.createElement("base");
  }

  public final QuoteElement createBlockQuoteElement() {
    return (QuoteElement) DOMImpl.impl.createElement("blockquote");
  }

  public final BRElement createBRElement() {
    return (BRElement) DOMImpl.impl.createElement("br");
  }

  public final ButtonElement createButtonElement() {
    return (ButtonElement) DOMImpl.impl.createElement("button");
  }

  public final TableCaptionElement createCaptionElement() {
    return (TableCaptionElement) DOMImpl.impl.createElement("caption");
  }

  public final InputElement createCheckInputElement() {
    return DOMImpl.impl.createInputElement("checkbox");
  }

  public final TableColElement createColElement() {
    return (TableColElement) DOMImpl.impl.createElement("col");
  }

  public final TableColElement createColGroupElement() {
    return (TableColElement) DOMImpl.impl.createElement("colgroup");
  }

  public final ModElement createDelElement() {
    return (ModElement) DOMImpl.impl.createElement("del");
  }

  public final DivElement createDivElement() {
    return (DivElement) DOMImpl.impl.createElement("div");
  }

  public final DListElement createDLElement() {
    return (DListElement) DOMImpl.impl.createElement("dl");
  }

  public final Element createElement(String tagName) {
    return DOMImpl.impl.createElement(tagName);
  }

  public final FieldSetElement createFieldSetElement() {
    return (FieldSetElement) DOMImpl.impl.createElement("fieldset");
  }

  public final InputElement createFileInputElement() {
    return DOMImpl.impl.createInputElement("file");
  }

  public final FormElement createFormElement() {
    return (FormElement) DOMImpl.impl.createElement("form");
  }

  public final FrameElement createFrameElement() {
    return (FrameElement) DOMImpl.impl.createElement("frame");
  }

  public final FrameSetElement createFrameSetElement() {
    return (FrameSetElement) DOMImpl.impl.createElement("frameset");
  }

  public final HeadElement createHeadElement() {
    return (HeadElement) DOMImpl.impl.createElement("head");
  }

  public final HeadingElement createHElement(int n) {
    assert (n >= 1) && (n <= 6);
    return (HeadingElement) DOMImpl.impl.createElement("h" + n);
  }

  public final InputElement createHiddenInputElement() {
    return DOMImpl.impl.createInputElement("hidden");
  }

  public final HRElement createHRElement() {
    return (HRElement) DOMImpl.impl.createElement("hr");
  }

  public final IFrameElement createIFrameElement() {
    return (IFrameElement) DOMImpl.impl.createElement("iframe");
  }

  public final ImageElement createImageElement() {
    return (ImageElement) DOMImpl.impl.createElement("img");
  }

  public final InputElement createImageInputElement() {
    return DOMImpl.impl.createInputElement("image");
  }

  public final ModElement createInsElement() {
    return (ModElement) DOMImpl.impl.createElement("ins");
  }

  public final LabelElement createLabelElement() {
    return (LabelElement) DOMImpl.impl.createElement("label");
  }

  public final LegendElement createLegendElement() {
    return (LegendElement) DOMImpl.impl.createElement("legend");
  }

  public final LIElement createLIElement() {
    return (LIElement) DOMImpl.impl.createElement("li");
  }

  public final LinkElement createLinkElement() {
    return (LinkElement) DOMImpl.impl.createElement("link");
  }

  public final MapElement createMapElement() {
    return (MapElement) DOMImpl.impl.createElement("map");
  }

  public final MetaElement createMetaElement() {
    return (MetaElement) DOMImpl.impl.createElement("meta");
  }

  public final ObjectElement createObjectElement() {
    return (ObjectElement) DOMImpl.impl.createElement("object");
  }

  public final OListElement createOLElement() {
    return (OListElement) DOMImpl.impl.createElement("ol");
  }

  public final OptGroupElement createOptGroupElement() {
    return (OptGroupElement) DOMImpl.impl.createElement("optgroup");
  }

  public final OptionElement createOptionElement() {
    return (OptionElement) DOMImpl.impl.createElement("option");
  }

  public final ParamElement createParamElement() {
    return (ParamElement) DOMImpl.impl.createElement("param");
  }

  public final InputElement createPasswordInputElement() {
    return DOMImpl.impl.createInputElement("password");
  }

  public final ParagraphElement createPElement() {
    return (ParagraphElement) DOMImpl.impl.createElement("p");
  }

  public final PreElement createPreElement() {
    return (PreElement) DOMImpl.impl.createElement("pre");
  }

  public final QuoteElement createQElement() {
    return (QuoteElement) DOMImpl.impl.createElement("q");
  }

  public final InputElement createRadioInputElement(String name) {
    return DOMImpl.impl.createInputRadioElement(name);
  }

  public final ScriptElement createScriptElement() {
    return (ScriptElement) DOMImpl.impl.createElement("script");
  }

  public final SelectElement createSelectElement() {
    return DOMImpl.impl.createSelectElement(false);
  }

  public final SelectElement createSelectElement(boolean multiple) {
    return DOMImpl.impl.createSelectElement(multiple);
  }

  public final SpanElement createSpanElement() {
    return (SpanElement) DOMImpl.impl.createElement("span");
  }

  public final StyleElement createStyleElement() {
    return (StyleElement) DOMImpl.impl.createElement("style");
  }

  public final TableElement createTableElement() {
    return (TableElement) DOMImpl.impl.createElement("table");
  }

  public final TableSectionElement createTBodyElement() {
    return (TableSectionElement) DOMImpl.impl.createElement("tbody");
  }

  public final TableCellElement createTDElement() {
    return (TableCellElement) DOMImpl.impl.createElement("td");
  }

  public final TextAreaElement createTextAreaElement() {
    return (TextAreaElement) DOMImpl.impl.createElement("textarea");
  }

  public final InputElement createTextInputElement() {
    return DOMImpl.impl.createInputElement("text");
  }

  public final native Text createTextNode(String data) /*-{
     return this.createTextNode(data);
   }-*/;

  public final TableSectionElement createTFootElement() {
    return (TableSectionElement) DOMImpl.impl.createElement("tfoot");
  }

  public final TableSectionElement createTHeadElement() {
    return (TableSectionElement) DOMImpl.impl.createElement("thead");
  }

  public final TableCellElement createTHElement() {
    return (TableCellElement) DOMImpl.impl.createElement("th");
  }

  public final TitleElement createTitleElement() {
    return (TitleElement) DOMImpl.impl.createElement("title");
  }

  public final TableRowElement createTRElement() {
    return (TableRowElement) DOMImpl.impl.createElement("tr");
  }

  public final UListElement createULElement() {
    return (UListElement) DOMImpl.impl.createElement("ul");
  }

  public final String createUniqueId() {
    return "uid-" + (++curUidIndex);
  }

  public final native BodyElement getBody() /*-{
     return this.body;
   }-*/;

  public final native String getDomain() /*-{
     return this.domain;
   }-*/;

  public final native Element getElementById(String id) /*-{
     return this.getElementById(id);
   }-*/;

  public final native NodeList<Element> getElementsByTagName(String tagName) /*-{
     return this.getElementsByTagName(tagName);
   }-*/;

  public final native String getReferrer() /*-{
     return this.referrer;
   }-*/;

  public final native String getTitle() /*-{
     return this.title;
   }-*/;

  public final native String getURL() /*-{
     return this.URL;
   }-*/;

  public final native void importNode(Node node, boolean deep) /*-{
    this.importNode(node, deep);
  }-*/;

  public final native void setTitle(String title) /*-{
     this.title = title;
   }-*/;
}
