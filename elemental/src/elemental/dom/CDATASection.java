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
  * <p>A CDATA Section can be used within XML to include extended portions of unescaped text, such that the symbols &lt; and &amp; do not need escaping as they normally do within XML when used as text.</p>
<p>It takes the form:</p>
<pre class="eval">&lt;![CDATA[  ... ]]&gt;
</pre>
<p>For example:</p>
<pre class="eval">&lt;foo&gt;Here is a CDATA section: &lt;![CDATA[  &lt; &gt; &amp; ]]&gt; with all kinds of unescaped text. &lt;/foo&gt;
</pre>
<p>The only sequence which is not allowed within a CDATA section is the closing sequence of a CDATA section itself:</p>
<pre class="eval">&lt;![CDATA[  ]]&gt; will cause an error   ]]&gt;
</pre>
<p>Note that CDATA sections should not be used (without hiding) within HTML.</p>
<p>As a CDATASection has no properties or methods unique to itself and only directly implements the Text interface, one can refer to <a title="En/DOM/Text" rel="internal" href="https://developer.mozilla.org/En/DOM/Text">Text</a> to find its properties and methods.</p>
  */
public interface CDATASection extends Text {
}
