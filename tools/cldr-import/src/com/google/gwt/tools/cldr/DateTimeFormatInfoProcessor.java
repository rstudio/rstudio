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
package com.google.gwt.tools.cldr;

import com.google.gwt.codegen.server.StringGenerator;
import com.google.gwt.i18n.rebind.DateTimePatternGenerator;
import com.google.gwt.i18n.server.MessageFormatUtils.ArgumentChunk;
import com.google.gwt.i18n.server.MessageFormatUtils.DefaultTemplateChunkVisitor;
import com.google.gwt.i18n.server.MessageFormatUtils.MessageStyle;
import com.google.gwt.i18n.server.MessageFormatUtils.StringChunk;
import com.google.gwt.i18n.server.MessageFormatUtils.TemplateChunk;
import com.google.gwt.i18n.server.MessageFormatUtils.VisitorAbortException;
import com.google.gwt.i18n.shared.DateTimeFormatInfo;
import com.google.gwt.i18n.shared.GwtLocale;
import com.google.gwt.i18n.shared.impl.cldr.DateTimeFormatInfoImpl;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Loads data needed to produce DateTimeFormatInfo implementations.
 */
public class DateTimeFormatInfoProcessor extends Processor {

  private static final String[] DAYS = new String[] {
      "sun", "mon", "tue", "wed", "thu", "fri", "sat"};

  /**
   * Map of skeleton format patterns and the method name suffix that uses them.
   */
  private static final Map<String, String> FORMATS;

  /**
   * Index of the formats, ordered by the method name.
   */
  private static final SortedMap<String, String> FORMAT_BY_METHOD;

  static {
    FORMATS = new HashMap<String, String>();
    FORMATS.put("d", "Day");
    FORMATS.put("hmm", "Hour12Minute");
    FORMATS.put("hmmss", "Hour12MinuteSecond");
    FORMATS.put("Hmm", "Hour24Minute");
    FORMATS.put("Hmmss", "Hour24MinuteSecond");
    FORMATS.put("mss", "MinuteSecond");
    FORMATS.put("MMM", "MonthAbbrev");
    FORMATS.put("MMMd", "MonthAbbrevDay");
    FORMATS.put("MMMM", "MonthFull");
    FORMATS.put("MMMMd", "MonthFullDay");
    FORMATS.put("MMMMEEEEd", "MonthFullWeekdayDay");
    FORMATS.put("Md", "MonthNumDay");
    FORMATS.put("y", "Year");
    FORMATS.put("yMMM", "YearMonthAbbrev");
    FORMATS.put("yMMMd", "YearMonthAbbrevDay");
    FORMATS.put("yMMMM", "YearMonthFull");
    FORMATS.put("yMMMMd", "YearMonthFullDay");
    FORMATS.put("yM", "YearMonthNum");
    FORMATS.put("yMd", "YearMonthNumDay");
    FORMATS.put("yMMMEEEd", "YearMonthWeekdayDay");
    FORMATS.put("yQQQQ", "YearQuarterFull");
    FORMATS.put("yQ", "YearQuarterShort");

    FORMAT_BY_METHOD = new TreeMap<String, String>();
    for (Map.Entry<String, String> entry : FORMATS.entrySet()) {
      FORMAT_BY_METHOD.put(entry.getValue(), entry.getKey());
    }
  }

  /**
   * Convert the unlocalized name of a day ("sun".."sat") into a day number of
   * the week, ie 0-6.
   * 
   * @param day abbreviated, unlocalized name of the day ("sun".."sat")
   * @return the day number, 0-6
   * @throws IllegalArgumentException if the day name is not found
   */
  private static int getDayNumber(String day) {
    for (int i = 0; i < DAYS.length; ++i) {
      if (DAYS[i].equals(day)) {
        return i;
      }
    }
    throw new IllegalArgumentException();
  }

  private final RegionLanguageData regionLanguageData;

