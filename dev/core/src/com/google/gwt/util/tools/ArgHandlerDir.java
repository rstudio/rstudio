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
package com.google.gwt.util.tools;

import java.io.File;

/**
 * Argument handler for arguments that are directories.  
 */
public abstract class ArgHandlerDir extends ArgHandler {

  @Override
  public String[] getTagArgs() {
    return new String[]{"dir"};
  }

  @Override
  public int handle(String[] args, int startIndex) {
    if (startIndex + 1 < args.length) {
      setDir(new File(args[startIndex + 1]));
      return 1;
    }

    System.err.println(getTag()
      + " should be followed by the name of a directory");
    return -1;
  }

  public abstract void setDir(File dir);

}
