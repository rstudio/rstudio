package org.rstudio.core.client.widget;

import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import com.google.gwt.user.client.ui.SuggestOracle;

import com.google.gwt.event.dom.client.HasBlurHandlers;
import com.google.gwt.event.dom.client.HasFocusHandlers;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.SuggestBox.DefaultSuggestionDisplay;

public interface SearchDisplay extends 
                              HasValueChangeHandlers<String>,
                              HasSelectionCommitHandlers<String>,
                              HasSelectionHandlers<SuggestOracle.Suggestion>,
                              HasFocusHandlers,
                              HasBlurHandlers,
                              HasText
{
   void setAutoSelectEnabled(boolean selectsFirstItem);
   void clear();
   
   // NOTE: only works if you are using the default display!
   DefaultSuggestionDisplay getSuggestionDisplay();
}
