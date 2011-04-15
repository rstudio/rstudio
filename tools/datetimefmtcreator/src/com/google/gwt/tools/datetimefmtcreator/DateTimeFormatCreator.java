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
package com.google.gwt.tools.datetimefmtcreator;

import com.google.gwt.i18n.client.constants.DateTimeConstantsImpl;
import com.google.gwt.i18n.rebind.LocaleUtils;
import com.google.gwt.i18n.shared.GwtLocale;
import com.google.gwt.i18n.shared.GwtLocaleFactory;

import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.util.ULocale;

import org.apache.tapestry.util.text.LocalizedProperties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Pattern;

/**
 * Generate implementations of DateTimeFormatInfoImpl for all the supported
 * locales.
 */
public class DateTimeFormatCreator {

  private static class DtfiGenerator {

    private static void buildPatterns(GwtLocale locale, TreeMap<Key, String[]> properties) {
      ULocale ulocale = new ULocale(ULocale.canonicalize(locale.getAsString()));
      DateTimePatternGenerator dtpg = DateTimePatternGenerator.getInstance(ulocale);
      for (Map.Entry<String, String> entry : patterns.entrySet()) {
        properties.put(new Key(locale, "format" + entry.getKey()), new String[] {dtpg
            .getBestPattern(entry.getValue())});
      }
    }

    private static GwtLocale findEarliestAncestor(GwtLocale locale, Set<GwtLocale> set) {
      if (set == null) {
        return null;
      }
      for (GwtLocale search : locale.getInheritanceChain()) {
        if (set.contains(search)) {
          return search;
        }
      }
      return null;
    }

    private static String quote(String value) {
      return value.replaceAll("\"", "\\\\\"");
    }

    private static String[] split(String target) {
      // We add an artificial end character to avoid the odd split() behavior
      // that drops the last item if it is only whitespace.
      target = target + "~";

      // Do not split on escaped commas.
      String[] args = target.split("(?<![\\\\]),");

      // Now remove the artificial ending we added above.
      // We have to do it before we escape and trim because otherwise
      // the artificial trailing '~' would prevent the last item from being
      // properly trimmed.
      if (args.length > 0) {
        int last = args.length - 1;
        args[last] = args[last].substring(0, args[last].length() - 1);
      }

      for (int i = 0; i < args.length; i++) {
        args[i] = args[i].replaceAll("\\\\,", ",").trim();
      }
      return args;
    }

    private File propDir;

    private File src;

    public DtfiGenerator(File src) {
      this.src = src;
      String packageName = DateTimeConstantsImpl.class.getPackage().getName();
      propDir = new File(src, packageName.replaceAll("\\.", "/"));
      if (!propDir.exists()) {
        System.err.println("Can't find directory for " + packageName);
        return;
      }
    }

