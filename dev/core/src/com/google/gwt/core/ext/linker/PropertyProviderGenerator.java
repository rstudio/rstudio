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
package com.google.gwt.core.ext.linker;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;

import java.util.SortedSet;

/**
 * An interface for generating a property provider JS implementation, rather
 * than having it defined in a module file.
 *
 * Use it like this:
 * <pre>
 *   &lt;property-provider name="foo" generator="org.example.FooGenerator"/&gt;
 * </pre>
 * A default implementation in JS can be included inside the property-provider
 * tag as usual, and will be used if the generator returns {@code null}.
 */
public interface PropertyProviderGenerator {

  /**
   * Generate a property provider.
   *
   * @param logger TreeLogger
   * @param possibleValues the possible values of this property
   * @param fallback the fallback value for this property, or null
   * @param configProperties the configuration properties for this module
   * @return the JS source of the property provider (the complete body of a JS
   *     function taking no arguments, including open/close braces), or null to
   *     use the default implementation in the property-provider tag
   * @throws UnableToCompleteException after logging the message if processing
   *     is unable to continue
   */
  String generate(TreeLogger logger, SortedSet<String> possibleValues,
      String fallback, SortedSet<ConfigurationProperty> configProperties)
      throws UnableToCompleteException;
}
