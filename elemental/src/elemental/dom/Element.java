/*
 * Copyright 2012 Google Inc.
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
package elemental.dom;
import elemental.html.HTMLCollection;
import elemental.util.Mappable;
import elemental.events.EventListener;
import elemental.html.ClientRect;
import elemental.css.CSSStyleDeclaration;
import elemental.html.ClientRectList;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * <p>This chapter provides a brief reference for the general methods, properties, and events available to most HTML and XML elements in the Gecko DOM.</p>
<p>Various W3C specifications apply to elements:</p>
<ul> <li><a class="external" rel="external" href="http://www.w3.org/TR/DOM-Level-2-Core/" title="http://www.w3.org/TR/DOM-Level-2-Core/" target="_blank">DOM Core Specification</a>—describes the core interfaces shared by most DOM objects in HTML and XML documents</li> <li><a class="external" rel="external" href="http://www.w3.org/TR/DOM-Level-2-HTML/" title="http://www.w3.org/TR/DOM-Level-2-HTML/" target="_blank">DOM HTML Specification</a>—describes interfaces for objects in HTML and XHTML documents that build on the core specification</li> <li><a class="external" rel="external" href="http://www.w3.org/TR/DOM-Level-2-Events/" title="http://www.w3.org/TR/DOM-Level-2-Events/" target="_blank">DOM Events Specification</a>—describes events shared by most DOM objects, building on the DOM Core and <a class="external" rel="external" href="http://www.w3.org/TR/DOM-Level-2-Views/" title="http://www.w3.org/TR/DOM-Level-2-Views/" target="_blank">Views</a> specifications</li> <li><a class="external" title="http://www.w3.org/TR/ElementTraversal/" rel="external" href="http://www.w3.org/TR/ElementTraversal/" target="_blank">Element Traversal Specification</a>—describes the new attributes that allow traversal of elements in the DOM&nbsp;tree 
<span>New in <a rel="custom" href="https://developer.mozilla.org/en/Firefox_3.5_for_developers">Firefox 3.5</a></span>
</li>
</ul>
<p>The articles listed here span the above and include links to the appropriate W3C DOM specification.</p>
<p>While these interfaces are generally shared by most HTML and XML elements, there are more specialized interfaces for particular objects listed in the DOM HTML Specification. Note, however, that these HTML&nbsp;interfaces are "only for [HTML 4.01] and [XHTML 1.0] documents and are not guaranteed to work with any future version of XHTML." The HTML 5 draft does state it aims for backwards compatibility with these HTML&nbsp;interfaces but says of them that "some features that were formerly deprecated, poorly supported, rarely used or considered unnecessary have been removed." One can avoid the potential conflict by moving entirely to DOM&nbsp;XML attribute methods such as <code>getAttribute()</code>.</p>
<p><code><a rel="custom" href="https://developer.mozilla.org/en/DOM/HTMLHtmlElement">Html</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/HTMLHeadElement">Head</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/HTMLLinkElement">Link</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/HTMLTitleElement">Title</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/HTMLMetaElement">Meta</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/HTMLBaseElement">Base</a></code>
, <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/HTMLIsIndexElement" class="new">IsIndex</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/HTMLStyleElement">Style</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/HTMLBodyElement">Body</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/HTMLFormElement">Form</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/HTMLSelectElement">Select</a></code>
, <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/HTMLOptGroupElement" class="new">OptGroup</a></code>
, <a title="en/HTML/Element/HTMLOptionElement" rel="internal" href="https://developer.mozilla.org/en/HTML/Element/HTMLOptionElement" class="new ">Option</a>, <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/HTMLInputElement">Input</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/HTMLTextAreaElement">TextArea</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/HTMLButtonElement">Button</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/HTMLLabelElement">Label</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/HTMLFieldSetElement">FieldSet</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/HTMLLegendElement">Legend</a></code>
, <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/HTMLUListElement" class="new">UList</a></code>
, <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/OList" class="new">OList</a></code>
, <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/DList" class="new">DList</a></code>
, <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/Directory" class="new">Directory</a></code>
, <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/Menu" class="new">Menu</a></code>
, <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/LI" class="new">LI</a></code>
, <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/Div" class="new">Div</a></code>
, <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/Paragraph" class="new">Paragraph</a></code>
, <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/Heading" class="new">Heading</a></code>
, <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/Quote" class="new">Quote</a></code>
, <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/Pre" class="new">Pre</a></code>
, <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/BR" class="new">BR</a></code>
, <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/BaseFont" class="new">BaseFont</a></code>
, <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/Font" class="new">Font</a></code>
, <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/HR" class="new">HR</a></code>
, <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/Mod" class="new">Mod</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/HTMLAnchorElement">Anchor</a></code>
, <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/Image" class="new">Image</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/HTMLObjectElement">Object</a></code>
, <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/Param" class="new">Param</a></code>
, <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/Applet" class="new">Applet</a></code>
, <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/Map" class="new">Map</a></code>
, <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/Area" class="new">Area</a></code>
, <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/Script" class="new">Script</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/HTMLTableElement">Table</a></code>
, <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/TableCaption" class="new">TableCaption</a></code>
, <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/TableCol" class="new">TableCol</a></code>
, <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/TableSection" class="new">TableSection</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/HTMLTableRowElement">TableRow</a></code>
, <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/TableCell" class="new">TableCell</a></code>
, <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/FrameSet" class="new">FrameSet</a></code>
, <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/Frame" class="new">Frame</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/HTMLIFrameElement">IFrame</a></code>
</p>
  */
