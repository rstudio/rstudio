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
package elemental.dom;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * <p>A comment is used to add notations within markup; although it is generally not displayed, it is still available to be read in the source view (in Firefox:&nbsp;View -&gt; Page Source).&nbsp; These are represented in HTML and XML as content between <code>&lt;!--</code> and&nbsp; <code>--&gt; . </code>In XML, the character sequence "--" cannot be used within a comment.</p>
<p>A comment has no special properties or methods of its own, but inherits those of <a title="En/DOM/CharacterData" rel="internal" href="https://developer.mozilla.org/En/DOM/CharacterData">CharacterData</a> (which inherits from <a title="en/DOM/Node" rel="internal" href="https://developer.mozilla.org/en/DOM/Node">Node</a>).</p>
  */
public interface Comment extends CharacterData {
}