  public DateTimeFormatInfoProcessor(File outputDir, Factory cldrFactory, LocaleData localeData) {
    super(outputDir, cldrFactory, localeData);
    regionLanguageData = new RegionLanguageData(cldrFactory);
  }

  @Override
  protected void cleanupData() {
    System.out.println("Removing duplicates from date/time formats");
    localeData.copyLocaleData("en", "default", "era-wide", "era-abbrev", "quarter-wide",
        "quarter-abbrev", "day-wide", "day-sa-wide", "day-narrow", "day-sa-narrow", "day-abbrev",
        "day-sa-abbrev", "month-wide", "month-sa-wide", "month-narrow", "month-sa-narrow",
        "month-abbrev", "month-sa-abbrev");
    removeUnusedFormats();
    localeData.removeDuplicates("predef");
    localeData.removeDuplicates("weekdata");
    localeData.removeDuplicates("date");
    localeData.removeDuplicates("time");
    localeData.removeDuplicates("dateTime");
    localeData.removeCompleteDuplicates("dayPeriod-abbrev");
    computePeriodRedirects("day");
    computePeriodRedirects("month");
    computePeriodRedirects("day");
    removePeriodDuplicates("day");
    removePeriodDuplicates("month");
    removePeriodDuplicates("quarter");
    removePeriodDuplicates("era");
  }

  /**
   * Generate an override for a method which takes String arguments, which
   * simply redirect to another method based on a default value.
   * 
   * @param pw
   * @param category
   * @param locale
   * @param method
   * @param args
   */
  protected void generateArgMethod(PrintWriter pw, String category, GwtLocale locale,
      String method, String... args) {
    String value = localeData.getEntry(category, locale, "default");
    if (value != null && value.length() > 0) {
      pw.println();
      if (getOverrides()) {
        pw.println("  @Override");
      }
      pw.print("  public String " + method + "(");
      String prefix = "";
      for (String arg : args) {
        pw.print(prefix + "String " + arg);
        prefix = ", ";
      }
      pw.println(") {");
      pw.print("    return " + method + Character.toTitleCase(value.charAt(0)) + value.substring(1)
          + "(");
      prefix = "";
      for (String arg : args) {
        pw.print(prefix + arg);
        prefix = ", ";
      }
      pw.println(");");
      pw.println("  }");
    }
  }

  /**
   * Generate an override for a method which takes String arguments.
   * 
   * @param pw
   * @param category
   * @param locale
   * @param key
   * @param method
   * @param args
   */
  protected void generateArgMethodRedirect(PrintWriter pw, String category, GwtLocale locale,
      String key, String method, final String... args) {
    String value = localeData.getEntry(category, locale, key);
    if (value != null) {
      pw.println();
      if (getOverrides()) {
        pw.println("  @Override");
      }
      pw.print("  public String " + method + "(");
      String prefix = "";
      for (String arg : args) {
        pw.print(prefix + "String " + arg);
        prefix = ", ";
      }
      pw.println(") {");
      final StringBuilder buf = new StringBuilder();
      final StringGenerator gen = StringGenerator.create(buf, false);
      try {
        List<TemplateChunk> chunks = MessageStyle.MESSAGE_FORMAT.parse(value);
        for (TemplateChunk chunk : chunks) {
          chunk.accept(new DefaultTemplateChunkVisitor() {
            @Override
            public void visit(ArgumentChunk argChunk) {
              gen.appendStringValuedExpression(args[argChunk.getArgumentNumber()]);
            }

            @Override
            public void visit(StringChunk stringChunk) {
              gen.appendStringLiteral(stringChunk.getString());
            }
          });
        }
      } catch (ParseException e) {
        throw new RuntimeException("Unable to parse pattern '" + value + "' for locale " + locale
            + " key " + category + "/" + key, e);
      } catch (VisitorAbortException e) {
        throw new RuntimeException("Unable to parse pattern '" + value + "' for locale " + locale
            + " key " + category + "/" + key, e);
      }
      gen.completeString();
      pw.println("    return " + buf.toString() + ";");
      pw.println("  }");
    }
  }

