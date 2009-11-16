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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deals with field references, e.g. the bits in braces here: <code>&lt;div
 * class="{style.enabled} fancy {style.impressive}" /></code>, by converting
 * them to java expressions (with the help of a {@link #Delegate}).
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
   * May be thrown by the {@link #Delegate} for badly formatted input.
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
     * @return the type any parsed field references are expected to return
     */
    JType getType();

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
    String handleFragment(String fragment)
        throws IllegalFieldReferenceException;

    /**
     * Called for each expanded field reference, to allow it to be stitched
     * together with surrounding fragments.
     */
    String handleReference(String reference)
        throws IllegalFieldReferenceException;
  }

  /**
   * Used by {@link #hasFieldReferences}. Passthrough implementation that notes
   * when handleReference has been called.
   */
  private static final class Telltale implements
      FieldReferenceConverter.Delegate {
    boolean hasComputed = false;

    public JType getType() {
      return null;
    }

    public String handleFragment(String fragment) {
      return fragment;
    }

    public String handleReference(String reference) {
      hasComputed = true;
      return reference;
    }

    public boolean hasComputed() {
      return hasComputed;
    }
  }

  private static final Pattern BRACES = Pattern.compile("[{]([^}]*)[}]");
  private static final Pattern LEGAL_FIRST_CHAR = Pattern.compile("^[$_a-zA-Z].*");

  
  /**
   * @return true if the given string holds one or more field references
   */
  public static boolean hasFieldReferences(String string) {
    Telltale telltale = new Telltale();
    new FieldReferenceConverter(null).convert(string, telltale);
    return telltale.hasComputed();
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
        fieldManager.registerFieldReference(fieldReference, delegate.getType());
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

    for (String segment : segments) {
      segment = cssConverter.convertName(segment);
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
