/*
 * CodeSearchWidget.java
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
package org.rstudio.studio.client.workbench.codesearch.ui;



import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.SearchDisplay;
import org.rstudio.core.client.widget.SearchWidget;
import org.rstudio.core.client.widget.TextBoxWithCue;
import org.rstudio.studio.client.workbench.codesearch.CodeSearch;
import org.rstudio.studio.client.workbench.codesearch.CodeSearchOracle;
import org.rstudio.studio.client.workbench.commands.Commands;

import com.google.gwt.user.client.ui.SuggestBox;
import com.google.inject.Inject;


public class CodeSearchWidget extends SearchWidget 
                              implements CodeSearch.Display
{
   @Inject
   public CodeSearchWidget(CodeSearchOracle oracle,
                           final Commands commands)
   {
      super(oracle, 
            new TextBoxWithCue("Go to file/function"), 
            new SuggestBox.DefaultSuggestionDisplay());
      
      oracle_ = oracle;   
      
      CodeSearchResources res = CodeSearchResources.INSTANCE;
      
      setIcon(new ImageResource2x(res.gotoFunction2x()));       
      
      addStyleName(res.styles().codeSearchWidget());
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

  
}
