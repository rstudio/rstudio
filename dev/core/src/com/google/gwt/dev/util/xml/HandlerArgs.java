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
package com.google.gwt.dev.util.xml;

import com.google.gwt.core.ext.UnableToCompleteException;

/**
 * A set of args for a given set of parameters, some of which may be set to
 * default values.
 */
public class HandlerArgs {

  // The real (non-normalized) names of the attributes, used to report errors.
  private final String[] attrNames;

  private final String[] argValues;

  private final HandlerParam[] handlerParams;

  private final int lineNumber;

  private final Schema schema;

  private final String elemName;

  public HandlerArgs(Schema schema, int lineNumber, String elemName,
      HandlerParam[] handlerParams) {
    this.schema = schema;
    this.lineNumber = lineNumber;
    this.elemName = elemName;
    this.handlerParams = handlerParams;
    attrNames = new String[handlerParams.length];
    argValues = new String[handlerParams.length];

    // Set default values.
    //
    for (int i = 0, n = handlerParams.length; i < n; ++i) {
      argValues[i] = this.handlerParams[i].getDefaultValue(schema);
    }
  }

  /**
   * @return the argument converted to a form that is expected to compatible
   *         with the associated parameter and will work for a reflection
   *         "invoke()" call
   */
  public Object convertToArg(int i) throws UnableToCompleteException {
    String value = argValues[i];
    if (value != null) {
      AttributeConverter converter = schema.getAttributeConverter(handlerParams[i].getParamType());
      return converter.convertToArg(schema, lineNumber, elemName, attrNames[i],
          value);
    } else if (handlerParams[i].isOptional()) {
        return null;
    } else {
      return new NullPointerException("Argument " + i + " was null");
    }
  }

  public int getArgCount() {
    return handlerParams.length;
  }

  public String getArgName(int i) {
    return handlerParams[i].getNormalizedName();
  }

  public boolean isArgSet(int i) {
    if (argValues[i] != null || handlerParams[i].isOptional()) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * @return <code>true</code> if the param for the specified attribute was
   *         set; <code>false</code> if no matching param was found
   */
  public boolean setArg(String attrName, String attrValue) {
    String normalizedAttrName = normalizeAttrName(attrName);
    for (int i = 0, n = handlerParams.length; i < n; ++i) {
      Object testParamName = handlerParams[i].getNormalizedName();
      if (testParamName.equals(normalizedAttrName)) {
        // Set it, but don't convert it yet.
        attrNames[i] = attrName;
        argValues[i] = attrValue;
        return true;
      }
    }
    return false;
  }

  private String normalizeAttrName(String attrName) {
    // NOTE: this is where other characters would be folded to '_'.
    return attrName.replace('-', '_');
  }
}
