/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.dev.util.log.speedtracer;

import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.EventType;

/**
 * Represents a type of event whose performance is tracked while running
 * {@link com.google.gwt.dev.DevMode}
 */
public enum DevModeEventType implements EventType {
  CLASS_BYTES_REWRITE("Class bytes rewrite", "DarkBlue"), // foil Eclipse
  COMPILATION_STATE_BUILDER_PROCESS("CompilationStateBuilder process",
      "CornflowerBlue"), //
  CREATE_UI("Create UI", "BlueViolet"), //
  GENERATED_UNITS_ADD("Generated units add", "FireBrick"), //
  GENERATOR_RUN("Generator run", "Red"), //
  JAVA_TO_JS_CALL("Java to JS call", "LightSteelBlue"), //
  JDT_COMPILE("JDT compile", "Blue"), //
  JETTY_STARTUP("Jetty startup", "Chartreuse"), //
  JS_TO_JAVA_CALL("JS to Java call", "LightSlateGray"), //
  MODULE_DEF_LOAD("ModuleDef load", "Yellow"), //
  MODULE_DEF_NORMALIZE("ModuleDef normalize", "GoldenRod"), //
  MODULE_DEF_REFRESH("ModuleDef refresh", "GoldenRod"), //
  MODULE_INIT("Module init", "LightGoldenRodYellow"), //
  MODULE_SPACE_CLASS_LOAD("ModuleSpace class load", "Ivory"), //
  MODULE_SPACE_HOST_CREATE("ModuleSpaceHost create", "Yellow"), //
  MODULE_SPACE_HOST_READY("ModuleSpaceHost ready", "Moccasin"), //
  MODULE_SPACE_LOAD("ModuleSpace load", "LemonChiffon"), //
  MODULE_SPACE_REBIND_AND_CREATE("ModuleSpace rebindAndCreate", "Crimson"), //
  ON_MODULE_LOAD("onModuleLoad", "LawnGreen"), //
  REBIND("Rebind", "Tomato"), //
  REBOUND_TYPE_CREATE("Rebound type create", "Crimson"), //
  RESOURCE_ORACLE_REFRESH("ResourceOracle refresh", "Orange"), //
  SLOW_STARTUP("Slow startup", "DarkSeaGreen"), //
  STARTUP("Startup", "LimeGreen"), //
  TYPE_ORACLE_ADD_NEW_UNITS("TypeOracle add new units", "Magenta"), //
  VISIT_CLASS_FILE("Visit class file", "RoyalBlue");

  final String cssColor;
  final String name;

  DevModeEventType(String name, String cssColor) {
    this.name = name;
    this.cssColor = cssColor;
  }

  public String getColor() {
    return cssColor;
  }

  public String getName() {
    return name;
  }
}
