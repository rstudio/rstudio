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

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * The DOM&nbsp;<code>ValidityState</code> interface represents the <em>validity states</em> that an element can be in, with respect to constraint validation.
  */
public interface ValidityState {


  /**
    * The element's custom validity message has been set to a non-empty string by calling the element's setCustomValidity() method.
    */
  boolean isCustomError();


  /**
    * The value does not match the specified 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/input#attr-pattern">pattern</a></code>
.
    */
  boolean isPatternMismatch();


  /**
    * The value is greater than the specified 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/input#attr-max">max</a></code>
.
    */
  boolean isRangeOverflow();


  /**
    * The value is less than the specified 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/input#attr-min">min</a></code>
.
    */
  boolean isRangeUnderflow();


  /**
    * The value does not fit the rules determined by 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/input#attr-step">step</a></code>
.
    */
  boolean isStepMismatch();


  /**
    * <p>The value exceeds the specified <strong>maxlength</strong> for <a title="en/DOM/HTMLInputElement" rel="internal" href="https://developer.mozilla.org/en/DOM/HTMLInputElement">HTMLInputElement</a> or <a title="en/DOM/textarea" rel="internal" href="https://developer.mozilla.org/en/DOM/HTMLTextAreaElement">HTMLTextAreaElement</a> objects.</p> <div class="note"><strong>Note:</strong> This will never be <code>true</code> in Gecko, because elements' values are prevented from being longer than <strong>maxlength</strong>.</div>
    */
  boolean isTooLong();


  /**
    * The value is not in the required syntax (when 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/input#attr-type">type</a></code>
 is <code>email</code> or <code>url</code>).
    */
  boolean isTypeMismatch();


  /**
    * No other constraint validation conditions are true.
    */
  boolean isValid();


  /**
    * The element has a 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/input#attr-required">required</a></code>
 attribute, but no value.
    */
  boolean isValueMissing();
}
