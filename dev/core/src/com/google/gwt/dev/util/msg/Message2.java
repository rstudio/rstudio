// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.msg;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;

public abstract class Message2 extends Message {

  public Message2(Type type, String fmt) {
    super(type, fmt, 2);
  }

  protected TreeLogger branch2(TreeLogger logger, Object arg1, Object arg2,
      Formatter fmt1, Formatter fmt2, Throwable caught) {
    return logger.branch(fType, compose2(arg1, arg2, fmt1, fmt2), caught);
  }

  protected String compose2(Object arg1, Object arg2, Formatter fmt1,
      Formatter fmt2) {

    // Format the objects.
    //
    String stringArg1 = (arg1 != null ? fmt1.format(arg1) : "null");
    String stringArg2 = (arg2 != null ? fmt2.format(arg2) : "null");
    
    // Decide how to order the inserts.
    // Tests are biased toward $1..$2 order.
    //
    String insert1 = (fArgIndices[0] == 0) ? stringArg1 : stringArg2;
    String insert2 = (fArgIndices[1] == 1) ? stringArg2 : stringArg1;

    // Cache the length of the inserts.
    //
    int lenInsert1 = insert1.length();
    int lenInsert2 = insert2.length();

    // Cache the length of each part.
    //
    int lenPart0 = fFmtParts[0].length;
    int lenPart1 = fFmtParts[1].length;
    int lenPart2 = fFmtParts[2].length;
    
    // Prep for copying.
    //
    int dest = 0;
    char[] chars = new char[fMinChars + lenInsert1 + lenInsert2];

    // literal + insert, part 0
    System.arraycopy(fFmtParts[0], 0, chars, dest, lenPart0);
    dest += lenPart0;
    
    insert1.getChars(0, lenInsert1, chars, dest);   
    dest += lenInsert1;

    // literal + insert, part 1
    System.arraycopy(fFmtParts[1], 0, chars, dest, lenPart1);
    dest += lenPart1;
    
    insert2.getChars(0, lenInsert2, chars, dest);   
    dest += lenInsert2;
    
    // final literal
    System.arraycopy(fFmtParts[2], 0, chars, dest, lenPart2);
    
    return new String(chars);
  }

  protected void log2(TreeLogger logger, Object arg1, Object arg2,
      Formatter fmt1, Formatter fmt2, Throwable caught) {
    if (logger.isLoggable(fType))
      logger.log(fType, compose2(arg1, arg2, fmt1, fmt2), caught);
  }
}
