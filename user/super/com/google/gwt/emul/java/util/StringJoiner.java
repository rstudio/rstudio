/*
 * Copyright 2015 Google Inc.
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
package java.util;

import static javaemul.internal.InternalPreconditions.checkNotNull;

/**
 * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/StringJoiner.html">
 * the official Java API doc</a> for details.
 */
public final class StringJoiner {

  private final String delimiter;
  private final String prefix;
  private final String suffix;

  private StringBuilder builder;
  private String emptyValue;

  public StringJoiner(CharSequence delimiter) {
    this(delimiter, "", "");
  }

  public StringJoiner(CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
    checkNotNull(delimiter, "delimiter");
    checkNotNull(prefix, "prefix");
    checkNotNull(suffix, "suffix");
    this.delimiter = delimiter.toString();
    this.prefix = prefix.toString();
    this.suffix = suffix.toString();
    this.emptyValue = this.prefix + this.suffix;
  }

  public StringJoiner add(CharSequence newElement) {
    initBuilderOrAddDelimiter();
    builder.append(newElement);
    return this;
  }

  public int length() {
    if (builder == null) {
      return emptyValue.length();
    }
    return builder.length() + suffix.length();
  }

  public StringJoiner merge(StringJoiner other) {
    if (other.builder != null) {
      // in case of other == this we need the length before adding delimiter to this (and thus other)
      // so we can skip the trailing delimiter of other when merging into this.
      int otherLength = other.builder.length();
      initBuilderOrAddDelimiter();
      builder.append(other.builder, other.prefix.length(), otherLength);
    }
    return this;
  }

  public StringJoiner setEmptyValue(CharSequence emptyValue) {
    this.emptyValue = emptyValue.toString();
    return this;
  }

  @Override
  public String toString() {
    if (builder == null) {
      return emptyValue;
    } else if (suffix.isEmpty()) {
      return builder.toString();
    } else {
      return builder.toString() + suffix;
    }
  }

  private void initBuilderOrAddDelimiter() {
    if (builder == null) {
      builder = new StringBuilder(prefix);
    } else {
      builder.append(delimiter);
    }
  }
}