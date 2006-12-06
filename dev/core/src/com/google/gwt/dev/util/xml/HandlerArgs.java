// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.xml;

import com.google.gwt.core.ext.UnableToCompleteException;

/**
 * A set of args for a given set of parameters, some of which may be set to
 * default values.
 */
public class HandlerArgs {

  public HandlerArgs(Schema schema, int lineNumber, String elemName,
      HandlerParam[] handlerParams) {
    fSchema = schema;
    fLineNumber = lineNumber;
    fElemName = elemName;
    fHandlerParams = handlerParams;
    fAttrNames = new String[handlerParams.length];
    fArgValues = new String[handlerParams.length];

    // Set default values.
    //
    for (int i = 0, n = handlerParams.length; i < n; ++i) {
      fArgValues[i] = fHandlerParams[i].getDefaultValue(schema);
    }
  }

  /**
   * @return
   *    the argument converted to a form that is expected to compatible with
   *    the associated parameter and will work for a reflection "invoke()" call
   * @throws UnableToCompleteException
   *    if the argument could not be converted
   */
  public Object convertToArg(int i) throws UnableToCompleteException {
    String value = fArgValues[i];
    if (value != null) {
      AttributeConverter converter = fSchema
        .getAttributeConverter(fHandlerParams[i].getParamType());
      return converter.convertToArg(fSchema, fLineNumber, fElemName,
        fAttrNames[i], value);
    } else {
      return new NullPointerException("Argument " + i + " was null");
    }
  }

  public int getArgCount() {
    return fHandlerParams.length;
  }

  public String getArgName(int i) {
    return fHandlerParams[i].getNormalizedName();
  }

  public boolean isArgSet(int i) {
    if (fArgValues[i] != null) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * @return 
   *    <code>true</code> if the param for the specified attribute was set; 
   *    <code>false</code> if no matching param was found
   */
  public boolean setArg(String attrName, String attrValue) {
    String normalizedAttrName = normalizeAttrName(attrName);
    for (int i = 0, n = fHandlerParams.length; i < n; ++i) {
      Object testParamName = fHandlerParams[i].getNormalizedName();
      if (testParamName.equals(normalizedAttrName)) {
        // Set it, but don't convert it yet.
        fAttrNames[i] = attrName;
        fArgValues[i] = attrValue;
        return true;
      }
    }
    return false;
  }

  private String normalizeAttrName(String attrName) {
    // NOTE: this is where other characters would be folded to '_'.
    return attrName.replace('-', '_');
  }

  // The real (non-normalized) names of the attributes, used to report errors.
  private final String[] fAttrNames;

  private final String[] fArgValues;

  private final HandlerParam[] fHandlerParams;

  private final int fLineNumber;

  private final Schema fSchema;

  private final String fElemName;
}
