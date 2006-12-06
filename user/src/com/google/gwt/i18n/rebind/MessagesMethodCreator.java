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
package com.google.gwt.i18n.rebind;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.i18n.rebind.util.MessagesInterfaceCreator;
import com.google.gwt.user.rebind.AbstractGeneratorClassCreator;
import com.google.gwt.user.rebind.AbstractMethodCreator;

import java.text.MessageFormat;
import java.text.ParseException;

/**
 * Creator for methods of the form String getX(arg0,...,argN).
 */
class MessagesMethodCreator extends AbstractMethodCreator {
  /**
   * Constructor for <code>MessagesMethodCreator</code>.
   * 
   * @param classCreator associated class creator
   */
  public MessagesMethodCreator(AbstractGeneratorClassCreator classCreator) {
    super(classCreator);
  }

  public void createMethodFor(TreeLogger logger, JMethod m, String template)
      throws UnableToCompleteException {
    int numParams = m.getParameters().length;

    // Compile time checks of the message
    Object[] expected;

    // Find safe string to store 'real' quotes during escape.
    // Using '~' rather than null string or one of a-X because it is very can
    // easily test what happens with multiple '~'s.
    String safeReplaceString = "~";
    while (template.indexOf(safeReplaceString) >= 0) {
      safeReplaceString += "~";
    }

    try {
      int numArgs = MessagesInterfaceCreator.numberOfMessageArgs(template);
      expected = new Object[numArgs];
    } catch (ParseException e) {
      logger.log(TreeLogger.INFO, "Failed to parse the message " + template
        + " so cannot verify the number of passed-in arguments", e);
      expected = new Object[numParams];
    }
    if (numParams != expected.length) {
      String s = "Wrong number of template arguments\n\t " + m.getName()
        + " args: " + numParams + "\n\t" + template + " args: "
        + expected.length;
      throw error(logger, s);
    }
    for (int i = 0; i < expected.length; i++) {
      expected[i] = safeReplaceString + " + arg" + i + " + "
        + safeReplaceString;
    }
    String formattedString;
    try {
      formattedString = MessageFormat.format(template, expected);
    } catch (IllegalArgumentException e) {
      throw error(logger, "Message Template '" + template
        + "' did not format correctly", e);
    }
    formattedString = wrap(formattedString);
    formattedString = formattedString.replaceAll(safeReplaceString, "\"");
    String templateToSplit = "return " + formattedString + ";";
    println(templateToSplit);
  }
}
