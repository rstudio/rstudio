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

package com.google.gwt.user.rebind.rpc;

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.ConfigurationProperty;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JRealClassType;
import com.google.gwt.util.regexfilter.RegexFilter;

import java.util.List;

class BlacklistTypeFilter implements TypeFilter {
  private static String PROP_RPC_BLACKLIST = "rpc.blacklist";

  /**
   * Configure {@link RegexFilter} for use for RPC blacklists.
   */
  private static class RpcBlacklist extends RegexFilter {
    public RpcBlacklist(TreeLogger logger, List<String> regexes) throws UnableToCompleteException {
      super(logger, regexes);
    }

    @Override
    protected boolean acceptByDefault() {
      return true;
    }

    @Override
    protected boolean entriesArePositiveByDefault() {
      return false;
    }
  }

  private final RpcBlacklist blacklist;
  private TreeLogger logger;

  public BlacklistTypeFilter(TreeLogger logger, PropertyOracle propertyOracle)
      throws UnableToCompleteException {
    ConfigurationProperty prop;
    try {
      prop = propertyOracle.getConfigurationProperty(PROP_RPC_BLACKLIST);
    } catch (BadPropertyValueException e) {
      logger.log(TreeLogger.ERROR, "Could not find property " + PROP_RPC_BLACKLIST);
      throw new UnableToCompleteException();
    }

    this.logger = logger.branch(TreeLogger.DEBUG, "Analyzing RPC blacklist information");
    blacklist = new RpcBlacklist(logger, prop.getValues());
  }

  public String getName() {
    return "BlacklistTypeFilter";
  }

  public boolean isAllowed(JClassType type) {
    String name = getBaseTypeName(type);
    // For types not handled by getBaseTypeName just return true.
    if (name == null) {
      return true;
    }

    return blacklist.isIncluded(logger, name);
  }

  /**
   * Returns a simple qualified name for simple types, including classes and
   * interfaces, parameterized, and raw types. Null is returned for other types
   * such as arrays and type parameters (e.g., 'E' in java.util.List<E>) because
   * filtering is meaningless for such types.
   */
  private String getBaseTypeName(JClassType type) {
    JClassType baseType = null;

    if (type instanceof JRealClassType) {
      baseType = type;
    } else if (type.isParameterized() != null) {
      baseType = type.isParameterized().getBaseType();
    } else if (type.isRawType() != null) {
      baseType = type.isRawType();
    }

    return baseType == null ? null : baseType.getQualifiedSourceName();
  }
}