  /**
   * Generate a method which returns a day number as an integer.
   * 
   * @param pw
   * @param locale
   * @param key
   * @param method
   */
  protected void generateDayNumber(PrintWriter pw, GwtLocale locale, String key, String method) {
    String day = localeData.getEntry("weekdata", locale, key);
    if (day != null) {
      int value = getDayNumber(day);
      pw.println();
      if (getOverrides()) {
        pw.println("  @Override");
      }
      pw.println("  public int " + method + "() {");
      pw.println("    return " + value + ";");
      pw.println("  }");
    }
  }

  /**
   * Generate a method which returns a format string for a given predefined
   * skeleton pattern.
   * 
   * @param locale
   * @param pw
   * @param skeleton
   * @param methodSuffix
   */
  protected void generateFormat(GwtLocale locale, PrintWriter pw, String skeleton,
      String methodSuffix) {
    String pattern = localeData.getEntry("predef", locale, skeleton);
    generateStringValue(pw, "format" + methodSuffix, pattern);
  }

  /**
   * Generate a series of methods which returns names in wide, narrow, and
   * abbreviated lengths plus their standalone versions.
   * 
   * @param pw
   * @param group
   * @param locale
   * @param methodPrefix
   * @param keys
   */
  protected void generateFullStringList(PrintWriter pw, String group, GwtLocale locale,
      String methodPrefix, String... keys) {
    generateStringListPair(pw, group, locale, methodPrefix, "Full", "wide", keys);
    generateStringListPair(pw, group, locale, methodPrefix, "Narrow", "narrow", keys);
    generateStringListPair(pw, group, locale, methodPrefix, "Short", "abbrev", keys);
  }

  /**
   * Generate an override of a standalone names list that simply redirects to
   * the non-standalone version.
   * 
   * @param pw
   * @param methodPrefix
   */
  protected void generateStandaloneRedirect(PrintWriter pw, String methodPrefix) {
    pw.println();
    if (getOverrides()) {
      pw.println("  @Override");
    }
    pw.println("  public String[] " + methodPrefix + "Standalone" + "() {");
    pw.println("    return " + methodPrefix + "();");
    pw.println("  }");
  }

  /**
   * Generate a method which returns a list of strings.
   * 
   * @param pw
   * @param category
   * @param fallbackCategory
   * @param locale
   * @param method
   * @param keys
   * @return true if the method was skipped as identical to its ancestor
   */
  protected boolean generateStringList(PrintWriter pw, String category, String fallbackCategory,
      GwtLocale locale, String method, String... keys) {
    Map<String, String> map = localeData.getEntries(category, locale);
    Map<String, String> fallback =
        fallbackCategory == null ? Collections.<String, String> emptyMap() : localeData.getEntries(
            fallbackCategory, locale);
    if (map == null || map.isEmpty() && fallback != null && !fallback.isEmpty()) {
      return true;
    }
    if (map != null && !map.isEmpty()) {
      if (fallbackCategory != null) {
        // see if the entry is the same as the fallback
        boolean different = false;
        for (String key : keys) {
          String value = map.get(key);
          if (value != null && !value.equals(fallback.get(key))) {
            different = true;
            break;
          }
        }
        if (!different) {
          return true;
        }
      }
      pw.println();
      if (getOverrides()) {
        pw.println("  @Override");
      }
      pw.println("  public String[] " + method + "() {");
      pw.print("    return new String[] {");
      boolean first = true;
      for (String key : keys) {
        String value = map.get(key);
        if (value == null) {
          value = fallback.get(key);
        }
        if (value == null) {
          System.err.println("Missing \"" + key + "\" in " + locale + "/" + category);
          value = "";
        }
        if (first) {
          first = false;
        } else {
          pw.print(",");
        }
        pw.print("\n        \"" + value.replace("\"", "\\\"") + "\"");
      }
      pw.println("\n    };");
      pw.println("  }");
    }
    return false;
  }

