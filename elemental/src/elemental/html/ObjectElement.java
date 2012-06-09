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
import elemental.svg.SVGDocument;
import elemental.dom.Document;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * DOM <code>Object</code> objects expose the <a title="http://dev.w3.org/html5/spec/the-iframe-element.html#htmlobjectelement" class=" external" rel="external nofollow" href="http://dev.w3.org/html5/spec/the-iframe-element.html#htmlobjectelement" target="_blank">HTMLObjectElement</a> (or <span><a rel="custom nofollow" href="https://developer.mozilla.org/en/HTML">HTML 4</a></span> <a class=" external" rel="external nofollow" href="http://www.w3.org/TR/2003/REC-DOM-Level-2-HTML-20030109/html.html#ID-9893177" title="http://www.w3.org/TR/2003/REC-DOM-Level-2-HTML-20030109/html.html#ID-9893177" target="_blank">HTMLObjectElement</a>) interface, which provides special properties and methods (beyond the regular <a rel="internal" href="https://developer.mozilla.org/en/DOM/element">element</a> object interface they also have available to them by inheritance) for manipulating the layout and presentation of Object element, representing external resources.
  */
public interface ObjectElement extends Element {


  /**
    * Alignment of the object relative to its context. 

<span title="">Obsolete</span> in 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span>.
    */
  String getAlign();

  void setAlign(String arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/object#attr-archive">archive</a></code>
&nbsp;HTML attribute, containing a list of archives for resources for this object. 

<span title="">Obsolete</span> in 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span>.
    */
  String getArchive();

  void setArchive(String arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/object#attr-border">border</a></code>
&nbsp;HTML attribute, specifying the width of a border around the object. 

<span title="">Obsolete</span> in 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span>.
    */
  String getBorder();

  void setBorder(String arg);


  /**
    * The name of an applet class file, containing either the applet's subclass, or the path to get to the class, including the class file itself. 

<span title="">Obsolete</span> in 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span>.
    */
  String getCode();

  void setCode(String arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/object#attr-codebase">codebase</a></code>
&nbsp;HTML attribute, specifying the base path to use to resolve relative URIs. 

<span title="">Obsolete</span> in 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span>.
    */
  String getCodeBase();

  void setCodeBase(String arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/object#attr-codetype">codetype</a></code>
&nbsp;HTML attribute, specifying the content type of the data. 

<span title="">Obsolete</span> in 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span>.
    */
  String getCodeType();

  void setCodeType(String arg);


  /**
    * The active document of the object element's nested browsing context, if any; otherwise null.
    */
  Document getContentDocument();


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/object#attr-data">data</a></code>
 HTML&nbsp;attribute, specifying the address of a resource's data.
    */
  String getData();

  void setData(String arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/object#attr-declare">declare</a></code>
 HTML&nbsp;attribute, indicating that this is a declaration, not an instantiation, of the object. 

<span title="">Obsolete</span> in 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span>.
    */
  boolean isDeclare();

  void setDeclare(boolean arg);


  /**
    * The object element's form owner, or null if there isn't one.
    */
  FormElement getForm();


  /**
    * Reflects the {{htmlattrxref("height", "object)}}&nbsp;HTML attribute, specifying the displayed height of the resource in CSS pixels.
    */
  String getHeight();

  void setHeight(String arg);


  /**
    * Horizontal space in pixels around the control. 

<span title="">Obsolete</span> in 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span>.
    */
  int getHspace();

  void setHspace(int arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/object#attr-name">name</a></code>
&nbsp;HTML attribute, specifying the name of the object (
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML">HTML 4</a></span>, or of a browsing context (
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span>.
    */
  String getName();

  void setName(String arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/object#attr-standby">standby</a></code>
 HTML&nbsp;attribute, specifying a message to display while the object loads. 

<span title="">Obsolete</span> in 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span>.
    */
  String getStandby();

  void setStandby(String arg);


  /**
    * Reflects the {{htmlattrxref("type", "object)}}&nbsp;HTML attribute, specifying the MIME type of the resource.
    */
  String getType();

  void setType(String arg);


  /**
    * Reflects the {{htmlattrxref("usemap", "object)}}&nbsp;HTML attribute, specifying a {{HTMLElement("map")}} element to use.
    */
  String getUseMap();

  void setUseMap(String arg);


  /**
    * A localized message that describes the validation constraints that the control does not satisfy (if any). This is the empty string if the control is not a candidate for constraint validation (<strong>willValidate</strong> is false), or it satisfies its constraints.
    */
  String getValidationMessage();


  /**
    * The validity states that this element is in.
    */
  ValidityState getValidity();


  /**
    * Horizontal space in pixels around the control. 

<span title="">Obsolete</span> in 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span>.
    */
  int getVspace();

  void setVspace(int arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/object#attr-width">width</a></code>
&nbsp;HTML attribute, specifying the displayed width of the resource in CSS pixels.
    */
  String getWidth();

  void setWidth(String arg);


  /**
    * Indicates whether the element is a candidate for constraint validation. Always false for <code>object</code> objects.
    */
  boolean isWillValidate();


  /**
    * Always returns true, because <code>object</code> objects are never candidates for constraint validation.
    */
  boolean checkValidity();

  SVGDocument getSVGDocument();


  /**
    * Sets a custom validity message for the element. If this message is not the empty string, then the element is suffering from a custom validity error, and does not validate.
    */
  void setCustomValidity(String error);
}
