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
package com.google.gwt.dev.util.arg;

import com.google.gwt.util.tools.ArgHandlerFile;

import java.io.File;

/**
 * Argument handler for processing the script style flag.
 */
public final class ArgHandlerDumpSignatures extends ArgHandlerFile {

  public ArgHandlerDumpSignatures() {
  }

  @Override
  public String getPurpose() {
    return "(deprecated) Dump the signatures of all loaded types and their members";
  }

  @Override
  public String getTag() {
    return "-dumpSignatures";
  }

  @Override
  public String[] getTagArgs() {
    return new String[]{"style"};
  }

  @Override
  public boolean isUndocumented() {
    return true;
  }

  @Override
  public void setFile(File file) {
    System.err.println("[WARN] '" + getTag() + "' is deprecated; ignoring");
  }
}
