/*
 * PanmirrorUISpelling.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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



package org.rstudio.studio.client.panmirror.ui;

import org.rstudio.studio.client.panmirror.spelling.PanmirrorWordRange;

import elemental2.core.JsArray;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsType;

@JsType
public class PanmirrorUISpelling {
   
   // realtime interface
   public GetBool realtimeEnabled;
   public CheckWord checkWord;
   public SuggestionList suggestionList;
   
   // dictionary
   public CheckWord isWordIgnored;
   public DictionaryFunction ignoreWord;
   public DictionaryFunction unignoreWord;
   public DictionaryFunction addToDictionary;
   
   // word breaking
   public BreakWords breakWords;
   public ClassifyCharacter classifyCharacter;
   
   @JsFunction
   public interface GetBool
   {
      boolean get();
   }
   
   @JsFunction
   public interface CheckWord
   {
      boolean check(String word);
   }
   
   @JsFunction
   public interface SuggestionList
   {
      JsArray<String> suggest(String word);
   }
   
   @JsFunction
   public interface DictionaryFunction
   {
      void call(String word);
   }
   
   @JsFunction
   public interface BreakWords
   {
      JsArray<PanmirrorWordRange> breakWords(String text);
   }
   
   @JsFunction
   public interface ClassifyCharacter
   {
      int classify(char ch);
   }
      
}
