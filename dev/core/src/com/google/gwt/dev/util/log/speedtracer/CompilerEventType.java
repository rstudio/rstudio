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
  COMPILE_PERMUTATIONS("CompilePermutations", "BurlyWood"), //
  JJS_COMPILE_PERMUTATION("JjsCompilePermutation", "Moccasin"), //
  DRAFT_OPTIMIZE("DraftOptimizer", "Blue"), //
  GENERATOR_CLIENT_BUNDLE("Generator ClientBundle", "#CCCC33"), //
  GENERATOR_I18N("Generator I18N", "#FF00CC"), //
  GENERATOR_RPC("Generator RPC", "#3300CC"), //
  GENERATOR_RPC_STOB("Generator RPC STOB", "#3300CC"), //
  GENERATOR_RPC_TYPE_SERIALIZER("Generator RPC Type Serializer", "#3300CC"), //
  GENERATOR_RPC_FIELD_SERIALIZER(
      "Generator RPC Field Serializer", "#3300CC"), //
  GENERATOR_UIBINDER("Generator UiBinder", "#FFFF00"), //
  GENERATOR_GIN("Generator GIN", "#009900"), //
  GENERATOR_OTHER("Generator (Other)", "Red"), //
  JDT_COMPILER("JdtCompiler", "#6c6"), //
  JDT_COMPILER_CSB_FROM_ORACLE("JdtCompiler CSB FromOracle", "LimeGreen"), //
  JDT_COMPILER_CSB_GENERATED("JdtCompiler CSB Generated", "DarkSeaGreen"), //
  JDT_COMPILER_SANDBOX("JdtCompiler Sandbox", "#494"), //
  LINK("Link", "LawnGreen"), //
  MAKE_SOYC_ARTIFACTS("MakeSoycArtifacts", "Chartreuse"), //
  MODULE_DEF("ModuleDef", "BlueViolet"), //
  OPTIMIZE("Optimize", "LightSlateGray"), //
  OPTIMIZE_JS("OptimizeJs", "#8b8bab"), //
  PRECOMPILE("Precompile", "CornflowerBlue"), //
  RESOURCE_ORACLE("ResourceOracle", "GoldenRod"), //
  TYPE_ORACLE_MEDIATOR("TypeOracleMediator", "LightSteelBlue"), //
  PRECOMPILE_CORE("CoreCompiler", "Crimson"), //
  WRITE_OBJECT_AS_FILE("WriteObjectAsFile", "Magenta"), //
  BUILD_AST("BuildAST", "DarkGoldenRod"), //
  FUNCTION_CLUSTER("JsFunctionClusterer", "Cornflower"), //
  BLOCKED("Blocked", "FloralWhite"), //
  GET_ALL_REBINDS("GetAllPossibleRebindAnswers", "Brown"), //
  FIND_ENTRY_POINTS("FindEntryPoints", "SlateGray"), //
  VALIDATE("Validate", "YellowGreen"), //
  NORMALIZER("Normalizer", "Peru"), //
  RECORD_REBINDS("RecordRebinds", "Chocolate"), //
  BUILD_TYPE_MAP_FOR_AST("BuildTypeMapForAST", "LightSeaGreen"), //
  GET_COMPILATION_UNITS("GetCompilationUnitDeclarations", "Gainsboro"), //
  GENERATE_JAVA_AST("GenerateJavaAST", "DodgerBlue"), //
  FIX_ASSIGNMENT_TO_UNBOX("FixAssignmentToUnbox", "Orange"), //
  ASSERTION_NORMALIZER("AssertionNormalizer", "LightPink"), //
  ASSERTION_REMOVER("AssertionRemover", "LightSalmon"), //
  REPLACE_REBINDS("ReplaceRebinds", "Snow"), //
  CREATE_UNIFIED_AST("CreateUnifiedAST", "BlueViolet"), //
  CHECK_FOR_ERRORS("CheckForErrors", "DimGrey"), //
  GRAPHICS_INIT("Graphics2D.createGraphics()", "Blue"), //
  ANALYZE_MODULE("AnalyzeModule", "LightBlue"), //
  COMPILE_MODULE("CompileModule", "LightBlue"), 
  LOAD_ARCHIVE("LoadArchive", "DarkSlateBlue"); //

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
