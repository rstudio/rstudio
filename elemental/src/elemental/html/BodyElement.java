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
package elemental.html;
import elemental.dom.Element;
import elemental.events.EventListener;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * DOM body elements expose the <a href="http://www.w3.org/TR/html5/sections.html#the-body-element" target="_blank" rel="external nofollow" class=" external" title="http://www.w3.org/TR/html5/sections.html#the-body-element">HTMLBodyElement</a> (or 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML">HTML 4</a></span> <a href="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-48250443" target="_blank" title="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-48250443" rel="external nofollow" class=" external"><code>HTMLBodyElement</code></a>) interface, which provides special properties (beyond the regular <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/element">element</a></code>
 object interface they also have available to them by inheritance) for manipulating body elements.
  */
public interface BodyElement extends Element {


  /**
    * Color of active hyperlinks.
    */
  String getALink();

  void setALink(String arg);


  /**
    * <p>URI for a background image resource.</p> <div class="note"><strong>Note:</strong> Starting in Gecko 7.0 (Firefox 7.0 / Thunderbird 7.0 / SeaMonkey 2.4)
, this value is no longer resolved as a URI; instead, it's treated as a simple string.</div>
    */
  String getBackground();

  void setBackground(String arg);


  /**
    * Background color for the document.
    */
  String getBgColor();

  void setBgColor(String arg);


  /**
    * Color of unvisited links.
    */
  String getLink();

  void setLink(String arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/body#attr-onbeforeunload">onbeforeunload</a></code>
 HTML&nbsp;attribute value for a function to call when the document is about to be unloaded.
    */
  EventListener getOnbeforeunload();

  void setOnbeforeunload(EventListener arg);


  /**
    * <p>Exposes the <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/window.onblur">window.onblur</a></code>
 event handler to call when the window loses focus.</p> <div class="note"><strong>Note:</strong> This handler is triggered when the event reaches the window, not the body element. Use <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/element.addEventListener">addEventListener()</a></code>
 to attach an event listener to the body element.</div>
    */
  EventListener getOnblur();

  void setOnblur(EventListener arg);


  /**
    * <p>Exposes the <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/window.onerror">window.onerror</a></code>
 event handler to call when the document fails to load properly.</p> <div class="note"><strong>Note:</strong> This handler is triggered when the event reaches the window, not the body element. Use <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/element.addEventListener">addEventListener()</a></code>
 to attach an event listener to the body element.</div>
    */
  EventListener getOnerror();

  void setOnerror(EventListener arg);


  /**
    * <p>Exposes the <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/window.onfocus">window.onfocus</a></code>
 event handler to call when the window gains focus.</p> <div class="note"><strong>Note:</strong> This handler is triggered when the event reaches the window, not the body element. Use <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/element.addEventListener">addEventListener()</a></code>
 to attach an event listener to the body element.</div>
    */
  EventListener getOnfocus();

  void setOnfocus(EventListener arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/body#attr-onhashchange">onhashchange</a></code>
 HTML&nbsp;attribute value for a function to call when the fragment identifier in the address of the document changes.
    */
  EventListener getOnhashchange();

  void setOnhashchange(EventListener arg);


  /**
    * <p>Exposes the <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/window.onload">window.onload</a></code>
 event handler to call when the window gains focus.</p> <div class="note"><strong>Note:</strong> This handler is triggered when the event reaches the window, not the body element. Use <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/element.addEventListener">addEventListener()</a></code>
 to attach an event listener to the body element.</div>
    */
  EventListener getOnload();

  void setOnload(EventListener arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/body#attr-onmessage">onmessage</a></code>
 HTML&nbsp;attribute value for a function to call when the document receives a message.
    */
  EventListener getOnmessage();

  void setOnmessage(EventListener arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/body#attr-onoffline">onoffline</a></code>
 HTML&nbsp;attribute value for a function to call when network communication fails.
    */
  EventListener getOnoffline();

  void setOnoffline(EventListener arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/body#attr-ononline">ononline</a></code>
 HTML&nbsp;attribute value for a function to call when network communication is restored.
    */
  EventListener getOnonline();

  void setOnonline(EventListener arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/body#attr-onpopstate">onpopstate</a></code>
 HTML&nbsp;attribute value for a function to call when the user has navigated session history.
    */
  EventListener getOnpopstate();

  void setOnpopstate(EventListener arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/body#attr-onresize">onresize</a></code>
 HTML&nbsp;attribute value for a function to call when the document has been resized.
    */
  EventListener getOnresize();

  void setOnresize(EventListener arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/body#attr-onpopstate">onpopstate</a></code>
 HTML&nbsp;attribute value for a function to call when the storage area has changed.
    */
  EventListener getOnstorage();

  void setOnstorage(EventListener arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/body#attr-onunload">onunload</a></code>
 HTML&nbsp;attribute value for a function to call when when the document is going away.
    */
  EventListener getOnunload();

  void setOnunload(EventListener arg);


  /**
    * Foreground color of text.
    */
  String getText();

  void setText(String arg);


  /**
    * Color of visited links.
    */
  String getVLink();

  void setVLink(String arg);
}
