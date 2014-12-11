/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.util.arg;

import com.google.gwt.thirdparty.guava.common.base.Splitter;
import com.google.gwt.util.tools.ArgHandlerString;

import java.util.List;

/**
 * An argument handler to parse the -setProperty argument. Set the allowed values of a given
 * property to the given values. The format is like: [-setProperty name=value,value,...,value].
 * -setProperty flag can be used multiple times to set different properties. If a property is set
 * multiple times, the last one over writes the others.
 */
public class ArgHandlerSetProperties extends ArgHandlerString {

  private final OptionSetProperties options;

  public ArgHandlerSetProperties(OptionSetProperties options) {
    this.options = options;
  }

  @Override
  public boolean setString(String str) {
    assert (str != null);
    List<String> nameValuePair = Splitter.on("=").trimResults().omitEmptyStrings().splitToList(str);
    if (nameValuePair.size() != 2) {
      return false;
    }
    String name = nameValuePair.get(0);
    String valuesList = nameValuePair.get(1);
    Iterable<String> values = Splitter.on(",").trimResults().omitEmptyStrings().split(valuesList);
    options.setPropertyValues(name, values);
    return true;
  }

  @Override
  public String getPurpose() {
    return "Set the values of a property in the form of propertyName=value1[,value2...].";
  }

  @Override
  public String getTag() {
    return "-setProperty";
  }

  @Override
  public String[] getTagArgs() {
    return new String[] {"name=value,value..."};
  }
}
