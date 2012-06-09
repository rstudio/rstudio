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
public interface DirectoryElement extends Element {


  /**
    * This Boolean attribute hints that the list should be rendered in a compact style. The interpretation of this attribute depends on the user agent and it doesn't work in all browsers. <div class="note"><strong>Usage note:&nbsp;</strong>Do not use this attribute, as it has been deprecated: the <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/dir">&lt;dir&gt;</a></code>
 element should be styled using <a title="en/CSS" rel="internal" href="https://developer.mozilla.org/en/CSS">CSS</a>. To give a similar effect than the <span>compact</span> attribute, the <a title="en/CSS" rel="internal" href="https://developer.mozilla.org/en/CSS">CSS</a> property <a title="en/CSS/line-height" rel="internal" href="https://developer.mozilla.org/en/CSS/line-height">line-height</a> can be used with a value of <span>80%</span>.</div>
    */
  boolean isCompact();

  void setCompact(boolean arg);
}
