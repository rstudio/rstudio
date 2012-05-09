// Computes the value of a given property that was determined during the
// compilation process.  This assumes that the bootstrap is embedded in an MD5
// specific file, and during the compilation phase, the PermutationsUtil class
// will take care of filling the __KNOWN_PROPERTIES__ section with the values
// of the properties for this specific permutation.  Note that if a permutation
// is valid for more than one property value (for example user.agent = chrome
// or safari) then one of those values will be chosen at random.  Since the
// different values resulted in the same MD5 file, we can assume that the
// behavior of this module is the same for either value.

  var properties = [];
  
  function computePropValue(propName) {
    if (propName in properties) {
      return properties[propName];
    }
    throw null;
  }
  
  __KNOWN_PROPERTIES__
  
  // It's unclear how/if this function is applicable in the context of the
  // properties being selected on the server.  Just return false for now.
  __gwt_isKnownPropertyValue = function(propName, propValue) {
    return false;
  };
  __MODULE_FUNC__.__computePropValue = computePropValue;

  // Gets a map of the non-constant, non-derived binding properties
  __MODULE_FUNC__.__getPropMap = function() {
    var result = {};
    for (var key in properties) {
      result[key] = properties[key];
    }
    return result;
  };

  // make properties available to super dev mode hook
  $wnd.__gwt_activeModules["__MODULE_NAME__"].bindings = __MODULE_FUNC__.__getPropMap;
