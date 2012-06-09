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
import elemental.stylesheets.MediaList;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * An object representing a single CSS media rule.&nbsp;<code>CSSMediaRule</code>&nbsp;implements the&nbsp;<code><a href="https://developer.mozilla.org/en/DOM/CSSRule" rel="custom">CSSRule</a></code>&nbsp;interface.
  */
public interface CSSMediaRule extends CSSRule {


  /**
    * Returns a <code><a title="en/DOM/CSSRuleList" rel="internal" href="https://developer.mozilla.org/en/DOM/CSSRuleList">CSSRuleList</a></code> of the CSS rules in the media rule.
    */
  CSSRuleList getCssRules();


  /**
    * Specifies the intended destination medium for style information.
    */
  MediaList getMedia();


  /**
    * Deletes a rule from the style sheet.
    */
  void deleteRule(int index);


  /**
    * Inserts a new style rule into the current style sheet.
    */
  int insertRule(String rule, int index);
}
