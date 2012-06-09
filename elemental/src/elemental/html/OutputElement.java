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
import elemental.dom.DOMSettableTokenList;
import elemental.dom.NodeList;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * 
  */
public interface OutputElement extends Element {


  /**
    * The default value of the element, initially the empty string.
    */
  String getDefaultValue();

  void setDefaultValue(String arg);


  /**
    * Indicates the control's form owner, reflecting the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/output#attr-form">form</a></code>
&nbsp;HTML&nbsp;attribute if it is defined.
    */
  FormElement getForm();


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/output#attr-for">for</a></code>
 HTML attribute, containing a list of IDs of other elements in the same document that contribute to (or otherwise affect) the calculated <strong>value</strong>.
    */
  DOMSettableTokenList getHtmlFor();

  void setHtmlFor(DOMSettableTokenList arg);


  /**
    * A list of label elements associated with this output element.
    */
  NodeList getLabels();


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/output#attr-name">name</a></code>
 HTML attribute, containing the name for the control that is submitted with form data.
    */
  String getName();

  void setName(String arg);


  /**
    * Must be the string <code>output</code>.
    */
  String getType();


  /**
    * A localized message that describes the validation constraints that the control does not satisfy (if any). This is the empty string if the control is not a candidate for constraint validation (<strong>willValidate</strong> is false), or it satisfies its constraints.
    */
  String getValidationMessage();


  /**
    * The validity states that this element is in.
    */
  ValidityState getValidity();


  /**
    * The value of the contents of the elements. Behaves like the <strong><a title="En/DOM/Node.textContent" rel="internal" href="https://developer.mozilla.org/En/DOM/Node.textContent">textContent</a></strong> property.
    */
  String getValue();

  void setValue(String arg);


  /**
    * <p>      in Gecko 2.0. Indicates whether the element is a candidate for constraint validation. It is false if any conditions bar it from constraint validation. (See <a rel="external" href="https://bugzilla.mozilla.org/show_bug.cgi?id=604673" class="external" title="">
bug 604673</a>
.)</p> <p>The standard behavior is to always return false because <code>output</code> objects are never candidates for constraint validation.</p>
    */
  boolean isWillValidate();


  /**
    * <p>      in Gecko 2.0. Returns false if the element is a candidate for constraint validation, and it does not satisfy its constraints. In this case, it also fires an <code>invalid</code> event at the element. It returns true if the element is not a candidate for constraint validation, or if it satisfies its constraints.</p> <p>The standard behavior is to always return true because <code>output</code> objects are never candidates for constraint validation.</p>
    */
  boolean checkValidity();


  /**
    * Sets a custom validity message for the element. If this message is not the empty string, then the element is suffering from a custom validity error, and does not validate.
    */
  void setCustomValidity(String error);
}
