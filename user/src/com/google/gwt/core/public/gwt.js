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
// This startup script should be included in host pages either just after
// <body> or inside the <head> after module <meta> tags.
//
(function(){
	var metas = document.getElementsByTagName("meta");

	for (var i = 0, n = metas.length; i < n; ++i) {
		var meta = metas[i];
		var name = meta.getAttribute("name");
		if (name) {
			if (name == "gwt:module") {
				var moduleName = meta.getAttribute("content");
				if (moduleName) {
					var eqPos = moduleName.lastIndexOf("=");
					if (eqPos != -1) {
						var base = moduleName.substring(0, eqPos);
						moduleName = moduleName.substring(eqPos + 1);
						window.__gwt_base = { };
						window.__gwt_base[moduleName] = base;
						moduleName = base + '/' + moduleName;
					}
					document.write('<script src="' + moduleName + '.nocache.js"></script>');
				}
			}
		}
	}
})();
