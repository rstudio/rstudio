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
  * <div><div>

<a rel="custom" href="http://mxr.mozilla.org/mozilla-central/source/dom/interfaces/xpath/nsIDOMXPathEvaluator.idl"><code>dom/interfaces/xpath/nsIDOMXPathEvaluator.idl</code></a><span><a rel="internal" href="https://developer.mozilla.org/en/Interfaces/About_Scriptable_Interfaces" title="en/Interfaces/About_Scriptable_Interfaces">Scriptable</a></span></div><span>This interface is used to evaluate XPath expressions against a DOM node.</span><div>Inherits from: <code><a rel="custom" href="https://developer.mozilla.org/en/XPCOM_Interface_Reference/nsISupports">nsISupports</a></code>
<span>Last changed in Gecko 1.7 
</span></div></div>
<p></p>
<p>Implemented by: <code>@mozilla.org/dom/xpath-evaluator;1</code>. To create an instance, use:</p>
<pre class="eval">var domXPathEvaluator = Components.classes["@mozilla.org/dom/xpath-evaluator;1"]
                        .createInstance(Components.interfaces.nsIDOMXPathEvaluator);
</pre>
  */
public interface XPathEvaluator {


  /**
    * <p>Creates an <code><a rel="custom" href="https://developer.mozilla.org/en/XPCOM_Interface_Reference/nsIDOMXPathExpression">nsIDOMXPathExpression</a></code>
 which can then be used for (repeated) evaluations.</p>
<div class="geckoVersionNote">
<p>
</p><div class="geckoVersionHeading">Gecko 1.9 note<div>(Firefox 3)
</div></div>
<p></p>
<p>Prior to Gecko 1.9, you could call this method on documents other than the one you planned to run the XPath against; starting with Gecko 1.9, however, you must call it on the same document.</p>
</div>

<div id="section_4"><span id="Parameters"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>expression</code></dt> <dd>A string representing the XPath to be created.</dd> <dt><code>resolver</code></dt> <dd>A name space resolver created by , or a user defined name space resolver. Read more on <a title="en/Introduction to using XPath in JavaScript#Implementing a User Defined Namespace Resolver" rel="internal" href="https://developer.mozilla.org/en/Introduction_to_using_XPath_in_JavaScript#Implementing_a_User_Defined_Namespace_Resolver">Implementing a User Defined Namespace Resolver</a> if you wish to take the latter approach.</dd>
</dl>
</div><div id="section_5"><span id="Return_value"></span><h6 class="editable">Return value</h6>
<p>An XPath expression, as an <code><a rel="custom" href="https://developer.mozilla.org/en/XPCOM_Interface_Reference/nsIDOMXPathExpression">nsIDOMXPathExpression</a></code>
 object.</p>
</div>
    */
  XPathExpression createExpression(String expression, XPathNSResolver resolver);


  /**
    * <p>Creates an <code><a rel="custom" href="https://developer.mozilla.org/en/XPCOM_Interface_Reference/nsIDOMXPathExpression">nsIDOMXPathExpression</a></code>
 which resolves name spaces with respect to the definitions in scope for a specified node. It is used to resolve prefixes within the XPath itself, so that they can be matched with the document. <code>null</code> is common for HTML documents or when no name space prefixes are used.</p>

<div id="section_7"><span id="Parameters_2"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>nodeResolver</code></dt> <dd>The node to be used as a context for name space resolution.</dd>
</dl>
</div><div id="section_8"><span id="Return_value_2"></span><h6 class="editable">Return value</h6>
<p>A name space resolver.</p>
</div>
    */
  XPathNSResolver createNSResolver(Node nodeResolver);


  /**
    * <p>Evaluate the specified XPath expression.</p>

<div id="section_10"><span id="Parameters_3"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>expression</code></dt> <dd>A string representing the XPath to be evaluated.</dd> <dt><code>contextNode</code></dt> <dd>A DOM Node to evaluate the XPath expression against. To evaluate against a whole document, use the <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/document.documentElement">document.documentElement</a></code>
.</dd> <dt><code>resolver</code></dt> <dd>A name space resolver created by , or a user defined name space resolver. Read more on <a title="en/Introduction to using XPath in JavaScript#Implementing a User Defined Namespace Resolver" rel="internal" href="https://developer.mozilla.org/en/Introduction_to_using_XPath_in_JavaScript#Implementing_a_User_Defined_Namespace_Resolver">Implementing a User Defined Namespace Resolver</a> if you wish to take the latter approach.</dd> <dt><code>type</code></dt> <dd>A number that corresponds to one of the type constants of <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/XPCOM_Interface_Reference/nsIXPathResult&amp;ident=nsIXPathResult" class="new">nsIXPathResult</a></code>
.</dd> <dt><code>result</code></dt> <dd>An existing <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/XPCOM_Interface_Reference/nsIXPathResult&amp;ident=nsIXPathResult" class="new">nsIXPathResult</a></code>
 to use for the result. Using <code>null</code> will create a new <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/XPCOM_Interface_Reference/nsIXPathResult&amp;ident=nsIXPathResult" class="new">nsIXPathResult</a></code>
.</dd>
</dl>
</div><div id="section_11"><span id="Return_value_3"></span><h6 class="editable">Return value</h6>
<p>An XPath result.</p>
</div>
    */
  XPathResult evaluate(String expression, Node contextNode, XPathNSResolver resolver, int type, XPathResult inResult);
}
