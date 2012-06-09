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
import elemental.dom.NodeList;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * DOM <code>Input</code> objects expose the <a title="http://dev.w3.org/html5/spec/the-input-element.html#htmlinputelement" class=" external" rel="external" href="http://dev.w3.org/html5/spec/the-input-element.html#htmlinputelement" target="_blank">HTMLInputElement</a> (or 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML">HTML 4</a></span> <a class=" external" rel="external" href="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-6043025" title="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-6043025" target="_blank"><code>HTMLInputElement</code></a>) interface, which provides special properties and methods (beyond the regular <a rel="internal" href="https://developer.mozilla.org/en/DOM/element">element</a> object interface they also have available to them by inheritance) for manipulating the layout and presentation of input elements.
  */
public interface InputElement extends Element {


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/input#attr-accept">accept</a></code>
 HTML&nbsp;attribute, containing comma-separated list of file types accepted by the server when <strong>type</strong> is <code>file</code>.
    */
  String getAccept();

  void setAccept(String arg);


  /**
    * Alignment of the element.

<span title="">Obsolete</span>&nbsp;in 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span>
    */
  String getAlign();

  void setAlign(String arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/input#attr-alt">alt</a></code>
&nbsp;HTML attribute, containing alternative text to use when <strong>type</strong> is <code>image.</code>
    */
  String getAlt();

  void setAlt(String arg);


  /**
    * Reflects the {{htmlattrxref("autocomplete", "input)}} HTML attribute, indicating whether the value of the control can be automatically completed by the browser. Ignored if the value of the <strong>type</strong> attribute is <span>hidden</span>, <span>checkbox</span>, <span>radio</span>, <span>file</span>, or a button type (<span>button</span>, <span>submit</span>, <span>reset</span>, <span>image</span>). Possible values are: <ul> <li><span>off</span>: The user must explicitly enter a value into this field for every use, or the document provides its own auto-completion method; the browser does not automatically complete the entry.</li> <li><span>on</span>: The browser can automatically complete the value based on values that the user has entered during previous uses.</li> </ul>
    */
  String getAutocomplete();

  void setAutocomplete(String arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/input#attr-autofocus">autofocus</a></code>
 HTML&nbsp;attribute, which specifies that a form control should have input focus when the page loads, unless the user overrides it, for example by typing in a different control. Only one form element in a document can have the <strong>autofocus</strong> attribute. It cannot be applied if the <strong>type</strong> attribute is set to <code>hidden</code> (that is, you cannot automatically set focus to a hidden control).
    */
  boolean isAutofocus();

  void setAutofocus(boolean arg);


  /**
    * The current state of the element when <strong>type</strong> is <code>checkbox</code> or <code>radio</code>.
    */
  boolean isChecked();

  void setChecked(boolean arg);


  /**
    * The default state of a radio button or checkbox as originally specified in HTML that created this object.
    */
  boolean isDefaultChecked();

  void setDefaultChecked(boolean arg);


  /**
    * The default value as originally specified in HTML that created this object.
    */
  String getDefaultValue();

  void setDefaultValue(String arg);

  String getDirName();

  void setDirName(String arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/input#attr-disabled">disabled</a></code>
 HTML attribute, indicating that the control is not available for interaction.
    */
  boolean isDisabled();

  void setDisabled(boolean arg);


  /**
    * A list of selected files.
    */
  FileList getFiles();

  void setFiles(FileList arg);


  /**
    * <p>The containing form element, if this element is in a form. If this element is not contained in a form element:</p> <ul> <li>
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span> this can be the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/form#attr-id">id</a></code>
 attribute of any <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/form">&lt;form&gt;</a></code>
 element in the same document. Even if the attribute is set on <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/input">&lt;input&gt;</a></code>
, this property will be <code>null</code>, if it isn't the id of a <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/form">&lt;form&gt;</a></code>
 element.</li> <li>
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML">HTML 4</a></span> this must be <code>null</code>.</li> </ul>
    */
  FormElement getForm();


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/input#attr-formaction">formaction</a></code>
 HTML attribute, containing the URI&nbsp;of a program that processes information submitted by the element. If specified, this attribute overrides the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/form#attr-action">action</a></code>
 attribute of the <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/form">&lt;form&gt;</a></code>
 element that owns this element.
    */
  String getFormAction();

  void setFormAction(String arg);

  String getFormEnctype();

  void setFormEnctype(String arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/input#attr-formmethod">formmethod</a></code>
&nbsp;HTML&nbsp;attribute, containing the HTTP&nbsp;method that the browser uses to submit the form. If specified, this attribute overrides the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/form#attr-method">method</a></code>
 attribute of the <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/form">&lt;form&gt;</a></code>
 element that owns this element.
    */
  String getFormMethod();

  void setFormMethod(String arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/input#attr-formnovalidate">formnovalidate</a></code>
&nbsp;HTML&nbsp;attribute, indicating that the form is not to be validated when it is submitted. If specified, this attribute overrides the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/form#attr-novalidate">novalidate</a></code>
 attribute of the <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/form">&lt;form&gt;</a></code>
 element that owns this element.
    */
  boolean isFormNoValidate();

  void setFormNoValidate(boolean arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/input#attr-formtarget">formtarget</a></code>
 HTML&nbsp;attribute, containing a name or keyword indicating where to display the response that is received after submitting the form. If specified, this attribute overrides the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/form#attr-target">target</a></code>
 attribute of the <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/form">&lt;form&gt;</a></code>
 element that owns this element.
    */
  String getFormTarget();

  void setFormTarget(String arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/input#attr-height">height</a></code>
 HTML attribute, which defines the height of the image displayed for the button, if the value of <strong>type</strong> is <span>image</span>.
    */
  int getHeight();

  void setHeight(int arg);

  boolean isIncremental();

  void setIncremental(boolean arg);


  /**
    * Indicates that a checkbox is neither on nor off.
    */
  boolean isIndeterminate();

  void setIndeterminate(boolean arg);


  /**
    * A list of <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/label">&lt;label&gt;</a></code>
 elements that are labels for this element.
    */
  NodeList getLabels();


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/input#attr-max">max</a></code>
 HTML&nbsp;attribute, containing the maximum (numeric or date-time) value for this item, which must not be less than its minimum (<strong>min</strong> attribute) value.
    */
  String getMax();

  void setMax(String arg);


  /**
    * <p>Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/input#attr-maxlength">maxlength</a></code>
&nbsp;HTML attribute, containing the maximum length of text (in Unicode code points) that the value can be changed to. The constraint is evaluated only when the value is changed</p> <div class="note"><strong>Note:</strong> If you set <code>maxlength</code> to a negative value programmatically, an exception will be thrown.</div>
    */
  int getMaxLength();

  void setMaxLength(int arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/input#attr-min">min</a></code>
 HTML&nbsp;attribute, containing the minimum (numeric or date-time) value for this item, which must not be greater than its maximum (<strong>max</strong> attribute) value.
    */
  String getMin();

  void setMin(String arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/input#attr-multiple">multiple</a></code>
 HTML&nbsp;attribute, indicating whether more than one value is possible (e.g., multiple files).
    */
  boolean isMultiple();

  void setMultiple(boolean arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/input#attr-name">name</a></code>
 HTML&nbsp;attribute, containing a name that identifies the element when submitting the form.
    */
  String getName();

  void setName(String arg);

  EventListener getOnwebkitspeechchange();

  void setOnwebkitspeechchange(EventListener arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/input#attr-pattern">pattern</a></code>
 HTML&nbsp;attribute, containing a regular expression that the control's value is checked against. The pattern must match the entire value, not just some subset. Use the <strong>title</strong> attribute to describe the pattern to help the user. This attribute applies when the value of the <strong>type</strong> attribute is <span>text</span>, <span>search</span>, <span>tel</span>, <span>url</span> or <span>email</span>; otherwise it is ignored.
    */
  String getPattern();

  void setPattern(String arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/input#attr-placeholder">placeholder</a></code>
 HTML&nbsp;attribute, containing a hint to the user of what can be entered in the control. The placeholder text must not contain carriage returns or line-feeds. This attribute applies when the value of the <strong>type</strong> attribute is <span>text</span>, <span>search</span>, <span>tel</span>, <span>url</span> or <span>email</span>; otherwise it is ignored.
    */
  String getPlaceholder();

  void setPlaceholder(String arg);


  /**
    * <p>Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/input#attr-readonly">readonly</a></code>
 HTML&nbsp;attribute, indicating that the user cannot modify the value of the control.</p> <p><span><a href="https://developer.mozilla.org/en/HTML/HTML5" rel="custom nofollow">HTML 5</a></span> This is ignored if the value of the <strong>type</strong> attribute is <span>hidden</span>, <span>range</span>, <span>color</span>, <span>checkbox</span>, <span>radio</span>, <span>file</span>, or a button type.</p>
    */
  boolean isReadOnly();

  void setReadOnly(boolean arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/input#attr-required">required</a></code>
 HTML&nbsp;attribute, indicating that the user must fill in a value before submitting a form.
    */
  boolean isRequired();

  void setRequired(boolean arg);


  /**
    * The direction in which selection occurred. This is "forward" if selection was performed in the start-to-end direction of the current locale, or "backward" for the opposite direction. This can also be "none"&nbsp;if the direction is unknown."
    */
  String getSelectionDirection();

  void setSelectionDirection(String arg);


  /**
    * The index of the end of selected text.
    */
  int getSelectionEnd();

  void setSelectionEnd(int arg);


  /**
    * The index of the beginning of selected text.
    */
  int getSelectionStart();

  void setSelectionStart(int arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/input#attr-size">size</a></code>
 HTML&nbsp;attribute, containing size of the control. This value is in pixels unless the value of <strong>type</strong> is <span>text</span> or <span>password</span>, in which case, it is an integer number of characters. 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span> Applies only when <strong>type</strong> is set to <span>text</span>, <span>search</span>, <span>tel</span>, <span>url</span>, <span>email</span>, or <span>password</span>; otherwise it is ignored.
    */
  int getSize();

  void setSize(int arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/input#attr-src">src</a></code>
 HTML&nbsp;attribute, which specifies a URI for the location of an image to display on the graphical submit button, if the value of <strong>type</strong> is <span>image</span>; otherwise it is ignored.
    */
  String getSrc();

  void setSrc(String arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/input#attr-step">step</a></code>
 HTML&nbsp;attribute, which works with<strong> min</strong> and <strong>max</strong> to limit the increments at which a numeric or date-time value can be set. It can be the string <span>any</span> or a positive floating point number. If this is not set to <span>any</span>, the control accepts only values at multiples of the step value greater than the minimum.
    */
  String getStep();

  void setStep(String arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/input#attr-type">type</a></code>
 HTML&nbsp;attribute, indicating the type of control to display. See 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/input#attr-type">type</a></code>
 attribute of <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/input">&lt;input&gt;</a></code>
 for possible values.
    */
  String getType();

  void setType(String arg);


  /**
    * A client-side image map. 

<span title="">Obsolete</span> in 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span>
    */
  String getUseMap();

  void setUseMap(String arg);


  /**
    * A localized message that describes the validation constraints that the control does not satisfy (if any). This is the empty string if the control is not a candidate for constraint validation (<strong>willValidate</strong> is false), or it satisfies its constraints.
    */
  String getValidationMessage();


  /**
    * The validity states that this element is in.&nbsp;
    */
  ValidityState getValidity();


  /**
    * Current value in the control.
    */
  String getValue();

  void setValue(String arg);


  /**
    * The value of the element, interpreted as a date, or <code>null</code> if conversion is not possible.
    */
  Date getValueAsDate();

  void setValueAsDate(Date arg);


  /**
    * <p>The value of the element, interpreted as one of the following in order:</p> <ol> <li>a time value</li> <li>a number</li> <li><code>null</code> if conversion is not possible</li> </ol>
    */
  double getValueAsNumber();

  void setValueAsNumber(double arg);

  boolean isWebkitGrammar();

  void setWebkitGrammar(boolean arg);

  boolean isWebkitSpeech();

  void setWebkitSpeech(boolean arg);

  boolean isWebkitdirectory();

  void setWebkitdirectory(boolean arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/input#attr-width">width</a></code>
 HTML&nbsp;attribute, which defines the width of the image displayed for the button, if the value of <strong>type</strong> is <span>image</span>.
    */
  int getWidth();

  void setWidth(int arg);


  /**
    * Indicates whether the element is a candidate for constraint validation. It is false if any conditions bar it from constraint validation.
    */
  boolean isWillValidate();


  /**
    * Returns false if the element is a candidate for constraint validation, and it does not satisfy its constraints. In this case, it also fires an <code>invalid</code> event at the element. It returns true if the element is not a candidate for constraint validation, or if it satisfies its constraints.
    */
  boolean checkValidity();


  /**
    * Selects the input text in the element, and focuses it so the user can subsequently replace the whole entry.
    */
  void select();


  /**
    * Sets a custom validity message for the element. If this message is not the empty string, then the element is suffering from a custom validity error, and does not validate.
    */
  void setCustomValidity(String error);


  /**
    * Selects a range of text in the element (but does not focus it). The optional <code>selectionDirection</code> parameter may be "forward" or "backward" to establish the direction in which selection was set, or "none"if the direction is unknown or not relevant. The default is "none". Specifying <code>selectionDirection</code> sets the value of the <code>selectionDirection</code> property.
    */
  void setSelectionRange(int start, int end);


  /**
    * Selects a range of text in the element (but does not focus it). The optional <code>selectionDirection</code> parameter may be "forward" or "backward" to establish the direction in which selection was set, or "none"if the direction is unknown or not relevant. The default is "none". Specifying <code>selectionDirection</code> sets the value of the <code>selectionDirection</code> property.
    */
  void setSelectionRange(int start, int end, String direction);


  /**
    * <p>Decrements the <strong>value</strong> by (<strong>step</strong> * <code>n</code>), where <code>n</code> defaults to 1 if not specified. Throws an INVALID_STATE_ERR exception:</p> <ul> <li>if the method is not applicable to for the current <strong>type</strong> value.</li> <li>if the element has no step value.</li> <li>if the <strong>value</strong> cannot be converted to a number.</li> <li>if the resulting value is above the <strong>max</strong> or below the <strong>min</strong>.&nbsp;</li> </ul>
    */
  void stepDown();


  /**
    * <p>Decrements the <strong>value</strong> by (<strong>step</strong> * <code>n</code>), where <code>n</code> defaults to 1 if not specified. Throws an INVALID_STATE_ERR exception:</p> <ul> <li>if the method is not applicable to for the current <strong>type</strong> value.</li> <li>if the element has no step value.</li> <li>if the <strong>value</strong> cannot be converted to a number.</li> <li>if the resulting value is above the <strong>max</strong> or below the <strong>min</strong>.&nbsp;</li> </ul>
    */
  void stepDown(int n);


  /**
    * <p>Increments the <strong>value</strong> by (<strong>step</strong> * <code>n</code>), where <code>n</code> defaults to 1 if not specified. Throws an INVALID_STATE_ERR exception:</p> <ul> <li>if the method is not applicable to for the current <strong>type</strong> value.</li> <li>if the element has no step value.</li> <li>if the <strong>value</strong> cannot be converted to a number.</li> <li>if the resulting value is above the <strong>max</strong> or below the <strong>min</strong>.</li> </ul>
    */
  void stepUp();


  /**
    * <p>Increments the <strong>value</strong> by (<strong>step</strong> * <code>n</code>), where <code>n</code> defaults to 1 if not specified. Throws an INVALID_STATE_ERR exception:</p> <ul> <li>if the method is not applicable to for the current <strong>type</strong> value.</li> <li>if the element has no step value.</li> <li>if the <strong>value</strong> cannot be converted to a number.</li> <li>if the resulting value is above the <strong>max</strong> or below the <strong>min</strong>.</li> </ul>
    */
  void stepUp(int n);
}
