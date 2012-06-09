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
import elemental.dom.NodeList;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * DOM&nbsp;<code>Button </code>objects expose the <a class=" external" title="http://www.w3.org/TR/html5/the-button-element.html#the-button-element" rel="external" href="http://www.w3.org/TR/html5/the-button-element.html#the-button-element" target="_blank">HTMLButtonElement</a> 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span>&nbsp;(or <a class=" external" title="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-34812697" rel="external" href="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-34812697" target="_blank">HTMLButtonElement</a> 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML">HTML 4</a></span>) interface, which provides properties and methods (beyond the <a href="https://developer.mozilla.org/en/DOM/element" rel="internal">element</a> object interface they also have available to them by inheritance) for manipulating the layout and presentation of button elements.
  */
public interface ButtonElement extends Element {


  /**
    * The control should have input focus when the page loads, unless the user overrides it, for example by typing in a different control. Only one form-associated element in a document can have this attribute specified.
    */
  boolean isAutofocus();

  void setAutofocus(boolean arg);


  /**
    * The control is disabled, meaning that it does not accept any clicks.
    */
  boolean isDisabled();

  void setDisabled(boolean arg);


  /**
    * <p>The form that this button is associated with. If the button is a descendant of a form element, then this attribute is the ID of that form element.</p> <p>If the button is not a descendant of a form element, then:</p> <ul> <li>
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span> The attribute can be the ID of any form element in the same document.</li> <li>
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML">HTML 4</a></span> The attribute is null.</li> </ul>
    */
  FormElement getForm();


  /**
    * The URI&nbsp;of a program that processes information submitted by the button. If specified, this attribute overrides the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/form#attr-action">action</a></code>
 attribute of the <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/form">&lt;form&gt;</a></code>
 element that owns this element.
    */
  String getFormAction();

  void setFormAction(String arg);

  String getFormEnctype();

  void setFormEnctype(String arg);


  /**
    * The HTTP&nbsp;method that the browser uses to submit the form. If specified, this attribute overrides the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/form#attr-method">method</a></code>
 attribute of the <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/form">&lt;form&gt;</a></code>
 element that owns this element.
    */
  String getFormMethod();

  void setFormMethod(String arg);


  /**
    * Indicates that the form is not to be validated when it is submitted. If specified, this attribute overrides the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/form#attr-enctype">enctype</a></code>
 attribute of the <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/form">&lt;form&gt;</a></code>
 element that owns this element.
    */
  boolean isFormNoValidate();

  void setFormNoValidate(boolean arg);


  /**
    * A name or keyword indicating where to display the response that is received after submitting the form. If specified, this attribute overrides the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/form#attr-target">target</a></code>
 attribute of the <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/form">&lt;form&gt;</a></code>
 element that owns this element.
    */
  String getFormTarget();

  void setFormTarget(String arg);


  /**
    * A list of <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/label">&lt;label&gt;</a></code>
 elements that are labels for this button.
    */
  NodeList getLabels();


  /**
    * The name of the object when submitted with a form. 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span> If specified, it must not be the empty string.
    */
  String getName();

  void setName(String arg);


  /**
    * <p>Indicates the behavior of the button. This is an enumerated attribute with the following possible values:</p> <ul> <li><code>submit</code>:&nbsp;The button submits the form. This is the default value if the attribute is not specified, 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span> or if it is dynamically changed to an empty or invalid value.</li> <li><code>reset</code>:&nbsp;The button resets the form.</li> <li><code>button</code>:&nbsp;The button does nothing.</li> </ul>
    */
  String getType();


  /**
    * A localized message that describes the validation constraints that the control does not satisfy (if any). This attribute is the empty string if the control is not a candidate for constraint validation (<strong>willValidate</strong> is false), or it satisfies its constraints.
    */
  String getValidationMessage();


  /**
    * The validity states that this button is in.
    */
  ValidityState getValidity();


  /**
    * The current form control value of the button.&nbsp;
    */
  String getValue();

  void setValue(String arg);


  /**
    * Indicates whether the button is a candidate for constraint validation. It is false if any conditions bar it from constraint validation.
    */
  boolean isWillValidate();


  /**
    * Not supported for button elements.
    */
  boolean checkValidity();


  /**
    * Not supported for button elements.
    */
  void setCustomValidity(String error);
}
