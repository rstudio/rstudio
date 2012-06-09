
package elemental.css;

import elemental.events.*;

/**
  * 
  */
public interface WebKitCSSKeyframesRule extends CSSRule {

  CSSRuleList getCssRules();

  String getName();

  void setName(String arg);

  void deleteRule(String key);

  WebKitCSSKeyframeRule findRule(String key);

  void insertRule(String rule);
}
