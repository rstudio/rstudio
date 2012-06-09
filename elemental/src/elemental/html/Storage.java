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
  * <p><strong>Storage</strong> is a <a class="external" rel="external" href="http://www.sqlite.org/" title="http://www.sqlite.org/" target="_blank">SQLite</a> database API. It is available to trusted callers, meaning extensions and Firefox components only.</p>
<p>The API is currently "unfrozen", which means it is subject to change at any time; in fact, it has changed somewhat with each release of Firefox since it was introduced, and will likely continue to do so for a while.</p>
<div class="note"><strong>Note:</strong> Storage is not the same as the <a title="en/DOM/Storage" rel="internal" href="https://developer.mozilla.org/en/DOM/Storage">DOM:Storage</a> feature which can be used by web pages to store persistent data or the <a title="en/Session_store_API" rel="internal" href="https://developer.mozilla.org/en/Session_store_API">Session store API</a> (an <a title="en/XPCOM" rel="internal" href="https://developer.mozilla.org/en/XPCOM">XPCOM</a> storage utility for use by extensions).</div>
  */
public interface Storage {

  int getLength();

  void clear();

  String getItem(String key);

  String key(int index);

  void removeItem(String key);

  void setItem(String key, String data);
}
