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

import com.google.gwt.i18n.shared.GwtLocale;
import com.google.gwt.i18n.shared.GwtLocaleFactory;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.List;

/**
 * Test GwtLocale.
 */
public class GwtLocaleTest extends TestCase {

  private GwtLocaleFactory factory = new GwtLocaleFactoryImpl();

  public void testAliases() {
    GwtLocale en = factory.fromString("en");
    List<GwtLocale> aliases = en.getAliases();
    assertEquals(aliases.get(0), en);
    GwtLocale enLatn = factory.fromString("en_Latn");
    assertContainsAndGetPosition(aliases, enLatn);
    assertTrue(en.usesSameScript(enLatn));
    assertTrue(enLatn.inheritsFrom(en));
    assertFalse(en.inheritsFrom(enLatn));
    assertFalse(en.inheritsFrom(en));
    GwtLocale pt = factory.fromString("pt");
    aliases = pt.getAliases();
    assertContainsAndGetPosition(aliases, factory.fromString("pt_BR"));
    GwtLocale iw = factory.fromString("iw");
    aliases = iw.getAliases();
    GwtLocale he = factory.fromString("he");
    assertEquals(aliases.get(0), he);
    assertContainsAndGetPosition(aliases, factory.fromString("iw_Hebr"));
    aliases = he.getAliases();
    assertContainsAndGetPosition(aliases, factory.fromString("iw_Hebr"));
    GwtLocale id = factory.fromString("id");
    aliases = id.getAliases();
    assertContainsAndGetPosition(aliases, factory.fromString("in"));
    GwtLocale mo = factory.fromString("mo");
    aliases = mo.getAliases();
    assertContainsAndGetPosition(aliases, factory.fromString("ro"));
    GwtLocale jv = factory.fromString("jv");
    aliases = jv.getAliases();
    assertContainsAndGetPosition(aliases, factory.fromString("jw"));
    GwtLocale ji = factory.fromString("ji");
    aliases = ji.getAliases();
    assertContainsAndGetPosition(aliases, factory.fromString("yi"));
  }

  public void testCanonicalForm() {
    GwtLocale zhCN = factory.fromString("zh_CN");
    GwtLocale zhHansCN = factory.fromString("zh_Hans_CN");
    GwtLocale zhHant = factory.fromString("zh_Hant");
    GwtLocale zhTW = factory.fromString("zh_TW");
    GwtLocale zhHantTW = factory.fromString("zh_Hant_TW");
    GwtLocale zhHans = factory.fromString("zh_Hans");
    assertEquals(zhCN, zhHansCN.getCanonicalForm());
    assertEquals(zhTW, zhHantTW.getCanonicalForm());
    assertEquals(zhCN, zhHans.getCanonicalForm());
    assertEquals(zhTW, zhHant.getCanonicalForm());
    GwtLocale paPK = factory.fromString("pa_PK");
    GwtLocale paArabPK = factory.fromString("pa_Arab_PK");
    assertEquals(paPK, paArabPK.getCanonicalForm());
  }

  public void testCompare() {
    GwtLocale[] locales = new GwtLocale[] {
        factory.fromString("en_US_POSIX"),
        factory.fromString("en"),
        factory.fromString("es-419"),
        factory.fromString("en-gb"),
        factory.fromString("en-AU-OUTBACK"),
        factory.fromString("es_AR"),
        factory.fromString("en_US"),
        factory.fromString("en_Latn"),
        factory.fromString("es-ES"),
    };
    Arrays.sort(locales);
    assertEquals("en", locales[0].getAsString());
    assertEquals("en_AU_OUTBACK", locales[1].getAsString());
    assertEquals("en_GB", locales[2].getAsString());
    assertEquals("en_US", locales[3].getAsString());
    assertEquals("en_US_POSIX", locales[4].getAsString());
    assertEquals("en_Latn", locales[5].getAsString());
    assertEquals("es_419", locales[6].getAsString());
    assertEquals("es_AR", locales[7].getAsString());
    assertEquals("es_ES", locales[8].getAsString());
    // Test equals against some non-GwtLocale class
    assertFalse(locales[0].equals(factory));
  }

