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
 * Represents a type of event whose performance is tracked while running the
 * {@link com.google.gwt.dev.Compiler}
 */
public enum CompilerEventType implements EventType {
  CODE_SPLITTER("CodeSplitter", "Yellow"), //
  COMPILE("Compiler", "DarkBlue"), //
  COMPILE_PERMUTATIONS("CompilePermutations", "Moccasin"), //
  DRAFT_OPTIMIZE("DraftOptimizer", "Blue"), //
  GENERATOR("Generator", "Red"), //
  JDT_COMPILER("JdtCompiler", "FireBrick"), //
  LINK("Link", "LawnGreen"), //
  MAKE_SOYC_ARTIFACTS("MakeSoycArtifacts", "Chartreuse"), //
  MODULE_DEF("ModuleDef", "BlueViolet"), //
  OPTIMIZE("Optimize", "LightSlateGray"), //
  PRECOMPILE("Precompile", "CornflowerBlue"), //
  RESOURCE_ORACLE("ResourceOracle", "GoldenRod"), //
  TYPE_ORACLE_MEDIATOR("TypeOracleMediator", "LightSteelBlue"); //

  final String cssColor;
  final String name;

  CompilerEventType(String name, String cssColor) {
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
