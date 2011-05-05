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
package com.google.gwt.i18n.client.impl.cldr;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.i18n.client.DefaultLocalizedNames;
import com.google.gwt.i18n.client.Localizable;

/**
 * A base class for client-side implementations of the {@link
 * com.google.gwt.i18n.client.LocalizedNames} interface.
 */
public abstract class LocalizedNamesImplBase extends DefaultLocalizedNames
    implements Localizable {
  
  /**
   * Add all entries in {@code override} to the original map, replacing
   * any existing entries.  This is used by subclasses that need to slightly
   * alter the data used by the parent locale.
   */
  protected static final native JavaScriptObject overrideMap(
      JavaScriptObject original, JavaScriptObject override) /*-{
    for (var key in override) {
      if (override.hasOwnProperty(key)) {
        original[key] = override[key];
      }
    }
    return original;
  }-*/;

  private JavaScriptObject jsoNameMap = null;

  @Override
  public final String getRegionNameImpl(String regionCode) {
    if (GWT.isScript()) {
      return getRegionNameNative(regionCode);
    } else {
      return super.getRegionNameImpl(regionCode);
    }
  }

  @Override
  protected String[] loadLikelyRegionCodes() {
    // If this override isn't here, LocalizableGenerator-produced overrides
    // fail with a visibility error in both javac and eclipse.
    return super.loadLikelyRegionCodes();
  }

  @Override
  protected final void loadNameMap() {
    if (GWT.isScript()) {
      jsoNameMap = loadNameMapNative(); 
    } else {
      loadNameMapJava();
    }
  }

  /**
   * Load the code=>name map for use in pure Java.  On return, nameMap will be
   * appropriately initialized.
   */
  protected void loadNameMapJava() {
    super.loadNameMap();
  }

  /**
   * Load the code=>name map for use in JS.
   * 
   * @return a JSO containing a map of country codes to localized names
   */
  protected abstract JavaScriptObject loadNameMapNative();

  @Override
  protected final boolean needsNameMap() {
    if (GWT.isScript()) {
      return jsoNameMap == null;
    } else {
      return super.needsNameMap();
    }
  }

  private native String getRegionNameNative(String regionCode) /*-{
    return this.@com.google.gwt.i18n.client.impl.cldr.LocalizedNamesImplBase::jsoNameMap[regionCode];
  }-*/;
}
