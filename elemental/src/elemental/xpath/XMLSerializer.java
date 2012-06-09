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
package elemental.xpath;
import elemental.dom.Node;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * <div dir="ltr" id="result_box">XMLSerializer can be used to convert DOM subtree or DOM document into text. XMLSerializer is available to unprivileged scripts.</div>
<p><code> </code></p>
<div class="note">
<div dir="ltr">XMLSerializer is mainly useful for applications and extensions based on the Mozilla platform. While it is available for web pages, it's not part of any standard and level of support in other browsers unknown.</div>
<div id="result_box" dir="ltr">&nbsp;</div>
</div>
  */
public interface XMLSerializer {


  /**
    * <div dir="ltr" id="serializeToString" class="">
&nbsp;&nbsp;&nbsp;&nbsp; Returns the serialized subtree of a string.
<dt></dt>
</div>
<div dir="ltr" id="serializeToStream"><strong>serializeToStream </strong></div>
<div dir="ltr">&nbsp;&nbsp;&nbsp;&nbsp; The subtree rooted by the specified element is serialized to a byte stream using the character set specified.&nbsp;&nbsp;&nbsp;&nbsp;</div>
<div dir="ltr" id="Example"><span>Example</span></div>

          <pre name="code" class="js">var s = new XMLSerializer();
 var d = document;
 var str = s.serializeToString(d);
 alert(str);</pre>
        

          <pre name="code" class="js">var s = new XMLSerializer();
 var stream = {
   close : function()
   {
     alert("Stream closed");
   },
   flush : function()
   {
   },
   write : function(string, count)
   {
     alert("'" + string + "'\n bytes count: " + count + "");
   }
 };
 s.serializeToStream(document, stream, "UTF-8");</pre>
    */
  String serializeToString(Node node);
}