  protected void generateStringListPair(PrintWriter pw, String group, GwtLocale locale,
      String methodPrefix, String width, String categorySuffix, String... keys) {
    generateStringList(pw, group + "-" + categorySuffix, null, locale, methodPrefix + width, keys);
    String redirect =
        localeData.getEntry(group + "-sa-" + categorySuffix + "-redirect", locale, "redirect");
    if ("yes".equals(redirect)) {
      generateStandaloneRedirect(pw, methodPrefix + width);
    } else {
      generateStringList(pw, group + "-sa-" + categorySuffix, group + "-" + categorySuffix, locale,
          methodPrefix + width + "Standalone", keys);
    }
  }

  @Override
  protected void loadData() throws IOException {
    System.out.println("Loading data for date/time formats");
    localeData.addVersions(cldrFactory);
    localeData.addEntries("predef", cldrFactory,
        "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/"
            + "availableFormats", "dateFormatItem", "id");
    localeData.addNameEntries("month", cldrFactory);
    localeData.addNameEntries("day", cldrFactory);
    localeData.addNameEntries("quarter", cldrFactory);

    // only add the entries we will use to avoid overriding a parent for
    // differences that don't matter.
    localeData.addEntries("dayPeriod-abbrev", cldrFactory,
        "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/"
            + "dayPeriodContext[@type=\"format\"]/"
            + "dayPeriodWidth[@type=\"abbreviated\"]/dayPeriod[@type=\"am\"]", "dayPeriod", "type");
    localeData.addEntries("dayPeriod-abbrev", cldrFactory,
        "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/"
            + "dayPeriodContext[@type=\"format\"]/"
            + "dayPeriodWidth[@type=\"abbreviated\"]/dayPeriod[@type=\"pm\"]", "dayPeriod", "type");

    localeData.addEntries("era-abbrev", cldrFactory,
        "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/eras/eraAbbr", "era", "type");
    localeData.addEntries("era-wide", cldrFactory,
        "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/eras/eraNames", "era", "type");
    localeData.addDateTimeFormatEntries("date", cldrFactory);
    localeData.addDateTimeFormatEntries("time", cldrFactory);
    localeData.addDateTimeFormatEntries("dateTime", cldrFactory);
    loadWeekData();
    loadFormatPatterns();
  }

