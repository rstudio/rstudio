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

import com.google.gwt.dev.cfg.BindingProperty;
import com.google.gwt.dev.cfg.PropertyProvider;

import junit.framework.TestCase;

/**
 * Tests for {@link StandardSelectionProperty}.
 */
public class StandardSelectionPropertyTest extends TestCase {

  private static final String FBV = "FBV";

  private static final String PROVIDER_MULTIFALLBACK =
      "provider text with fallback=/*-FALLBACK-*/, repeated /*-FALLBACK-*//*-FALLBACK-*/";
  private static final String PROVIDER_MULTIFALLBACK_EMPTY =
    "provider text with fallback=, repeated ";
  private static final String PROVIDER_MULTIFALLBACK_FBV =
    "provider text with fallback=FBV, repeated FBVFBV";

  private static final String PROVIDER_NOFALLBACK = "provider text without fallback";

  public void testNoFallback() {
    BindingProperty bp = new BindingProperty("doesNotUseFallback");
    PropertyProvider provider = new PropertyProvider(PROVIDER_NOFALLBACK);
    bp.setProvider(provider);
    StandardSelectionProperty property = new StandardSelectionProperty(bp);
    assertEquals(PROVIDER_NOFALLBACK, property.getPropertyProvider());

    provider = new PropertyProvider(PROVIDER_MULTIFALLBACK);
    bp.setProvider(provider);
    property = new StandardSelectionProperty(bp);
    assertEquals(PROVIDER_MULTIFALLBACK_EMPTY, property.getPropertyProvider());
  }

  public void testWithFallback() {
    BindingProperty bp = new BindingProperty("doesUseFallback");
    bp.setFallback(FBV);
    PropertyProvider provider = new PropertyProvider(PROVIDER_NOFALLBACK);
    bp.setProvider(provider);
    StandardSelectionProperty property = new StandardSelectionProperty(bp);
    assertEquals(PROVIDER_NOFALLBACK, property.getPropertyProvider());

    provider = new PropertyProvider(PROVIDER_MULTIFALLBACK);
    bp.setProvider(provider);
    property = new StandardSelectionProperty(bp);
    assertEquals(PROVIDER_MULTIFALLBACK_FBV, property.getPropertyProvider());
  }
}
