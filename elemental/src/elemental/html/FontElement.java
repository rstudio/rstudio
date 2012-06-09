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
  * Obsolete
  */
public interface FontElement extends Element {


  /**
    * This attribute sets the text color using either a named color or a color specified in the hexadecimal #RRGGBB format.
    */
  String getColor();

  void setColor(String arg);


  /**
    * This attribute contains a comma-sperated list of one or more font names. The document text in the default style is rendered in the first font face that the client's browser supports. If no font listed is installed on the local system, the browser typically defaults to the proportional or fixed-width font for that system.
    */
  String getFace();

  void setFace(String arg);


  /**
    * This attribute specifies the font size as either a numeric or relative value. Numeric values range from <span>1</span> to <span>7</span> with <span>1</span> being the smallest and <span>3</span> the default. It can be defined using a relative value, like <span>+2</span> or <span>-3</span>, which set it relative to the value of the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/basefont#attr-size">size</a></code>
 attribute of the <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/basefont">&lt;basefont&gt;</a></code>
 element, or relative to <span>3</span>, the default value, if none does exist.
    */
  String getSize();

  void setSize(String arg);
}
