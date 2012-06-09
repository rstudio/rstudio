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
import elemental.html.HTMLAllCollection;
import elemental.stylesheets.StyleSheetList;
import elemental.html.CanvasRenderingContext;
import elemental.events.EventTarget;
import elemental.html.HTMLCollection;
import elemental.traversal.NodeFilter;
import elemental.css.CSSStyleDeclaration;
import elemental.events.Touch;
import elemental.xpath.XPathNSResolver;
import elemental.xpath.XPathResult;
import elemental.ranges.Range;
import elemental.html.Location;
import elemental.html.Selection;
import elemental.html.Window;
import elemental.xpath.XPathExpression;
import elemental.traversal.NodeIterator;
import elemental.events.Event;
import elemental.traversal.TreeWalker;
import elemental.html.HeadElement;
import elemental.events.TouchList;
import elemental.events.EventListener;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;
import elemental.svg.*;

import java.util.Date;

/**
  * <p>Each web page loaded in the browser has its own <strong>document</strong> object. This object serves as an entry point to the web page's content (the <a title="en/Using_the_W3C_DOM_Level_1_Core" rel="internal" href="https://developer.mozilla.org/en/Using_the_W3C_DOM_Level_1_Core">DOM tree</a>, including elements such as <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/body">&lt;body&gt;</a></code>
 and <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/table">&lt;table&gt;</a></code>
) and provides functionality global to the document (such as obtaining the page's URL and creating new elements in the document).</p>
<p>A document object can be obtained from various APIs:</p>
<ul> <li>Most commonly, you work with the document the script is running in by using <code>document</code> in document's <a title="en/HTML/Element/Script" rel="internal" href="https://developer.mozilla.org/En/HTML/Element/Script">scripts</a>. (The same document can also be referred to as <a title="window.document" rel="internal" href="https://developer.mozilla.org/en/DOM/window.document"><code>window.document</code></a>.)</li> <li>The document of an iframe via the iframe's <code><a title="en/DOM/HTMLIFrameElement#Properties" rel="internal" href="https://developer.mozilla.org/en/DOM/HTMLIFrameElement#Properties">contentDocument</a></code> property.</li> <li>The <a title="en/XMLHttpRequest#Attributes" rel="internal" href="https://developer.mozilla.org/en/nsIXMLHttpRequest#Attributes"><code>responseXML</code> of an <code>XMLHttpRequest</code> object</a>.</li> <li>The document, that given node or element belongs to, can be retrieved using the node's <code><a title="en/DOM/Node.ownerDocument" rel="internal" href="https://developer.mozilla.org/En/DOM/Node.ownerDocument">ownerDocument</a></code> property.</li> <li>...and more.</li>
</ul>
<p>Depending on the kind of the document (e.g. <a title="en/HTML" rel="internal" href="https://developer.mozilla.org/en/HTML">HTML</a> or <a title="en/XML" rel="internal" href="https://developer.mozilla.org/en/XML">XML</a>) different APIs may be available on the document object. This theoretical availability of APIs is usually described in terms of <em>implementing interfaces</em> defined in the relevant W3C DOM specifications:</p>
<ul> <li>All document objects implement the DOM Core <a class="external" rel="external" href="http://www.w3.org/TR/DOM-Level-2-Core/core.html#i-Document" title="http://www.w3.org/TR/DOM-Level-2-Core/core.html#i-Document" target="_blank"><code>Document</code></a> and <code><a title="en/DOM/Node" rel="internal" href="https://developer.mozilla.org/en/DOM/Node">Node</a></code> interfaces, meaning that the "core" properties and methods are available for all kinds of documents.</li> <li>In addition to the generalized DOM Core document interface, HTML documents also implement the <code><a class="external" rel="external" href="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-26809268" title="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-26809268" target="_blank">HTMLDocument</a></code> interface, which is a more specialized interface for dealing with HTML documents (e.g., <a title="en/DOM/document.cookie" rel="internal" href="https://developer.mozilla.org/en/DOM/document.cookie">document.cookie</a>, <a title="en/DOM/document.alinkColor" rel="internal" href="https://developer.mozilla.org/en/DOM/document.alinkColor">document.alinkColor</a>).</li> <li><a title="en/XUL" rel="internal" href="https://developer.mozilla.org/en/XUL">XUL</a> documents (available to Mozilla add-on and application developers) implement their own additions to the core Document functionality.</li>
</ul>
<p>Methods or properties listed here that are part of a more specialized interface have an asterisk (*) next to them and have additional information in the&nbsp; Availability column.</p>
<p>Note that some APIs listed below are not available in all browsers for various reasons:</p>
<ul> <li><strong>Obsolete</strong>: on its way of being removed from supporting browsers.</li> <li><strong>Non-standard</strong>: either an experimental feature not (yet?) agreed upon by all vendors, or a feature targeted specifically at the code running in a specific browser (e.g. Mozilla has a few DOM APIs created for its add-ons and application development).</li> <li>Part of a completed or an emerging standard, but not (yet?) implemented in all browsers or implemented in the newest versions of the browsers.</li>
</ul>
<p>Detailed browser compatibility tables are located at the pages describing each property or method.</p>
  */
