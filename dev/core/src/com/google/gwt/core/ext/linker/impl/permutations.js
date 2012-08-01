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

function getCompiledCodeFilename() {

  // A multi-tier lookup map that uses actual property values to quickly find
  // the strong name of the cache.js file to load.
  var answers = [];

  var softPermutationId;

  // Deferred-binding mapper function.  Sets a value into the several-level-deep
  // answers map. The keys are specified by a non-zero-length propValArray,
  // which should be a flat array target property values. Used by the generated
  // PERMUTATIONS code.
  function unflattenKeylistIntoAnswers(propValArray, value) {
    var answer = answers;
    for (var i = 0, n = propValArray.length - 1; i < n; ++i) {
      // lazy initialize an empty object for the current key if needed
      answer = answer[propValArray[i]] || (answer[propValArray[i]] = []);
    }
    // set the final one to the value
    answer[propValArray[n]] = value;
  }

  // Provides the computePropvalue() function and sets the 
  // __gwt_isKnownPropertyValue and MODULE_FUNC__.__computePropValue variables 
  __PROPERTIES__
  
  sendStats('bootstrap', 'selectingPermutation');
  if (isHostedMode()) {
    return computeUrlForResource("__HOSTED_FILENAME__"); 
  }
  var strongName;
  try {
    // __PERMUTATIONS_BEGIN__
    // Permutation logic is injected here. this code populates the 
    // answers variable.
    // __PERMUTATIONS_END__
    var idx = strongName.indexOf(':');
    if (idx != -1) {
      softPermutationId = parseInt(strongName.substring(idx + 1), 10);
      strongName = strongName.substring(0, idx);
    }
  } catch (e) {
    // intentionally silent on property failure
  }
  __MODULE_FUNC__.__softPermutationId = softPermutationId;
  return computeUrlForResource(strongName + '.cache.js');
}
