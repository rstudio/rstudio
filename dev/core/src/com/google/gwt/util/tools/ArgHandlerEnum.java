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

import static com.google.gwt.thirdparty.guava.common.base.Preconditions.checkNotNull;

import com.google.gwt.thirdparty.guava.common.base.Ascii;
import com.google.gwt.thirdparty.guava.common.base.Enums;
import com.google.gwt.thirdparty.guava.common.base.Joiner;
import com.google.gwt.thirdparty.guava.common.base.Preconditions;
import com.google.gwt.thirdparty.guava.common.base.Predicate;
import com.google.gwt.thirdparty.guava.common.collect.FluentIterable;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

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
   * Constructor, default value must be handled by the user code.
   */
  public ArgHandlerEnum(Class<T> optionsEnumClass) {
    this(optionsEnumClass, null, false);
  }

  /**
   * Constructor, allows to specify the default value for the option and whether to accept or
   * prefixes as abbreviations.
   * <p>
   * When {@code defaultValue} is null, handling of the default for the option is left to be
   * handled by the user code.
   */
  public ArgHandlerEnum(
      Class<T> optionsEnumClass, @Nullable T defaultValue, boolean allowAbbreviation) {
    Preconditions.checkArgument(optionsEnumClass.getEnumConstants().length > 1);
    this.optionsEnumClass = checkNotNull(optionsEnumClass);
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
    // A command line argument corresponding to an enum option has a parameter
    // (the desired value), will be at startIndex + 1.
    int optionValueIndex = startIndex + 1;
    if (optionValueIndex < args.length) {
      String value = Ascii.toUpperCase(args[optionValueIndex].trim());
      T mode = matchOption(value);
      if (mode == null) {
        System.err.format("%s is not a valid option for %s\n", value, getTag());
        System.err.format("%s value must be one of %s.\n",
            getTag(), getFormattedOptionNames(", ", " or ", optionsEnumClass));
        return -1;
      }
      setValue(mode);
      return 1;
    }

    System.err.format("Missing argument for %s must be followed by one of %s.\n" +
        getTag(), getFormattedOptionNames(", ", " or ", optionsEnumClass));
    return -1;
  }

  protected String getPurposeString(String prefix) {
    String maybeExperimentalString = isExperimental() ? "EXPERIMENTAL: " : "";
    String defaultValueString = defaultValue != null ?
        " (defaults to " + defaultValue.name() + ")" : "";

    return String.format("%s%s %s%s", maybeExperimentalString, prefix,
        getFormattedOptionNames(", ", " or ", optionsEnumClass), defaultValueString);
  }

  /**
   * Override to handle the setting of an enum value.
   */
  public abstract void setValue(T value);

  private List<String> getEnumNames(Class<T> optionsEnumClass) {
    return FluentIterable.from(Arrays.asList(optionsEnumClass.getEnumConstants()))
        .transform(Enums.stringConverter(optionsEnumClass).reverse())
        .toList();
  }

  private String getFormattedOptionNames(String separator, Class<T> optionsEnumClass) {
    return getFormattedOptionNames(separator, separator, optionsEnumClass);
  }

  private String getFormattedOptionNames(
      String separator, String lastSeparator, Class<T> optionsEnumClass) {
    List<String> enumNames = getEnumNames(optionsEnumClass);
    List<String> allNamesButLast = enumNames.subList(0, enumNames.size() - 1);
    String lastName = enumNames.get(enumNames.size() - 1);

    return Joiner.on(separator).join(allNamesButLast) + lastSeparator + lastName;
  }

  private Predicate<Enum<?>> buildMatchPredicate(final String value) {
    return
        new Predicate<Enum<?>>() {
          @Override
          public boolean apply(Enum<?> t) {
            if (allowAbbreviation && value.length() >= ABBREVIATION_MIN_SIZE) {
              return t.name().startsWith(value);
            }
            return t.name().equals(value);
          }
        };
  }

  private T matchOption(String value) {
    List<T> matchedOptions = FluentIterable.from(Arrays.asList(optionsEnumClass.getEnumConstants()))
        .filter(buildMatchPredicate(value))
        .toList();
    return matchedOptions.size() == 1 ? matchedOptions.get(0) : null;
  }
}