public interface Document extends Node, NodeSelector {

/**
 * Contains the set of standard values used with {@link #createEvent}.
 */
public interface Events {
  public static final String CUSTOM = "CustomEvent";
  public static final String KEYBOARD = "KeyboardEvent";
  public static final String MESSAGE = "MessageEvent";
  public static final String MOUSE = "MouseEvent";
  public static final String MUTATION = "MutationEvent";
  public static final String OVERFLOW = "OverflowEvent";
  public static final String PAGE_TRANSITION = "PageTransitionEvent";
  public static final String PROGRESS = "ProgressEvent";
  public static final String STORAGE = "StorageEvent";
  public static final String TEXT = "TextEvent";
  public static final String UI = "UIEvent";
  public static final String WEBKIT_ANIMATION = "WebKitAnimationEvent";
  public static final String WEBKIT_TRANSITION = "WebKitTransitionEvent";
  public static final String WHEEL = "WheelEvent";
  public static final String SVGS = "SVGEvents";
  public static final String SVG_ZOOMS = "SVGZoomEvents";
  public static final String TOUCH = "TouchEvent";
}

/**
 * Contains the set of standard values returned by {@link #readyState}.
 */
public interface ReadyState {

  /**
   * Indicates the document is still loading and parsing.
   */
  public static final String LOADING = "loading";

  /**
   * Indicates the document is finished parsing but is still loading
   * subresources.
   */
  public static final String INTERACTIVE = "interactive";

  /**
   * Indicates the document and all subresources have been loaded.
   */
  public static final String COMPLETE = "complete";
}


  /**
    * Returns a string containing the URL of the current document.
    */
  String getURL();


  /**
    * Returns the currently focused element
    */
  Element getActiveElement();


  /**
    * Returns or sets the color of active links in the document body.
    */
  String getAlinkColor();

  void setAlinkColor(String arg);

  HTMLAllCollection getAll();

  void setAll(HTMLAllCollection arg);


  /**
    * Returns a list of all of the anchors in the document.
    */
  HTMLCollection getAnchors();


  /**
    * Returns an ordered list of the applets within a document.
    */
  HTMLCollection getApplets();


  /**
    * Gets/sets the background color of the current document.
    */
  String getBgColor();

  void setBgColor(String arg);


  /**
    * Returns the BODY node of the current document.
    */
  Element getBody();

  void setBody(Element arg);


  /**
    * Returns the character set being used by the document.
    */
  String getCharacterSet();

  String getCharset();

  void setCharset(String arg);


  /**
    * Indicates whether the document is rendered in Quirks or Strict mode.
    */
  String getCompatMode();


  /**
    * Returns a semicolon-separated list of the cookies for that document or sets a single cookie.
    */
  String getCookie();

