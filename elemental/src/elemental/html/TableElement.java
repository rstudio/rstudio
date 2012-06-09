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
  * <code>table</code> objects expose the <code><a class="external" rel="external" href="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-64060425" title="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-64060425" target="_blank">HTMLTableElement</a></code> interface, which provides special properties and methods (beyond the regular <a rel="internal" href="https://developer.mozilla.org/en/DOM/element" title="en/DOM/element">element</a> object interface they also have available to them by inheritance) for manipulating the layout and presentation of tables in HTML.

  */
public interface TableElement extends Element {


  /**
    * <b>align</b> gets/sets the alignment of the table.

    */
  String getAlign();

  void setAlign(String arg);


  /**
    * <b>bgColor</b> gets/sets the background color of the table.

    */
  String getBgColor();

  void setBgColor(String arg);


  /**
    * <b>border</b> gets/sets the table border.

    */
  String getBorder();

  void setBorder(String arg);


  /**
    * <b>caption</b> returns the table caption.

    */
  TableCaptionElement getCaption();

  void setCaption(TableCaptionElement arg);


  /**
    * <b>cellPadding</b> gets/sets the cell padding.

    */
  String getCellPadding();

  void setCellPadding(String arg);


  /**
    * <b>cellSpacing</b> gets/sets the spacing around the table.

    */
  String getCellSpacing();

  void setCellSpacing(String arg);


  /**
    * <b>frame</b> specifies which sides of the table have borders.

    */
  String getFrame();

  void setFrame(String arg);


  /**
    * <b>rows</b> returns the rows in the table.

    */
  HTMLCollection getRows();


  /**
    * <b>rules</b> specifies which interior borders are visible.

    */
  String getRules();

  void setRules(String arg);


  /**
    * <b>summary</b> gets/sets the table summary.

    */
  String getSummary();

  void setSummary(String arg);


  /**
    * <b>tBodies</b> returns the table bodies.

    */
  HTMLCollection getTBodies();


  /**
    * <b>tFoot</b> returns the table footer.

    */
  TableSectionElement getTFoot();

  void setTFoot(TableSectionElement arg);


  /**
    * <b>tHead</b> returns the table head.

    */
  TableSectionElement getTHead();

  void setTHead(TableSectionElement arg);


  /**
    * <b>width</b> gets/sets the width of the table.

    */
  String getWidth();

  void setWidth(String arg);


  /**
    * <b>createCaption</b> creates a new caption for the table.

    */
  Element createCaption();

  Element createTBody();


  /**
    * <b>createTFoot</b> creates a table footer.

    */
  Element createTFoot();


  /**
    * <b>createTHead</b> creates a table header.

    */
  Element createTHead();


  /**
    * <b>deleteCaption</b> removes the table caption.

    */
  void deleteCaption();


  /**
    * <b>deleteRow</b> removes a row.

    */
  void deleteRow(int index);


  /**
    * <b>deleteTFoot</b> removes a table footer.

    */
  void deleteTFoot();


  /**
    * <b>deleteTHead</b> removes the table header.

    */
  void deleteTHead();


  /**
    * <b>insertRow</b> inserts a new row.

    */
  Element insertRow(int index);
}