  /**
   * Write an output file.
   * 
   * @param locale
   * @param clientShared "client" or "shared", determines the package names
   *     being written to
   * @throws IOException
   * @throws FileNotFoundException
   */
  protected void writeOneOutputFile(GwtLocale locale, String clientShared) throws IOException,
      FileNotFoundException {
    // TODO(jat): make uz_UZ inherit from uz_Cyrl rather than uz, for example
    String myClass;
    String pathSuffix;
    if (locale.isDefault()) {
      if ("client".equals(clientShared)) {
        // The client default is hand-written code that extends the shared one.
        return;
      }
      myClass = "DefaultDateTimeFormatInfo";
      pathSuffix = "/";
    } else {
      myClass = "DateTimeFormatInfoImpl" + localeSuffix(locale);
      pathSuffix = "/impl/cldr/";
    }
    GwtLocale parent = localeData.inheritsFrom(locale);
    PrintWriter pw = createOutputFile(clientShared + pathSuffix + myClass + ".java");
    printHeader(pw);
    pw.print("package com.google.gwt.i18n." + clientShared);
    // GWT now requires JDK 1.6, so we always generate @Overrides
    setOverrides(true);
    if (!locale.isDefault()) {
      pw.print(".impl.cldr");
    }
    pw.println(";");
    pw.println();
    pw.println("// DO NOT EDIT - GENERATED FROM CLDR AND ICU DATA");
    pw.println("//  cldrVersion=" + CLDRFile.GEN_VERSION);
    Map<String, String> map = localeData.getEntries("version", locale);
    for (Map.Entry<String, String> entry : map.entrySet()) {
      pw.println("//  " + entry.getKey() + "=" + entry.getValue());
    }
    pw.println();
    if (locale.isDefault()) {
      pw.println("/**");
      pw.println(" * Default implementation of DateTimeFormatInfo interface, "
          + "using values from");
      pw.println(" * the CLDR root locale.");
      pw.println(" * <p>");
      pw.println(" * Users who need to create their own DateTimeFormatInfo "
          + "implementation are");
      pw.println(" * encouraged to extend this class so their implementation "
          + "won't break when   ");
      pw.println(" * new methods are added.");
      pw.println(" */");
    } else {
      pw.println("/**");
      pw.println(" * Implementation of DateTimeFormatInfo for the \"" + locale + "\" locale.");
      pw.println(" */");
    }
    pw.print("public class " + myClass);
    if (locale.isDefault()) {
      pw.print(" implements " + DateTimeFormatInfo.class.getSimpleName());
    } else {
      pw.print(" extends ");
      pw.print(DateTimeFormatInfoImpl.class.getSimpleName());
      if (!parent.isDefault()) {
        pw.print('_');
        pw.print(parent.getAsString());
      }
    }
    pw.println(" {");

    // write AM/PM names
    generateStringList(pw, "dayPeriod-abbrev", null, locale, "ampms", "am", "pm");

    // write standard date formats
    generateArgMethod(pw, "date", locale, "dateFormat");
    generateStringMethod(pw, "date", locale, "full", "dateFormatFull");
    generateStringMethod(pw, "date", locale, "long", "dateFormatLong");
    generateStringMethod(pw, "date", locale, "medium", "dateFormatMedium");
    generateStringMethod(pw, "date", locale, "short", "dateFormatShort");

    // write methods for assembling date/time formats
    generateArgMethod(pw, "dateTime", locale, "dateTime", "timePattern", "datePattern");
    generateArgMethodRedirect(pw, "dateTime", locale, "full", "dateTimeFull", "timePattern",
        "datePattern");
    generateArgMethodRedirect(pw, "dateTime", locale, "long", "dateTimeLong", "timePattern",
        "datePattern");
    generateArgMethodRedirect(pw, "dateTime", locale, "medium", "dateTimeMedium", "timePattern",
        "datePattern");
    generateArgMethodRedirect(pw, "dateTime", locale, "short", "dateTimeShort", "timePattern",
        "datePattern");

    // write era names
    generateStringList(pw, "era-wide", null, locale, "erasFull", "0", "1");
    generateStringList(pw, "era-abbrev", null, locale, "erasShort", "0", "1");

    // write firstDayOfTheWeek
    generateDayNumber(pw, locale, "firstDay", "firstDayOfTheWeek");

    // write predefined date/time formats
    for (Map.Entry<String, String> entry : FORMAT_BY_METHOD.entrySet()) {
      generateFormat(locale, pw, entry.getValue(), entry.getKey());
    }

    // write month names
    generateFullStringList(pw, "month", locale, "months", "1", "2", "3", "4", "5", "6", "7", "8",
        "9", "10", "11", "12");

    // write quarter names
    generateStringList(pw, "quarter-wide", null, locale, "quartersFull", "1", "2", "3", "4");
    generateStringList(pw, "quarter-abbrev", null, locale, "quartersShort", "1", "2", "3", "4");

    // write standard time formats
    generateArgMethod(pw, "time", locale, "timeFormat");
    generateStringMethod(pw, "time", locale, "full", "timeFormatFull");
    generateStringMethod(pw, "time", locale, "long", "timeFormatLong");
    generateStringMethod(pw, "time", locale, "medium", "timeFormatMedium");
    generateStringMethod(pw, "time", locale, "short", "timeFormatShort");

    // write weekday names
    generateFullStringList(pw, "day", locale, "weekdays", DAYS);

    // write weekend boundaries
    generateDayNumber(pw, locale, "weekendEnd", "weekendEnd");
    generateDayNumber(pw, locale, "weekendStart", "weekendStart");

    if (locale.isDefault()) {
      pw.println();
      pw.println("  @Override");
      pw.println("  public String dateFormat() {");
      pw.println("    return dateFormatMedium();");
      pw.println("  }");
      pw.println();
      pw.println("  @Override");
      pw.println("  public String dateTime(String timePattern, String datePattern) {");
      pw.println("    return datePattern + \" \" + timePattern;");
      pw.println("  }");
      pw.println();
      pw.println("  @Override");
      pw.println("  public String timeFormat() {");
      pw.println("    return timeFormatMedium();");
      pw.println("  }");
    }

    pw.println("}");
    pw.close();
  }