  void setCookie(String arg);

  String getDefaultCharset();


  /**
    * Returns a reference to the window object.
    */
  Window getDefaultView();


  /**
    * Gets/sets WYSYWIG editing capability of <a title="en/Midas" rel="internal" href="https://developer.mozilla.org/en/Midas">Midas</a>. It can only be used for HTML documents.
    */
  String getDesignMode();

  void setDesignMode(String arg);


  /**
    * Gets/sets directionality (rtl/ltr) of the document
    */
  String getDir();

  void setDir(String arg);


  /**
    * Returns the Document Type Definition (DTD) of the current document.
    */
  DocumentType getDoctype();


  /**
    * Returns the Element that is a direct child of document. For HTML documents, this is normally the HTML element.
    */
  Element getDocumentElement();


  /**
    * Returns the document location.
    */
  String getDocumentURI();

  void setDocumentURI(String arg);


  /**
    * Returns the domain of the current document.
    */
  String getDomain();

  void setDomain(String arg);


  /**
    * Returns a list of the embedded OBJECTS within the current document.
    */
  HTMLCollection getEmbeds();


  /**
    * Gets/sets the foreground color, or text color, of the current document.
    */
  String getFgColor();

  void setFgColor(String arg);


  /**
    * Returns a list of the FORM elements within the current document.
    */
  HTMLCollection getForms();


  /**
    * Returns the HEAD node of the current document.
    */
  HeadElement getHead();


  /**
    * Gets/sets the height of the current document.
    */
  int getHeight();


  /**
    * Returns a list of the images in the current document.
    */
  HTMLCollection getImages();


  /**
    * Returns the DOM implementation associated with the current document.
    */
  DOMImplementation getImplementation();


  /**
    * Returns the encoding used when the document was parsed.
    */
  String getInputEncoding();


  /**
    * Returns the date on which the document was last modified.
    */
  String getLastModified();


  /**
    * Gets/sets the color of hyperlinks in the document.
    */
  String getLinkColor();

  void setLinkColor(String arg);


  /**
    * Returns a list of all the hyperlinks in the document.
    */
  HTMLCollection getLinks();


  /**
    * Returns the URI of the current document.
    */
  Location getLocation();

  void setLocation(Location arg);

  EventListener getOnabort();

  void setOnabort(EventListener arg);

  EventListener getOnbeforecopy();

  void setOnbeforecopy(EventListener arg);

  EventListener getOnbeforecut();

  void setOnbeforecut(EventListener arg);

  EventListener getOnbeforepaste();

  void setOnbeforepaste(EventListener arg);

  EventListener getOnblur();

  void setOnblur(EventListener arg);

  EventListener getOnchange();

  void setOnchange(EventListener arg);

  EventListener getOnclick();

  void setOnclick(EventListener arg);

  EventListener getOncontextmenu();

  void setOncontextmenu(EventListener arg);

  EventListener getOncopy();

  void setOncopy(EventListener arg);

  EventListener getOncut();

  void setOncut(EventListener arg);

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

  EventListener getOnfocus();

  void setOnfocus(EventListener arg);

  EventListener getOninput();

  void setOninput(EventListener arg);

  EventListener getOninvalid();

  void setOninvalid(EventListener arg);

  EventListener getOnkeydown();

  void setOnkeydown(EventListener arg);

  EventListener getOnkeypress();

  void setOnkeypress(EventListener arg);

  EventListener getOnkeyup();

  void setOnkeyup(EventListener arg);

  EventListener getOnload();

  void setOnload(EventListener arg);

  EventListener getOnmousedown();

  void setOnmousedown(EventListener arg);

  EventListener getOnmousemove();

  void setOnmousemove(EventListener arg);

  EventListener getOnmouseout();

  void setOnmouseout(EventListener arg);

  EventListener getOnmouseover();

  void setOnmouseover(EventListener arg);

  EventListener getOnmouseup();

