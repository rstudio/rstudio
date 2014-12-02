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
package com.google.gwt.i18n.client.impl.cldr;

import com.google.gwt.core.client.JavaScriptObject;

// DO NOT EDIT - GENERATED FROM CLDR DATA

/**
 * Localized names for the "zh_Hans_SG" locale.
 */
public class LocalizedNamesImpl_zh_Hans_SG extends LocalizedNamesImpl_zh_Hans {

  @Override
  protected void loadNameMapJava() {
    super.loadNameMapJava();
    namesMap.put("CP", "克利柏顿岛");
    namesMap.put("ME", "黑山");
    namesMap.put("PM", "圣皮埃尔和密克隆");
  }

  @Override
  protected JavaScriptObject loadNameMapNative() {
    return overrideMap(super.loadNameMapNative(), loadMyNameMap());
  }

  private native JavaScriptObject loadMyNameMap() /*-{
    return {
        "CP": "克利柏顿岛",
        "ME": "黑山",
        "PM": "圣皮埃尔和密克隆"
    };
  }-*/;
}
