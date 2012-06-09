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
import elemental.stylesheets.StyleSheet;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * <p>An object implementing the <code>CSSStyleSheet</code> interface represents a single <a title="en/CSS" rel="internal" href="https://developer.mozilla.org/en/CSS">CSS</a> style sheet.</p>
<p>A CSS style sheet consists of CSS rules, each of which can be manipulated through an object that corresponds to that rule and that implements the <code><a title="en/DOM/cssRule" rel="internal" href="https://developer.mozilla.org/en/DOM/cssRule">CSSRule</a></code> interface. The <code>CSSStyleSheet</code> itself lets you examine and modify its corresponding style sheet, including its list of rules.</p>
<p>In practice, every <code>CSSStyleSheet</code> also implements the more generic <code><a title="en/DOM/StyleSheet" rel="internal" href="https://developer.mozilla.org/en/DOM/stylesheet">StyleSheet</a></code> interface. A list of <code>CSSStyleSheet</code>-implementing objects corresponding to the style sheets for a given document can be reached by the <code><a title="en/DOM/document.styleSheets" rel="internal" href="https://developer.mozilla.org/en/DOM/document.styleSheets">document.styleSheets</a></code> property, if the document is styled by an external CSS style sheet or an inline <code><a title="en/HTML/element/style" rel="internal" href="https://developer.mozilla.org/en/HTML/Element/style">style</a></code> element.</p>
  */
public interface CSSStyleSheet extends StyleSheet {


  /**
    * Returns a <code><a title="en/DOM/CSSRuleList" rel="internal" href="https://developer.mozilla.org/en/DOM/CSSRuleList">CSSRuleList</a></code> of the CSS rules in the style sheet.
    */
  CSSRuleList getCssRules();


  /**
    * If this style sheet is imported into the document using an <code><a title="en/CSS/@import" rel="internal" href="https://developer.mozilla.org/en/CSS/@import">@import</a></code> rule, the <code>ownerRule</code> property will return that <code><a title="en/DOM/CSSImportRule" rel="internal" href="https://developer.mozilla.org/en/DOM/CSSImportRule" class="new ">CSSImportRule</a></code>, otherwise it returns <code>null</code>.
    */
  CSSRule getOwnerRule();

  CSSRuleList getRules();

  int addRule(String selector, String style);

  int addRule(String selector, String style, int index);


  /**
    * Deletes a rule from the style sheet.
    */
  void deleteRule(int index);


  /**
    * Inserts a new style rule into the current style sheet.
    */
  int insertRule(String rule, int index);

  void removeRule(int index);
}
