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

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * <p>Beginning with Firefox 4, the old <a title="en/Error Console" rel="internal" href="https://developer.mozilla.org/en/Error_Console">Error Console</a> has been deprecated in favor of the new, improved Web&nbsp;Console. The Web Console is something of a heads-up display for the web, letting you view error messages and other logged information. In addition, there are methods you can call to output information to the console, making it a useful debugging aid, and you can evaluate JavaScript on the fly.</p>
<p><a title="webconsole.png" rel="internal" href="https://developer.mozilla.org/@api/deki/files/4748/=webconsole.png"><img alt="webconsole.png" class="internal default" src="https://developer.mozilla.org/@api/deki/files/4748/=webconsole.png"></a></p>
<p>The Web Console won't replace more advanced debugging tools like <a class="external" title="http://getfirebug.com/" rel="external" href="http://getfirebug.com/" target="_blank">Firebug</a>; what it does give you, however, is a way to let remote users of your site or web application gather and report console logs and other information to you. It also provides a lightweight way to debug content if you don't happen to have Firebug installed when something goes wrong.</p>
<div class="note"><strong>Note:</strong> The Error Console is still available; you can re-enable it by changing the <code>devtools.errorconsole.enabled</code> preference to <code>true</code> and restarting the browser.</div>
  */
public interface Console {

  MemoryInfo getMemory();

  Indexable getProfiles();

  void assertCondition(boolean condition, Object arg);

  void count();

  void debug(Object arg);

  void dir();

  void dirxml();

  void error(Object arg);

  void group(Object arg);

  void groupCollapsed(Object arg);

  void groupEnd();

  void info(Object arg);

  void log(Object arg);

  void markTimeline();

  void profile(String title);

  void profileEnd(String title);

  void time(String title);

  void timeEnd(String title, Object arg);

  void timeStamp(Object arg);

  void trace(Object arg);

  void warn(Object arg);
}
