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
import elemental.dom.Node;
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
  * <code>DOM select</code> elements share all of the properties and methods of other HTML elements described in the <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/element">element</a></code>
 section. They also have the specialized interface <a class="external" title="http://dev.w3.org/html5/spec/the-button-element.html#htmlselectelement" rel="external" href="http://dev.w3.org/html5/spec/the-button-element.html#htmlselectelement" target="_blank">HTMLSelectElement</a> (or 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML">HTML 4</a></span> <a class="external" title="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-94282980" rel="external" href="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-94282980" target="_blank">HTMLSelectElement</a>).
  */
public interface SelectElement extends Element {


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/select#attr-autofocus">autofocus</a></code>
 HTML attribute, which indicates whether the control should have input focus when the page loads, unless the user overrides it, for example by typing in a different control. Only one form-associated element in a document can have this attribute specified. 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span> 
<span title="(Firefox 4 / Thunderbird 3.3 / SeaMonkey 2.1)
">Requires Gecko 2.0</span>
    */
  boolean isAutofocus();

  void setAutofocus(boolean arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/select#attr-disabled">disabled</a></code>
 HTML attribute, which indicates whether the control is disabled. If it is disabled, it does not accept clicks.
    */
  boolean isDisabled();

  void setDisabled(boolean arg);


  /**
    * The form that this element is associated with. If this element is a descendant of a form element, then this attribute is the ID of that form element. If the element is not a descendant of a form element, then: <ul> <li>
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span> The attribute can be the ID of any form element in the same document.</li> <li>
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML">HTML 4</a></span> The attribute is null.</li> </ul> <strong>Read only.</strong>
    */
  FormElement getForm();


  /**
    * A list of label elements associated with this select element.
    */
  NodeList getLabels();


  /**
    * The number of <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/option">&lt;option&gt;</a></code>
 elements in this <code>select</code> element.
    */
  int getLength();

  void setLength(int arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/select#attr-multiple">multiple</a></code>
 HTML attribute, whichindicates whether multiple items can be selected.
    */
  boolean isMultiple();

  void setMultiple(boolean arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/select#attr-name">name</a></code>
 HTML attribute, containing the name of this control used by servers and DOM search functions.
    */
  String getName();

  void setName(String arg);


  /**
    * The set of <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/option">&lt;option&gt;</a></code>
 elements contained by this element. <strong>Read only.</strong>
    */
  HTMLOptionsCollection getOptions();


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/select#attr-required">required</a></code>
 HTML attribute, which indicates whether the user is required to select a value before submitting the form. 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span> 
<span title="(Firefox 4 / Thunderbird 3.3 / SeaMonkey 2.1)
">Requires Gecko 2.0</span>
    */
  boolean isRequired();

  void setRequired(boolean arg);


  /**
    * The index of the first selected <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/option">&lt;option&gt;</a></code>
 element.
    */
  int getSelectedIndex();

  void setSelectedIndex(int arg);


  /**
    * The set of options that are selected. 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span>
    */
  HTMLCollection getSelectedOptions();


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/select#attr-size">size</a></code>
 HTML attribute, which contains the number of visible items in the control. The default is 1, 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span> unless <strong>multiple</strong> is true, in which case it is 4.
    */
  int getSize();

  void setSize(int arg);


  /**
    * The form control's type. When <strong>multiple</strong> is true, it returns <code>select-multiple</code>; otherwise, it returns <code>select-one</code>.<strong>Read only.</strong>
    */
  String getType();


  /**
    * A localized message that describes the validation constraints that the control does not satisfy (if any). This attribute is the empty string if the control is not a candidate for constraint validation (<strong>willValidate</strong> is false), or it satisfies its constraints.<strong>Read only.</strong> 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span> 
<span title="(Firefox 4 / Thunderbird 3.3 / SeaMonkey 2.1)
">Requires Gecko 2.0</span>
    */
  String getValidationMessage();


  /**
    * The validity states that this control is in. <strong>Read only.</strong> 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span> 
<span title="(Firefox 4 / Thunderbird 3.3 / SeaMonkey 2.1)
">Requires Gecko 2.0</span>
    */
  ValidityState getValidity();


  /**
    * The value of this form control, that is, of the first selected option.
    */
  String getValue();

  void setValue(String arg);


  /**
    * Indicates whether the button is a candidate for constraint validation. It is false if any conditions bar it from constraint validation. <strong>Read only.</strong> 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span> 
<span title="(Firefox 4 / Thunderbird 3.3 / SeaMonkey 2.1)
">Requires Gecko 2.0</span>
    */
  boolean isWillValidate();


  /**
    * <p>Adds an element to the collection of <code>option</code> elements for this <code>select</code> element.</p>

<div id="section_6"><span id="Parameters"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>element</code></dt> <dd>An item to add to the collection of options.</dd> <dt><code>before</code> 
<span title="(Firefox 7.0 / Thunderbird 7.0 / SeaMonkey 2.4)
">Optional from Gecko 7.0</span>
</dt> <dd>An item (or 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span>&nbsp;index of an item) that the new item should be inserted before. If this parameter is <code>null</code> (or the index does not exist), the new element is appended to the end of the collection.</dd>
</dl>
<div id="section_7"><span id="Examples"></span><h5 class="editable">Examples</h5>
<div id="section_8"><span id="Creating_Elements_from_Scratch"></span><h6 class="editable">Creating Elements from Scratch</h6>

          <pre name="code" class="js">var sel = document.createElement("select");
var opt1 = document.createElement("option");
var opt2 = document.createElement("option");

opt1.value = "1";
opt1.text = "Option: Value 1";

opt2.value = "2";
opt2.text = "Option: Value 2";

sel.add(opt1, null);
sel.add(opt2, null);

/*
  Produces the following, conceptually:

  &lt;select&gt;
    &lt;option value="1"&gt;Option: Value 1&lt;/option&gt;
    &lt;option value="2"&gt;Option: Value 2&lt;/option&gt;
  &lt;/select&gt;
*&#47;</pre>
        
<p>From 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span> and <span title="(Firefox 7.0 / Thunderbird 7.0 / SeaMonkey 2.4)
">Gecko&nbsp;7.0</span> the before parameter is optional. So the following is accepted.</p>
<pre class="deki-transform">...
sel.add(opt1);
sel.add(opt2);
...
</pre>
</div><div id="section_9"><span id="Append_to_an_Existing_Collection"></span><h6 class="editable">Append to an Existing Collection</h6>

          <pre name="code" class="js">var sel = document.getElementById("existingList");

var opt = document.createElement("option");
opt.value = "3";
opt.text = "Option: Value 3";

sel.add(opt, null);

/*
  Takes the existing following select object:

  &lt;select id="existingList" name="existingList"&gt;
    &lt;option value="1"&gt;Option: Value 1&lt;/option&gt;
    &lt;option value="2"&gt;Option: Value 2&lt;/option&gt;
  &lt;/select&gt;

  And changes it to:

  &lt;select id="existingList" name="existingList"&gt;
    &lt;option value="1"&gt;Option: Value 1&lt;/option&gt;
    &lt;option value="2"&gt;Option: Value 2&lt;/option&gt;
    &lt;option value="3"&gt;Option: Value 3&lt;/option&gt;
  &lt;/select&gt;
*&#47;</pre>
        
<p>From 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span> and <span title="(Firefox 7.0 / Thunderbird 7.0 / SeaMonkey 2.4)
">Gecko&nbsp;7.0</span> the before parameter is optional. So the following is accepted.</p>
<pre class="deki-transform">...
sel.add(opt);
...
</pre>
</div><div id="section_10"><span id="Inserting_to_an_Existing_Collection"></span><h6 class="editable">Inserting to an Existing Collection</h6>

          <pre name="code" class="js">var sel = document.getElementById("existingList");

var opt = document.createElement("option");
opt.value = "3";
opt.text = "Option: Value 3";

sel.add(opt, sel.options[1]);

/*
  Takes the existing following select object:

  &lt;select id="existingList" name="existingList"&gt;
    &lt;option value="1"&gt;Option: Value 1&lt;/option&gt;
    &lt;option value="2"&gt;Option: Value 2&lt;/option&gt;
  &lt;/select&gt;

  And changes it to:

  &lt;select id="existingList" name="existingList"&gt;
    &lt;option value="1"&gt;Option: Value 1&lt;/option&gt;
    &lt;option value="3"&gt;Option: Value 3&lt;/option&gt;
    &lt;option value="2"&gt;Option: Value 2&lt;/option&gt;
  &lt;/select&gt;
*&#47;</pre>
        
<dl> <dt></dt>
</dl>
<p>

</p><div><span>Obsolete since Gecko 5.0 (Firefox 5.0 / Thunderbird 5.0 / SeaMonkey 2.2)
</span><span id="blur()"></span></div></div></div></div>
    */
  void add(Element element, Element before);


  /**
    * <div id="section_11"><p><span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span> Checks whether the element has any constraints and whether it satisfies them. If the element fails its constraints, the browser fires a cancelable <code>invalid</code> event at the element (and returns false).</p>

</div><div id="section_12"><span id="Parameters_3"></span>

</div><div id="section_13"><span id="Return_value"></span><h6 class="editable">Return value</h6>
<p>A <code>false</code> value if the <code>select</code> element is a candidate for constraint evaluation and it does not satisfy its constraints. Returns true if the element is not constrained, or if it satisfies its constraints.</p>
<p>

</p><div><span>Obsolete since Gecko 5.0 (Firefox 5.0 / Thunderbird 5.0 / SeaMonkey 2.2)
</span><span id="focus()"></span></div></div>
    */
  boolean checkValidity();


  /**
    * <div id="section_14"><p><span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span> Gets an item from the options collection for this <code>select</code> element. You can also access an item by specifying the index in array-style brackets or parentheses, without calling this method explicitly.</p>

</div><div id="section_15"><span id="Parameters_5"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>index</code></dt> <dd>The zero-based index into the collection of the option to get.</dd>
</dl>
</div><div id="section_16"><span id="Return_value_2"></span><h6 class="editable">Return value</h6>
<p>The node at the specified index, or <code>null</code> if such a node does not exist in the collection.</p>
<p>
</p><div>
<span id="namedItem()"></span></div></div>
    */
  Node item(int index);


  /**
    * <div id="section_16"><p><span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span> Gets the item in the options collection with the specified name. The name string can match either the <strong>id</strong> or the <strong>name</strong> attribute of an option node. You can also access an item by specifying the name in array-style brackets or parentheses, without calling this method explicitly.</p>

</div><div id="section_17"><span id="Parameters_6"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>name</code></dt> <dd>The name of the option to get.</dd>
</dl>
</div><div id="section_18"><span id="Return_value_3"></span><h6 class="editable">Return value</h6>
<ul> <li>A node, if there is exactly one match.</li> <li><code>null</code> if there are no matches.</li> <li>A <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/NodeList">NodeList</a></code>
 in tree order of nodes whose <strong>name</strong> or <strong>id</strong> attributes match the specified name.</li>
</ul>
</div>
    */
  Node namedItem(String name);


  /**
    * <p>Removes the element at the specified index from the options collection for this select element.</p>

<div id="section_20"><span id="Parameters_7"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>index</code></dt> <dd>The zero-based index of the option element to remove from the collection.</dd>
</dl>
<div id="section_21"><span id="Example"></span><h5 class="editable">Example</h5>

          <pre name="code" class="js">var sel = document.getElementById("existingList");
sel.remove(1);

/*
  Takes the existing following select object:

  &lt;select id="existingList" name="existingList"&gt;
    &lt;option value="1"&gt;Option: Value 1&lt;/option&gt;
    &lt;option value="2"&gt;Option: Value 2&lt;/option&gt;
    &lt;option value="3"&gt;Option: Value 3&lt;/option&gt;
  &lt;/select&gt;

  And changes it to:

  &lt;select id="existingList" name="existingList"&gt;
    &lt;option value="1"&gt;Option: Value 1&lt;/option&gt;
    &lt;option value="3"&gt;Option: Value 3&lt;/option&gt;
  &lt;/select&gt;
*&#47;</pre>
        
<p>
</p><div>
<span id="setCustomValidity()"></span></div></div></div>
    */
  void remove(int index);


  /**
    * <p>Removes the element at the specified index from the options collection for this select element.</p>

<div id="section_20"><span id="Parameters_7"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>index</code></dt> <dd>The zero-based index of the option element to remove from the collection.</dd>
</dl>
<div id="section_21"><span id="Example"></span><h5 class="editable">Example</h5>

          <pre name="code" class="js">var sel = document.getElementById("existingList");
sel.remove(1);

/*
  Takes the existing following select object:

  &lt;select id="existingList" name="existingList"&gt;
    &lt;option value="1"&gt;Option: Value 1&lt;/option&gt;
    &lt;option value="2"&gt;Option: Value 2&lt;/option&gt;
    &lt;option value="3"&gt;Option: Value 3&lt;/option&gt;
  &lt;/select&gt;

  And changes it to:

  &lt;select id="existingList" name="existingList"&gt;
    &lt;option value="1"&gt;Option: Value 1&lt;/option&gt;
    &lt;option value="3"&gt;Option: Value 3&lt;/option&gt;
  &lt;/select&gt;
*&#47;</pre>
        
<p>
</p><div>
<span id="setCustomValidity()"></span></div></div></div>
    */
  void remove(OptionElement option);


  /**
    * <p><span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span> only. Sets the custom validity message for the selection element to the specified message. Use the empty string to indicate that the element does <em>not</em> have a custom validity error.</p>

<div id="section_22"><span id="Parameters_8"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>error</code></dt> <dd>The string to use for the custom validity message.</dd>
</dl></div>
    */
  void setCustomValidity(String error);
}