public interface Element extends Node, NodeSelector, ElementTraversal {

    static final int ALLOW_KEYBOARD_INPUT = 1;

  String getAccessKey();

  void setAccessKey(String arg);


  /**
    * The number of child nodes that are elements.
    */
  int getChildElementCount();


  /**
    * A live <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/XPCOM_Interface_Reference/nsIDOMNodeList&amp;ident=nsIDOMNodeList" class="new">nsIDOMNodeList</a></code>
 of the current child elements.
    */
  HTMLCollection getChildren();


  /**
    * Token list of class attribute
    */
  DOMTokenList getClassList();


  /**
    * Gets/sets the class of the element.
    */
  String getClassName();

  void setClassName(String arg);


  /**
    * The inner height of an element.
    */
  int getClientHeight();


  /**
    * The width of the left border of an element.
    */
  int getClientLeft();


  /**
    * The width of the top border of an element.
    */
  int getClientTop();


  /**
    * The inner width of an element.
    */
  int getClientWidth();


  /**
    * Gets/sets whether or not the element is editable.
    */
  String getContentEditable();

  void setContentEditable(String arg);


  /**
    * Allows access to read and write custom data attributes on the element.
    */
  Mappable getDataset();


  /**
    * Gets/sets the directionality of the element.
    */
  String getDir();

  void setDir(String arg);

  boolean isDraggable();

  void setDraggable(boolean arg);


  /**
    * The first direct child element of an element, or <code>null</code> if the element has no child elements.
    */
  Element getFirstElementChild();

  boolean isHidden();

  void setHidden(boolean arg);


  /**
    * Gets/sets the id of the element.
    */
  String getId();

  void setId(String arg);


  /**
    * Gets/sets the markup of the element's content.
    */
  String getInnerHTML();

  void setInnerHTML(String arg);

  String getInnerText();

  void setInnerText(String arg);


  /**
    * Indicates whether or not the content of the element can be edited. Read only.
    */
  boolean isContentEditable();


  /**
    * Gets/sets the language of an element's attributes, text, and element contents.
    */
  String getLang();

  void setLang(String arg);


  /**
    * The last direct child element of an element, or <code>null</code> if the element has no child elements.
    */
  Element getLastElementChild();


  /**
    * The element immediately following the given one in the tree, or <code>null</code> if there's no sibling node.
    */
  Element getNextElementSibling();


  /**
    * The height of an element, relative to the layout.
    */
  int getOffsetHeight();


  /**
    * The distance from this element's left border to its <code>offsetParent</code>'s left border.
    */
  int getOffsetLeft();


  /**
    * The element from which all offset calculations are currently computed.
    */
  Element getOffsetParent();


  /**
    * The distance from this element's top border to its <code>offsetParent</code>'s top border.
    */
  int getOffsetTop();


  /**
    * The width of an element, relative to the layout.
    */
  int getOffsetWidth();

  EventListener getOnabort();

  void setOnabort(EventListener arg);

  EventListener getOnbeforecopy();

  void setOnbeforecopy(EventListener arg);

  EventListener getOnbeforecut();

  void setOnbeforecut(EventListener arg);

  EventListener getOnbeforepaste();

  void setOnbeforepaste(EventListener arg);


  /**
    * Returns the event handling code for the blur event.
    */
  EventListener getOnblur();

  void setOnblur(EventListener arg);


  /**
    * Returns the event handling code for the change event.
    */
  EventListener getOnchange();

  void setOnchange(EventListener arg);


  /**
    * Returns the event handling code for the click event.
    */
  EventListener getOnclick();

  void setOnclick(EventListener arg);


