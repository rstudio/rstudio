/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.util.tools;

import com.google.gwt.thirdparty.guava.common.base.Function;
import com.google.gwt.thirdparty.guava.common.base.Joiner;
import com.google.gwt.thirdparty.guava.common.base.Predicate;
import com.google.gwt.thirdparty.guava.common.collect.Collections2;
import com.google.gwt.thirdparty.guava.common.collect.Lists;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * A generic arg handler for options defined by enums.
 *
 * @param <T> enum type providing option values.
 */
public abstract class ArgHandlerEnum<T extends Enum<T>> extends ArgHandler {
  private final Class<T> optionsEnumClass;
  private final T defaultValue;
  private final boolean allowAbbreviation;

  private static final int ABBREVIATION_MIN_SIZE = 3;

  /**
   * Constructor, assumes that the default value for the options is the first enum value.
   */
  public ArgHandlerEnum(Class<T> optionsEnumClass) {
    this(optionsEnumClass, null, false);
  }

  /**
   * Constructor, allows to specify the default value for the option and whether to accept or
   * prefixes as abbreviations.
   */
  public ArgHandlerEnum(Class<T> optionsEnumClass, T defaultValue, boolean allowAbbreviation) {
    this.optionsEnumClass = optionsEnumClass;
    this.defaultValue = defaultValue;
    this.allowAbbreviation = allowAbbreviation;
  }

  @Override
  public String[] getDefaultArgs() {
    if (defaultValue == null) {
      return null;
    }
    return new String[] { getTag(), defaultValue.name() };
  }

  @Override
  public String[] getTagArgs() {
    return new String[]{ "(" + getFormattedOptionNames("|", optionsEnumClass) + ")" };
  }

  @Override
  public final int handle(String[] args, int startIndex) {
    if (startIndex + 1 < args.length) {
      String value = args[startIndex + 1].trim().toUpperCase(Locale.ENGLISH);
      T mode = matchOption(value);
      if (mode == null) {
        System.err.println(value + " is not a valid option for " + getTag());
        System.err.println(
            getTag() + " value must be one of " +
                getFormattedOptionNames(", ", " or ", optionsEnumClass) + ".");
        return -1;
      }
      setValue(mode);
      return 1;
    }

    System.err.println("Missing argument for " + getTag() + " must be followed by one of " +
        getFormattedOptionNames(", ", " or ", optionsEnumClass) + ".");
    return -1;
  }

  protected String getPurposeString(String prefix) {
    return (isExperimental() ? "EXPERIMENTAL: " : "") + prefix + " " +
        getFormattedOptionNames(", ", " or ", optionsEnumClass) +
        (defaultValue != null ? " (defaults to " + defaultValue.name() + ")" : "");
  }

  /**
   * Override to handle the setting of an enum value.
   */
  public abstract void setValue(T value);

  private List<String> getEnumNames(Class<T> optionsEnumClass) {
    return Lists.transform(Arrays.asList(optionsEnumClass.getEnumConstants()),
        new Function<T, String>() {
          @Override
          public String apply(T t) {
            return t.name();
          }
        });
  }

  private String getFormattedOptionNames(String separator,Class<T> optionsEnumClass) {
    return getFormattedOptionNames(separator, separator, optionsEnumClass);
  }

  private String getFormattedOptionNames(String separator,String lastSeparator,
      Class<T> optionsEnumClass) {
    List<String> enumNames = getEnumNames(optionsEnumClass);
    List<String> allNamesButLast = enumNames.subList(0, enumNames.size() - 1);
    String lastName = enumNames.get(enumNames.size() - 1);

    return Joiner.on(lastSeparator).join(Joiner.on(separator).join(allNamesButLast), lastName);
  }

  private T matchOption(final String value) {
    try {
      Collection<T> matches = Collections2.filter(
          Arrays.asList(optionsEnumClass.getEnumConstants()),
              new Predicate<T>() {
                @Override
                public boolean apply(T t) {
                  if (allowAbbreviation && value.length() >= ABBREVIATION_MIN_SIZE) {
                    return t.name().startsWith(value);
                  }
                  return t.name().equals(value);
                }
              });
      if (matches.size() == 1) {
        return matches.iterator().next();
      }
    } catch (IllegalArgumentException e) {
    }
    return null;
  }
}
