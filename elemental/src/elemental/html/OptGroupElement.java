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
  * In a web form, the HTML <em>optgroup</em> element (&lt;optgroup&gt;) creates a grouping of options within a <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/select">&lt;select&gt;</a></code>
 element.
  */
public interface OptGroupElement extends Element {


  /**
    * If this Boolean attribute is set, none of the items in this option group is selectable. Often browsers grey out such control and it won't received any browsing events, like mouse clicks or focus-related ones.
    */
  boolean isDisabled();

  void setDisabled(boolean arg);


  /**
    * The name of the group of options, which the browser can use when labeling the options in the user interface. This attribute is mandatory if this element is used.
    */
  String getLabel();

  void setLabel(String arg);
}
