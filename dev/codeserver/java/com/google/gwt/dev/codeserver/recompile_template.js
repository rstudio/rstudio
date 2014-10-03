/*
 * Copyright 2014 Google Inc.
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
(function(){
  var $wnd = window;
  var $doc = $wnd.document;
  var $namespace = {};
  var moduleName = __MODULE_NAME__;

  var executeMain = function(){
    __LIB_JS__

    var metaTagParser = new $namespace.lib.MetaTagParser(moduleName);

    //Property providers need a certain environment to run
    var __gwt_getMetaProperty = function(name) {
      return metaTagParser.get()[name];
    };

    __PROPERTY_PROVIDERS__
    __MAIN__
  };

  if (/loaded|complete/.test($doc.readyState)) {
    executeMain();
  } else {
    //defer app script insertion until the body is ready
    if($wnd.addEventListener){
      $wnd.addEventListener('DOMContentLoaded', executeMain, false);
    } else{
      $wnd.attachEvent('onload', executeMain);
    }
  }
})();
