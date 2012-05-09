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

  // Maps property names onto sets of legal values for that property.
  var values = [];

  // Maps property names onto a function to compute that property.
  var providers = [];

  // Computes the value of a given property.  propName must be a valid property
  // name. Used by the generated PERMUTATIONS code.
  function computePropValue(propName) {
    var value = providers[propName](), allowedValuesMap = values[propName];
    if (value in allowedValuesMap) {
      return value;
    }
    var allowedValuesList = [];
    for (var k in allowedValuesMap) {
      allowedValuesList[allowedValuesMap[k]] = k;
    }
    if (__propertyErrorFunc) {
      __propertyErrorFunc(propName, allowedValuesList, value);
    }
    throw null;
  }

  // __PROPERTIES_BEGIN__
  // Properties logic is injected here. This code populates the values and
  // providers variables
  // __PROPERTIES_END__

  // Determines whether or not a particular property value is allowed. Called by
  // property providers.
  __gwt_isKnownPropertyValue = function(propName, propValue) {
    return propValue in values[propName];
  };

  // Gets a map of the non-constant, non-derived binding properties
  __MODULE_FUNC__.__getPropMap = function() {
    var result = {};
    for (var key in values) {
      result[key] = computePropValue(key);
    }
    return result;
  };

  __MODULE_FUNC__.__computePropValue = computePropValue;
  $wnd.__gwt_activeModules["__MODULE_NAME__"].bindings = __MODULE_FUNC__.__getPropMap;
