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
  * <p>The <code>script</code> element is used to embed or reference an executable script within an <abbr>HTML</abbr> or <abbr>XHTML</abbr> document.</p>
<p>Scripts without <code>async</code> or <code>defer</code> attributes are fetched and executed immediately, before the browser continues to parse the page.</p>
  */
public interface ScriptElement extends Element {


  /**
    * Set this Boolean attribute to indicate that the browser should, if possible, execute the script asynchronously. It has no effect on inline scripts (i.e., scripts that don't have the <strong>src</strong> attribute). In older browsers that don't support the <strong>async</strong> attribute, parser-inserted scripts block the parser; script-inserted scripts execute asynchronously in IE and WebKit, but synchronously in Opera and pre-4.0 Firefox. In Firefox 4.0, the <code>async</code> DOM&nbsp;property defaults to <code>true</code> for script-created scripts, so the default behavior matches the behavior of IE&nbsp;and WebKit. To request script-inserted external scripts be executed in the insertion order in browsers where the <code>document.createElement("script").async</code> evaluates to <code>true</code> (such as Firefox 4.0), set <code>.async=false</code> on the scripts you want to maintain order. Never call <code>document.write()</code> from an <code>async</code> script. In Gecko 1.9.2, calling <code>document.write()</code> has an unpredictable effect. In Gecko 2.0, calling <code>document.write()</code> from an <code>async</code> script has no effect (other than printing a warning to the error console).
    */
  boolean isAsync();

  void setAsync(boolean arg);

  String getCharset();

  void setCharset(String arg);

  String getCrossOrigin();

  void setCrossOrigin(String arg);


  /**
    * This Boolean attribute is set to indicate to a browser that the script is meant to be executed after the document has been parsed. Since this feature hasn't yet been implemented by all other major browsers, authors should not assume that the script’s execution will actually be deferred. Never call <code>document.write()</code> from a <code>defer</code> script (since Gecko 1.9.2, this will blow away the document). The <code>defer</code> attribute shouldn't be used on scripts that don't have the <code>src</code> attribute. Since Gecko 1.9.2, the <code>defer</code> attribute is ignored on scripts that don't have the <code>src</code> attribute. However, in Gecko 1.9.1 even inline scripts are deferred if the <code>defer</code> attribute is set.
    */
  boolean isDefer();

  void setDefer(boolean arg);

  String getEvent();

  void setEvent(String arg);

  String getHtmlFor();

  void setHtmlFor(String arg);


  /**
    * This attribute specifies the <abbr>URI</abbr> of an external script; this can be used as an alternative to embedding a script directly within a document. <code>script</code> elements with an <code>src</code> attribute specified should not have a script embedded within its tags.
    */
  String getSrc();

  void setSrc(String arg);

  String getText();

  void setText(String arg);


  /**
    * This attribute identifies the scripting language of code embedded within a <code>script</code> element or referenced via the element’s <code>src</code> attribute. This is specified as a <abbr title="Multi-purpose Internet Mail Extensions">MIME</abbr> type; examples of supported <abbr title="Multi-purpose Internet Mail Extensions">MIME</abbr> types include <code>text/javascript</code>, <code>text/ecmascript</code>, <code>application/javascript</code>, and <code>application/ecmascript</code>. If this attribute is absent, the script is treated as JavaScript.
    */
  String getType();

  void setType(String arg);
}
