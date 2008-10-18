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
package com.google.gwt.user.rebind;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;

/**
 * Super class for AbstractMethod and AbstractClass creators. The primary
 * purpose is to pull up helper methods for logging and printing.
 */
public class AbstractSourceCreator {
  
  /**
   * Creates a branch for the treelogger.
   * @param logger
   * @param message
   * @return treelogger
   */
  protected static TreeLogger branch(TreeLogger logger, String message) {
    return logger.branch(TreeLogger.TRACE, message, null);
  }

  /**
   * Convenience method to use TreeLogger error pattern.
   * @param logger logger to print to
   * @param msg msg
   * @return the exception to throw
   */
  protected static UnableToCompleteException error(TreeLogger logger, String msg) {
    logger.log(TreeLogger.ERROR, msg, null);
    return new UnableToCompleteException();
  }

  /**
   * Convenience method to use TreeLogger error pattern.
   * @param logger logger to print to
   * @param msg msg
   * @return the exception to throw
   */
  protected static UnableToCompleteException error(TreeLogger logger, String msg, Throwable cause) {
    logger.log(TreeLogger.ERROR, msg, cause);
    return new UnableToCompleteException();
  }
  
  /**
   * Convenience method to use TreeLogger error pattern.
   * @param logger logger to print to
   * @param e throwable
   * @return th exception to throw
   */
  protected static UnableToCompleteException error(TreeLogger logger,
      Throwable e) {
    logger.log(TreeLogger.ERROR, e.getMessage(), e);
    return new UnableToCompleteException();
  }

  /**
   * Returns the String represention of the java type for a primitive for
   * example int/Integer, float/Float.
   * 
   * @param type
   * @return the string representation
   */
  protected static String getJavaObjectTypeFor(JPrimitiveType type) {
    if (type == JPrimitiveType.INT) {
      return "Integer";
    } else {
      String s = type.getSimpleSourceName();
      return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
  }

  /**
   * Helper method used to wrap a string constant with quotes. Must use to
   * enable string escaping.
   * 
   * @param wrapMe String to wrap
   * @return wrapped String
   */
  protected static String wrap(String wrapMe) {
    return "\"" + Generator.escape(wrapMe) + "\"";
  }
}
