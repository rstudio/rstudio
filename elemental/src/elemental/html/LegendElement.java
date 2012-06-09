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
  * DOM&nbsp;Legend objects inherit all of the properties and methods of DOM <a href="https://developer.mozilla.org/en/DOM/HTMLElement" title="en/DOM/HTMLElement" rel="internal">HTMLElement</a>, and also expose the <a title="http://www.w3.org/TR/html5/forms.html#htmllegendelement" class=" external" rel="external nofollow" href="http://www.w3.org/TR/html5/forms.html#htmllegendelement" target="_blank">HTMLLegendElement</a> 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span> (or <a class=" external" title="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-21482039" rel="external" href="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-21482039" target="_blank">HTMLLegendElement</a> 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML">HTML 4</a></span>) interface.
  */
public interface LegendElement extends Element {


  /**
    * Alignment relative to the form set. 

<span title="">Obsolete</span> in 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span>, 

<span class="deprecatedInlineTemplate" title="">Deprecated</span>

 in 
<span>HTML 4.01</span>
    */
  String getAlign();

  void setAlign(String arg);


  /**
    * The form that this legend belongs to. If the legend has a fieldset element as its parent, then this attribute returns the same value as the <strong>form</strong> attribute on the parent fieldset element. Otherwise, it returns null.
    */
  FormElement getForm();
}
