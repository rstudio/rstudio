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
  * Used with the <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/canvas">&lt;canvas&gt;</a></code>
 element. Returned by <a title="en/DOM/CanvasRenderingContext2D" rel="internal" href="https://developer.mozilla.org/en/DOM/CanvasRenderingContext2D">CanvasRenderingContext2D</a>'s <a title="en/DOM/CanvasRenderingContext2D.createImageData" rel="internal" href="https://developer.mozilla.org/en/DOM/CanvasRenderingContext2D.createImageData" class="new ">createImageData</a> and <a title="en/DOM/CanvasRenderingContext2D.getImageData" rel="internal" href="https://developer.mozilla.org/en/DOM/CanvasRenderingContext2D.getImageData" class="new ">getImageData</a> (and accepted as first argument in <a title="en/DOM/CanvasRenderingContext2D.putImageData" rel="internal" href="https://developer.mozilla.org/en/DOM/CanvasRenderingContext2D.putImageData" class="new ">putImageData</a>)
  */
public interface ImageData {

  Uint8ClampedArray getData();

  int getHeight();

  int getWidth();
}
