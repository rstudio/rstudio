/*
 * EditorLanguage.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.reditor;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import org.rstudio.studio.client.common.reditor.resources.REditorResources;

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
   public static final String STYLES_URL =
         REditorResources.INSTANCE.colors().getUrl();

   // All possible parser URLs we might want to use must be listed here.
   // CodeMirror must have access to them all at startup or else it won't
   // be able to dynamically switch between them.
   private static final String[] ALL_PARSER_URLS = {
         REditorResources.INSTANCE.parser_r().getUrl(),
         REditorResources.INSTANCE.parser_latex().getUrl(),
         REditorResources.INSTANCE.parser_sweave().getUrl(),
         REditorResources.INSTANCE.parser_dummy().getUrl(),
   };

   public static final EditorLanguage LANG_R = new EditorLanguage(
         0, "RParser", true);
   public static final EditorLanguage LANG_TEX = new EditorLanguage(
         1, "LatexParser", false);
   public static final EditorLanguage LANG_SWEAVE = new EditorLanguage(
         2, "SweaveParser", true);
   public static final EditorLanguage LANG_PLAIN = new EditorLanguage(
         3, "DummyParser", false);

   /**
    *
    * @param parserIndex The index within ALL_PARSER_URLS that corresponds with
    *    this language's parser.
    * @param parserName The name of the parser--it's found at the top of the
    *    parser .js file. This MUST match the value inside the .js file or else
    *    dynamic language switching (Save As... with a different extension)
    *    won't work.
    * @param useRCompletion If true, then Tab is intercepted for completion
    *    purposes. If not, then CodeMirror always handles it.
    */
   public EditorLanguage(int parserIndex,
                         String parserName,
                         boolean useRCompletion)
   {
      parserIndex_ = parserIndex;
      parserName_ = parserName;
      useRCompletion_ = useRCompletion;
   }

   public JsArrayString getAllParserUrlsWithThisOneLast()
   {
      JsArrayString arr = JavaScriptObject.createArray().cast();
      for (int i = 0; i < ALL_PARSER_URLS.length; i++)
      {
         arr.push(ALL_PARSER_URLS[i]);
      }
      arr.push(ALL_PARSER_URLS[parserIndex_]);
      return arr;
   }

   public String getParserName()
   {
      return parserName_;
   }

   public boolean useRCompletion()
   {
      return useRCompletion_;
   }

   private final int parserIndex_;
   private final String parserName_;
   private final boolean useRCompletion_;
}
