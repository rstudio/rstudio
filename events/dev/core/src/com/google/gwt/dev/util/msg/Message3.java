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
package com.google.gwt.dev.util.msg;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;

/**
 * 3-arg message.
 */
public abstract class Message3 extends Message {

  protected Message3(Type type, String fmt) {
    super(type, fmt, 3);
  }

  protected TreeLogger branch3(TreeLogger logger, Object arg1, Object arg2,
      Object arg3, Formatter fmt1, Formatter fmt2, Formatter fmt3,
      Throwable caught) {
    return logger.branch(type, compose3(arg1, arg2, arg3, fmt1, fmt2, fmt3),
        caught);
  }

  protected String compose3(Object arg1, Object arg2, Object arg3,
      Formatter fmt1, Formatter fmt2, Formatter fmt3) {

    // Format the objects.
    //
    String stringArg1 = (arg1 != null ? fmt1.format(arg1) : "null");
    String stringArg2 = (arg2 != null ? fmt2.format(arg2) : "null");
    String stringArg3 = (arg3 != null ? fmt3.format(arg3) : "null");

    // Decide how to order the inserts.
    // Tests are biased toward $1..$2 order.
    //
    String insert1 = (argIndices[0] == 0) ? stringArg1 : ((argIndices[0] == 1)
        ? stringArg2 : stringArg3);
    String insert2 = (argIndices[1] == 1) ? stringArg2 : ((argIndices[1] == 0)
        ? stringArg1 : stringArg3);
    String insert3 = (argIndices[2] == 2) ? stringArg3 : ((argIndices[2] == 0)
        ? stringArg1 : stringArg2);

    // Cache the length of the inserts.
    //
    int lenInsert1 = insert1.length();
    int lenInsert2 = insert2.length();
    int lenInsert3 = insert3.length();

    // Cache the length of each part.
    //
    int lenPart0 = fmtParts[0].length;
    int lenPart1 = fmtParts[1].length;
    int lenPart2 = fmtParts[2].length;
    int lenPart3 = fmtParts[3].length;

    // Prep for copying.
    //
    int dest = 0;
    char[] chars = new char[minChars + lenInsert1 + lenInsert2 + lenInsert3];

    // literal + insert, part 0
    System.arraycopy(fmtParts[0], 0, chars, dest, lenPart0);
    dest += lenPart0;

    insert1.getChars(0, lenInsert1, chars, dest);
    dest += lenInsert1;

    // literal + insert, part 1
    System.arraycopy(fmtParts[1], 0, chars, dest, lenPart1);
    dest += lenPart1;

    insert2.getChars(0, lenInsert2, chars, dest);
    dest += lenInsert2;

    // literal + insert, part 2
    System.arraycopy(fmtParts[2], 0, chars, dest, lenPart2);
    dest += lenPart2;

    insert3.getChars(0, lenInsert3, chars, dest);
    dest += lenInsert3;

    // final literal
    System.arraycopy(fmtParts[3], 0, chars, dest, lenPart3);

    return new String(chars);
  }

  protected void log3(TreeLogger logger, Object arg1, Object arg2, Object arg3,
      Formatter fmt1, Formatter fmt2, Formatter fmt3, Throwable caught) {
    if (logger.isLoggable(type)) {
      logger.log(type, compose3(arg1, arg2, arg3, fmt1, fmt2, fmt3), caught);
    }
  }
}
