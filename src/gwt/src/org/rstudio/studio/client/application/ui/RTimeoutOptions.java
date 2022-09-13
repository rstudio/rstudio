/*
 * RTimeoutOptions.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.application.ui;

import org.rstudio.studio.client.application.Desktop;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.studio.client.application.StudioClientApplicationConstants;

public class RTimeoutOptions extends Composite
{
   private static RTimeoutOptionsUiBinder uiBinder = GWT.create(RTimeoutOptionsUiBinder.class);

   interface RTimeoutOptionsUiBinder extends UiBinder<Widget, RTimeoutOptions>
   {
   }
   
   public interface RTimeoutObserver
   {
      void onReload();
      void onSafeMode();
      void onTerminate();
   }

   public RTimeoutOptions()
   {
      initWidget(uiBinder.createAndBindUi(this));

      reload_.addClickHandler((click) ->
      {
         setStatus(constants_.reloadingText());
         observer_.onReload();
      });

      if (Desktop.isDesktop())
      {
         // We don't currently have plumbing for safe mode retries in desktop mode.
         safeMode_.setVisible(false);
      }
      else
      {
         safeMode_.addClickHandler((click) ->
         {
            setStatus(constants_.retryInSafeModeText());
            observer_.onSafeMode();
         });
      }
      
      terminate_.addClickHandler((click) ->
      {
         setStatus(constants_.terminatingRText());
         observer_.onTerminate();
      });
   }
   
   public void setObserver(RTimeoutObserver observer)
   {
      observer_ = observer;
   }

   public String getMessage()
   {
      return visibleMsg_.getText();
   }

   private void setStatus(String status)
   {
      // Disable buttons to prevent attempts to abort these abortive procedures
      // (which won't work)
      reload_.setEnabled(false);
      terminate_.setEnabled(false);
      safeMode_.setEnabled(false);
      
      // Show user what we're up to
      status_.setText(status);
   }

   RTimeoutObserver observer_;
   
   @UiField Button reload_;
   @UiField Button safeMode_;
   @UiField Button terminate_;
   @UiField Label status_;
   @UiField Label visibleMsg_;
   private static final StudioClientApplicationConstants constants_ = GWT.create(StudioClientApplicationConstants.class);
}
