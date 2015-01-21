/*
 * RSConnectAuthPresenter.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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
package org.rstudio.studio.client.rsconnect.ui;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class RSConnectAuthPresenter implements IsWidget
{
   public interface Display extends IsWidget
   {
      void showClaimUrl(String url);
   }
   
   @Inject
   public RSConnectAuthPresenter(Display view)
   {
      view_ = view;
   }     

   @Override
   public Widget asWidget()
   {
      return view_.asWidget();
   }
   
   public void showClaimUrl(String url)
   {
      view_.showClaimUrl(url);
   }
   
   private final Display view_;
}