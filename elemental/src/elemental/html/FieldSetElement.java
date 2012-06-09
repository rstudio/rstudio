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
  * DOM&nbsp;<code>fieldset</code> elements expose the <a class=" external" title="http://dev.w3.org/html5/spec/forms.html#htmlfieldsetelement" rel="external" href="http://dev.w3.org/html5/spec/forms.html#htmlfieldsetelement" target="_blank">HTMLFieldSetElement</a>&nbsp; (
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML">HTML 4</a></span> <a class=" external" title="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-7365882" rel="external" href="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-7365882" target="_blank">HTMLFieldSetElement</a>) interface, which provides special properties and methods (beyond the regular <a rel="internal" href="https://developer.mozilla.org/en/DOM/element">element</a> object interface they also have available to them by inheritance) for manipulating the layout and presentation of field-set elements.
  */
public interface FieldSetElement extends Element {


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/fieldset#attr-disabled">disabled</a></code>
 HTML&nbsp;attribute, indicating whether the user can interact with the control.
    */
  boolean isDisabled();

  void setDisabled(boolean arg);


  /**
    * The elements belonging to this field set.
    */
  HTMLCollection getElements();


  /**
    * The containing form element, if this element is in a form. Otherwise, the element the <a title="en/HTML/Element/fieldset#attr-name" rel="internal" href="https://developer.mozilla.org/en/HTML/Element/fieldset#attr-name">name content attribute</a> points to 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span>. (<code>null</code> in 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML">HTML 4</a></span>.)
    */
  FormElement getForm();


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/fieldset#attr-name">name</a></code>
 HTML&nbsp;attribute, containing the name of the field set, used for submitting the form.
    */
  String getName();

  void setName(String arg);


  /**
    * Must be the string <code>fieldset</code>.
    */
  String getType();


  /**
    * A localized message that describes the validation constraints that the element does not satisfy (if any). This is the empty string if the element&nbsp; is not a candidate for constraint validation (<strong>willValidate</strong> is false), or it satisfies its constraints.
    */
  String getValidationMessage();


  /**
    * The validity states that this element is in.
    */
  ValidityState getValidity();


  /**
    * Always false because <code>fieldset</code> objects are never candidates for constraint validation.
    */
  boolean isWillValidate();


  /**
    * Always returns true because <code>fieldset</code> objects are never candidates for constraint validation.
    */
  boolean checkValidity();


  /**
    * Sets a custom validity message for the field set. If this message is not the empty string, then the field set is suffering from a custom validity error, and does not validate.
    */
  void setCustomValidity(String error);
}
