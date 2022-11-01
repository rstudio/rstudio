/*
 * CodeSearchWidget.java
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
package org.rstudio.studio.client.workbench.codesearch.ui;

import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.SearchDisplay;
import org.rstudio.core.client.widget.SearchWidget;
import org.rstudio.core.client.widget.TextBoxWithCue;
import org.rstudio.studio.client.workbench.codesearch.CodeSearch;
import org.rstudio.studio.client.workbench.codesearch.CodeSearchConstants;
import org.rstudio.studio.client.workbench.codesearch.CodeSearchOracle;

import com.google.gwt.user.client.ui.SuggestBox;
import com.google.inject.Inject;


public class CodeSearchWidget extends SearchWidget 
                              implements CodeSearch.Display
{
   @Inject
   public CodeSearchWidget(CodeSearchOracle oracle)
   {
      super(constants_.codeSearchLabel(),
            oracle,
            new TextBoxWithCue(constants_.textBoxWithCue()),
            new SuggestBox.DefaultSuggestionDisplay());
      
      oracle_ = oracle;   
      
      CodeSearchResources res = CodeSearchResources.INSTANCE;
      setIcon(new ImageResource2x(res.gotoFunction2x()));
      addStyleName(res.styles().codeSearchWidget());
      
      ElementIds.assignElementId(this, ElementIds.CODE_SEARCH_WIDGET);
   }

   @Override
   public SearchDisplay getSearchDisplay()
   {
      return this;
   }
   
   @Override
   public void setCueText(String text)
   {
      ((TextBoxWithCue)getTextBox()).setCueText(text);
   }
   
   @Override
   public CodeSearchOracle getSearchOracle()
   {
      return oracle_;
   }
   
   private final CodeSearchOracle oracle_;
   private static final CodeSearchConstants constants_ = GWT.create(CodeSearchConstants.class);
}
