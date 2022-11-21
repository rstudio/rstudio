/*
 * PanmirrorUISpelling.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */



package org.rstudio.studio.client.panmirror.ui;

import elemental2.core.Function;
import org.rstudio.studio.client.panmirror.spelling.PanmirrorWordRange;

import elemental2.core.JsArray;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsType;

@JsType
public class PanmirrorUISpelling {
   
   // realtime interface
   public CheckWord checkWords;
   public SuggestionList suggestionList;
   
   // dictionary
   public ShouldCheckWord isWordIgnored;
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
   public interface ShouldCheckWord
   {
      boolean check(String word);
   }

   @JsFunction
   public interface CheckWord
   {
      String[] checkWords(String[] word);
   }
   
   @JsFunction
   public interface SuggestionList
   {
      void suggestionList(String word, Function callback);
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
