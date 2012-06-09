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
  * DOM table column objects (which may correspond to <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/col">&lt;col&gt;</a></code>
&nbsp;or <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/colgroup">&lt;colgroup&gt;</a></code>
 HTML elements) expose the <a target="_blank" href="http://www.w3.org/TR/html5/tabular-data.html#htmltablecolelement" rel="external nofollow" class=" external" title="http://www.w3.org/TR/html5/tabular-data.html#htmltablecolelement">HTMLTableColElement</a> (or <span><a href="https://developer.mozilla.org/en/HTML" rel="custom nofollow">HTML 4</a></span> <a target="_blank" title="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-84150186" href="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-84150186" rel="external nofollow" class=" external"><code>HTMLTableColElement</code></a>) interface, which provides special properties (beyond the regular <a href="https://developer.mozilla.org/en/DOM/element" rel="internal">element</a> object interface they also have available to them by inheritance) for manipulating table column elements.
  */
public interface TableColElement extends Element {


  /**
    * Indicates the horizontal alignment of the cell data in the column.
    */
  String getAlign();

  void setAlign(String arg);


  /**
    * Alignment character for cell data.
    */
  String getCh();

  void setCh(String arg);


  /**
    * Offset for the alignment character.
    */
  String getChOff();

  void setChOff(String arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/col#attr-span">span</a></code>
 HTML&nbsp;attribute, indicating the number of columns to apply this object's attributes to. Must be a positive integer.
    */
  int getSpan();

  void setSpan(int arg);


  /**
    * Indicates the vertical alignment of the cell data in the column.
    */
  String getVAlign();

  void setVAlign(String arg);


  /**
    * Default column width.
    */
  String getWidth();

  void setWidth(String arg);
}