  void setOnmouseup(EventListener arg);

  EventListener getOnmousewheel();

  void setOnmousewheel(EventListener arg);

  EventListener getOnpaste();

  void setOnpaste(EventListener arg);


  /**
    * <dl><dd>Returns the event handling code for the <code>readystatechange</code> event.</dd>
</dl>
<div class="geckoVersionNote"> <p>
</p><div class="geckoVersionHeading">Gecko 9.0 note<div>(Firefox 9.0 / Thunderbird 9.0 / SeaMonkey 2.6)
</div></div>
<p></p> <p>Starting in Gecko 9.0 (Firefox 9.0 / Thunderbird 9.0 / SeaMonkey 2.6)
, you can now use the syntax <code>if ("onabort" in document)</code> to determine whether or not a given event handler property exists. This is because event handler interfaces have been updated to be proper web IDL interfaces. See <a title="en/DOM/DOM event handlers" rel="internal" href="https://developer.mozilla.org/en/DOM/DOM_event_handlers">DOM event handlers</a> for details.</p>
</div>
    */
  EventListener getOnreadystatechange();

  void setOnreadystatechange(EventListener arg);

  EventListener getOnreset();

  void setOnreset(EventListener arg);

  EventListener getOnscroll();

  void setOnscroll(EventListener arg);

  EventListener getOnsearch();

  void setOnsearch(EventListener arg);

  EventListener getOnselect();

  void setOnselect(EventListener arg);

  EventListener getOnselectionchange();

  void setOnselectionchange(EventListener arg);

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
    * Returns a list of the available plugins.
    */
  HTMLCollection getPlugins();

  String getPreferredStylesheetSet();


  /**
    * Returns loading status of the document
    */
  String getReadyState();


  /**
    * Returns the URI of the page that linked to this page.
    */
  String getReferrer();


  /**
    * Returns all the <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/script">&lt;script&gt;</a></code>
 elements on the document.
    */
  HTMLCollection getScripts();

  String getSelectedStylesheetSet();

  void setSelectedStylesheetSet(String arg);


  /**
    * Returns a list of the stylesheet objects on the current document.
    */
  StyleSheetList getStyleSheets();


  /**
    * Returns the title of the current document.
    */
  String getTitle();

  void setTitle(String arg);


  /**
    * Gets/sets the color of visited hyperlinks.
    */
  String getVlinkColor();

  void setVlinkColor(String arg);

  Element getWebkitCurrentFullScreenElement();

  boolean isWebkitFullScreenKeyboardInputAllowed();

  Element getWebkitFullscreenElement();

  boolean isWebkitFullscreenEnabled();

  boolean isWebkitHidden();

  boolean isWebkitIsFullScreen();

  String getWebkitVisibilityState();


  /**
    * Returns the width of the current document.
    */
  int getWidth();


  /**
    * Returns the encoding as determined by the XML declaration.<br> <div class="note">Firefox 10 and later don't implement it anymore.</div>
    */
  String getXmlEncoding();


  /**
    * Returns <code>true</code> if the XML declaration specifies the document is standalone (<em>e.g.,</em> An external part of the DTD affects the document's content), else <code>false</code>.
    */
  boolean isXmlStandalone();

  void setXmlStandalone(boolean arg);


  /**
    * Returns the version number as specified in the XML declaration or <code>"1.0"</code> if the declaration is absent.
    */
  String getXmlVersion();

  void setXmlVersion(String arg);


  /**
    * <dd>Adopt node from an external document</dd> <dt><code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Node.appendChild">Node.appendChild</a></code>
</dt> <dd>Adds a node to the end of the list of children of a specified parent node.</dd>
    */
  Node adoptNode(Node source);

  Range caretRangeFromPoint(int x, int y);


  /**
    * Creates a new attribute node and returns it.
    */
  Attr createAttribute(String name);


  /**
    * Creates a new attribute node in a given namespace and returns it.
    */
  Attr createAttributeNS(String namespaceURI, String qualifiedName);


