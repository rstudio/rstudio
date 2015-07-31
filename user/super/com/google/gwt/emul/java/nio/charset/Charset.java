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

import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A minimal GWT emulation of {@link Charset}.
 */
public abstract class Charset implements Comparable<Charset> {
  static final Charset ISO_8859_1 = new Charset("ISO-8859-1") { };
  static final Charset UTF_8 = new Charset("UTF-8") { };

  private static final class AvailableCharsets {
    private static final SortedMap<String, Charset> CHARSETS;
    static {
      SortedMap<String, Charset> map = new TreeMap<String, Charset>() { };
      map.put(ISO_8859_1.name(), ISO_8859_1);
      map.put(UTF_8.name(), UTF_8);
      CHARSETS = Collections.unmodifiableSortedMap(map);
    }
  }

  public static SortedMap<String, Charset> availableCharsets() {
    return AvailableCharsets.CHARSETS;
  }

  public static Charset forName(String charsetName) {
    if (charsetName == null) {
      throw new IllegalArgumentException("Null charset name");
    } else if (!isLegalCharsetName(charsetName)) {
      throw new IllegalCharsetNameException(charsetName);
    }
    Charset charset = AvailableCharsets.CHARSETS.get(charsetName.toUpperCase());
    if (charset == null) {
      throw new UnsupportedCharsetException(charsetName);
    }
    return charset;
  }

  private static native boolean isLegalCharsetName(String name) /*-{
    return /^[A-Za-z0-9][\w-:\.\+]*$/.test(name);
  }-*/;

  private final String name;

  private Charset(String name) {
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
