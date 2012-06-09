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
package elemental.svg;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * <p>The <code>SVGNumber</code> interface correspond to the <a title="https://developer.mozilla.org/en/SVG/Content_type#Number" rel="internal" href="https://developer.mozilla.org/en/SVG/Content_type#Number">&lt;number&gt;</a> basic data type.</p>
<p>An <code>SVGNumber</code> object can be designated as read only, which means that attempts to modify the object will result in an exception being thrown.</p>
  */
public interface SVGNumber {


  /**
    * <p>The value of the given attribute.</p> <p><strong>Exceptions on setting:</strong> a <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/DOMException">DOMException</a></code>
 with code <code>NO_MODIFICATION_ALLOWED_ERR</code> is Raised on an attempt to change the value of a read only attribute.</p>
    */
  double getValue();

  void setValue(double arg);
}
