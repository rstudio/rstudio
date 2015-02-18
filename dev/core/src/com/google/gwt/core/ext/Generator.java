/*
 * Copyright 2006 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.core.ext;

import com.google.gwt.thirdparty.guava.common.base.Strings;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Generates source code for subclasses during deferred binding requests. Subclasses must be
 * thread-safe.
 * <p>
 * If annotated by {@code @RunsLocal}, a generator can minimize its impact on compilation speed. See
 * {@link RunsLocal} for details.
 * <p>
 * Resource reading should be done through the ResourceOracle in the provided GeneratorContext (not
 * via ClassLoader.getResource(), File, or URL) so that Generator Resource dependencies can be
 * detected and used to facilitate fast incremental recompiles.
 */
public abstract class Generator {

  /**
   * An optional annotation indicating that a Generator can be run with local information during
   * incremental compilation.
   * <p>
   * When this annotation is applied, the generator cannot access global level type information
   * (e.g. {@code JClassType#getSubTypes} or {@code TypeOracle#getTypes}) and also accesses to
   * property values are restricted to the ones defined by {@link #requiredProperties}.
   * <p>
   * This information is used by Generator invocation during incremental compilation to run
   * Generators as early as possible in the compile tree (and thus as parallelized and cached as
   * possible).
   */
  @Inherited
  @Retention(RetentionPolicy.RUNTIME)
  public @interface RunsLocal {

    /**
     * A special value for {@link #requiresProperties} to indicate that any property can affect this
     * generator's output. While this gives access to any property value, this may slowdown the
     * compilation speed to precompute all property values.
     */
    String ALL = "%ALL%";

    /**
     * The list of names of properties which will be accessed by this Generator. It is assumed that
     * any change in the values of these properties will affect the content of Generator output.
     * <p>
     * Any Generator that depends on properties will have its execution delayed to the point in the
     * compile tree where it is known that the properties it cares about have stopped changing. In
     * general this result of pushing Generator execution towards the root of the tree has negative
     * performance consequences on incremental compile performance.
     * <p>
     * Generators that want to be as fast as possible should strive not to read any properties.
     * <p>
     * Can be set to {@code RunsLocal.ALL} to indicate a need to arbitrarily access any property.
     */
    String[] requiresProperties() default {};
  }

  private static final int MAX_SIXTEEN_BIT_NUMBER_STRING_LENGTH = 5;

  /**
   * Escapes string content to be a valid string literal.
   *
   * @return an escaped version of <code>unescaped</code>, suitable for being enclosed in double
   *         quotes in Java source
   */
  public static String escape(String unescaped) {
    int extra = 0;
    for (int in = 0, n = unescaped.length(); in < n; ++in) {
      switch (unescaped.charAt(in)) {
        case '\0':
        case '\n':
        case '\r':
        case '\"':
        case '\\':
          ++extra;
          break;
      }
    }

    if (extra == 0) {
      return unescaped;
    }

    char[] oldChars = unescaped.toCharArray();
    char[] newChars = new char[oldChars.length + extra];
    for (int in = 0, out = 0, n = oldChars.length; in < n; ++in, ++out) {
      char c = oldChars[in];
      switch (c) {
        case '\0':
          newChars[out++] = '\\';
          c = '0';
          break;
        case '\n':
          newChars[out++] = '\\';
          c = 'n';
          break;
        case '\r':
          newChars[out++] = '\\';
          c = 'r';
          break;
        case '\"':
          newChars[out++] = '\\';
          c = '"';
          break;
        case '\\':
          newChars[out++] = '\\';
          c = '\\';
          break;
      }
      newChars[out] = c;
    }

    return String.valueOf(newChars);
  }

  /**
   * Returns an escaped version of a String that is valid as a Java class name.<br />
   *
   * Illegal characters become "_" + the character integer padded to 5 digits like "_01234". The
   * padding prevents collisions like the following "_" + "123" + "4" = "_" + "1234". The "_" escape
   * character is escaped to "__".
   */
  public static String escapeClassName(String unescapedString) {
    char[] unescapedCharacters = unescapedString.toCharArray();
    StringBuilder escapedCharacters = new StringBuilder();

    boolean firstCharacter = true;
    for (char unescapedCharacter : unescapedCharacters) {
      if (firstCharacter && !Character.isJavaIdentifierStart(unescapedCharacter)) {
        // Escape characters that can't be the first in a class name.
        escapeAndAppendCharacter(escapedCharacters, unescapedCharacter);
      } else if (!Character.isJavaIdentifierPart(unescapedCharacter)) {
        // Escape characters that can't be in a class name.
        escapeAndAppendCharacter(escapedCharacters, unescapedCharacter);
      } else if (unescapedCharacter == '_') {
        // Escape the escape character.
        escapedCharacters.append("__");
      } else {
        // Leave valid characters alone.
        escapedCharacters.append(unescapedCharacter);
      }

      firstCharacter = false;
    }

    return escapedCharacters.toString();
  }

  private static void escapeAndAppendCharacter(
      StringBuilder escapedCharacters, char unescapedCharacter) {
    String numberString = Integer.toString(unescapedCharacter);
    numberString = Strings.padStart(numberString, MAX_SIXTEEN_BIT_NUMBER_STRING_LENGTH, '0');
    escapedCharacters.append("_" + numberString);
  }

  /**
   * Generate a default constructible subclass of the requested type. The generator throws
   * <code>UnableToCompleteException</code> if for any reason it cannot provide a substitute class
   *
   * @return the name of a subclass to substitute for the requested class, or return
   *         <code>null</code> to cause the requested type itself to be used
   */
  public abstract String generate(TreeLogger logger, GeneratorContext context, String typeName)
      throws UnableToCompleteException;
}
