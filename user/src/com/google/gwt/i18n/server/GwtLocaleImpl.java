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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class representing GWT locales and conversion to/from other formats.
 * 
 * These locales correspond to BCP47.
 */
public class GwtLocaleImpl implements GwtLocale {
  // TODO(jat): implement a version of this suitable for client-side use,
  // probably using a generator to include only the information relevant to
  // the set of locales supported, and then figure out a way to get that into
  // the property provider to handle inheritance there.

  /**
   * Maps deprecated language codes to the canonical code.  Strings are always
   * in pairs, with the first being the canonical code and the second being
   * a deprecated code which maps to it.
   * <p>
   * Source: http://www.loc.gov/standards/iso639-2/php/code_changes.php
   * <p> 
   * TODO: consider building maps if this list grows much.
   */
  private static final String[] deprecatedLanguages = new String[] {
    "he", "iw",   // Hebrew
    "id", "in",   // Indonesian 
    "jv", "jw",   // Javanese, typo in original publication 
    "ro", "mo",   // Moldovian
    "yi", "ji",   // Yiddish
  };

  /**
   * Maps deprecated region codes to the canonical code.  Strings are always
   * in pairs, with the first being the canonical code and the second being
   * a deprecated code which maps to it.
   * 
   * Note that any mappings which split an old code into multiple new codes
   * cannot be done automatically (such as cs -> rs/me) -- perhaps we could
   * have a way of flagging region codes which are no longer valid and allow
   * an appropriate warning message.
   * <p>
   * Source: http://en.wikipedia.org/wiki/ISO_3166-1
   * <p>
   * TODO: consider building maps if this list grows much.
   */
  private static final String[] deprecatedRegions = new String[] {
    // East Timor - http://www.iso.org/iso/newsletter_v-5_east_timor.pdf
    "TL", "TP",
  };

  /**
   * Add in the missing locale of a deprecated pair.
   * 
   * @param factory GwtLocaleFactory to create new instances with
   * @param locale locale to add deprecated pair for
   * @param aliases where to store the alias if present
   */
  private static void addDeprecatedPairs(GwtLocaleFactory factory,
      GwtLocale locale, List<GwtLocale> aliases) {
    int n = deprecatedLanguages.length;
    for (int i = 0; i < n; i += 2) {
      if (deprecatedLanguages[i].equals(locale.getLanguage())) {
        aliases.add(factory.fromComponents(deprecatedLanguages[i + 1],
            locale.getScript(), locale.getRegion(), locale.getVariant()));
        break;
      }
      if (deprecatedLanguages[i + 1].equals(locale.getLanguage())) {
        aliases.add(factory.fromComponents(deprecatedLanguages[i],
            locale.getScript(), locale.getRegion(), locale.getVariant()));
        break;
      }
    }
  }

  private static void addImmediateParentRegions(GwtLocaleFactory factory,
      GwtLocale locale, Collection<GwtLocale> work) {
    String language = locale.getLanguage();
    String script = locale.getScript();
    String region = locale.getRegion();
    String variant = locale.getVariant();
    if (variant != null) {
      work.add(factory.fromComponents(language, script, region, null));
    }
    Set<String> immediateParents = RegionInheritance.getImmediateParents(region);
    for (String parent : immediateParents) {
      work.add(factory.fromComponents(language, script, parent, variant));
      if (variant != null) {
        work.add(factory.fromComponents(language, script, parent, null));
      }
    }
    if (immediateParents.isEmpty()) {
      work.add(factory.fromComponents(language, script, null, variant));
      if (variant != null) {
        work.add(factory.fromComponents(language, script, null, null));
      }
    }
    if (script != null) {
      work.add(factory.fromComponents(language, null, region, variant));
      if (variant != null) {
        work.add(factory.fromComponents(language, null, region, null));
      }
    }
  }

  /**
   * Add inherited regions for a given locale.
   * 
   * @param factory
   * @param inherits
   * @param language
   * @param script
   * @param region
   * @param variant
   */
  private static void addParentRegionLocales(GwtLocaleFactory factory,
      List<GwtLocale> inherits, String language, String script, String region,
      String variant) {
    for (String parent : RegionInheritance.getAllAncestors(region)) {
      inherits.add(factory.fromComponents(language, script, parent, variant));
    }
  }

