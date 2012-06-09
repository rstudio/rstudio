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
package elemental.css;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * An object representing a single CSS style rule. <code>CSSStyleRule</code> implements the <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/CSSRule">CSSRule</a></code>
 interface.
  */
public interface CSSStyleRule extends CSSRule {


  /**
    * Gets/sets the textual representation of the selector for this rule, e.g. <code>"h1,h2"</code>.
    */
  String getSelectorText();

  void setSelectorText(String arg);


  /**
    * Returns the <code><a title="en/DOM/CSSStyleDeclaration" rel="internal" href="https://developer.mozilla.org/en/DOM/CSSStyleDeclaration">CSSStyleDeclaration</a></code> object for the rule. <strong>Read only.</strong>
    */
  CSSStyleDeclaration getStyle();
}
