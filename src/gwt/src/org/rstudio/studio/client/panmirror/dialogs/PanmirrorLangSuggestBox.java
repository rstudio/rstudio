/*
 * PanmirrorLangSuggestBox.java
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


package org.rstudio.studio.client.panmirror.dialogs;


import java.util.ArrayList;

import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.TextBox;


public class PanmirrorLangSuggestBox extends SuggestBox
{
   public PanmirrorLangSuggestBox(String[] languages)
   {
      super(
         suggestOracle(languages),
         new TextBox(),
         new LangaugesSuggestionDisplay()
      );
      setLimit(8);
   }
   
   private static class LangaugesSuggestionDisplay extends SuggestBox.DefaultSuggestionDisplay
   {
      public LangaugesSuggestionDisplay()
      {
         addPopupStyleName(RES.styles().langSuggestionDisplay());
      }
      public void addPopupStyleName(String name)
      {
         this.getPopupPanel().addStyleName(name);
      }
   }
   

   private static SuggestOracle suggestOracle(String[] languages) 
   {
      return new SuggestOracle() 
      {

         @Override
         public void requestSuggestions(Request request, Callback callback)
         {
            String query = request.getQuery();
            
            ArrayList<Suggestion> suggestions = new ArrayList<Suggestion>();
            for (int i=0; i<languages.length; i++)
            {
               String language = languages[i];
               if (language.startsWith(query))
                  suggestions.add(new LanguageSuggestion(language));
               
               if (suggestions.size() > request.getLimit())
                  break;
            }
            
            callback.onSuggestionsReady(request, new Response(suggestions));
         }
         
         class LanguageSuggestion implements Suggestion
         {
            public LanguageSuggestion(String language)
            {
               language_ = language;
            }

            @Override
            public String getDisplayString()
            {
               return language_;
            }

            @Override
            public String getReplacementString()
            {
               return language_;
            }
            
            private final String language_;
            
         }
         
      };
   }
   
   
   private static PanmirrorDialogsResources RES = PanmirrorDialogsResources.INSTANCE;

}