  /**
   * Add special aliases for a given locale.
   *
   * This includes things like choosing the default region for Chinese based on
   * the script, handling Norwegian language changes, and treating pt_BR as the
   * default pt type.
   *
   * @param factory GwtLocaleFactory to create new instances with
   * @param locale locale to add deprecated pair for
   * @param aliases where to store the alias if present
   */
  private static void addSpecialAliases(GwtLocaleFactory factory,
      GwtLocale locale, String initialScript, List<GwtLocale> aliases) {
    String language = locale.getLanguage();
    String script = locale.getScript();
    String region = locale.getRegion();
    String variant = locale.getVariant();
    if ("zh".equals(language) && region == null
        && (initialScript == null || initialScript.equals(script))) {
      // Add aliases for main users of particular scripts, but only if the
      // script matches what was initially supplied.  This is to avoid cases
      // like zh_TW => zh => zh_CN, while still allowing zh => zh_CN.
      aliases.add(factory.fromComponents("zh", script,
          "Hant".equals(script) ? "TW" : "CN", variant));
    } else if ("no".equals(language)) {
      if (variant == null || "BOKMAL".equals(variant)) {
        aliases.add(factory.fromComponents("nb", script, region, null));
        aliases.add(factory.fromComponents("no-bok", script, region, null));
      } else if ("NYNORSK".equals(variant)) {
        aliases.add(factory.fromComponents("nn", script, region, null));
        aliases.add(factory.fromComponents("no-nyn", script, region, null));
      }
    } else if ("nb".equals(language)) {
      aliases.add(factory.fromComponents("no", script, region, "BOKMAL"));
    } else if ("nn".equals(language)) {
      aliases.add(factory.fromComponents("no", script, region, "NYNORSK"));
    } else if ("pt".equals(language)) {
      if (region == null) {
        aliases.add(factory.fromComponents("pt", script, "BR", variant));
      } else if ("BR".equals(region)) {
        aliases.add(factory.fromComponents("pt", script, null, variant));
      }
    }
  }

  private static boolean equalsNullCheck(String str1, String str2) {
    if (str1 == null) {
      return str2 == null;
    }
    return str1.equals(str2);
  }

  /**
   * Compare strings, accounting for nulls (which are treated as before any
   * other value).
   * 
   * @param a first string
   * @param b second string
   * 
   * @return positive if a>b, negative if a<b, 0 if equal
   */
  private static int stringCompare(String a, String b) {
    if (a == null) {
      return b == null ? 0 : -1;
    }
    if (b == null) {
      return 1;
    }
    return a.compareTo(b);
  }

  private final GwtLocaleFactory factory;

  private final String language;

  private final String region;

  private final String script;

  private final String variant;

  private final Object cacheLock = new Object[0];
  
  // protected by cacheLock
  private List<GwtLocale> cachedSearchList;

  // protected by cacheLock
  private List<GwtLocale> cachedAliases;

  /**
   * Must only be called from a factory to preserve instance caching.
   */
  GwtLocaleImpl(GwtLocaleFactory factory, String language, String region,
      String script, String variant) {
    this.factory = factory;
    this.language = language;
    this.region = region;
    this.script = script;
    this.variant = variant;
  }

