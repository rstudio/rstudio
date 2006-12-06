// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.jdt.RebindOracle;
import com.google.gwt.dev.jdt.RebindPermutationOracle;

import java.util.Map;

final class RebindPermOracleImpl implements RebindPermutationOracle,
    RebindOracle {

  public RebindPermOracleImpl(Map rebinds) {
    this.rebinds = rebinds;
  }

  public String[] getAllPossibleRebindAnswers(TreeLogger logger,
      String sourceTypeName) throws UnableToCompleteException {
    return new String[]{rebind(logger, sourceTypeName)};
  }

  public String rebind(TreeLogger logger, String sourceTypeName)
      throws UnableToCompleteException {
    String result = (String) rebinds.get(sourceTypeName);
    if (result != null) {
      return result;
    } else {
      return sourceTypeName;
    }
  }

  private final Map rebinds;
}
