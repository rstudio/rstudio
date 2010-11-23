/*
 * Copyright 2009 Google Inc.
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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.ConfigurationProperty;
import com.google.gwt.core.ext.linker.PropertyProviderGenerator;
import com.google.gwt.dev.cfg.BindingProperty;
import com.google.gwt.dev.cfg.PropertyProvider;
import com.google.gwt.dev.shell.FailErrorLogger;

import junit.framework.TestCase;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Tests for {@link StandardSelectionProperty}.
 */
public class StandardSelectionPropertyTest extends TestCase {

  /**
   * Test property provider generator.
   */
  public static class MyProviderGenerator
      implements PropertyProviderGenerator {

    public String generate(TreeLogger logger, SortedSet<String> possibleValues,
        String fallback, SortedSet<ConfigurationProperty> configProperties)
        throws UnableToCompleteException {
      return "good " + fallback;
    }
  }

  private static final String FBV = "FBV";

  private static final String PROVIDER_MULTIFALLBACK =
      "provider text with fallback=/*-FALLBACK-*/, repeated /*-FALLBACK-*//*-FALLBACK-*/";
  private static final String PROVIDER_MULTIFALLBACK_EMPTY =
    "provider text with fallback=, repeated ";
  private static final String PROVIDER_MULTIFALLBACK_FBV =
    "provider text with fallback=FBV, repeated FBVFBV";

  private static final String PROVIDER_NOFALLBACK = "provider text without fallback";

  private static final TreeLogger logger = new FailErrorLogger();

  private static final SortedSet<ConfigurationProperty> configProperties = new TreeSet<ConfigurationProperty>();

  public void testNoFallback() throws UnableToCompleteException {
    BindingProperty bp = new BindingProperty("doesNotUseFallback");
    PropertyProvider provider = new PropertyProvider(PROVIDER_NOFALLBACK);
    bp.setProvider(provider);
    StandardSelectionProperty property = new StandardSelectionProperty(bp);
    assertEquals(PROVIDER_NOFALLBACK, property.getPropertyProvider(logger,
        configProperties));

    provider = new PropertyProvider(PROVIDER_MULTIFALLBACK);
    bp.setProvider(provider);
    property = new StandardSelectionProperty(bp);
    assertEquals(PROVIDER_MULTIFALLBACK_EMPTY, property.getPropertyProvider(
        logger, configProperties));
  }

  public void testPropertyProviderGenerator() throws UnableToCompleteException {
    BindingProperty bp = new BindingProperty("providerGenerator");
    bp.setFallback(FBV);
    PropertyProvider provider = new PropertyProvider("bad");
    bp.setProvider(provider);
    bp.setProviderGenerator(MyProviderGenerator.class);
    StandardSelectionProperty property = new StandardSelectionProperty(bp);
    assertEquals("good " + FBV, property.getPropertyProvider(logger,
        configProperties));
  }

  public void testWithFallback() throws UnableToCompleteException {
    BindingProperty bp = new BindingProperty("doesUseFallback");
    bp.setFallback(FBV);
    PropertyProvider provider = new PropertyProvider(PROVIDER_NOFALLBACK);
    bp.setProvider(provider);
    StandardSelectionProperty property = new StandardSelectionProperty(bp);
    assertEquals(PROVIDER_NOFALLBACK, property.getPropertyProvider(logger,
        configProperties));

    provider = new PropertyProvider(PROVIDER_MULTIFALLBACK);
    bp.setProvider(provider);
    property = new StandardSelectionProperty(bp);
    assertEquals(PROVIDER_MULTIFALLBACK_FBV, property.getPropertyProvider(
        logger, configProperties));
  }
}
