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
package com.google.gwt.uibinder.rebind;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.uibinder.rebind.XMLElement.Location;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Wraps a {@link TreeLogger} with handy {@link String#format} style methods and
 * can be told to die. Perhaps we should instead add die(), warn(), etc. to
 * Treelogger.
 */
public class MortalLogger {
  /**
   * A valid logger that ignores all messages, handy for testing.
   */
  public static final MortalLogger NULL = new MortalLogger(TreeLogger.NULL);

  protected static String locationOf(XMLElement context) {
    if (context == null) {
      return "";
    }

    Location location = context.getLocation();
    if (location != null) {
      String displayFileName = location.getSystemId();
      if (displayFileName == null) {
        // We see this in the test cases that don't use actual source files
        displayFileName = "Unknown";
      } else {
        // Parse the system id as a URI, which it almost always is
        try {
          URI uri = new URI(location.getSystemId());
          String path = uri.getPath();
          if (path != null) {
            displayFileName = path.substring(path.lastIndexOf('/') + 1);
          }
        } catch (URISyntaxException e) {
          // Fall back to the raw system id
        }
      }
      // Log in a way that usually triggers IDE hyperlinks
      return ": " + context.toString() + " (" + displayFileName + ":"
          + location.getLineNumber() + ")";
    } else {
      /*
       * This shouldn't occur unless the XMLElement came from a DOM Node created
       * by something other than W3cDocumentBuilder.
       */
      return " " + context.toString();
    }
  }

  private final TreeLogger logger;

  public MortalLogger(TreeLogger logger) {
    this.logger = logger;
  }
  
  /**
   * Post an error message and halt processing. This method always throws an
   * {@link UnableToCompleteException}.
   */
  public void die(String message, Object... params)
      throws UnableToCompleteException {
    die(null, message, params);
  }

  /**
   * Post an error message about a specific XMLElement and halt processing. This
   * method always throws an {@link UnableToCompleteException}.
   */
  public void die(XMLElement context, String message, Object... params)
      throws UnableToCompleteException {
    logLocation(TreeLogger.ERROR, context, String.format(message, params));
    throw new UnableToCompleteException();
  }

  public TreeLogger getTreeLogger() {
    return logger;
  }

  public void logLocation(TreeLogger.Type type, XMLElement context,
      String message) {
    message += locationOf(context);
    logger.log(type, message);
  }

  /**
   * Post a warning message.
   */
  public void warn(String message, Object... params) {
    warn(null, message, params);
  }

  /**
   * Post a warning message related to a specific XMLElement.
   */
  public void warn(XMLElement context, String message, Object... params) {
    logLocation(TreeLogger.WARN, context, String.format(message, params));
  }
}
