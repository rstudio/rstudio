// Copyright 2007 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License. You may obtain a copy of
// the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// License for the specific language governing permissions and limitations under
// the License.
//
// This startup script is for legacy support and is now deprecated. Instead of
// using this script, just include the selection script directly.
//
(function(){
  var metas = document.getElementsByTagName("meta");
  for (var i = 0, n = metas.length; i < n; ++i) {
    var meta = metas[i], name = meta.getAttribute("name");
    if (name == "gwt:module") {
      var modName, content = meta.getAttribute("content");
      if (content) {
        var eqPos = content.lastIndexOf("=");
        if (eqPos != -1) {
          modName = content.substring(eqPos + 1);
          content = content.substring(0, eqPos) + '/' + modName;
        } else {
          modName = content;
        }
        document.write('<script src="' + content + '.nocache.js"></script>');
      }
    }
  }
})();
