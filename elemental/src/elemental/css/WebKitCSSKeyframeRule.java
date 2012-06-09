
package elemental.css;

import elemental.events.*;

/**
  * 
  */
public interface WebKitCSSKeyframeRule extends CSSRule {

  String getKeyText();

  void setKeyText(String arg);

  CSSStyleDeclaration getStyle();
}