  /**
    * Creates a new CDATA node and returns it.
    */
  CDATASection createCDATASection(String data);


  /**
    * Creates a new comment node and returns it.
    */
  Comment createComment(String data);


  /**
    * Creates a new document fragment.
    */
  DocumentFragment createDocumentFragment();


  /**
    * Creates a new element with the given tag name.
    */
  Element createElement(String tagName);


  /**
    * Creates a new element with the given tag name and namespace URI.
    */
  Element createElementNS(String namespaceURI, String qualifiedName);


  /**
    * Creates a new entity reference object and returns it.
    */
  EntityReference createEntityReference(String name);


  /**
    * Creates an event.
    */
  Event createEvent(String eventType);


  /**
    * Compiles an <code><a title="en/XPathExpression" rel="internal" href="https://developer.mozilla.org/en/XPathExpression">XPathExpression</a></code> which can then be used for (repeated) evaluations.
    */
  XPathExpression createExpression(String expression, XPathNSResolver resolver);


  /**
    * Creates an XPathNSResolver.
    */
  XPathNSResolver createNSResolver(Node nodeResolver);

  NodeIterator createNodeIterator(Node root, int whatToShow, NodeFilter filter, boolean expandEntityReferences);


  /**
    * Creates a new processing instruction element and returns it.
    */
  ProcessingInstruction createProcessingInstruction(String target, String data);


  /**
    * Creates a Range object.
    */
  Range createRange();


  /**
    * Creates a text node.
    */
  Text createTextNode(String data);

  Touch createTouch(Window window, EventTarget target, int identifier, int pageX, int pageY, int screenX, int screenY, int webkitRadiusX, int webkitRadiusY, float webkitRotationAngle, float webkitForce);

  TouchList createTouchList();


  /**
    * Creates a <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/treeWalker">treeWalker</a></code>
 object.
    */
  TreeWalker createTreeWalker(Node root, int whatToShow, NodeFilter filter, boolean expandEntityReferences);


  /**
    * Returns the element visible at the specified coordinates.
    */
  Element elementFromPoint(int x, int y);


  /**
    * Evaluates an XPath expression.
    */
  XPathResult evaluate(String expression, Node contextNode, XPathNSResolver resolver, int type, XPathResult inResult);


  /**
    * Executes a <a title="en/Midas" rel="internal" href="https://developer.mozilla.org/en/Midas">Midas</a> command.
    */
  boolean execCommand(String command, boolean userInterface, String value);

  CanvasRenderingContext getCSSCanvasContext(String contextId, String name, int width, int height);


  /**
    * Returns an object reference to the identified element.
    */
  Element getElementById(String elementId);


  /**
    * Returns a list of elements with the given class name.
    */
  NodeList getElementsByClassName(String tagname);


  /**
    * Returns a list of elements with the given name.
    */
  NodeList getElementsByName(String elementName);


  /**
    * Returns a list of elements with the given tag name.
    */
  NodeList getElementsByTagName(String tagname);


  /**
    * <dd>Returns a list of elements with the given tag name and namespace.</dd> <dt><code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/Node.getFeature" class="new">Node.getFeature</a></code>
</dt>
    */
  NodeList getElementsByTagNameNS(String namespaceURI, String localName);

  CSSStyleDeclaration getOverrideStyle(Element element, String pseudoElement);


  /**
    * <dd>Returns a <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Selection">Selection</a></code>
 object related to text selected in the document.</dd> <dt><code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Node.getUserData">Node.getUserData</a></code>
</dt> <dd>Returns any data previously set on the node via setUserData() by key</dd> <dt><code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Node.hasAttributes">Node.hasAttributes</a></code>
</dt> <dd>Indicates whether the node possesses attributes</dd> <dt><code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Node.hasChildNodes">Node.hasChildNodes</a></code>
</dt> <dd>Returns a Boolean value indicating whether the current element has child nodes or not.</dd>
    */
  Selection getSelection();