  public int compareTo(GwtLocale o) {
    int c = stringCompare(language, o.getLanguage());
    if (c == 0) {
      c = stringCompare(script, o.getScript());
    }
    if (c == 0) {
      c = stringCompare(region, o.getRegion());
    }
    if (c == 0) {
      c = stringCompare(variant, o.getVariant());
    }
    return c;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof GwtLocale)) {
      return false;
    }
    GwtLocale other = (GwtLocale) obj;
    return equalsNullCheck(language, other.getLanguage())
        && equalsNullCheck(region, other.getRegion())
        && equalsNullCheck(script, other.getScript())
        && equalsNullCheck(variant, other.getVariant());
  }

  public List<GwtLocale> getAliases() {
    // TODO(jat): more locale aliases? better way to encode them?
    synchronized (cacheLock) {
      if (cachedAliases == null) {
        cachedAliases = new ArrayList<GwtLocale>();
        GwtLocale canonicalForm = getCanonicalForm();
        Set<GwtLocale> seen = new HashSet<GwtLocale>();
        cachedAliases.add(canonicalForm);
        ArrayList<GwtLocale> nextGroup = new ArrayList<GwtLocale>();
        nextGroup.add(this);
        // Account for default script
        String defaultScript = DefaultLanguageScripts.getDefaultScript(language,
            region);
        if (defaultScript != null) {
          if (script == null) {
            nextGroup.add(factory.fromComponents(language, defaultScript,
                region, variant));
          } else if (script.equals(defaultScript)) {
            nextGroup.add(factory.fromComponents(language, null, region,
                variant));
          }
        }
        String initialScript = script == null ? defaultScript : script;
        while (!nextGroup.isEmpty()) {
          List<GwtLocale> thisGroup = nextGroup;
          nextGroup = new ArrayList<GwtLocale>();
          for (GwtLocale locale : thisGroup) {
            if (seen.contains(locale)) {
              continue;
            }
            seen.add(locale);
            if (!locale.equals(canonicalForm)) {
              cachedAliases.add(locale);
            }
            addDeprecatedPairs(factory, locale, nextGroup);
            addSpecialAliases(factory, locale, initialScript, nextGroup);
          }
        }
        cachedAliases = Collections.unmodifiableList(cachedAliases);
      }
      return cachedAliases;    
    }
  }

  public String getAsString() {
    StringBuilder buf = new StringBuilder();
    if (language != null) {
      buf.append(language);
    }
    if (script != null) {
      buf.append('_');
      buf.append(script);
    }
    if (region != null) {
      buf.append('_');
      buf.append(region);
    }
    if (variant != null) {
      buf.append('_');
      buf.append(variant);
    }
    return buf.toString();
  }

  /**
   * Returns this locale in canonical form.  Changes for canonical form are:
   * <ul>
   *  <li>Deprecated language/region tags are replaced with official versions
   * </ul>
   * 
   * @return GwtLocale instance 
   */
  public GwtLocale getCanonicalForm() {
    String canonLanguage = language;
    String canonScript = script;
    String canonRegion = region;
    String canonVariant = variant;
    // Handle deprecated language codes
    int n = deprecatedLanguages.length;
    for (int i = 0; i < n; i += 2) {
      if (deprecatedLanguages[i + 1].equals(canonLanguage)) {
        canonLanguage = deprecatedLanguages[i];
        break;
      }
    }
    // Handle deprecated region codes
    n = deprecatedRegions.length;
    for (int i = 0; i < n; i += 2) {
      if (deprecatedRegions[i + 1].equals(canonRegion)) {
        canonRegion = deprecatedRegions[i];
        break;
      }
    }
    // Special-case Chinese default scripts
    if ("zh".equals(canonLanguage)) {
      if (canonRegion != null) {
        if ("CN".equals(canonRegion) && "Hans".equals(canonScript)) {
          canonScript = null;
        } else if ("TW".equals(canonRegion) && "Hant".equals(canonScript)) {
          canonScript = null;
        }
      } else if ("Hans".equals(canonScript)) {
        canonRegion = "CN";
        canonScript = null;
      } else if ("Hant".equals(canonScript)) {
        canonRegion = "TW";
        canonScript = null;
      }
    }
    // Special-case no->nb/nn split
    if ("no-bok".equals(canonLanguage)) {
      canonLanguage = "nb";
      canonVariant = null;
    } else if ("no-nyn".equals(canonLanguage)) {
      canonLanguage = "nn";
      canonVariant = null;
    } else if ("no".equals(canonLanguage)) {
      if (canonVariant == null || "BOKMAL".equals(canonVariant)) {
        canonLanguage = "nb";
        canonVariant = null;
      } else if ("NYNORSK".equals(canonVariant)) {
        canonLanguage = "nn";
        canonVariant = null;
      }
    }
    // Remove any default script for the language
    if (canonScript != null && canonScript.equals(
        DefaultLanguageScripts.getDefaultScript(canonLanguage, canonRegion))) {
      canonScript = null;
    }
    return factory.fromComponents(canonLanguage, canonScript, canonRegion,
        canonVariant);
  }

  public List<GwtLocale> getCompleteSearchList() {
    // TODO(jat): base order on distance from the initial locale, such as the
    // draft proposal at:
    //   http://cldr.unicode.org/development/design-proposals/languagedistance
    // This will ensure that zh_Hant will come before zh in the search list for
    // zh_TW, and pa_Arab to come before pa in the search list for pa_PK.
    synchronized (cacheLock) {
      if (cachedSearchList == null) {
        cachedSearchList = new ArrayList<GwtLocale>();
        Set<GwtLocale> seen = new HashSet<GwtLocale>();
        String initialScript = script;
        if (initialScript == null) {
          initialScript = DefaultLanguageScripts.getDefaultScript(language,
              region);
        }
        List<GwtLocale> thisGroup = new ArrayList<GwtLocale>();
        if (initialScript != null) {
          // Make sure the default script is listed first in the search list,
          // which ensures that zh_Hant appears before zh in the search list for
          // zh_TW.
          thisGroup.add(factory.fromComponents(language, initialScript, region,
              variant));
        }
        thisGroup.add(this);
        seen.addAll(thisGroup);
        GwtLocale defLocale = factory.getDefault();
        seen.add(defLocale);
        while (!thisGroup.isEmpty()) {
          cachedSearchList.addAll(thisGroup);
          List<GwtLocale> nextGroup = new ArrayList<GwtLocale>();
          for (GwtLocale locale : thisGroup) {
            List<GwtLocale> aliases = locale.getAliases();
            aliases = filterBadScripts(aliases, initialScript);
            List<GwtLocale> work = new ArrayList<GwtLocale>(aliases);
            work.removeAll(seen);
            nextGroup.addAll(work);
            seen.addAll(work);
            work.clear();
            if (locale.getRegion() != null) {
              addImmediateParentRegions(factory, locale, work);
            } else if (locale.getVariant() != null) {
              work.add(factory.fromComponents(locale.getLanguage(),
                  locale.getScript(), null, null));
            } else if (locale.getScript() != null) {
              work.add(factory.fromComponents(locale.getLanguage(), null, null,
                  null));
            }
            work.removeAll(seen);
            nextGroup.addAll(work);
            seen.addAll(work);
          }
          thisGroup = nextGroup;
        }
        if (!isDefault()) {
          cachedSearchList.add(defLocale);
        }
        cachedSearchList = Collections.unmodifiableList(cachedSearchList);
      }
      return cachedSearchList;
    }
  }

  /**
   * Return a list of locales to search for, in order of preference.  The
   * current locale is always first on the list.  Aliases are not included
   * in the list -- use {@link #getAliases} to expand those.
   * 
   * @return inheritance list
   */
  public List<GwtLocale> getInheritanceChain() {
    List<GwtLocale> inherits = new ArrayList<GwtLocale>();
    inherits.add(this);
    if (variant != null) {
      inherits.add(factory.fromComponents(language, script, region, null));
    }
    if (region != null) {
      addParentRegionLocales(factory, inherits, language, script, region,
          null);
      inherits.add(factory.fromComponents(language, script, null, null));
    }
    if (script != null) {
      inherits.add(factory.fromComponents(language, null, region, null));
      addParentRegionLocales(factory, inherits, language, null, region, null);
      if (region != null) {
        inherits.add(factory.fromComponents(language, null, null, null));
      }
    }
    if (language != null) {
      inherits.add(factory.fromComponents(null, null, null, null));
    }
    return inherits;
  }

  public String getLanguage() {
    return language;
  }

  public String getLanguageNotNull() {
    return language == null ? "" : language;
  }

  public String getRegion() {
    return region;
  }

  public String getRegionNotNull() {
    return region == null ? "" : region;
  }

  public String getScript() {
    return script;
  }

  public String getScriptNotNull() {
    return script == null ? "" : script;
  }

  public String getVariant() {
    return variant;
  }

  public String getVariantNotNull() {
    return variant == null ? "" : variant;
  }

  @Override
  public int hashCode() {
    int result = ((language == null) ? 0 : language.hashCode());
    result = 37 * result + ((region == null) ? 0 : region.hashCode());
    result = 43 * result + ((script == null) ? 0 : script.hashCode());
    result = 53 * result + ((variant == null) ? 0 : variant.hashCode());
    return result;
  }

  /**
   * Return true if this locale inherits from the specified locale.  Note that
   * locale.inheritsFrom(locale) is false -- if you want that to be true, you
   * should just use locale.getInheritanceChain().contains(x).
   * 
   * @param parent locale to test against
   * @return true if parent is an ancestor of this locale
   */
  public boolean inheritsFrom(GwtLocale parent) {
    if (equals(parent)) {
      return false;
    }
    return getInheritanceChain().contains(parent);
  }

  public boolean isDefault() {
    return language == null;
  }

  @Override
  public String toString() {
    if (language == null) {
      return DEFAULT_LOCALE;
    }
    return getAsString();
  }

  /**
   * Checks if this locale uses the same script as another locale, taking into
   * account default scripts.
   * 
   * @param other
   * @return true if the scripts are the same
   */
  public boolean usesSameScript(GwtLocale other) {
    String myScript = script != null ? script : DefaultLanguageScripts.getDefaultScript(language,
        region);
    String otherScript = other.getScript() != null ? other.getScript()
        : DefaultLanguageScripts.getDefaultScript(other.getLanguage(), other.getRegion());
    // two locales with an unspecified script and no default for the language
    // match only if the language is the same
    if (myScript == null) {
      return equalsNullCheck(language, other.getLanguage());
    } else {
      return myScript.equals(otherScript);
    }
  }

  /**
   * Remove aliases which are incompatible with the initial script provided or
   * defaulted in a locale.
   * 
   * @param aliases
   * @param initialScript
   * @return copy of aliases with incompatible scripts removed
   */
  private List<GwtLocale> filterBadScripts(List<GwtLocale> aliases,
      String initialScript) {
    if (initialScript == null) {
      return aliases;
    }
    List<GwtLocale> result = new ArrayList<GwtLocale>();
    for (GwtLocale alias : aliases) {
      String aliasScript = alias.getScript();
      if (aliasScript == null || aliasScript.equals(initialScript)) {
        result.add(alias);
      }
    }
    return result;
  }
}