  /**
    * Returns the event handling code for the contextmenu event.
    */
  EventListener getOncontextmenu();

  void setOncontextmenu(EventListener arg);


  /**
    * Returns the event handling code for the copy event.
    */
  EventListener getOncopy();

  void setOncopy(EventListener arg);


  /**
    * Returns the event handling code for the cut event.
    */
  EventListener getOncut();

  void setOncut(EventListener arg);


  /**
    * Returns the event handling code for the dblclick event.
    */
  EventListener getOndblclick();

  void setOndblclick(EventListener arg);

  EventListener getOndrag();

  void setOndrag(EventListener arg);

  EventListener getOndragend();

  void setOndragend(EventListener arg);

  EventListener getOndragenter();

  void setOndragenter(EventListener arg);

  EventListener getOndragleave();

  void setOndragleave(EventListener arg);

  EventListener getOndragover();

  void setOndragover(EventListener arg);

  EventListener getOndragstart();

  void setOndragstart(EventListener arg);

  EventListener getOndrop();

  void setOndrop(EventListener arg);

  EventListener getOnerror();

  void setOnerror(EventListener arg);


  /**
    * Returns the event handling code for the focus event.
    */
  EventListener getOnfocus();

  void setOnfocus(EventListener arg);

  EventListener getOninput();

  void setOninput(EventListener arg);

  EventListener getOninvalid();

  void setOninvalid(EventListener arg);


  /**
    * Returns the event handling code for the keydown event.
    */
  EventListener getOnkeydown();

  void setOnkeydown(EventListener arg);


  /**
    * Returns the event handling code for the keypress event.
    */
  EventListener getOnkeypress();

  void setOnkeypress(EventListener arg);


  /**
    * Returns the event handling code for the keyup event.
    */
  EventListener getOnkeyup();

  void setOnkeyup(EventListener arg);

  EventListener getOnload();

  void setOnload(EventListener arg);


  /**
    * Returns the event handling code for the mousedown event.
    */
  EventListener getOnmousedown();

  void setOnmousedown(EventListener arg);


  /**
    * Returns the event handling code for the mousemove event.
    */
  EventListener getOnmousemove();

  void setOnmousemove(EventListener arg);


  /**
    * Returns the event handling code for the mouseout event.
    */
  EventListener getOnmouseout();

  void setOnmouseout(EventListener arg);


  /**
    * Returns the event handling code for the mouseover event.
    */
  EventListener getOnmouseover();

  void setOnmouseover(EventListener arg);


  /**
    * Returns the event handling code for the mouseup event.
    */
  EventListener getOnmouseup();

  void setOnmouseup(EventListener arg);

  EventListener getOnmousewheel();

  void setOnmousewheel(EventListener arg);


  /**
    * Returns the event handling code for the paste event.
    */
  EventListener getOnpaste();

  void setOnpaste(EventListener arg);

  EventListener getOnreset();

  void setOnreset(EventListener arg);


  /**
    * Returns the event handling code for the scroll event.
    */
  EventListener getOnscroll();

  void setOnscroll(EventListener arg);

  EventListener getOnsearch();

  void setOnsearch(EventListener arg);

  EventListener getOnselect();

  void setOnselect(EventListener arg);

  EventListener getOnselectstart();

  void setOnselectstart(EventListener arg);

  EventListener getOnsubmit();

  void setOnsubmit(EventListener arg);

  EventListener getOntouchcancel();

  void setOntouchcancel(EventListener arg);

  EventListener getOntouchend();

  void setOntouchend(EventListener arg);

  EventListener getOntouchmove();

  void setOntouchmove(EventListener arg);

  EventListener getOntouchstart();

  void setOntouchstart(EventListener arg);

  EventListener getOnwebkitfullscreenchange();

  void setOnwebkitfullscreenchange(EventListener arg);

  EventListener getOnwebkitfullscreenerror();

  void setOnwebkitfullscreenerror(EventListener arg);


  /**
    * Gets the markup of the element including its content. When used as a setter, replaces the element with nodes parsed from the given string.
    */
  String getOuterHTML();

  void setOuterHTML(String arg);

  String getOuterText();

  void setOuterText(String arg);


  /**
    * The element immediately preceding the given one in the tree, or <code>null</code> if there is no sibling element.
    */
  Element getPreviousElementSibling();


  /**
    * The scroll view height of an element.
    */
  int getScrollHeight();


  /**
    * Gets/sets the left scroll offset of an element.
    */
  int getScrollLeft();

  void setScrollLeft(int arg);


