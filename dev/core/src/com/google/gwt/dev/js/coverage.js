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

window.onbeforeunload = function() {
  var merge_coverage = function(x, y) {
    var merge = function(x, y, merger) {
      for (var key in y)
        if (x.hasOwnProperty(key))
          x[key] = merger(x[key], y[key]);
        else
          x[key] = y[key];
        return x;
    };

    merge(x, y, function(u, v) {
      return merge(u, v, Math.max);
    });
  };

  var coverage = JSON.parse(localStorage.getItem('gwt_coverage'));
  if (coverage !== null)
    merge_coverage($coverage, coverage);
  localStorage.setItem('gwt_coverage', JSON.stringify($coverage));
};