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

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * <div>

<a rel="custom" href="http://mxr.mozilla.org/mozilla-central/source/dom/interfaces/base/nsIDOMClientRect.idl"><code>dom/interfaces/base/nsIDOMClientRect.idl</code></a><span><a rel="internal" href="https://developer.mozilla.org/en/Interfaces/About_Scriptable_Interfaces" title="en/Interfaces/About_Scriptable_Interfaces">Scriptable</a></span></div><span>Represents a rectangular box. The type of box is specified by the method that returns such an object. It is returned by functions like <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/element.getBoundingClientRect">element.getBoundingClientRect</a></code>
.</span><div><div>1.0</div><div>11.0</div><div></div><div>Introduced</div><div>Gecko 1.9</div><div title="Introduced in Gecko 1.9 (Firefox 3)
"></div><div title="Last changed in Gecko 1.9.1 (Firefox 3)
"></div></div>
<div>Inherits from: <code><a rel="custom" href="https://developer.mozilla.org/en/XPCOM_Interface_Reference/nsISupports">nsISupports</a></code>
<span>Last changed in Gecko 1.9.1 (Firefox 3.5 / Thunderbird 3.0 / SeaMonkey 2.0)
</span></div>
  */
public interface ClientRect {


  /**
    * Y-coordinate, relative to the viewport origin, of the bottom of the rectangle box. <strong>Read only.</strong>
    */
  float getBottom();


  /**
    * Height of the rectangle box (This is identical to <code>bottom</code> minus <code>top</code>). <strong>Read only.</strong>
    */
  float getHeight();


  /**
    * X-coordinate, relative to the viewport origin, of the left of the rectangle box. <strong>Read only.</strong>
    */
  float getLeft();


  /**
    * X-coordinate, relative to the viewport origin, of the right of the rectangle box. <strong>Read only.</strong>
    */
  float getRight();


  /**
    * Y-coordinate, relative to the viewport origin, of the top of the rectangle box. <strong>Read only.</strong>
    */
  float getTop();


  /**
    * Width of the rectangle box (This is identical to <code>right</code> minus <code>left</code>). <strong>Read only.</strong> 
<span title="(Firefox 3.5 / Thunderbird 3.0 / SeaMonkey 2.0)
">Requires Gecko 1.9.1</span>
    */
  float getWidth();
}
