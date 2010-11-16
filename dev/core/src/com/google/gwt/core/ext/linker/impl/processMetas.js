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
  var metaProps = {}
  var propertyErrorFunc;
  var onLoadErrorFunc;

  var metas = $doc.getElementsByTagName('meta');
  for (var i = 0, n = metas.length; i < n; ++i) {
    var meta = metas[i]
    , name = meta.getAttribute('name')
    , content;

    if (name) {
      name = name.replace('__MODULE_NAME__::', '');
      if (name.indexOf('::') >= 0) {
        // It's for a different module
        continue;
      }

      if (name == 'gwt:property') {
        content = meta.getAttribute('content');
        if (content) {
          var value, eq = content.indexOf('=');
          if (eq >= 0) {
            name = content.substring(0, eq);
            value = content.substring(eq + 1);
          } else {
            name = content;
            value = '';
          }
          metaProps[name] = value;
        }
      } else if (name == 'gwt:onPropertyErrorFn') {
        content = meta.getAttribute('content');
        if (content) {
          try {
            propertyErrorFunc = eval(content);
          } catch (e) {
            alert('Bad handler \"' + content +
              '\" for \"gwt:onPropertyErrorFn\"');
          }
        }
      } else if (name == 'gwt:onLoadErrorFn') {
        content = meta.getAttribute('content');
        if (content) {
          try {
            onLoadErrorFunc = eval(content);
          } catch (e) {
            alert('Bad handler \"' + content + '\" for \"gwt:onLoadErrorFn\"');
          }
        }
      }
    }
  }

  // Set some of the variables in the main script
  __gwt_getMetaProperty = function(name) {
    var value = metaProps[name];
    return (value == null) ? null : value;
  }
  __propertyErrorFunction = propertyErrorFunc;
  __MODULE_FUNC__.__errFn = onLoadErrorFunc;
}
