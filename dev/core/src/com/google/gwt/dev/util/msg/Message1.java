// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.msg;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;

public abstract class Message1 extends Message {

  public Message1(Type type, String fmt) {
    super(type, fmt, 1);
  }

  protected TreeLogger branch1(TreeLogger logger, Object arg1, Formatter fmt1,
      Throwable caught) {
    return logger.branch(fType, compose1(arg1, fmt1), caught);
  }

  protected String compose1(Object arg1, Formatter fmt1) {
    // Format the objects.
    //
    String stringArg1 = (arg1 != null ? fmt1.format(arg1) : "null");
    
    // To maintain consistency with the other impls, we use an insert var.
    //
    String insert1 = stringArg1;  

    // Cache the length of the inserts.
    //
    int lenInsert1 = insert1.length();

    // Cache the length of each part.
    //
    int lenPart0 = fFmtParts[0].length;
    int lenPart1 = fFmtParts[1].length;
    
    // Prep for copying.
    //
    int dest = 0;
    char[] chars = new char[fMinChars + lenInsert1];

    // literal + insert, part 0
    System.arraycopy(fFmtParts[0], 0, chars, dest, lenPart0);
    dest += lenPart0;
    
    insert1.getChars(0, lenInsert1, chars, dest);   
    dest += lenInsert1;

    // final literal
    System.arraycopy(fFmtParts[1], 0, chars, dest, lenPart1);
    
    return new String(chars);
  }

  protected void log1(TreeLogger logger, Object arg1, Formatter fmt1,
      Throwable caught) {
    if (logger.isLoggable(fType))
      logger.log(fType, compose1(arg1, fmt1), caught);
  }
}
