/*
 * SearchDisplay.java
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
package org.rstudio.core.client.widget;

import com.google.gwt.event.dom.client.HasBlurHandlers;
import com.google.gwt.event.dom.client.HasFocusHandlers;
import com.google.gwt.event.dom.client.HasKeyDownHandlers;
import com.google.gwt.event.logical.shared.HasCloseHandlers;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.SuggestBox.DefaultSuggestionDisplay;
import com.google.gwt.user.client.ui.SuggestOracle;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;

public interface SearchDisplay extends
                              HasValue<String>,
                              HasSelectionCommitHandlers<String>,
                              HasSelectionHandlers<SuggestOracle.Suggestion>,
                              HasCloseHandlers<SearchDisplay>,
                              HasFocusHandlers,
                              HasBlurHandlers,
                              HasText,
                              HasKeyDownHandlers,
                              CanFocus
{
   void setAutoSelectEnabled(boolean selectsFirstItem);
   void clear();
   String getLastValue();
   
   // NOTE: only works if you are using the default display!
   DefaultSuggestionDisplay getSuggestionDisplay();
}
