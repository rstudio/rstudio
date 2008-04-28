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

  /*
   * Used by createUniqueId(). This is intentionally static to make the
   * generated id unique across all documents (even though createUniqueId() only
   * requires that an id be unique to a particular document, this will minimize
   * the probability of accidental collisions, and is slightly more efficient).
   */
  private static int curUidIndex = 0;

  /**
   * Gets the default document. This is the document in which the module is
   * running.
   * 
   * @return the default document
   */
  public static native Document get() /*-{
    return $doc;
  }-*/;

  protected Document() {
  }

  /**
   * Creates an &lt;a&gt; element.
   * 
   * @return the newly created element
   */
  public final AnchorElement createAnchorElement() {
    return (AnchorElement) DOMImpl.impl.createElement("a");
  }

  /**
   * Creates an &lt;area&gt; element.
   * 
   * @return the newly created element
   */
  public final AreaElement createAreaElement() {
    return (AreaElement) DOMImpl.impl.createElement("area");
  }

  /**
   * Creates a &lt;base&gt; element.
   * 
   * @return the newly created element
   */
  public final BaseElement createBaseElement() {
    return (BaseElement) DOMImpl.impl.createElement("base");
  }

  /**
   * Creates a &lt;blockquote&gt; element.
   * 
   * @return the newly created element
   */
  public final QuoteElement createBlockQuoteElement() {
    return (QuoteElement) DOMImpl.impl.createElement("blockquote");
  }

  /**
   * Creates a &lt;br&gt; element.
   * 
   * @return the newly created element
   */
  public final BRElement createBRElement() {
    return (BRElement) DOMImpl.impl.createElement("br");
  }

  /**
   * Creates a &lt;button&gt; element.
   * 
   * @return the newly created element
   */
  public final ButtonElement createButtonElement() {
    return (ButtonElement) DOMImpl.impl.createElement("button");
  }

  /**
   * Creates a &lt;caption&gt; element.
   * 
   * @return the newly created element
   */
  public final TableCaptionElement createCaptionElement() {
    return (TableCaptionElement) DOMImpl.impl.createElement("caption");
  }

  /**
   * Creates an &lt;input type='checkbox'&gt; element.
   * 
   * @return the newly created element
   */
  public final InputElement createCheckInputElement() {
    return DOMImpl.impl.createInputElement("checkbox");
  }

  /**
   * Creates a &lt;col&gt; element.
   * 
   * @return the newly created element
   */
  public final TableColElement createColElement() {
    return (TableColElement) DOMImpl.impl.createElement("col");
  }

  /**
   * Creates a &lt;colgroup&gt; element.
   * 
   * @return the newly created element
   */
  public final TableColElement createColGroupElement() {
    return (TableColElement) DOMImpl.impl.createElement("colgroup");
  }

  /**
   * Creates a &lt;del&gt; element.
   * 
   * @return the newly created element
   */
  public final ModElement createDelElement() {
    return (ModElement) DOMImpl.impl.createElement("del");
  }

  /**
   * Creates a &lt;div&gt; element.
   * 
   * @return the newly created element
   */
  public final DivElement createDivElement() {
    return (DivElement) DOMImpl.impl.createElement("div");
  }

  /**
   * Creates a &lt;dl&gt; element.
   * 
   * @return the newly created element
   */
  public final DListElement createDLElement() {
    return (DListElement) DOMImpl.impl.createElement("dl");
  }

  /**
   * Creates a new element.
   * 
   * @param tagName the tag name of the element to be created
   * @return the newly created element
   */
  public final Element createElement(String tagName) {
    return DOMImpl.impl.createElement(tagName);
  }

  /**
   * Creates a &lt;fieldset&gt; element.
   * 
   * @return the newly created element
   */
  public final FieldSetElement createFieldSetElement() {
    return (FieldSetElement) DOMImpl.impl.createElement("fieldset");
  }

  /**
   * Creates an &lt;input type='file'&gt; element.
   * 
   * @return the newly created element
   */
  public final InputElement createFileInputElement() {
    return DOMImpl.impl.createInputElement("file");
  }

  /**
   * Creates a &lt;form&gt; element.
   * 
   * @return the newly created element
   */
  public final FormElement createFormElement() {
    return (FormElement) DOMImpl.impl.createElement("form");
  }

  /**
   * Creates a &lt;frame&gt; element.
   * 
   * @return the newly created element
   */
  public final FrameElement createFrameElement() {
    return (FrameElement) DOMImpl.impl.createElement("frame");
  }

  /**
   * Creates a &lt;frameset&gt; element.
   * 
   * @return the newly created element
   */
  public final FrameSetElement createFrameSetElement() {
    return (FrameSetElement) DOMImpl.impl.createElement("frameset");
  }

  /**
   * Creates a &lt;head&gt; element.
   * 
   * @return the newly created element
   */
  public final HeadElement createHeadElement() {
    return (HeadElement) DOMImpl.impl.createElement("head");
  }

  /**
   * Creates an &lt;h(n)&gt; element.
   * 
   * @param n the type of heading, from 1 to 6 inclusive
   * @return the newly created element
   */
  public final HeadingElement createHElement(int n) {
    assert (n >= 1) && (n <= 6);
    return (HeadingElement) DOMImpl.impl.createElement("h" + n);
  }

  /**
   * Creates an &lt;input type='hidden'&gt; element.
   * 
   * @return the newly created element
   */
  public final InputElement createHiddenInputElement() {
    return DOMImpl.impl.createInputElement("hidden");
  }

  /**
   * Creates an &lt;hr&gt; element.
   * 
   * @return the newly created element
   */
  public final HRElement createHRElement() {
    return (HRElement) DOMImpl.impl.createElement("hr");
  }

  /**
   * Creates an &lt;iframe&gt; element.
   * 
   * @return the newly created element
   */
  public final IFrameElement createIFrameElement() {
    return (IFrameElement) DOMImpl.impl.createElement("iframe");
  }

  /**
   * Creates an &lt;img&gt; element.
   * 
   * @return the newly created element
   */
  public final ImageElement createImageElement() {
    return (ImageElement) DOMImpl.impl.createElement("img");
  }

  /**
   * Creates an &lt;input type='image'&gt; element.
   * 
   * @return the newly created element
   */
  public final InputElement createImageInputElement() {
    return DOMImpl.impl.createInputElement("image");
  }

  /**
   * Creates an &lt;ins&gt; element.
   * 
   * @return the newly created element
   */
  public final ModElement createInsElement() {
    return (ModElement) DOMImpl.impl.createElement("ins");
  }

  /**
   * Creates a &lt;label&gt; element.
   * 
   * @return the newly created element
   */
  public final LabelElement createLabelElement() {
    return (LabelElement) DOMImpl.impl.createElement("label");
  }

  /**
   * Creates a &lt;legend&gt; element.
   * 
   * @return the newly created element
   */
  public final LegendElement createLegendElement() {
    return (LegendElement) DOMImpl.impl.createElement("legend");
  }

  /**
   * Creates a &lt;li&gt; element.
   * 
   * @return the newly created element
   */
  public final LIElement createLIElement() {
    return (LIElement) DOMImpl.impl.createElement("li");
  }

  /**
   * Creates a &lt;link&gt; element.
   * 
   * @return the newly created element
   */
  public final LinkElement createLinkElement() {
    return (LinkElement) DOMImpl.impl.createElement("link");
  }

  /**
   * Creates a &lt;map&gt; element.
   * 
   * @return the newly created element
   */
  public final MapElement createMapElement() {
    return (MapElement) DOMImpl.impl.createElement("map");
  }

  /**
   * Creates a &lt;meta&gt; element.
   * 
   * @return the newly created element
   */
  public final MetaElement createMetaElement() {
    return (MetaElement) DOMImpl.impl.createElement("meta");
  }

  /**
   * Creates a &lt;object&gt; element.
   * 
   * @return the newly created element
   */
  public final ObjectElement createObjectElement() {
    return (ObjectElement) DOMImpl.impl.createElement("object");
  }

  /**
   * Creates an &lt;ol&gt; element.
   * 
   * @return the newly created element
   */
  public final OListElement createOLElement() {
    return (OListElement) DOMImpl.impl.createElement("ol");
  }

  /**
   * Creates an &lt;optgroup&gt; element.
   * 
   * @return the newly created element
   */
  public final OptGroupElement createOptGroupElement() {
    return (OptGroupElement) DOMImpl.impl.createElement("optgroup");
  }

  /**
   * Creates an &lt;option&gt; element.
   * 
   * @return the newly created element
   */
  public final OptionElement createOptionElement() {
    return (OptionElement) DOMImpl.impl.createElement("option");
  }

  /**
   * Creates a &lt;param&gt; element.
   * 
   * @return the newly created element
   */
  public final ParamElement createParamElement() {
    return (ParamElement) DOMImpl.impl.createElement("param");
  }

  /**
   * Creates an &lt;input type='password'&gt; element.
   * 
   * @return the newly created element
   */
  public final InputElement createPasswordInputElement() {
    return DOMImpl.impl.createInputElement("password");
  }

  /**
   * Creates a &lt;p&gt; element.
   * 
   * @return the newly created element
   */
  public final ParagraphElement createPElement() {
    return (ParagraphElement) DOMImpl.impl.createElement("p");
  }

  /**
   * Creates a &lt;pre&gt; element.
   * 
   * @return the newly created element
   */
  public final PreElement createPreElement() {
    return (PreElement) DOMImpl.impl.createElement("pre");
  }

  /**
   * Creates a &lt;q&gt; element.
   * 
   * @return the newly created element
   */
  public final QuoteElement createQElement() {
    return (QuoteElement) DOMImpl.impl.createElement("q");
  }

  /**
   * Creates an &lt;input type='radio'&gt; element.
   * 
   * @param name the name of the radio input (used for grouping)
   * @return the newly created element
   */
  public final InputElement createRadioInputElement(String name) {
    return DOMImpl.impl.createInputRadioElement(name);
  }

  /**
   * Creates a &lt;script&gt; element.
   * 
   * @return the newly created element
   */
  public final ScriptElement createScriptElement() {
    return (ScriptElement) DOMImpl.impl.createElement("script");
  }

  /**
   * Creates a &lt;select&gt; element.
   * 
   * @return the newly created element
   */
  public final SelectElement createSelectElement() {
    return DOMImpl.impl.createSelectElement(false);
  }

  /**
   * Creates a &lt;select&gt; element.
   * 
   * @param multiple <code>true</code> to allow multiple-selection
   * @return the newly created element
   */
  public final SelectElement createSelectElement(boolean multiple) {
    return DOMImpl.impl.createSelectElement(multiple);
  }

  /**
   * Creates a &lt;span&gt; element.
   * 
   * @return the newly created element
   */
  public final SpanElement createSpanElement() {
    return (SpanElement) DOMImpl.impl.createElement("span");
  }

  /**
   * Creates a &lt;style&gt; element.
   * 
   * @return the newly created element
   */
  public final StyleElement createStyleElement() {
    return (StyleElement) DOMImpl.impl.createElement("style");
  }

  /**
   * Creates a &lt;table&gt; element.
   * 
   * @return the newly created element
   */
  public final TableElement createTableElement() {
    return (TableElement) DOMImpl.impl.createElement("table");
  }

  /**
   * Creates a &lt;tbody&gt; element.
   * 
   * @return the newly created element
   */
  public final TableSectionElement createTBodyElement() {
    return (TableSectionElement) DOMImpl.impl.createElement("tbody");
  }

  /**
   * Creates a &lt;td&gt; element.
   * 
   * @return the newly created element
   */
  public final TableCellElement createTDElement() {
    return (TableCellElement) DOMImpl.impl.createElement("td");
  }

  /**
   * Creates a &lt;textarea&gt; element.
   * 
   * @return the newly created element
   */
  public final TextAreaElement createTextAreaElement() {
    return (TextAreaElement) DOMImpl.impl.createElement("textarea");
  }

  /**
   * Creates an &lt;input type='text'&gt; element.
   * 
   * @return the newly created element
   */
  public final InputElement createTextInputElement() {
    return DOMImpl.impl.createInputElement("text");
  }

  /**
   * Creates a text node.
   * 
   * @param data the text node's initial text
   * @return the newly created element
   */
  public final native Text createTextNode(String data) /*-{
    return this.createTextNode(data);
  }-*/;

  /**
   * Creates a &lt;tfoot&gt; element.
   * 
   * @return the newly created element
   */
  public final TableSectionElement createTFootElement() {
    return (TableSectionElement) DOMImpl.impl.createElement("tfoot");
  }

  /**
   * Creates a &lt;thead&gt; element.
   * 
   * @return the newly created element
   */
  public final TableSectionElement createTHeadElement() {
    return (TableSectionElement) DOMImpl.impl.createElement("thead");
  }

  /**
   * Creates a &lt;th&gt; element.
   * 
   * @return the newly created element
   */
  public final TableCellElement createTHElement() {
    return (TableCellElement) DOMImpl.impl.createElement("th");
  }

  /**
   * Creates a &lt;title&gt; element.
   * 
   * @return the newly created element
   */
  public final TitleElement createTitleElement() {
    return (TitleElement) DOMImpl.impl.createElement("title");
  }

  /**
   * Creates a &lt;tr&gt; element.
   * 
   * @return the newly created element
   */
  public final TableRowElement createTRElement() {
    return (TableRowElement) DOMImpl.impl.createElement("tr");
  }

  /**
   * Creates a &lt;ul&gt; element.
   * 
   * @return the newly created element
   */
  public final UListElement createULElement() {
    return (UListElement) DOMImpl.impl.createElement("ul");
  }

  /**
   * Creates an identifier guaranteed to be unique within this document.
   * 
   * This is useful for allocating element id's.
   * 
   * @return a unique identifier
   */
  public final String createUniqueId() {
    return "uid-" + (++curUidIndex);
  }

  /**
   * The element that contains the content for the document. In documents with
   * BODY contents, returns the BODY element.
   * 
   * @return the document's body
   */
  public final native BodyElement getBody() /*-{
    return this.body;
  }-*/;

  /**
   * The domain name of the server that served the document, or null if the
   * server cannot be identified by a domain name.
   * 
   * @return the document's domain, or <code>null</code> if none exists
   */
  public final native String getDomain() /*-{
    return this.domain;
  }-*/;

  /**
   * Returns the {@link Element} whose id is given by elementId. If no such
   * element exists, returns null. Behavior is not defined if more than one
   * element has this id.
   * 
   * @param elementId the unique id value for an element
   * @return the matching element
   */
  public final native Element getElementById(String elementId) /*-{
    return this.getElementById(elementId);
  }-*/;

  /**
   * Returns a {@link NodeList} of all the {@link Element Elements} with a given
   * tag name in the order in which they are encountered in a preorder traversal
   * of the document tree.
   * 
   * @param tagName the name of the tag to match on (the special value
   *          <code>"*"</code> matches all tags)
   * @return a list containing all the matched elements
   */
  public final native NodeList<Element> getElementsByTagName(String tagName) /*-{
    return this.getElementsByTagName(tagName);
  }-*/;

  /**
   * Returns the URI of the page that linked to this page. The value is an empty
   * string if the user navigated to the page directly (not through a link, but,
   * for example, via a bookmark).
   * 
   * @return the referrer URI
   */
  public final native String getReferrer() /*-{
    return this.referrer;
  }-*/;

  /**
   * Gets the title of a document as specified by the TITLE element in the head
   * of the document.
   * 
   * @return the document's title
   */
  public final native String getTitle() /*-{
    return this.title;
  }-*/;

  /**
   * Gets the absolute URI of this document.
   * 
   * @return the document URI
   */
  public final native String getURL() /*-{
    return this.URL;
  }-*/;

  /**
   * Imports a node from another document to this document.
   * 
   * The returned node has no parent; ({@link Node#getParentNode()} is null).
   * The source node is not altered or removed from the original document; this
   * method creates a new copy of the source node.
   * 
   * For all nodes, importing a node creates a node object owned by the
   * importing document, with attribute values identical to the source node's
   * nodeName and nodeType, plus the attributes related to namespaces (prefix,
   * localName, and namespaceURI). As in the cloneNode operation on a Node, the
   * source node is not altered. Additional information is copied as appropriate
   * to the nodeType, attempting to mirror the behavior expected if a fragment
   * of XML or HTML source was copied from one document to another, recognizing
   * that the two documents may have different DTDs in the XML case.
   * 
   * @param node the node to import
   * @param deep If <code>true</code>, recursively import the subtree under
   *          the specified node; if <code>false</code>, import only the node
   *          itself, as explained above
   */
  public final native void importNode(Node node, boolean deep) /*-{
    this.importNode(node, deep);
  }-*/;

  /**
   * Sets the title of a document as specified by the TITLE element in the head
   * of the document.
   * 
   * @param title the document's new title
   */
  public final native void setTitle(String title) /*-{
    this.title = title;
  }-*/;
}