  public void testDefault() {
    GwtLocale def1 = factory.getDefault();
    GwtLocale def2 = factory.fromString("default");
    assertSame(def1, def2);
    assertTrue(def1.isDefault());
    assertEquals("", def1.getLanguageNotNull());
    assertEquals("", def1.getScriptNotNull());
    assertEquals("", def1.getRegionNotNull());
    assertEquals("", def1.getVariantNotNull());
    GwtLocale def3 = factory.fromString(null);
    assertSame(def1, def3);
    GwtLocale def4 = factory.fromComponents("", "", "", "");
    assertSame(def1, def4);
  }

  public void testFromComponents() {
    GwtLocale test = factory.fromComponents("a", "b", "c", "d");
    assertEquals("a", test.getLanguage());
    assertEquals("B", test.getScript());
    assertEquals("C", test.getRegion());
    assertEquals("D", test.getVariant());
  }

  public void testFromString() {
    GwtLocale ha = factory.fromString("ha_Arab_NG");
    assertEquals("ha", ha.getLanguage());
    assertEquals("Arab", ha.getScript());
    assertEquals("NG", ha.getRegion());
    assertNull(ha.getVariant());
    GwtLocale us1 = factory.fromString("en-us");
    GwtLocale us2 = factory.fromString("en_US");
    assertSame(us1, us2);
    GwtLocale us3 = factory.fromString("EN_uS");
    assertSame(us1, us3);
    assertEquals("en", us3.getLanguageNotNull());
    assertNull(us3.getScript());
    assertEquals("US", us3.getRegionNotNull());
    assertNull(us3.getVariant());
    GwtLocale en = factory.fromString("en-VARIANT");
    assertEquals("en", en.getLanguage());
    assertNull(en.getScript());
    assertNull(en.getRegion());
    assertEquals("VARIANT", en.getVariantNotNull());
    en = factory.fromString("en_variant");
    assertEquals("en", en.getLanguage());
    assertNull(en.getScript());
    assertNull(en.getRegion());
    assertEquals("VARIANT", en.getVariant());
    GwtLocale zh = factory.fromString("zh-min-nan-Hant-CN");
    assertEquals("zh-min-nan", zh.getLanguage());
    assertEquals("Hant", zh.getScriptNotNull());
    assertEquals("CN", zh.getRegion());
    assertNull(zh.getVariant());
    assertFalse(ha.usesSameScript(zh));
    zh = factory.fromString("zh_min_nan_Hant_CN");
    assertEquals("zh-min-nan", zh.getLanguage());
    GwtLocale variant4 = factory.fromString("en-1234");
    assertNull(variant4.getScript());
    assertEquals("1234", variant4.getVariant());
    GwtLocale extLang = factory.fromString("en-ext");
    assertEquals("en-ext", extLang.getLanguage());
    try {
      factory.fromString("english_USA");
      fail("Should have thrown IllegalArgumentException on english_USA");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testInheritance() {
    GwtLocale en = factory.fromString("en_Latn_US_VARIANT");
    List<GwtLocale> chain = en.getInheritanceChain();
    assertEquals(12, chain.size());
    assertEquals(en, chain.get(0));
    assertEquals(factory.fromString("en_Latn_US"), chain.get(1));
    assertEquals(factory.fromString("en_Latn_021"), chain.get(2));
    assertEquals(factory.fromString("en_Latn_019"), chain.get(3));
    assertEquals(factory.fromString("en_Latn_001"), chain.get(4));
    assertEquals(factory.fromString("en_Latn"), chain.get(5));
    assertEquals(factory.fromString("en_US"), chain.get(6));
    assertEquals(factory.fromString("en_021"), chain.get(7));
    assertEquals(factory.fromString("en_019"), chain.get(8));
    assertEquals(factory.fromString("en_001"), chain.get(9));
    assertEquals(factory.fromString("en"), chain.get(10));
    assertEquals(factory.getDefault(), chain.get(11));
  }

  public void testInstanceCache() {
    GwtLocale a = factory.fromString("en_US");
    GwtLocale b = factory.fromString("en_US");
    assertFalse(a.isDefault());
    assertSame(a, b);
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }

  public void testPrivate() {
    GwtLocale priv = factory.fromString("x-mylang");
    assertEquals("x-mylang", priv.getLanguage());
    assertNull(priv.getScript());
    assertNull(priv.getRegion());
    assertNull(priv.getVariant());
    assertFalse(priv.isDefault());
  }
  public void testScriptAliases() {
    GwtLocale zhCN = factory.fromString("zh_CN");
    List<GwtLocale> aliases = zhCN.getAliases();
    assertEquals(2, aliases.size());
    assertEquals(aliases.get(0), zhCN);
    assertContainsAndGetPosition(aliases, factory.fromString("zh_Hans_CN"));
    GwtLocale zhHant = factory.fromString("zh_Hant");
    aliases = zhHant.getAliases();
    assertEquals(aliases.get(0), factory.fromString("zh_TW"));
    assertContainsAndGetPosition(aliases, factory.fromString("zh_Hant_TW"));
    GwtLocale zhHans = factory.fromString("zh_Hans");
    aliases = zhHans.getAliases();
    assertEquals(aliases.get(0), zhCN);
    assertContainsAndGetPosition(aliases, factory.fromString("zh_Hans_CN"));
    assertContainsAndGetPosition(aliases, zhHans);
    GwtLocale paPK = factory.fromString("pa_PK");
    GwtLocale paArabPK = factory.fromString("pa_Arab_PK");
    aliases = paPK.getAliases();
    assertContainsAndGetPosition(aliases, paArabPK);
  }

  public void testSearchList() {
    GwtLocale nbNO = factory.fromString("nb_NO");
    List<GwtLocale> searchList = nbNO.getCompleteSearchList();
    int idx_nb_154 = assertContainsAndGetPosition(searchList,
        factory.fromString("nb_154"));
    assertContainsAndGetPosition(searchList,
        factory.fromString("no_154_BOKMAL"));
    int idx_no = assertContainsAndGetPosition(searchList,
        factory.fromString("no_BOKMAL"));
    assertTrue("nb_154 should come before no_BOKMAL", idx_nb_154 < idx_no);
    int idx_default = assertContainsAndGetPosition(searchList,
        factory.getDefault());
    assertEquals(searchList.size() - 1, idx_default);
    GwtLocale esMX = factory.fromString("es_MX");
    searchList = esMX.getCompleteSearchList();
    assertContainsAndGetPosition(searchList, factory.fromString("es_013"));
    assertContainsAndGetPosition(searchList, factory.fromString("es_419"));
    GwtLocale srCyrlBA = factory.fromString("sr_Cyrl_BA");
    searchList = srCyrlBA.getCompleteSearchList();
    assertContainsAndGetPosition(searchList, factory.fromString("sr_Cyrl"));
    assertContainsAndGetPosition(searchList, factory.fromString("sr_BA"));
    assertContainsAndGetPosition(searchList, factory.fromString("sr"));
    assertContainsAndGetPosition(searchList, factory.getDefault());
    GwtLocale noNynorsk = factory.fromString("no_NYNORSK");
    searchList = noNynorsk.getCompleteSearchList();
    assertContainsAndGetPosition(searchList, factory.fromString("nn"));
    assertContainsAndGetPosition(searchList, factory.fromString("no"));
    GwtLocale zhTW = factory.fromString("zh_TW");
    searchList = zhTW.getCompleteSearchList();
    int hantPos = assertContainsAndGetPosition(searchList,
        factory.fromString("zh_Hant"));
    int zhPos = assertContainsAndGetPosition(searchList,
        factory.fromString("zh"));
    assertNotContains(searchList, factory.fromString("zh_Hans"));
    assertTrue("zh_Hant should appear before zh in zh_TW searchlist "
        + searchList, hantPos < zhPos);
    idx_default = assertContainsAndGetPosition(searchList,
        factory.getDefault());
    assertEquals(searchList.size() - 1, idx_default);
    GwtLocale pa = factory.fromString("pa");
    GwtLocale paPK = factory.fromString("pa_PK");
    GwtLocale paArab = factory.fromString("pa_Arab");
    GwtLocale paGuru = factory.fromString("pa_Guru");
    searchList = paPK.getCompleteSearchList();
    int arabPos = assertContainsAndGetPosition(searchList, paArab);
    int paPos = assertContainsAndGetPosition(searchList, pa);
    assertNotContains(searchList, paGuru);
    assertTrue("pa_Arab should appear before pa in pa_PK searchlist "
        + searchList, arabPos < paPos);
  }

  private <T> int assertContainsAndGetPosition(List<T> list, T value) {
    int idx = list.indexOf(value);
    assertTrue("List " + list + " should have contained " + value,
        idx >= 0);
    return idx;
  }

  private <T> void assertNotContains(List<T> list, T value) {
    assertFalse("List " + list + " should not have contained " + value,
        list.contains(value));
  }
}
