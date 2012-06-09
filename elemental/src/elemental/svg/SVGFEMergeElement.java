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
  * The feMerge filter allows filter effects to be applied concurrently instead of sequentially. This is achieved by other filters storing their output via the 
<code><a rel="internal" href="https://developer.mozilla.org/en/SVG/Attribute/result" class="new">result</a></code> attribute and then accessing it in a <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/feMergeNode">&lt;feMergeNode&gt;</a></code>
 child.
  */
public interface SVGFEMergeElement extends SVGElement, SVGFilterPrimitiveStandardAttributes {
}
