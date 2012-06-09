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
  * The <em>HTML Table Cell Element</em> (<code>&lt;td&gt;</code>) defines a cell that content data.
  */
public interface TableCellElement extends Element {


  /**
    * This attribute contains a short abbreviated description of the content of the cell. Some user-agents, such as speech readers, may present this description before the content itself. <div class="note"><strong>Note: </strong>Do not use this attribute as it is obsolete in the latest standard: instead either consider starting the cell content by an independent abbreviated content itself or use the abbreviated content as the cell content and use the long content as the description of the cell by putting it in the <strong>title</strong> attribute.</div>
    */
  String getAbbr();

  void setAbbr(String arg);


  /**
    * This enumerated attribute specifies how horizontal alignment of each cell content will be handled. Possible values are: <ul> <li><span>left</span>, aligning the content to the left of the cell</li> <li><span>center</span>, centering the content in the cell</li> <li><span>right</span>, aligning the content to the right of the cell</li> <li><span>justify</span>, inserting spaces into the textual content so that the content is justified in the cell</li> <li><span>char</span>, aligning the textual content on a special character with a minimal offset, defined by the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/td#attr-char">char</a></code>
 and 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/td#attr-charoff">charoff</a></code>
 attributes 
<span class="unimplementedInlineTemplate">Unimplemented (see<a rel="external" href="https://bugzilla.mozilla.org/show_bug.cgi?id=2212" class="external" title="">
bug 2212</a>
)</span>
.</li> </ul> <p>If this attribute is not set,&nbsp; the <span>left</span> value is assumed.</p> <div class="note"><strong>Note: </strong>Do not use this attribute as it is obsolete (not supported) in the latest standard. <ul> <li>To achieve the same effect as the <span>left</span>, <span>center</span>, <span>right</span> or <span>justify</span> values, use the CSS <code><a rel="custom" href="https://developer.mozilla.org/en/CSS/text-align">text-align</a></code>
 property on it.</li> <li>To achieve the same effect as the <span>char</span> value, in CSS3, you can use the value of the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/td#attr-char">char</a></code>
 as the value of the <code><a rel="custom" href="https://developer.mozilla.org/en/CSS/text-align">text-align</a></code>
 property 
<span class="unimplementedInlineTemplate">Unimplemented</span>
.</li> </ul> </div>
    */
  String getAlign();

  void setAlign(String arg);


  /**
    * This attribute contains a list of space-separated strings. Each string is the ID of a group of cells that this header applies to. <div class="note"><strong>Note: </strong>Do not use this attribute as it is obsolete in the latest standard: instead use the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/td#attr-scope">scope</a></code>
 attribute.</div>
    */
  String getAxis();

  void setAxis(String arg);

  String getBgColor();

  void setBgColor(String arg);

  int getCellIndex();

  String getCh();

  void setCh(String arg);

  String getChOff();

  void setChOff(String arg);

  int getColSpan();

  void setColSpan(int arg);


  /**
    * This attributes a list of space-separated strings, each corresponding to the <strong>id</strong> attribute of the <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/th">&lt;th&gt;</a></code>
 elements that applies to this element.
    */
  String getHeaders();

  void setHeaders(String arg);

  String getHeight();

  void setHeight(String arg);

  boolean isNoWrap();

  void setNoWrap(boolean arg);

  int getRowSpan();

  void setRowSpan(int arg);

  String getScope();

  void setScope(String arg);

  String getVAlign();

  void setVAlign(String arg);

  String getWidth();

  void setWidth(String arg);
}
