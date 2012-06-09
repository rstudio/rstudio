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
  * <strong>Note:</strong>&nbsp;This page describes the Keygen Element interface as specified, not as currently implemented by Gecko. See <a rel="external" href="https://bugzilla.mozilla.org/show_bug.cgi?id=101019" class="external" title="">
bug 101019</a>
 for details and status.
  */
public interface KeygenElement extends Element {


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/keygen#attr-autofocus">autofocus</a></code>
&nbsp;HTML attribute, indicating that the form control should have input focus when the page loads.
    */
  boolean isAutofocus();

  void setAutofocus(boolean arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/keygen#attr-challenge">challenge</a></code>
 HTML&nbsp;attribute, containing a challenge string that is packaged with the submitted key.
    */
  String getChallenge();

  void setChallenge(String arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/keygen#attr-disabled">disabled</a></code>
&nbsp;HTML attribute, indicating that the control is not available for interaction.
    */
  boolean isDisabled();

  void setDisabled(boolean arg);


  /**
    * Indicates the control's form owner, reflecting the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/keygen#attr-form">form</a></code>
&nbsp;HTML&nbsp;attribute if it is defined.
    */
  FormElement getForm();


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/keygen#attr-keytype">keytype</a></code>
 HTML&nbsp;attribute, containing the type of key used.
    */
  String getKeytype();

  void setKeytype(String arg);


  /**
    * A list of label elements associated with this keygen element.
    */
  NodeList getLabels();


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/keygen#attr-name">name</a></code>
 HTML attribute, containing the name for the control that is submitted with form data.
    */
  String getName();

  void setName(String arg);


  /**
    * Must be the value <code>keygen</code>.
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
    * Always false because <code>keygen</code> objects are never candidates for constraint validation.
    */
  boolean isWillValidate();


  /**
    * Always returns true because <code>keygen</code> objects are never candidates for constraint validation.
    */
  boolean checkValidity();


  /**
    * Sets a custom validity message for the element. If this message is not the empty string, then the element is suffering from a custom validity error, and does not validate.
    */
  void setCustomValidity(String error);
}