  /**
    * <dd>Returns a clone of a node from an external document</dd> <dt><code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Node.insertBefore">Node.insertBefore</a></code>
</dt> <dd>Inserts the specified node before a reference node as a child of the current node.</dd> <dt><code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Node.isDefaultNamespace">Node.isDefaultNamespace</a></code>
</dt> <dd>Returns true if the namespace is the default namespace on the given node</dd> <dt><code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Node.isEqualNode">Node.isEqualNode</a></code>
</dt> <dd>Indicates whether the node is equal to the given node</dd> <dt><code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Node.isSameNode">Node.isSameNode</a></code>
</dt> <dd>Indicates whether the node is the same as the given node</dd> <dt><code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Node.isSupported">Node.isSupported</a></code>
</dt> <dd>Tests whether the DOM implementation implements a specific feature and that feature is supported by this node or document</dd>
    */
  Node importNode(Node importedNode);


  /**
    * <dd>Returns a clone of a node from an external document</dd> <dt><code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Node.insertBefore">Node.insertBefore</a></code>
</dt> <dd>Inserts the specified node before a reference node as a child of the current node.</dd> <dt><code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Node.isDefaultNamespace">Node.isDefaultNamespace</a></code>
</dt> <dd>Returns true if the namespace is the default namespace on the given node</dd> <dt><code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Node.isEqualNode">Node.isEqualNode</a></code>
</dt> <dd>Indicates whether the node is equal to the given node</dd> <dt><code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Node.isSameNode">Node.isSameNode</a></code>
</dt> <dd>Indicates whether the node is the same as the given node</dd> <dt><code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Node.isSupported">Node.isSupported</a></code>
</dt> <dd>Tests whether the DOM implementation implements a specific feature and that feature is supported by this node or document</dd>
    */
  Node importNode(Node importedNode, boolean deep);


  /**
    * Returns true if the <a title="en/Midas" rel="internal" href="https://developer.mozilla.org/en/Midas">Midas</a> command can be executed on the current range.
    */
  boolean queryCommandEnabled(String command);


  /**
    * Returns true if the <a title="en/Midas" rel="internal" href="https://developer.mozilla.org/en/Midas">Midas</a> command is in a indeterminate state on the current range.
    */
  boolean queryCommandIndeterm(String command);


  /**
    * Returns true if the <a title="en/Midas" rel="internal" href="https://developer.mozilla.org/en/Midas">Midas</a> command has been executed on the current range.
    */
  boolean queryCommandState(String command);

  boolean queryCommandSupported(String command);


  /**
    * Returns the current value of the current range for <a title="en/Midas" rel="internal" href="https://developer.mozilla.org/en/Midas">Midas</a> command. As of Firefox 2.0.0.2, queryCommandValue will return an empty string when a command value has not been explicitly set.
    */
  String queryCommandValue(String command);


  /**
    * Returns the first Element node within the document, in document order, that matches the specified selectors.
    */
  Element querySelector(String selectors);


  /**
    * Returns a list of all the Element nodes within the document that match the specified selectors.
    */
  NodeList querySelectorAll(String selectors);

  void webkitCancelFullScreen();

  void webkitExitFullscreen();

  void captureEvents();


  /**
    * <dd>In majority of modern browsers, including recent versions of Firefox and Internet Explorer, this method does nothing.</dd> <dt><code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Node.cloneNode">Node.cloneNode</a></code>
</dt> <dd>Makes a copy of a node or document</dd>
    */
  void clear();


  /**
    * <dd>Closes a document stream for writing.</dd> <dt><code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Node.compareDocumentPosition">Node.compareDocumentPosition</a></code>
</dt> <dd>Compares the position of the current node against another node in any other document.</dd>
    */
  void close();


  /**
    * Returns <code>true</code> if the focus is currently located anywhere inside the specified document.
    */
  boolean hasFocus();


  /**
    * Opens a document stream for writing.
    */
  void open();


