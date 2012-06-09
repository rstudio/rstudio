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
  * <p>An object implementing the <code>CSSRule</code> DOM interface represents a single CSS rule. References to a <code>CSSRule</code>-implementing object may be obtained by looking at a <a title="en/DOM/stylesheet" rel="internal" href="https://developer.mozilla.org/en/DOM/CSSStyleSheet">CSS style sheet's</a> <code><a title="en/DOM/CSSStyleSheet/cssRules" rel="internal" href="https://developer.mozilla.org/en/DOM/CSSStyleSheet">cssRules</a></code> list.</p>
<p>There are several kinds of rules. The <code>CSSRule</code> interface specifies the properties common to all rules, while properties unique to specific rule types are specified in the more specialized interfaces for those rules' respective types.</p>
  */
public interface CSSRule {

    static final int CHARSET_RULE = 2;

    static final int FONT_FACE_RULE = 5;

    static final int IMPORT_RULE = 3;

    static final int MEDIA_RULE = 4;

    static final int PAGE_RULE = 6;

    static final int STYLE_RULE = 1;

    static final int UNKNOWN_RULE = 0;

    static final int WEBKIT_KEYFRAMES_RULE = 7;

    static final int WEBKIT_KEYFRAME_RULE = 8;


  /**
    * Returns the textual representation of the rule, e.g. <code>"h1,h2 { font-size: 16pt }"</code>
    */
  String getCssText();

  void setCssText(String arg);


  /**
    * Returns the containing rule, otherwise <code>null</code>. E.g. if this rule is a style rule inside an <code><a title="en/CSS/@media" rel="internal" href="https://developer.mozilla.org/en/CSS/@media">@media</a></code> block, the parent rule would be that <code><a title="en/DOM/CSSMediaRule" rel="internal" href="https://developer.mozilla.org/en/DOM/CSSMediaRule">CSSMediaRule</a></code>.
    */
  CSSRule getParentRule();


  /**
    * Returns the <code><a title="en/DOM/CSSStyleSheet" rel="internal" href="https://developer.mozilla.org/en/DOM/CSSStyleSheet">CSSStyleSheet</a></code> object for the style sheet that contains this rule
    */
  CSSStyleSheet getParentStyleSheet();


  /**
    * One of the <a rel="custom" href="https://developer.mozilla.org/en/DOM/cssRule#Type_constants">Type constants</a>&nbsp;indicating the type of CSS&nbsp;rule.
    */
  int getType();
}