    public void generate() throws FileNotFoundException, IOException {
      final Pattern dtcProps = Pattern.compile("DateTimeConstantsImpl(.*)\\.properties");
      String[] propFiles = propDir.list(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          return dtcProps.matcher(name).matches();
        }
      });
      TreeMap<Key, String[]> properties = new TreeMap<Key, String[]>();
      GwtLocaleFactory factory = LocaleUtils.getLocaleFactory();
      collectPropertyData(propFiles, properties, factory);
      Map<GwtLocale, Set<GwtLocale>> parents = removeInheritedValues(properties);
      generateSources(properties, parents);
    }

    private void addLocaleParent(Map<GwtLocale, Set<GwtLocale>> parents, GwtLocale keyLocale,
        GwtLocale parentLocale) {
      Set<GwtLocale> parentSet = parents.get(keyLocale);
      if (parentSet == null) {
        parentSet = new HashSet<GwtLocale>();
        parents.put(keyLocale, parentSet);
      }
      parentSet.add(parentLocale);
    }

    @SuppressWarnings("unchecked")
    private void collectPropertyData(String[] propFiles, TreeMap<Key, String[]> properties,
        GwtLocaleFactory factory) throws FileNotFoundException, IOException {
      for (String propFile : propFiles) {
        if (!propFile.startsWith("DateTimeConstantsImpl") || !propFile.endsWith(".properties")) {
          continue;
        }
        int len = propFile.length();
        String suffix = propFile.substring(21, len - 11);
        if (suffix.startsWith("_")) {
          suffix = suffix.substring(1);
        }
        GwtLocale locale = factory.fromString(suffix).getCanonicalForm();
        File f = new File(propDir, propFile);
        FileInputStream str = null;
        try {
          str = new FileInputStream(f);
          LocalizedProperties props = new LocalizedProperties();
          props.load(str);
          Map<String, String> map = props.getPropertyMap();
          for (Map.Entry<String, String> entry : map.entrySet()) {
            String[] value = split(entry.getValue());
            if ("dateFormats".equals(entry.getKey()) || "timeFormats".equals(entry.getKey())
                || "weekendRange".equals(entry.getKey())) {
              // split these out into separate fields
              for (int i = 0; i < value.length; ++i) {
                Key key = new Key(locale, entry.getKey() + i);
                properties.put(key, new String[] {value[i]});
              }
            } else {
              Key key = new Key(locale, entry.getKey());
              properties.put(key, value);
            }
          }
          buildPatterns(locale, properties);
        } finally {
          if (str != null) {
            str.close();
          }
        }
      }
    }

    private PrintWriter createClassSource(String packageName, String className)
        throws FileNotFoundException {
      String path = packageName.replace('.', '/') + "/" + className + ".java";
      File f = new File(src, path);
      FileOutputStream ostr = new FileOutputStream(f);
      PrintWriter out = new PrintWriter(ostr);
      out.println("/*");
      out.println(" * Copyright 2010 Google Inc.");
      out.println(" * ");
      out.println(" * Licensed under the Apache License, Version 2.0 (the \"License\"); you may not");
      out.println(" * use this file except in compliance with the License. You may obtain a copy of");
      out.println(" * the License at");
      out.println(" * ");
      out.println(" * http://www.apache.org/licenses/LICENSE-2.0");
      out.println(" *");
      out.println(" * Unless required by applicable law or agreed to in writing, software");
      out.println(" * distributed under the License is distributed on an \"AS IS\" BASIS, WITHOUT");
      out.println(" * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the");
      out.println(" * License for the specific language governing permissions and limitations under");
      out.println(" * the License.");
      out.println(" */");
      out.println("package " + packageName + ";");
      out.println();
      out.println("// DO NOT EDIT - GENERATED FROM CLDR DATA");
      out.println();
      return out;
    }

    private void generateAlias(GwtLocale locale, GwtLocale parent) throws IOException {
      System.out.println("Generating alias " + locale);
      String suffix;
      if (parent.isDefault()) {
        suffix = "";
      } else {
        suffix = "_" + parent.getAsString();
      }
      String packageName = "com.google.gwt.i18n.client.impl.cldr";
      String className = "DateTimeFormatInfoImpl_" + locale.getAsString();
      PrintWriter out = null;
      try {
        out = createClassSource(packageName, className);
        out.println("/**");
        out.println(" * Locale \"" + locale + "\" is an alias for \"" + parent + "\".");
        out.println(" */");
        out.println("public class " + className + " extends DateTimeFormatInfoImpl" + suffix + " {");
        out.println("}");
      } finally {
        if (out != null) {
          out.close();
        }
      }

    }

    private void generateLocale(GwtLocale locale, GwtLocale parent, Map<String, String[]> values)
        throws IOException {
      System.out.println("Generating locale " + locale);
      boolean addOverrides = true;
      PrintWriter out = null;
      try {
        if (locale.isDefault()) {
          String packageName = "com.google.gwt.i18n.client";
          String className = "DefaultDateTimeFormatInfo";
          out = createClassSource(packageName, className);
          out.println("/**");
          out.println(" * Default implementation of DateTimeFormatInfo interface, using values from");
          out.println(" * the CLDR root locale.");
          out.println(" * <p>");
          out.println(" * Users who need to create their own DateTimeFormatInfo implementation are");
          out.println(" * encouraged to extend this class so their implementation won't break when");
          out.println(" * new methods are added.");
          out.println(" */");
          out.println("public class DefaultDateTimeFormatInfo implements DateTimeFormatInfo {");
          addOverrides = false;
        } else {
          String suffix;
          if (parent.isDefault()) {
            suffix = "";
          } else {
            suffix = "_" + parent.getAsString();
          }
          String packageName = "com.google.gwt.i18n.client.impl.cldr";
          String className = "DateTimeFormatInfoImpl_" + locale.getAsString();
          out = createClassSource(packageName, className);
          out.println("/**");
          out.println(" * Implementation of DateTimeFormatInfo for locale \"" + locale + "\".");
          out.println(" */");
          out.println("public class " + className + " extends DateTimeFormatInfoImpl" + suffix
              + " {");
        }
        Set<String> keySet = values.keySet();
        String[] keys = keySet.toArray(new String[keySet.size()]);
        Arrays.sort(keys, new Comparator<String>() {
          public int compare(String a, String b) {
            String mappedA = a;
            String mappedB = b;
            FieldMapping field = fieldMap.get(a);
            if (field != null) {
              mappedA = field.methodName;
            }
            field = fieldMap.get(b);
            if (field != null) {
              mappedB = field.methodName;
            }
            return mappedA.compareTo(mappedB);
          }
        });
        for (String key : keys) {
          String[] value = values.get(key);
          FieldMapping mapping = fieldMap.get(key);
          Class<?> type = value.length > 1 ? String[].class : String.class;
          String related = null;
          String name = key;
          if (mapping != null) {
            name = mapping.methodName;
            type = mapping.type;
            related = mapping.related;
          }
          String[] relatedValue = values.get(related);
          String relayMethod = null;
          if (Arrays.equals(value, relatedValue)) {
            relayMethod = fieldMap.get(related).methodName;
          }
          out.println();
          if (addOverrides) {
            out.println("  @Override");
          }
          out.println("  public " + type.getSimpleName() + " " + name + "() {");
          out.print("    return ");
          if (relayMethod != null) {
            out.println(relayMethod + "();");
          } else {
            if (type.isArray()) {
              out.println("new " + type.getSimpleName() + " { ");
              out.print("        ");
            }
            boolean first = true;
            for (String oneValue : value) {
              if (!first) {
                out.println(",");
                out.print("        ");
              }
              if (type == int.class || type == int[].class) {
                out.print(Integer.valueOf(oneValue) - 1);
              } else {
                out.print("\"" + quote(oneValue) + "\"");
              }
              first = false;
            }
            if (type.isArray()) {
              out.println();
              out.println("    };");
            } else {
              out.println(";");
            }
          }
          out.println("  }");
        }
        if (locale.isDefault()) {
          // TODO(jat): actually generate these from CLDR data
          out.println();
          out.println("  public String dateFormat() {");
          out.println("    return dateFormatMedium();");
          out.println("  }");
          out.println();
          out.println("  public String dateTime(String timePattern, String datePattern) {");
          out.println("    return datePattern + \" \" + timePattern;");
          out.println("  }");
          out.println();
          out.println("  public String dateTimeFull(String timePattern, String datePattern) {");
          out.println("    return dateTime(timePattern, datePattern);");
          out.println("  }");
          out.println();
          out.println("  public String dateTimeLong(String timePattern, String datePattern) {");
          out.println("    return dateTime(timePattern, datePattern);");
          out.println("  }");
          out.println();
          out.println("  public String dateTimeMedium(String timePattern, String datePattern) {");
          out.println("    return dateTime(timePattern, datePattern);");
          out.println("  }");
          out.println();
          out.println("  public String dateTimeShort(String timePattern, String datePattern) {");
          out.println("    return dateTime(timePattern, datePattern);");
          out.println("  }");
          out.println();
          out.println("  public String timeFormat() {");
          out.println("    return timeFormatMedium();");
          out.println("  }");
        }
        out.println("}");
      } finally {
        if (out != null) {
          out.close();
        }
      }
    }

    private void generateSources(TreeMap<Key, String[]> properties,
        Map<GwtLocale, Set<GwtLocale>> parents) throws IOException {
      Set<GwtLocale> locales = new HashSet<GwtLocale>();
      // process sorted locales/keys, generating each locale on change
      GwtLocale lastLocale = null;
      Map<String, String[]> thisLocale = new HashMap<String, String[]>();
      for (Entry<Key, String[]> entry : properties.entrySet()) {
        if (lastLocale != null && lastLocale != entry.getKey().locale) {
          GwtLocale parent = findEarliestAncestor(lastLocale, parents.get(lastLocale));
          generateLocale(lastLocale, parent, thisLocale);
          thisLocale.clear();
          lastLocale = null;
        }
        if (lastLocale == null) {
          lastLocale = entry.getKey().locale;
          locales.add(lastLocale);
        }
        thisLocale.put(entry.getKey().key, entry.getValue());
      }
      if (lastLocale != null) {
        GwtLocale parent = findEarliestAncestor(lastLocale, parents.get(lastLocale));
        generateLocale(lastLocale, parent, thisLocale);
      }
      Set<GwtLocale> seen = new HashSet<GwtLocale>(locales);
      for (GwtLocale locale : locales) {
        for (GwtLocale alias : locale.getAliases()) {
          if (!seen.contains(alias)) {
            seen.add(alias);
            // generateAlias(alias, locale);
          }
        }
      }
    }

    /**
     * Check if a given entry within a locale is inherited from a parent.
     * 
     * @param properties
     * @param parents
     * @param key
     * @param value
     * @return true if the value is the same as the first parent defining that
     *         value
     */
    private boolean isInherited(TreeMap<Key, String[]> properties,
        Map<GwtLocale, Set<GwtLocale>> parents, Key key, String[] value) {
      GwtLocale keyLocale = key.locale;
      if (keyLocale.isDefault()) {
        // never delete entries from default
        return false;
      }
      List<GwtLocale> list = keyLocale.getInheritanceChain();
      String[] parent = null;
      for (int i = 1; i < list.size(); ++i) {
        Key parentKey = new Key(list.get(i), key.key);
        parent = properties.get(parentKey);
        if (parent != null) {
          GwtLocale parentLocale = parentKey.locale;
          addLocaleParent(parents, keyLocale, parentLocale);
          break;
        }
      }
      return Arrays.equals(value, parent);
    }

    /**
     * Remove inherited values and return a map of inherited-from locales for
     * each locale.
     * 
     * @param properties
     * @return inheritance map
     */
    private Map<GwtLocale, Set<GwtLocale>> removeInheritedValues(TreeMap<Key, String[]> properties) {
      // remove entries identical to a parent locale
      Map<GwtLocale, Set<GwtLocale>> parents = new HashMap<GwtLocale, Set<GwtLocale>>();
      Set<Entry<Key, String[]>> entrySet = properties.entrySet();
      Iterator<Entry<Key, String[]>> it = entrySet.iterator();
      while (it.hasNext()) {
        Entry<Key, String[]> entry = it.next();
        if (isInherited(properties, parents, entry.getKey(), entry.getValue())) {
          it.remove();
        }
      }
      return parents;
    }
  }

  private static class FieldMapping {
    public final String methodName;
    public final Class<?> type;
    public final String related;

    public FieldMapping(String methodName, Class<?> type) {
      this(methodName, type, null);
    }

    public FieldMapping(String methodName, Class<?> type, String related) {
      this.methodName = methodName;
      this.type = type;
      this.related = related;
    }
  }

  private static class Key implements Comparable<Key> {
    public final GwtLocale locale;
    public final String key;

    public Key(GwtLocale locale, String key) {
      this.locale = locale;
      this.key = key;
    }

    public int compareTo(Key other) {
      int c = locale.compareTo(other.locale);
      if (c == 0) {
        c = key.compareTo(other.key);
      }
      return c;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      Key other = (Key) obj;
      return locale.equals(other.locale) && key.equals(other.key);
    }

    @Override
    public int hashCode() {
      return locale.hashCode() * 31 + key.hashCode();
    }

    @Override
    public String toString() {
      return locale.toString() + "/" + key;
    }
  }

  private static final Map<String, FieldMapping> fieldMap = new HashMap<String, FieldMapping>();

  private static Map<String, String> patterns = new HashMap<String, String>();

  static {
    fieldMap.put("ampms", new FieldMapping("ampms", String[].class));
    fieldMap.put("dateFormats0", new FieldMapping("dateFormatFull", String.class));
    fieldMap.put("dateFormats1", new FieldMapping("dateFormatLong", String.class));
    fieldMap.put("dateFormats2", new FieldMapping("dateFormatMedium", String.class));
    fieldMap.put("dateFormats3", new FieldMapping("dateFormatShort", String.class));
    fieldMap.put("timeFormats0", new FieldMapping("timeFormatFull", String.class));
    fieldMap.put("timeFormats1", new FieldMapping("timeFormatLong", String.class));
    fieldMap.put("timeFormats2", new FieldMapping("timeFormatMedium", String.class));
    fieldMap.put("timeFormats3", new FieldMapping("timeFormatShort", String.class));
    fieldMap.put("eraNames", new FieldMapping("erasFull", String[].class));
    fieldMap.put("eras", new FieldMapping("erasShort", String[].class));
    fieldMap.put("quarters", new FieldMapping("quartersFull", String[].class));
    fieldMap.put("shortQuarters", new FieldMapping("quartersShort", String[].class));
    fieldMap.put("firstDayOfTheWeek", new FieldMapping("firstDayOfTheWeek", Integer.class));
    fieldMap.put("months", new FieldMapping("monthsFull", String[].class));
    fieldMap.put("standaloneMonths", new FieldMapping("monthsFullStandalone", String[].class,
        "months"));
    fieldMap.put("narrowMonths", new FieldMapping("monthsNarrow", String[].class));
    fieldMap.put("standaloneNarrowMonths", new FieldMapping("monthsNarrowStandalone",
        String[].class, "narrowMonths"));
    fieldMap.put("shortMonths", new FieldMapping("monthsShort", String[].class));
    fieldMap.put("standaloneShortMonths", new FieldMapping("monthsShortStandalone", String[].class,
        "shortMonths"));
    fieldMap.put("weekendRange0", new FieldMapping("weekendStart", int.class));
    fieldMap.put("weekendRange1", new FieldMapping("weekendEnd", int.class));
    fieldMap.put("firstDayOfTheWeek", new FieldMapping("firstDayOfTheWeek", int.class));
    fieldMap.put("weekdays", new FieldMapping("weekdaysFull", String[].class));
    fieldMap.put("standaloneWeekdays", new FieldMapping("weekdaysFullStandalone", String[].class,
        "weekdays"));
    fieldMap.put("shortWeekdays", new FieldMapping("weekdaysShort", String[].class));
    fieldMap.put("standaloneShortWeekdays", new FieldMapping("weekdaysShortStandalone",
        String[].class, "shortWeekdays"));
    fieldMap.put("narrowWeekdays", new FieldMapping("weekdaysNarrow", String[].class));
    fieldMap.put("standaloneNarrowWeekdays", new FieldMapping("weekdaysNarrowStandalone",
        String[].class, "narrowWeekdays"));

    // patterns to use with DateTimePatternGenerator
    patterns.put("Day", "d");
    patterns.put("Hour12Minute", "hmm");
    patterns.put("Hour12MinuteSecond", "hmmss");
    patterns.put("Hour24Minute", "Hmm");
    patterns.put("Hour24MinuteSecond", "Hmmss");
    patterns.put("MinuteSecond", "mss");
    patterns.put("MonthAbbrev", "MMM");
    patterns.put("MonthAbbrevDay", "MMMd");
    patterns.put("MonthFull", "MMMM");
    patterns.put("MonthFullDay", "MMMMd");
    patterns.put("MonthFullWeekdayDay", "MMMMEEEEd");
    patterns.put("MonthNumDay", "Md");
    patterns.put("Year", "y");
    patterns.put("YearMonthAbbrev", "yMMM");
    patterns.put("YearMonthAbbrevDay", "yMMMd");
    patterns.put("YearMonthFull", "yMMMM");
    patterns.put("YearMonthFullDay", "yMMMMd");
    patterns.put("YearMonthNum", "yM");
    patterns.put("YearMonthNumDay", "yMd");
    patterns.put("YearMonthWeekdayDay", "yMMMEEEd");
    patterns.put("YearQuarterFull", "yQQQQ");
    patterns.put("YearQuarterShort", "yQ");
  }

  /**
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      System.err.println("Usage: " + DateTimeFormatCreator.class.getSimpleName() + " gwt-root-dir");
      return;
    }
    File gwt = new File(args[0]);
    File src = new File(gwt, "user/src");
    if (!gwt.exists() || !src.exists()) {
      System.err.println(args[0] + " doesn't appear to be a GWT root directory");
      return;
    }
    new DtfiGenerator(src).generate();
  }
}
