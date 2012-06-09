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
  * DOM <code>table row</code> objects expose the <code><a class="external" rel="external" href="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-6986576" title="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-6986576" target="_blank">HTMLTableRowElement</a></code> interface, which provides special properties and methods (beyond the regular <a title="en/DOM/element" rel="internal" href="https://developer.mozilla.org/en/DOM/element">element</a> object interface they also have available to them by inheritance) for manipulating the layout and presentation of rows in an HTML table.
  */
public interface TableRowElement extends Element {


  /**
    * <a title="en/DOM/tableRow.bgColor" rel="internal" href="https://developer.mozilla.org/en/DOM/tableRow.bgColor" class="new ">row.bgColor</a> 

<span class="deprecatedInlineTemplate" title="">Deprecated</span>
    */
  String getAlign();

  void setAlign(String arg);


  /**
    * row.cells
    */
  String getBgColor();

  void setBgColor(String arg);


  /**
    * row.ch
    */
  HTMLCollection getCells();


  /**
    * row.chOff
    */
  String getCh();

  void setCh(String arg);


  /**
    * row.rowIndex
    */
  String getChOff();

  void setChOff(String arg);


  /**
    * row.sectionRowIndex
    */
  int getRowIndex();


  /**
    * row.vAlign
    */
  int getSectionRowIndex();

  String getVAlign();

  void setVAlign(String arg);


  /**
    * row.insertCell
    */
  void deleteCell(int index);

  Element insertCell(int index);
}
