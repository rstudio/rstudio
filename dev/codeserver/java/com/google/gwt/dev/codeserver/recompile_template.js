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
  // Variables declared in this scope will be seen by both recompile_lib and the property providers.
  var $wnd = window;
  var $doc = $wnd.document;
  var __moduleName = __MODULE_NAME__;

  // Because GWT linker architecture allows property providers to use global variables
  // we need to make sure that these are defined in the scope of property providers.
  // Initializes the property providers.
  // Note: The parameters of this function are used by property providers so they cannot be renamed
  // and need to be kept in sync with PropertySource.__getProvidersAndValues.
  // TODO(dankurka): refactor linkers and templates to not use global symbols anymore.
  var __initPropertyProviders = function(__gwt_getMetaProperty, __gwt_isKnownPropertyValue){
    __PROPERTY_PROVIDERS__
    return {values: values, providers: providers};
  };

  var executeMain = function() {
    // $namespace is used by the library to publish its symbols
    var $namespace = {};
    __LIB_JS__
    __MAIN__
  };

  if (/loaded|complete/.test($doc.readyState)) {
    executeMain();
  } else {
    //defer app script insertion until the body is ready
    if($wnd.addEventListener){
      $wnd.addEventListener('load', executeMain, false);
    } else{
      $wnd.attachEvent('onload', executeMain);
    }
  }
})();
