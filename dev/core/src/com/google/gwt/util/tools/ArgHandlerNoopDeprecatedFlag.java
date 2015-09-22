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

import static com.google.gwt.thirdparty.guava.common.base.Preconditions.checkArgument;

/**
 * Argument handler for deprecated no-op flags.
 */
public class ArgHandlerNoopDeprecatedFlag extends ArgHandler {

  private final int numberOfArgumentsToConsume;
  private String[] tags;

  public ArgHandlerNoopDeprecatedFlag(int numberOfArgumentsToConsume, String... tags) {
    checkArgument(tags != null);
    checkArgument(tags.length != 0);

    this.numberOfArgumentsToConsume = numberOfArgumentsToConsume;
    this.tags = tags;
  }

  public ArgHandlerNoopDeprecatedFlag(String... tags) {
    this(0, tags);
  }

  @Override
  public boolean isUndocumented() {
    return true;
  }

  @Override
  public String getPurpose() {
    return "DEPRECATED: Has no effect and will be removed in a future release";
  }

  @Override
  public String getTag() {
    return tags[0];
  }

  @Override
  public String[] getTagArgs() {
    return tags;
  }

  @Override
  public int handle(String[] args, int startIndex) {
    String tag = args[startIndex];
    if (startIndex + numberOfArgumentsToConsume >= args.length) {
      System.err.println(tag + " should be followed by parameters.");
      return -1;
    }
    System.err.println(getWarningMessage(tag));

    return numberOfArgumentsToConsume;
  }

  protected String getWarningMessage(String tag) {
    return "The " + tag + " option is deprecated. It will be removed in a future release and will"
        + " throw an error if it is still used.";
  }
}
