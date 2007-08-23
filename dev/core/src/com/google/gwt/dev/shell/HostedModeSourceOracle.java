/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.dev.shell;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.CompilationUnitProvider;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.jdt.StandardSourceOracle;
import com.google.gwt.dev.jdt.StaticCompilationUnitProvider;
import com.google.gwt.util.tools.Utility;

import java.io.IOException;

/**
 * Does a little extra magic to handle hosted mode JSNI and
 * <code>GWT.create()</code>.
 */
public class HostedModeSourceOracle extends StandardSourceOracle {

  private final JsniInjector injector;

  public HostedModeSourceOracle(TypeOracle typeOracle) {
    super(typeOracle);
    this.injector = new JsniInjector(typeOracle);
  }

  @Override
  protected CompilationUnitProvider doFilterCompilationUnit(TreeLogger logger,
      String typeName, CompilationUnitProvider existing)
      throws UnableToCompleteException {

    /*
     * MAGIC: The implementation of GWT can be very different between hosted
     * mode and web mode. The compiler has special knowledge of GWT for web
     * mode. The source for hosted mode is in GWT.java-hosted.
     */
    if (typeName.equals("com.google.gwt.core.client.GWT")) {
      try {
        String source = Utility.getFileFromClassPath("com/google/gwt/core/client/GWT.java-hosted");
        return new StaticCompilationUnitProvider("com.google.gwt.core.client",
            "GWT", source.toCharArray());
      } catch (IOException e) {
        logger.log(
            TreeLogger.ERROR,
            "Unable to load 'com/google/gwt/core/client/GWT.java-hosted' from class path; is your installation corrupt?",
            e);
        throw new UnableToCompleteException();
      }
    }

    // Otherwise, it's a regular translatable type, but we want to make sure
    // its JSNI stuff, if any, gets handled.
    //
    CompilationUnitProvider jsnified = injector.inject(logger, existing);
    return jsnified;
  }
}