  /**
    * <dt><code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Node.removeChild">Node.removeChild</a></code>
</dt> <dd>Removes a child node from the DOM</dd>
    */
  void releaseEvents();


  /**
    * Writes text to a document.
    */
  void write(String text);


  /**
    * Write a line of text to a document.
    */
  void writeln(String text);

  AnchorElement createAnchorElement();

  AppletElement createAppletElement();

  AreaElement createAreaElement();

  AudioElement createAudioElement();

  BRElement createBRElement();

  BaseElement createBaseElement();

  BaseFontElement createBaseFontElement();

  BodyElement createBodyElement();

  ButtonElement createButtonElement();

  CanvasElement createCanvasElement();

  ContentElement createContentElement();

  DListElement createDListElement();

  DetailsElement createDetailsElement();

  DirectoryElement createDirectoryElement();

  DivElement createDivElement();

  EmbedElement createEmbedElement();

  FieldSetElement createFieldSetElement();

  FontElement createFontElement();

  FormElement createFormElement();

  FrameElement createFrameElement();

  FrameSetElement createFrameSetElement();

  HRElement createHRElement();

  HeadElement createHeadElement();

  HeadingElement createHeadingElement();

  HtmlElement createHtmlElement();

  IFrameElement createIFrameElement();

  ImageElement createImageElement();

  InputElement createInputElement();

  KeygenElement createKeygenElement();

  LIElement createLIElement();

  LabelElement createLabelElement();

  LegendElement createLegendElement();

  LinkElement createLinkElement();

  MapElement createMapElement();

  MarqueeElement createMarqueeElement();

  MediaElement createMediaElement();

  MenuElement createMenuElement();

  MetaElement createMetaElement();

  MeterElement createMeterElement();

  ModElement createModElement();

  OListElement createOListElement();

  ObjectElement createObjectElement();

  OptGroupElement createOptGroupElement();

  OptionElement createOptionElement();

  OutputElement createOutputElement();

  ParagraphElement createParagraphElement();

  ParamElement createParamElement();

  PreElement createPreElement();

  ProgressElement createProgressElement();

  QuoteElement createQuoteElement();

  SVGAElement createSVGAElement();

  SVGAltGlyphDefElement createSVGAltGlyphDefElement();

  SVGAltGlyphElement createSVGAltGlyphElement();

  SVGAltGlyphItemElement createSVGAltGlyphItemElement();

  SVGAnimateColorElement createSVGAnimateColorElement();

  SVGAnimateElement createSVGAnimateElement();

  SVGAnimateMotionElement createSVGAnimateMotionElement();

  SVGAnimateTransformElement createSVGAnimateTransformElement();

  SVGAnimationElement createSVGAnimationElement();

  SVGCircleElement createSVGCircleElement();

  SVGClipPathElement createSVGClipPathElement();

  SVGComponentTransferFunctionElement createSVGComponentTransferFunctionElement();

  SVGCursorElement createSVGCursorElement();

  SVGDefsElement createSVGDefsElement();

  SVGDescElement createSVGDescElement();

  SVGEllipseElement createSVGEllipseElement();

  SVGFEBlendElement createSVGFEBlendElement();

  SVGFEColorMatrixElement createSVGFEColorMatrixElement();

  SVGFEComponentTransferElement createSVGFEComponentTransferElement();

  SVGFECompositeElement createSVGFECompositeElement();

  SVGFEConvolveMatrixElement createSVGFEConvolveMatrixElement();

  SVGFEDiffuseLightingElement createSVGFEDiffuseLightingElement();

  SVGFEDisplacementMapElement createSVGFEDisplacementMapElement();

  SVGFEDistantLightElement createSVGFEDistantLightElement();

  SVGFEDropShadowElement createSVGFEDropShadowElement();

  SVGFEFloodElement createSVGFEFloodElement();

  SVGFEFuncAElement createSVGFEFuncAElement();

  SVGFEFuncBElement createSVGFEFuncBElement();

