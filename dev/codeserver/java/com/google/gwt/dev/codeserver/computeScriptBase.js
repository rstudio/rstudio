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

/**
 * A simplified version of computeScriptBase.js that's used only when running
 * in Super Dev Mode. (We don't want the default version because it allows the
 * web page to override it using a meta tag.)
 *
 * Prerequisite: we assume that the first script tag using a URL ending with
 * "/__MODULE_NAME__.nocache.js" is the one that loaded us. Normally this happens
 * because DevModeRedirectHook.js loaded this nocache.js script by prepending a
 * script tag with an absolute URL to head. (However, it's also okay for an html
 * file included in the GWT compiler's output to load the nocache.js file using
 * a relative URL.)
 */
function computeScriptBase() {
  // TODO(skybrian) This approach won't work for workers.

  $wnd.__gwt_activeModules['__MODULE_NAME__'].superdevmode = true;

  var expectedSuffix = '/__MODULE_NAME__.nocache.js';

  var scriptTags = $doc.getElementsByTagName('script');
  for (var i = 0;; i++) {
    var tag = scriptTags[i];
    if (!tag) {
      break;
    }
    var candidate = tag.src;
    var lastMatch = candidate.lastIndexOf(expectedSuffix);
    if (lastMatch == candidate.length - expectedSuffix.length) {
      // Assumes that either the URL is absolute, or it's relative
      // and the html file is hosted by this code server.
      return candidate.substring(0, lastMatch + 1);
    }
  }

  $wnd.alert('Unable to load Super Dev Mode version of ' + __MODULE_NAME__ + ".");
}
