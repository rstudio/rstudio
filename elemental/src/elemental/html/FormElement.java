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
  * <p><code>FORM</code> elements share all of the properties and methods of other HTML elements described in the <a title="en/DOM/element" rel="internal" href="https://developer.mozilla.org/en/DOM/element">element</a> section.</p>
<p>This interface provides methods to create and modify <code>FORM</code> elements using the DOM.</p>
  */
public interface FormElement extends Element {


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/form#attr-accept-charset">accept-charset</a></code>
&nbsp;HTML&nbsp;attribute, containing a list of character encodings that the server accepts.
    */
  String getAcceptCharset();

  void setAcceptCharset(String arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/form#attr-action">action</a></code>
&nbsp;HTML&nbsp;attribute, containing the URI&nbsp;of a program that processes the information submitted by the form.
    */
  String getAction();

  void setAction(String arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/form#attr-autocomplete">autocomplete</a></code>
 HTML&nbsp;attribute, containing a string that indicates whether the controls in this form can have their values automatically populated by the browser.
    */
  String getAutocomplete();

  void setAutocomplete(String arg);


  /**
    * All the form controls belonging to this form element.
    */
  HTMLCollection getElements();


  /**
    * Synonym for <strong>enctype</strong>.
    */
  String getEncoding();

  void setEncoding(String arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/form#attr-enctype">enctype</a></code>
&nbsp;HTML&nbsp;attribute, indicating the type of content that is used to transmit the form to the server. Only specified values can be set.
    */
  String getEnctype();

  void setEnctype(String arg);


  /**
    * The number of controls in the form.
    */
  int getLength();


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/form#attr-method">method</a></code>
&nbsp;HTML&nbsp;attribute, indicating the HTTP&nbsp;method used to submit the form. Only specified values can be set.
    */
  String getMethod();

  void setMethod(String arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/form#attr-name">name</a></code>
&nbsp;HTML&nbsp;attribute, containing the name of the form.
    */
  String getName();

  void setName(String arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/form#attr-novalidate">novalidate</a></code>
 HTML attribute, indicating that the form should not be validated.
    */
  boolean isNoValidate();

  void setNoValidate(boolean arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/form#attr-target">target</a></code>
 HTML attribute, indicating where to display the results received from submitting the form.
    */
  String getTarget();

  void setTarget(String arg);

  boolean checkValidity();


  /**
    * Resets the forms to its initial state.
    */
  void reset();


  /**
    * Submits the form to the server.
    */
  void submit();
}