  SVGFEFuncGElement createSVGFEFuncGElement();

  SVGFEFuncRElement createSVGFEFuncRElement();

  SVGFEGaussianBlurElement createSVGFEGaussianBlurElement();

  SVGFEImageElement createSVGFEImageElement();

  SVGFEMergeElement createSVGFEMergeElement();

  SVGFEMergeNodeElement createSVGFEMergeNodeElement();

  SVGFEMorphologyElement createSVGFEMorphologyElement();

  SVGFEOffsetElement createSVGFEOffsetElement();

  SVGFEPointLightElement createSVGFEPointLightElement();

  SVGFESpecularLightingElement createSVGFESpecularLightingElement();

  SVGFESpotLightElement createSVGFESpotLightElement();

  SVGFETileElement createSVGFETileElement();

  SVGFETurbulenceElement createSVGFETurbulenceElement();

  SVGFilterElement createSVGFilterElement();

  SVGFontElement createSVGFontElement();

  SVGFontFaceElement createSVGFontFaceElement();

  SVGFontFaceFormatElement createSVGFontFaceFormatElement();

  SVGFontFaceNameElement createSVGFontFaceNameElement();

  SVGFontFaceSrcElement createSVGFontFaceSrcElement();

  SVGFontFaceUriElement createSVGFontFaceUriElement();

  SVGForeignObjectElement createSVGForeignObjectElement();

  SVGGElement createSVGGElement();

  SVGGlyphElement createSVGGlyphElement();

  SVGGlyphRefElement createSVGGlyphRefElement();

  SVGGradientElement createSVGGradientElement();

  SVGHKernElement createSVGHKernElement();

  SVGImageElement createSVGImageElement();

  SVGLineElement createSVGLineElement();

  SVGLinearGradientElement createSVGLinearGradientElement();

  SVGMPathElement createSVGMPathElement();

  SVGMarkerElement createSVGMarkerElement();

  SVGMaskElement createSVGMaskElement();

  SVGMetadataElement createSVGMetadataElement();

  SVGMissingGlyphElement createSVGMissingGlyphElement();

  SVGPathElement createSVGPathElement();

  SVGPatternElement createSVGPatternElement();

  SVGPolygonElement createSVGPolygonElement();

  SVGPolylineElement createSVGPolylineElement();

  SVGRadialGradientElement createSVGRadialGradientElement();

  SVGRectElement createSVGRectElement();

  SVGSVGElement createSVGElement();

  SVGScriptElement createSVGScriptElement();

  SVGSetElement createSVGSetElement();

  SVGStopElement createSVGStopElement();

  SVGStyleElement createSVGStyleElement();

  SVGSwitchElement createSVGSwitchElement();

  SVGSymbolElement createSVGSymbolElement();

  SVGTRefElement createSVGTRefElement();

  SVGTSpanElement createSVGTSpanElement();

  SVGTextContentElement createSVGTextContentElement();

  SVGTextElement createSVGTextElement();

  SVGTextPathElement createSVGTextPathElement();

  SVGTextPositioningElement createSVGTextPositioningElement();

  SVGTitleElement createSVGTitleElement();

  SVGUseElement createSVGUseElement();

  SVGVKernElement createSVGVKernElement();

  SVGViewElement createSVGViewElement();

  ScriptElement createScriptElement();

  SelectElement createSelectElement();

  ShadowElement createShadowElement();

  SourceElement createSourceElement();

  SpanElement createSpanElement();

  StyleElement createStyleElement();

  TableCaptionElement createTableCaptionElement();

  TableCellElement createTableCellElement();

  TableColElement createTableColElement();

  TableElement createTableElement();

  TableRowElement createTableRowElement();

  TableSectionElement createTableSectionElement();

  TextAreaElement createTextAreaElement();

  TitleElement createTitleElement();

  TrackElement createTrackElement();

  UListElement createUListElement();

  UnknownElement createUnknownElement();

  VideoElement createVideoElement();
}
