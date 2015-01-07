/*
 * EditorLanguage.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.reditor;

/**
 * Models a language for CodeMirror.
 *
 * == HOW TO ADD A NEW LANGUAGE TO THE SOURCE EDITOR ==
 * 1) Edit ./resources/colors.css, add all necessary CSS rules there
 * 2) Put your parser file in ./resources/
 * 3) Add your parser to REditorResources, following the example of the other
 *    parsers
 * 4) Add your parser to this class's ALL_PARSER_URLS
 * 5) In this class, add a static LANG_xyz field for your language
 * 6) In this class, edit the static getLanguageForExtension to return your
 *    EditorLanguage for any applicable extensions
 */
public class EditorLanguage
{
   // RStudio-maintained or extended modes
   public static final EditorLanguage LANG_R = new EditorLanguage(
         "mode/r", true);
   public static final EditorLanguage LANG_RDOC = new EditorLanguage(
         "mode/rdoc", false);
   public static final EditorLanguage LANG_TEX = new EditorLanguage(
         "mode/tex", false);
   public static final EditorLanguage LANG_SWEAVE = new EditorLanguage(
         "mode/sweave", true);
   public static final EditorLanguage LANG_RMARKDOWN = new EditorLanguage(
         "mode/rmarkdown", true);
   public static final EditorLanguage LANG_DCF = new EditorLanguage(
         "mode/dcf", false);
   public static final EditorLanguage LANG_RHTML = new EditorLanguage(
         "mode/rhtml", true);
   public static final EditorLanguage LANG_CPP = new EditorLanguage(
         "mode/c_cpp", true);
   
   // Modes borrowed from Ace
   public static final EditorLanguage LANG_PLAIN = new EditorLanguage(
         "ace/mode/text", false);
   public static final EditorLanguage LANG_MARKDOWN = new EditorLanguage(
         "ace/mode/markdown", false);
   public static final EditorLanguage LANG_HTML = new EditorLanguage(
         "ace/mode/html", false);
   public static final EditorLanguage LANG_CSS = new EditorLanguage(
         "ace/mode/css", true);
   public static final EditorLanguage LANG_JAVASCRIPT = new EditorLanguage(
         "ace/mode/javascript", true);
   public static final EditorLanguage LANG_PYTHON = new EditorLanguage(
         "ace/mode/python", true);
   public static final EditorLanguage LANG_SQL = new EditorLanguage(
         "ace/mode/sql", true);
   public static final EditorLanguage LANG_SH = new EditorLanguage(
         "ace/mode/sh", true);
   public static final EditorLanguage LANG_YAML = new EditorLanguage(
         "ace/mode/yaml", true);
   public static final EditorLanguage LANG_XML = new EditorLanguage(
         "ace/mode/xml", true);
   
   public static final EditorLanguage LANG_CLOJURE = new EditorLanguage("ace/mode/clojure", true);
   public static final EditorLanguage LANG_COFFEE = new EditorLanguage("ace/mode/coffee", true);
   public static final EditorLanguage LANG_GITIGNORE = new EditorLanguage("ace/mode/gitignore", true);
   public static final EditorLanguage LANG_GO = new EditorLanguage("ace/mode/golang", true);
   public static final EditorLanguage LANG_GROOVY = new EditorLanguage("ace/mode/groovy", true);
   public static final EditorLanguage LANG_HASKELL = new EditorLanguage("ace/mode/haskell", true);
   public static final EditorLanguage LANG_HAXE = new EditorLanguage("ace/mode/haxe", true);
   public static final EditorLanguage LANG_JAVA = new EditorLanguage("ace/mode/java", true);
   public static final EditorLanguage LANG_JULIA = new EditorLanguage("ace/mode/julia", true);
   public static final EditorLanguage LANG_LISP = new EditorLanguage("ace/mode/lisp", true);
   public static final EditorLanguage LANG_LUA = new EditorLanguage("ace/mode/lua", true);
   public static final EditorLanguage LANG_MATLAB = new EditorLanguage("ace/mode/matlab", true);
   public static final EditorLanguage LANG_PERL = new EditorLanguage("ace/mode/perl", true);
   public static final EditorLanguage LANG_RUBY = new EditorLanguage("ace/mode/ruby", true);
   public static final EditorLanguage LANG_RUST = new EditorLanguage("ace/mode/rust", true);
 
   /**
    *
    * @param parserName The name of the parser--it's found at the top of the
    *    parser .js file
    * e. This MUST match the value inside the .js file or else
    *    dynamic language switching (Save As... with a different extension)
    *    won't work.
    * @param useRCompletion If true, then Tab is intercepted for completion
    */
   public EditorLanguage(
         String parserName,
         boolean useRCompletion)
   {
      parserName_ = parserName;
      useRCompletion_ = useRCompletion;
   }

   public String getParserName()
   {
      return parserName_;
   }

   public boolean useRCompletion()
   {
      return useRCompletion_;
   }

   private final String parserName_;
   private final boolean useRCompletion_;
}
