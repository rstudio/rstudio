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

package java.nio.charset;

import static javaemul.internal.InternalPreconditions.checkArgument;

import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

import javaemul.internal.EmulatedCharset;
import javaemul.internal.NativeRegExp;

/**
 * A minimal emulation of {@link Charset}.
 */
public abstract class Charset implements Comparable<Charset> {

  private static final class AvailableCharsets {
    private static final SortedMap<String, Charset> CHARSETS;
    static {
      SortedMap<String, Charset> map = new TreeMap<String, Charset>() { };
      map.put(EmulatedCharset.ISO_8859_1.name(), EmulatedCharset.ISO_8859_1);
      map.put(EmulatedCharset.ISO_LATIN_1.name(), EmulatedCharset.ISO_LATIN_1);
      map.put(EmulatedCharset.UTF_8.name(), EmulatedCharset.UTF_8);
      CHARSETS = Collections.unmodifiableSortedMap(map);
    }
  }

  public static SortedMap<String, Charset> availableCharsets() {
    return AvailableCharsets.CHARSETS;
  }

  public static Charset forName(String charsetName) {
    checkArgument(charsetName != null, "Null charset name");

    charsetName = charsetName.toUpperCase();
    if (EmulatedCharset.ISO_8859_1.name().equals(charsetName)) {
      return EmulatedCharset.ISO_8859_1;
    } else if (EmulatedCharset.ISO_LATIN_1.name().equals(charsetName)) {
      return EmulatedCharset.ISO_LATIN_1;
    } else if (EmulatedCharset.UTF_8.name().equals(charsetName)) {
      return EmulatedCharset.UTF_8;
    }

    if (!createLegalCharsetNameRegex().test(charsetName)) {
      throw new IllegalCharsetNameException(charsetName);
    } else {
      throw new UnsupportedCharsetException(charsetName);
    }
  }

  private static native NativeRegExp createLegalCharsetNameRegex() /*-{
    return /^[A-Za-z0-9][\w-:\.\+]*$/;
  }-*/;

  private final String name;

  protected Charset(String name, String[] aliasesIgnored) {
    this.name = name;
  }

  public final String name() {
    return name;
  }

  @Override
  public final int compareTo(Charset that) {
    return this.name.compareToIgnoreCase(that.name);
  }

  @Override
  public final int hashCode() {
    return name.hashCode();
  }

  @Override
  public final boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof Charset)) {
      return false;
    }
    Charset that = (Charset) o;
    return this.name.equals(that.name);
  }

  @Override
  public final String toString() {
    return name;
  }
}
