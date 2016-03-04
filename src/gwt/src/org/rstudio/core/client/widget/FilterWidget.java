/*
 * FilterWidget.java
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
package org.rstudio.core.client.widget;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SuggestOracle;

public abstract class FilterWidget extends Composite
{
   public abstract void filter(String query);
   
   public FilterWidget()
   {
      this("Filter...");
   }
   
   public FilterWidget(String placeholder)
   {
      SuggestOracle oracle = new SuggestOracle()
      {
         @Override
         public void requestSuggestions(Request request, Callback callback)
         {
         }
      };
      
      searchWidget_ = new SearchWidget(oracle);
      searchWidget_.setPlaceholderText(placeholder);
      searchWidget_.addValueChangeHandler(new ValueChangeHandler<String>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<String> event)
         {
            filter(event.getValue());
         }
      });
      initWidget(searchWidget_);
   }
   
   private final SearchWidget searchWidget_;
}
