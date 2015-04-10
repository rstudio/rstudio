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

package com.google.gwt.core.ext.linker.impl;

import com.google.gwt.core.ext.LinkerContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.CompilationResult;
import com.google.gwt.core.ext.linker.ConfigurationProperty;
import com.google.gwt.core.ext.linker.SelectionProperty;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.jjs.JsOutputOption;

import java.util.Map.Entry;
import java.util.SortedSet;

/**
 * A utility class to fill in the properties javascript in linker templates.
 */
public class PropertiesUtil {
  public static String addKnownPropertiesJs(TreeLogger logger,
      CompilationResult result) {
    StringBuilder propertiesJs = new StringBuilder();

    // Multiple values for a property can result in one permutation. For
    // example, this permutation may be valid for safari and chrome.  However,
    // we need to pick one, since the computePropValue() needs to return a
    // single value.  It actually doesn't matter which one we pick. The fact
    // that safari and chrome compiled into one permutation indicates that
    // for this module, the behavior is the same.  Therefore, we just pick
    // the first one.
    for (Entry<SelectionProperty, String> entry :
         result.getPropertyMap().first().entrySet()) {
      propertiesJs.append("properties['");
      propertiesJs.append(entry.getKey().getName());
      propertiesJs.append("'] = '");
      propertiesJs.append(entry.getValue());
      propertiesJs.append("';");
    }
    return propertiesJs.toString();
  }

  public static StringBuffer addPropertiesJs(StringBuffer selectionScript,
      TreeLogger logger, LinkerContext context)
      throws UnableToCompleteException {
    int startPos;

    // Add property providers
    startPos = selectionScript.indexOf("// __PROPERTIES_END__");
    if (startPos == -1) {
      return selectionScript;
    }
    selectionScript.insert(startPos, generatePropertyProviders(logger, context));
    return selectionScript;
  }

  /**
   * Returns JavaScript that declares and initializes the "providers" and "values" variables.
   *
   * <p>Requires $doc and $wnd variables to be defined. (And possibly others; this is unclear.)
   *
   * <p>Provides "providers" and "values" variables.</p>.
   *
   * <ul>
   *   <li>"providers" is a mapping from each binding property (string) to a no-argument function
   *   that determines its value.</li>
   *   <li>"values" is a mapping from each binding property (string) to the set of allowed values
   *   (represented as a mapping with the value as key).
   *   </li>
   * </ul>
   */
  public static String generatePropertiesSnippet(ModuleDef module, TreeLogger compileLogger)
      throws UnableToCompleteException {

    // TODO: PropertyProviderGenerator should specify the JavaScript environment that a
    // property provider can assume so the caller of this function knows what it should provide.

    LinkerContext linkerContext = new StandardLinkerContext(compileLogger, module,
        module.getPublicResourceOracle(), JsOutputOption.PRETTY);

    String initProvidersJs =
        generatePropertyProviders(compileLogger, linkerContext);

    return "var providers = {};\nvar values = {};\n" + initProvidersJs + "\n";
  }

  /**
   * Generates JavaScript to create a property provider for each non-derived binding property
   * in the given linker. The JavaScript mutates the "providers" and "values" variables which
   * must already exist.
   */
  private static String generatePropertyProviders(TreeLogger logger, LinkerContext context)
      throws UnableToCompleteException {
    StringBuilder out = new StringBuilder();
    for (SelectionProperty p : context.getProperties()) {
      out.append(generatePropertyProvider(logger, p, context.getConfigurationProperties()));
    }
    return out.toString();
  }

  private static String generatePropertyProvider(TreeLogger logger,
      SelectionProperty prop, SortedSet<ConfigurationProperty> configurationProperties)
      throws UnableToCompleteException {
    StringBuilder toReturn = new StringBuilder();

    if (prop.tryGetValue() == null && !prop.isDerived()) {
      toReturn.append("providers['" + prop.getName() + "'] = function()");
      toReturn.append(prop.getPropertyProvider(logger, configurationProperties));
      toReturn.append(";");

      toReturn.append("values['" + prop.getName() + "'] = {");
      boolean needsComma = false;
      int counter = 0;
      for (String value : prop.getPossibleValues()) {
        if (needsComma) {
          toReturn.append(",");
        } else {
          needsComma = true;
        }
        toReturn.append("'" + value + "':");
        toReturn.append(counter++);
      }
      toReturn.append("};");
    }

    return toReturn.toString();
  }
}