  /**
    * Gets/sets the top scroll offset of an element.
    */
  int getScrollTop();

  void setScrollTop(int arg);


  /**
    * The scroll view width of an element.
    */
  int getScrollWidth();


  /**
    * Controls <a title="en/Controlling_spell_checking_in_HTML_forms" rel="internal" href="https://developer.mozilla.org/en/HTML/Controlling_spell_checking_in_HTML_forms">spell-checking</a> (present on all HTML&nbsp;elements)
    */
  boolean isSpellcheck();

  void setSpellcheck(boolean arg);


  /**
    * An object representing the declarations of an element's style attributes.
    */
  CSSStyleDeclaration getStyle();


  /**
    * Gets/sets the position of the element in the tabbing order.
    */
  int getTabIndex();

  void setTabIndex(int arg);


  /**
    * The name of the tag for the given element.
    */
  String getTagName();


  /**
    * A string that appears in a popup box when mouse is over the element.
    */
  String getTitle();

  void setTitle(String arg);

  boolean isTranslate();

  void setTranslate(boolean arg);

  String getWebkitRegionOverflow();

  String getWebkitdropzone();

  void setWebkitdropzone(String arg);


  /**
    *  Removes keyboard focus from the current element.
    */
  void blur();


  /**
    *  Gives keyboard focus to the current element.
    */
  void focus();


  /**
    *  Retrieve the value of the named attribute from the current node.
    */
  String getAttribute(String name);


  /**
    *  Retrieve the value of the attribute with the specified name and namespace, from the current node.
    */
  String getAttributeNS(String namespaceURI, String localName);


  /**
    *  Retrieve the node representation of the named attribute from the current node.
    */
  Attr getAttributeNode(String name);


  /**
    *  Retrieve the node representation of the attribute with the specified name and namespace, from the current node.
    */
  Attr getAttributeNodeNS(String namespaceURI, String localName);

  ClientRect getBoundingClientRect();

  ClientRectList getClientRects();

  NodeList getElementsByClassName(String name);


  /**
    *  Retrieve a set of all descendant elements, of a particular tag name, from the current element.
    */
  NodeList getElementsByTagName(String name);


  /**
    *  Retrieve a set of all descendant elements, of a particular tag name and namespace, from the current element.
    */
  NodeList getElementsByTagNameNS(String namespaceURI, String localName);


  /**
    *  Check if the element has the specified attribute, or not.
    */
  boolean hasAttribute(String name);


  /**
    *  Check if the element has the specified attribute, in the specified namespace, or not.
    */
  boolean hasAttributeNS(String namespaceURI, String localName);

  Element querySelector(String selectors);

  NodeList querySelectorAll(String selectors);


  /**
    *  Remove the named attribute from the current node.
    */
  void removeAttribute(String name);


  /**
    *  Remove the attribute with the specified name and namespace, from the current node.
    */
  void removeAttributeNS(String namespaceURI, String localName);


  /**
    *  Remove the node representation of the named attribute from the current node.
    */
  Attr removeAttributeNode(Attr oldAttr);

  void scrollByLines(int lines);

  void scrollByPages(int pages);


  /**
    *  Scrolls the page until the element gets into the view.
    */
  void scrollIntoView();


  /**
    *  Scrolls the page until the element gets into the view.
    */
  void scrollIntoView(boolean alignWithTop);

  void scrollIntoViewIfNeeded();

  void scrollIntoViewIfNeeded(boolean centerIfNeeded);


  /**
    *  Set the value of the named attribute from the current node.
    */
  void setAttribute(String name, String value);


  /**
    *  Set the value of the attribute with the specified name and namespace, from the current node.
    */
  void setAttributeNS(String namespaceURI, String qualifiedName, String value);


  /**
    *  Set the node representation of the named attribute from the current node.
    */
  Attr setAttributeNode(Attr newAttr);


  /**
    *  Set the node representation of the attribute with the specified name and namespace, from the current node.
    */
  Attr setAttributeNodeNS(Attr newAttr);


  /**
    *  Returns whether or not the element would be selected by the specified selector string.
    */
  boolean webkitMatchesSelector(String selectors);


  /**
    *  Asynchronously asks the browser to make the element full-screen.
    */
  void webkitRequestFullScreen(int flags);

  void webkitRequestFullscreen();


  /**
    *  Simulates a click on the current element.
    */
  void click();

  Element insertAdjacentElement(String where, Element element);


  /**
    *  Parses the text as HTML or XML and inserts the resulting nodes into the tree in the position given.
    */
  void insertAdjacentHTML(String where, String html);

  void insertAdjacentText(String where, String text);
}