  @Override
  protected void writeOutputFiles() throws IOException {
    System.out.println("Writing output for date/time formats");
    for (GwtLocale locale : localeData.getNonEmptyLocales()) {
      // TODO(jat): remove client when we no longer need it
      writeOneOutputFile(locale, "client");
      writeOneOutputFile(locale, "shared");
    }
  }

  /**
   * @param period
   */
  private void computePeriodRedirects(String period) {
    localeData.computeRedirects(period + "-abbrev", period + "-sa-abbrev");
    localeData.computeRedirects(period + "-narrow", period + "-sa-narrow");
    localeData.computeRedirects(period + "-wide", period + "-sa-wide");
  }

  private void loadFormatPatterns() {
    localeData.addEntries("predef", cldrFactory,
        "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/"
            + "availableFormats", "dateFormatItem", "id");
    for (GwtLocale locale : localeData.getAllLocales()) {
      DateTimePatternGenerator dtpg = new DateTimePatternGenerator(locale);
      for (Map.Entry<String, String> entry : FORMATS.entrySet()) {
        String skeleton = entry.getKey();
        String cldrPattern = localeData.getEntry("predef", locale, skeleton);
        String pattern = dtpg.getBestPattern(skeleton);
        if (cldrPattern != null && !cldrPattern.equals(pattern)) {
          System.err.println("Mismatch on skeleton pattern in locale " + locale + " for skeleton '"
              + skeleton + "': icu='" + pattern + "', cldr='" + cldrPattern + "'");
        }
        localeData.addEntry("predef", locale, skeleton, pattern);
      }
    }
  }

  /**
   * Load the week start and weekend range values from CLDR.
   */
  private void loadWeekData() {
    localeData.addTerritoryEntries("weekdata", cldrFactory, regionLanguageData,
        "//supplementalData/weekData/firstDay", "firstDay", "day");
    localeData.addTerritoryEntries("weekdata", cldrFactory, regionLanguageData,
        "//supplementalData/weekData/weekendStart", "weekendStart", "day");
    localeData.addTerritoryEntries("weekdata", cldrFactory, regionLanguageData,
        "//supplementalData/weekData/weekendEnd", "weekendEnd", "day");
    localeData.addTerritoryEntries("weekdata", cldrFactory, regionLanguageData,
        "//supplementalData/weekData/minDays", "minDays", "count");
  }

  /**
   * Remove duplicates from period names.
   * 
   * @param group
   */
  private void removePeriodDuplicates(String group) {
    removePeriodWidthDuplicates(group, "wide");
    removePeriodWidthDuplicates(group, "abbrev");
    removePeriodWidthDuplicates(group, "narrow");
  }

  private void removePeriodWidthDuplicates(String group, String width) {
    localeData.removeCompleteDuplicates(group + "-" + width);
    localeData.removeCompleteDuplicates(group + "-sa-" + width);
    localeData.removeCompleteDuplicates(group + "-sa-" + width + "-redirect");
  }

  private void removeUnusedFormats() {
    for (GwtLocale locale : localeData.getAllLocales()) {
      Set<String> toRemove = new HashSet<String>();
      Map<String, String> map = localeData.getEntries("predef", locale);
      for (Entry<String, String> entry : map.entrySet()) {
        if (!FORMATS.containsKey(entry.getKey())) {
          toRemove.add(entry.getKey());
        }
      }
      localeData.removeEntries("predef", locale, toRemove);
    }
  }
}
