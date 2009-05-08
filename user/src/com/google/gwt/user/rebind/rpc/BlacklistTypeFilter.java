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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

class BlacklistTypeFilter implements TypeFilter {
  
  private List<Boolean> includeType;
  private TreeLogger logger;
  private List<Pattern> typePatterns;
  private List<String> values;
  
  public BlacklistTypeFilter(TreeLogger logger, PropertyOracle propertyOracle)
      throws UnableToCompleteException {
    this.logger = logger.branch(TreeLogger.DEBUG,
        "Analyzing RPC blacklist information");
    try {
      ConfigurationProperty prop
          = propertyOracle.getConfigurationProperty("rpc.blacklist");

      values = prop.getValues();
      int size = values.size();
      typePatterns = new ArrayList<Pattern>(size);
      includeType = new ArrayList<Boolean>(size);

      // TODO investigate grouping multiple patterns into a single regex
      for (String regex : values) {
        // Patterns that don't start with [+-] are considered to be [-]
        boolean include = false;
        // Ignore empty regexes
        if (regex.length() == 0) {
          logger.log(TreeLogger.ERROR, "Got empty RPC blacklist entry");
          throw new UnableToCompleteException();
        }
        char c = regex.charAt(0);
        if (c == '+' || c == '-') {
          regex = regex.substring(1); // skip initial character
          include = (c == '+');
        }
        try {
          Pattern p = Pattern.compile(regex);
          typePatterns.add(p);
          includeType.add(include);
          
          logger.log(TreeLogger.DEBUG,
              "Got RPC blacklist entry '" + regex + "'");
        } catch (PatternSyntaxException e) {
          logger.log(TreeLogger.ERROR,
              "Got malformed RPC blacklist entry '" + regex + "'");
          throw new UnableToCompleteException();
        }
      }
    } catch (BadPropertyValueException e) {
      logger.log(TreeLogger.DEBUG, "No RPC blacklist entries present");
    }
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
    
    // Process patterns in reverse order for early exit
    int size = typePatterns.size();
    for (int idx = size - 1; idx >= 0; idx--) {
      logger.log(TreeLogger.DEBUG, "Considering RPC rule " + values.get(idx)
          + " for type " + name);
      boolean include = includeType.get(idx);
      Pattern pattern = typePatterns.get(idx);
      if (pattern.matcher(name).matches()) {
        if (include) {
          logger.log(TreeLogger.DEBUG, "Whitelisting type " + name
              + " according to rule " + values.get(idx));
          return true;
        } else {
          logger.log(TreeLogger.DEBUG, "Blacklisting type " + name
              + " according to rule " + values.get(idx));
          return false;
        }
      }
    }
    
    // Type does not match any pattern, pass it through
    return true;
  }

  /**
   * Returns a simple qualified name for simple types, including classes and
   * interfaces, parameterized, and raw types.  Null is returned for other types
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

