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

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * <p>DOM&nbsp;<em>option</em> elements elements share all of the properties and methods of other HTML elements described in the <a title="en/DOM/element" rel="internal" href="https://developer.mozilla.org/en/DOM/element">element</a> section. They also have the specialized interface <a title="http://dev.w3.org/html5/spec/the-button-element.html#htmloptionelement" class=" external" rel="external" href="http://dev.w3.org/html5/spec/the-button-element.html#htmloptionelement" target="_blank">HTMLOptionElement</a> (or 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML">HTML 4</a></span> <a class="external" title="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-70901257" rel="external" href="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-70901257" target="_blank">HTMLOptionElement</a>).</p>
<p>No methods are defined on this interface.</p>
  */
public interface OptionElement extends Element {


  /**
    * Reflects the value of the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/option#attr-selected">selected</a></code>
 HTML attribute. which indicates whether the option is selected by default.
    */
  boolean isDefaultSelected();

  void setDefaultSelected(boolean arg);


  /**
    * Reflects the value of the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/option#attr-disabled">disabled</a></code>
 HTML&nbsp;attribute, which indicates that the option is unavailable to be selected. An option can also be disabled if it is a child of an <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/optgroup">&lt;optgroup&gt;</a></code>
 element that is disabled.
    */
  boolean isDisabled();

  void setDisabled(boolean arg);


  /**
    * If the option is a descendent of a <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/select">&lt;select&gt;</a></code>
 element, then this property has the same value as the <code>form</code> property of the corresponding {{DomXref("HTMLSelectElement") object; otherwise, it is null.
    */
  FormElement getForm();


  /**
    * The position of the option within the list of options it belongs to, in tree-order. If the option is not part of a list of options, the value is 0.
    */
  int getIndex();


  /**
    * Reflects the value of the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/option#attr-label">label</a></code>
 HTML attribute, which provides a label for the option. If this attribute isn't specifically set, reading it returns the element's text content.
    */
  String getLabel();

  void setLabel(String arg);


  /**
    * Indicates whether the option is selected.
    */
  boolean isSelected();

  void setSelected(boolean arg);


  /**
    * Contains the text content of the element.
    */
  String getText();

  void setText(String arg);


  /**
    * Reflects the value of the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/option#attr-value">value</a></code>
 HTML attribute, if it exists; otherwise reflects value of the <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/textContent" class="new">textContent</a></code>
&nbsp;IDL&nbsp;attribute.
    */
  String getValue();

  void setValue(String arg);
}
