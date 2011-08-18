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
package com.google.gwt.uibinder.attributeparsers;

import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.uibinder.rebind.FieldManager;
import com.google.gwt.uibinder.rebind.XMLElement;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deals with field references, e.g. the bits in braces here: <code>&lt;div
 * class="{style.enabled} fancy {style.impressive}" /></code>, by converting
 * them to java expressions (with the help of a
 * {@link com.google.gwt.uibinder.attributeparsers.FieldReferenceConverter.Delegate
 * Delegate}).
 * <p>
 * A field reference is one or more segments separated by dots. The first
 * segment is considered to be a reference to a ui field, and succeeding
 * segments are method calls. So, <code>"{able.baker.charlie}"</code> becomes
 * <code>"able.baker().charlie()"</code>.
 * <p>
 * A field reference starts with '{' and is followed immediately by a character
 * that can legally start a java identifier&mdash;that is a letter, $, or
 * underscore. Braces not followed by such a character are left in place.
 * <p>
 * For convenience when dealing with generated CssResources, field segments with
 * dashes are converted to camel case. That is, {able.baker-charlie} is the same
 * as {able.bakerCharlie}
 * <p>
 * Opening braces may be escape by doubling them. That is, "{{foo}" will
 * converted to "{foo}", with no field reference detected.
 */
public class FieldReferenceConverter {
  /**
   * May be thrown by the
   * {@link com.google.gwt.uibinder.attributeparsers.FieldReferenceConverter.Delegate
   * Delegate} for badly formatted input.
   */
  @SuppressWarnings("serial")
  public static class IllegalFieldReferenceException extends RuntimeException {
  }

  /**
   * Responsible for the bits around and between the field references. May throw
   * IllegalFieldReferenceException as it sees fit.
   */
  interface Delegate {
    /**
     * Returns the types any parsed field references are expected to return.
     * Multiple values indicates an overload. E.g., in <a href={...}> either a
     * String or a SafeUri is allowed.
     */
    JType[] getTypes();

    /**
     * Called for fragment around and between field references.
     * <p>
     * Note that it will be called with empty strings if these surrounding bits
     * are empty. E.g., "{style.enabled} fancy {style.impressive}" would call
     * this method three times, with "", " fancy ", and "".
     * <p>
     * A string with no field references is treated as a single fragment, and
     * causes a single call to this method.
     */
    String handleFragment(String fragment) throws IllegalFieldReferenceException;

    /**
     * Called for each expanded field reference, to allow it to be stitched
     * together with surrounding fragments.
     */
    String handleReference(String reference) throws IllegalFieldReferenceException;
  }

  /**
   * Used by {@link FieldReferenceConverter#countFieldReferences}. Passthrough
   * implementation that counts the number of calls handleReference has been called,
   * so that we know how many field references a given string contains.
   */
  private static final class Telltale implements FieldReferenceConverter.Delegate {
    private int computedCount;

    public int getComputedCount() {
      return computedCount;
    }

    public JType[] getTypes() {
      return new JType[0];
    }

    public String handleFragment(String fragment) {
      return fragment;
    }

    public String handleReference(String reference) {
      computedCount++;
      return reference;
    }
  }

  private static final Pattern BRACES = Pattern.compile("[{]([^}]*)[}]");
  private static final Pattern LEGAL_FIRST_CHAR = Pattern.compile("^[$_a-zA-Z].*");
  private static final String DOTS_AND_PARENS = "[().]";

  /**
   * Returns the number of field references in the given string.
   */
  public static int countFieldReferences(String string) {
    Telltale telltale = new Telltale();
    new FieldReferenceConverter(null).convert(null, string, telltale);
    return telltale.getComputedCount();
  }

  /**
   * Reverses most of the work of {@link #convert}, turning a java expresion
   * back into a dotted path.
   */
  public static String expressionToPath(String expression) {
    String[] chunks = expression.split(DOTS_AND_PARENS);
    StringBuilder b = new StringBuilder();
    for (String chunk : chunks) {
      if (b.length() > 0 && chunk.length() > 0) {
        b.append(".");
      }
      b.append(chunk);
    }
    return b.toString();
  }

  /**
   * Returns true if the given string holds one or more field references.
   */
  public static boolean hasFieldReferences(String string) {
    return countFieldReferences(string) > 0;
  }

  private final CssNameConverter cssConverter = new CssNameConverter();
  private final FieldManager fieldManager;

  /**
   * @param fieldManager to register parsed references with. May be null
   */
  FieldReferenceConverter(FieldManager fieldManager) {
    this.fieldManager = fieldManager;
  }

  /**
   * @throws IllegalFieldReferenceException if the delegate does
   */
  public String convert(String in, Delegate delegate) {
    return convert(null, in, delegate);
  }

  /**
   * @throws IllegalFieldReferenceException if the delegate does
   */
  public String convert(XMLElement source, String in, Delegate delegate) {
    StringBuilder b = new StringBuilder();
    int nextFindStart = 0;
    int lastMatchEnd = 0;

    Matcher m = BRACES.matcher(in);
    while (m.find(nextFindStart)) {
      String fieldReference = m.group(1);
      if (!legalFirstCharacter(fieldReference)) {
        nextFindStart = m.start() + 2;
        continue;
      }

      String precedingFragment = in.substring(lastMatchEnd, m.start());
      precedingFragment = handleFragment(precedingFragment, delegate);
      b.append(precedingFragment);

      if (fieldManager != null) {
        fieldManager.registerFieldReference(source, fieldReference, delegate.getTypes());
      }
      fieldReference = expandDots(fieldReference);
      b.append(delegate.handleReference(fieldReference));
      nextFindStart = lastMatchEnd = m.end();
    }

    b.append(handleFragment(in.substring(lastMatchEnd), delegate));
    return b.toString();
  }

  private String expandDots(String value) {
    StringBuilder b = new StringBuilder();
    String[] segments = value.split("[.]");

    for (int i = 0; i < segments.length; ++i) {
      String segment = cssConverter.convertName(segments[i]);

      // The first segment is converted to a field getter. So,
      // "bundle.whatever" becomes "get_bundle().whatever".
      if (fieldManager != null && i == 0) {
        segment = fieldManager.convertFieldToGetter(segment);
      }

      if (b.length() == 0) {
        b.append(segment); // field name
      } else {
        b.append(".").append(segment).append("()");
      }
    }
    return b.toString();
  }

  private String handleFragment(String fragment, Delegate delegate) {
    fragment = fragment.replace("{{", "{");
    return delegate.handleFragment(fragment);
  }

  private boolean legalFirstCharacter(String fieldReference) {
    return LEGAL_FIRST_CHAR.matcher(fieldReference).matches();
  }
}
