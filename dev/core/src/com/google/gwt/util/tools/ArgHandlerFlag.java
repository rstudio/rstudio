/*
 * Copyright 2006 Google Inc.
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

import com.google.gwt.dev.util.Empty;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Argument handler for boolean flags that have no parameters.
 *
 * Supports toggling the boolean value on and off using -label and -nolabel tag variants and
 * calculating a meaningful purpose including default value.
 */
public abstract class ArgHandlerFlag extends ArgHandler {

  private Map<String, Boolean> valuesByTag;

  protected void addTagValue(String tag, boolean value) {
    initValuesByTag();
    valuesByTag.put(tag, value);
  }

  /**
   * Returns the default value that will appear in help messages.
   */
  public abstract boolean getDefaultValue();

  @Override
  public String getHelpTag() {
    return "-" + (isExperimental() ? "X" : "") + "[no]" + getLabel();
  }

  /**
   * The root String that will be munged into -label and -nolabel variants for flag value toggling.
   * Should follow the verb[Adjective]Noun naming pattern. For example:
   *
   * @Override
   * public String getLabel() {
   *   return "allowMissingSrc";
   * }
   */
  public String getLabel() {
    return "";
  }

  @Override
  public final String getPurpose() {
    return (isExperimental() ? "EXPERIMENTAL: " : "") + getPurposeSnippet() + " " + "(defaults to "
        + (getDefaultValue() ? "ON" : "OFF") + ")";
  }

  /**
   * Returns a description that will be mixed together with default value to come up with the
   * overall flag purpose.
   */
  public abstract String getPurposeSnippet();

  /**
   * The primary tag matched by this argument handler.
   */
  @Override
  public final String getTag() {
    String label = getLabel();
    if (label == "") {
      return "";
    }
    return "-" + (isExperimental() ? "X" : "") + label;
  }

  @Override
  public String[] getTagArgs() {
    return Empty.STRINGS;
  }

  @Override
  public final String[] getTags() {
    initValuesByTag();
    Set<String> tags = valuesByTag.keySet();
    return tags.toArray(new String[tags.size()]);
  }

  // @VisibleForTesting
  boolean getValueByTag(String tag) {
    initValuesByTag();
    return valuesByTag.get(tag);
  }

  @Override
  public int handle(String[] args, int startIndex) {
    String tag = args[startIndex];
    Boolean value = getValueByTag(tag);
    return setFlag(value) ? 0 : 1;
  }

  private void initValuesByTag() {
    if (valuesByTag != null) {
      return;
    }

    valuesByTag = new LinkedHashMap<String, Boolean>();
    valuesByTag.put("-" + (isExperimental() ? "X" : "") + getLabel(), true);
    valuesByTag.put("-" + (isExperimental() ? "X" : "") + "no" + getLabel(), false);
  }

  @Override
  public boolean isRequired() {
    return false;
  }

  /**
   * Takes the explicitly provided value and propagates it into whatever option settings this flag
   * controls.
   *
   * @param value the new value for the flag.
   * @return whether the assignment was valid.
   */
  public abstract boolean setFlag(boolean value);
}
