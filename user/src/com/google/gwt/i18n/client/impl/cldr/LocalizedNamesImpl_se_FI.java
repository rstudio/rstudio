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
//  number=$Revision: 5663 $
//  date=$Date: 2011-04-25 15:35:18 -0400 (Mon, 25 Apr 2011) $
//  type=root


/**
 * Localized names for the "se_FI" locale.
 */
public class LocalizedNamesImpl_se_FI extends LocalizedNamesImpl_se {

  @Override
  protected void loadNameMapJava() {
    super.loadNameMapJava();
    namesMap.put("002", "Afrihká");
    namesMap.put("005", "Mátta-Amerihká");
    namesMap.put("011", "Oarji-Afrihká");
    namesMap.put("015", "Davvi-Afrihká");
    namesMap.put("017", "Gaska-Afrihká");
    namesMap.put("021", "Davvi-Amerihká");
    namesMap.put("030", "Nuorta-Ásia");
    namesMap.put("034", "Mátta-Ásia");
    namesMap.put("039", "Mátta-Eurohpa");
    namesMap.put("143", "Gaska-Ásia");
    namesMap.put("145", "Oarji-Ásia");
    namesMap.put("150", "Eurohpa");
    namesMap.put("151", "Nuorta-Eurohpá");
    namesMap.put("154", "Davvi-Eurohpa");
    namesMap.put("155", "Oarji-Eurohpa");
  }

  @Override
  protected JavaScriptObject loadNameMapNative() {
    return overrideMap(super.loadNameMapNative(), loadMyNameMap());
  }

  private native JavaScriptObject loadMyNameMap() /*-{
    return {
        "002": "Afrihká",
        "005": "Mátta-Amerihká",
        "011": "Oarji-Afrihká",
        "015": "Davvi-Afrihká",
        "017": "Gaska-Afrihká",
        "021": "Davvi-Amerihká",
        "030": "Nuorta-Ásia",
        "034": "Mátta-Ásia",
        "039": "Mátta-Eurohpa",
        "143": "Gaska-Ásia",
        "145": "Oarji-Ásia",
        "150": "Eurohpa",
        "151": "Nuorta-Eurohpá",
        "154": "Davvi-Eurohpa",
        "155": "Oarji-Eurohpa"
    };
  }-*/;
}
