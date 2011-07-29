package org.rstudio.studio.client.workbench.codesearch;

import java.util.ArrayList;

import org.rstudio.core.client.widget.SearchWidget;

import com.google.gwt.user.client.ui.SuggestOracle;

public class CodeSearchWidget extends SearchWidget
{
   public CodeSearchWidget()
   {
      super(new SuggestOracle()
      {
         @Override
         public void requestSuggestions(Request request,
                                        Callback callback)
         {
            callback.onSuggestionsReady(
                  request,
                  new Response(new ArrayList<Suggestion>()));
         }
      });
   }
}
