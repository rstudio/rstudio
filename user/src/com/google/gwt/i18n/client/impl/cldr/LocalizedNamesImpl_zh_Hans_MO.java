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

// DO NOT EDIT - GENERATED FROM CLDR DATA:
//  cldrVersion=21.0
//  number=$Revision: 6465 $
//  type=MO
//  date=$Date: 2012-01-27 12:47:35 -0500 (Fri, 27 Jan 2012) $


/**
 * Localized names for the "zh_Hans_MO" locale.
 */
public class LocalizedNamesImpl_zh_Hans_MO extends LocalizedNamesImpl_zh_Hans {

  @Override
  protected void loadNameMapJava() {
    super.loadNameMapJava();
    namesMap.put("CC", "科科斯（基林）群岛");
    namesMap.put("CP", "克利柏顿岛");
    namesMap.put("GP", "瓜德罗普");
    namesMap.put("LA", "老挝");
    namesMap.put("ME", "黑山");
    namesMap.put("MS", "蒙塞拉特");
    namesMap.put("SC", "塞舌尔");
  }

  @Override
  protected JavaScriptObject loadNameMapNative() {
    return overrideMap(super.loadNameMapNative(), loadMyNameMap());
  }

  private native JavaScriptObject loadMyNameMap() /*-{
    return {
        "CC": "科科斯（基林）群岛",
        "CP": "克利柏顿岛",
        "GP": "瓜德罗普",
        "LA": "老挝",
        "ME": "黑山",
        "MS": "蒙塞拉特",
        "SC": "塞舌尔"
    };
  }-*/;
}
