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
package com.google.gwt.i18n.server;

import java.util.HashMap;
import java.util.Map;

/**
 * Encodes the mapping of languages to their default script.
 */
public class DefaultLanguageScripts {

  /**
   * Maps languages to their default script.
   */
  private static Map<String, String> defaultScripts;

  static {
    // From http://www.iana.org/assignments/language-subtag-registry, and CLDR
    // alias data.
    defaultScripts = new HashMap<String, String>();
    defaultScripts.put("ab", "Cyrl");
    defaultScripts.put("af", "Latn");
    defaultScripts.put("am", "Ethi");
    defaultScripts.put("ar", "Arab");
    defaultScripts.put("as", "Beng");
    defaultScripts.put("az_AZ", "Latn");
    defaultScripts.put("az_IR", "Arab");
    defaultScripts.put("ay", "Latn");
    defaultScripts.put("be", "Cyrl");
    defaultScripts.put("bg", "Cyrl");
    defaultScripts.put("bn", "Beng");
    defaultScripts.put("bs", "Latn");
    defaultScripts.put("ca", "Latn");
    defaultScripts.put("ch", "Latn");
    defaultScripts.put("cs", "Latn");
    defaultScripts.put("cy", "Latn");
    defaultScripts.put("da", "Latn");
    defaultScripts.put("de", "Latn");
    defaultScripts.put("dv", "Thaa");
    defaultScripts.put("dz", "Tibt");
    defaultScripts.put("el", "Grek");
    defaultScripts.put("en", "Latn");
    defaultScripts.put("eo", "Latn");
    defaultScripts.put("es", "Latn");
    defaultScripts.put("et", "Latn");
    defaultScripts.put("eu", "Latn");
    defaultScripts.put("fa", "Arab");
    defaultScripts.put("fi", "Latn");
    defaultScripts.put("fj", "Latn");
    defaultScripts.put("fo", "Latn");
    defaultScripts.put("fr", "Latn");
    defaultScripts.put("frr", "Latn");
    defaultScripts.put("fy", "Latn");
    defaultScripts.put("ga", "Latn");
    defaultScripts.put("gl", "Latn");
    defaultScripts.put("gn", "Latn");
    defaultScripts.put("gu", "Latn");
    defaultScripts.put("gv", "Latn");
    defaultScripts.put("ha_GH", "Latn");
    defaultScripts.put("ha_NE", "Latn");
    defaultScripts.put("ha_NG", "Latn");
    defaultScripts.put("ha_SD", "Arab");
    defaultScripts.put("he", "Hebr");
    defaultScripts.put("hi", "Deva");
    defaultScripts.put("hr", "Latn");
    defaultScripts.put("ht", "Latn");
    defaultScripts.put("hu", "Latn");
    defaultScripts.put("hy", "Armn");
    defaultScripts.put("id", "Latn");
    defaultScripts.put("in", "Latn");
    defaultScripts.put("is", "Latn");
    defaultScripts.put("it", "Latn");
    defaultScripts.put("iw", "Hebr");
    defaultScripts.put("ja", "Jpan");
    defaultScripts.put("ka", "Geor");
    defaultScripts.put("kk", "Cyrl");
    defaultScripts.put("kk_KZ", "Cyrl"); // ?
    defaultScripts.put("kl", "Latn");
    defaultScripts.put("km", "Khmr");
    defaultScripts.put("kn", "Knda");
    defaultScripts.put("ko", "Kore");
    defaultScripts.put("ku_IQ", "Arab");
    defaultScripts.put("ku_IR", "Arab");
    defaultScripts.put("ku_SY", "Latn");
    defaultScripts.put("ku_TR", "Latn");
    defaultScripts.put("la", "Latn");
    defaultScripts.put("lb", "Latn");
    defaultScripts.put("ln", "Latn");
    defaultScripts.put("lo", "Laoo");
    defaultScripts.put("lt", "Latn");
    defaultScripts.put("lv", "Latn");
    defaultScripts.put("mg", "Latn");
    defaultScripts.put("mh", "Latn");
    defaultScripts.put("mk", "Cyrl");
    defaultScripts.put("ml", "Mlym");
    defaultScripts.put("mn_CN", "Mong");
    defaultScripts.put("mn_MN", "Cyrl");
    defaultScripts.put("mo", "Latn");
    defaultScripts.put("mr", "Deva");
    defaultScripts.put("ms", "Latn");
    defaultScripts.put("mt", "Latn");
    defaultScripts.put("my", "Mymr");
    defaultScripts.put("na", "Latn");
    defaultScripts.put("nb", "Latn");
    defaultScripts.put("nd", "Latn");
    defaultScripts.put("ne", "Deva");
    defaultScripts.put("nl", "Latn");
    defaultScripts.put("nn", "Latn");
    defaultScripts.put("no", "Latn");
    defaultScripts.put("nr", "Latn");
    defaultScripts.put("ny", "Latn");
    defaultScripts.put("om", "Latn");
    defaultScripts.put("or", "Latn");
    defaultScripts.put("pa", "Guru");
    defaultScripts.put("pa_PK", "Arab");
    defaultScripts.put("pl", "Latn");
    defaultScripts.put("ps", "Arab");
    defaultScripts.put("pt", "Latn");
    defaultScripts.put("qu", "Latn");
    defaultScripts.put("rn", "Latn");
    defaultScripts.put("ro", "Latn");
    defaultScripts.put("ru", "Cyrl");
    defaultScripts.put("rw", "Latn");
    defaultScripts.put("sg", "Latn");
    defaultScripts.put("si", "Sinh");
    defaultScripts.put("sk", "Latn");
    defaultScripts.put("sl", "Latn");
    defaultScripts.put("sm", "Latn");
    defaultScripts.put("so", "Latn");
    defaultScripts.put("sq", "Latn");
    defaultScripts.put("sr_BA", "Cyrl");
    defaultScripts.put("sr_CS", "Cyrl");
    defaultScripts.put("sr_ME", "Latn");
    defaultScripts.put("sr_RS", "Cyrl");
    defaultScripts.put("sr_YU", "Cyrl");
    defaultScripts.put("ss", "Latn");
    defaultScripts.put("st", "Latn");
    defaultScripts.put("sv", "Latn");
    defaultScripts.put("sw", "Latn");
    defaultScripts.put("ta", "Taml");
    defaultScripts.put("te", "Telu");
    defaultScripts.put("tg_TJ", "Cyrl");
    defaultScripts.put("th", "Thai");
    defaultScripts.put("ti", "Ethi");
    defaultScripts.put("tl", "Latn");
    defaultScripts.put("tn", "Latn");
    defaultScripts.put("to", "Latn");
    defaultScripts.put("tr", "Latn");
    defaultScripts.put("ts", "Latn");
    defaultScripts.put("ug_CN", "Arab");
    defaultScripts.put("uk", "Cyrl");
    defaultScripts.put("ur", "Arab");
    defaultScripts.put("uz_AF", "Arab");
    defaultScripts.put("uz_UZ", "Cyrl");
    defaultScripts.put("ve", "Latn");
    defaultScripts.put("vi", "Latn");
    defaultScripts.put("wo", "Latn");
    defaultScripts.put("wo_SN", "Latn"); // ?
    defaultScripts.put("xh", "Latn");
    defaultScripts.put("yi", "Hebr");
    defaultScripts.put("zh_CN", "Hans");
    defaultScripts.put("zh_HK", "Hant");
    defaultScripts.put("zh_MO", "Hant");
    defaultScripts.put("zh_SG", "Hans");
    defaultScripts.put("zh_TW", "Hant");
    defaultScripts.put("zu", "Latn");
    defaultScripts.put("dsb", "Latn");
    defaultScripts.put("frs", "Latn");
    defaultScripts.put("gsw", "Latn");
    defaultScripts.put("hsb", "Latn");
    defaultScripts.put("kok", "Deva");
    defaultScripts.put("mai", "Deva");
    defaultScripts.put("men", "Latn");
    defaultScripts.put("nds", "Latn");
    defaultScripts.put("niu", "Latn");
    defaultScripts.put("nqo", "Nkoo");
    defaultScripts.put("nso", "Latn");
    defaultScripts.put("shi_MA", "Latn");
    defaultScripts.put("tem", "Latn");
    defaultScripts.put("tkl", "Latn");
    defaultScripts.put("tmh", "Latn");
    defaultScripts.put("tpi", "Latn");
    defaultScripts.put("tvl", "Latn");
    defaultScripts.put("tzm_MA", "Latn");
    defaultScripts.put("zbl", "Blis");
  }

  /**
   * Returns the default script for a language, or null if none.
   * 
   * @param language language code to get default script for (in lowercase)
   * @return default script for language, or null if none
   */
  public static String getDefaultScript(String language) {
    return defaultScripts.get(language);
  }

  /**
   * Returns the default script for a language/region combination, or null if
   * none.
   * 
   * @param language language code to get default script for (in lowercase)
   * @param region region code to get default script for (in uppercase); may be
   *     null
   * @return default script for language/region, or null if none
   */
  public static String getDefaultScript(String language, String region) {
    String script = null;
    if (region != null) {
      script = defaultScripts.get(language + "_" + region);
    }
    if (script == null) {
      script = defaultScripts.get(language);
    }
    return script;
  }
}
