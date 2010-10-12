/*
 * Copyright 2010 Google Inc.
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

/** Called to slurp up all <meta> tags:
 * gwt:property, gwt:onPropertyErrorFn, gwt:onLoadErrorFn
 * 
 * This is included into the selection scripts
 * wherever PROCESS_METAS appears with underlines
 * on each side.
 */
function processMetas() {
  // Set some of the variables in the main script
  __gwt_getMetaProperty = function(name) {
    return null;
  }
  __propertyErrorFunction = null;
  __MODULE_FUNC__.__errFn = null;
}
