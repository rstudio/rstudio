/*
 * ApplicationView.java
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

package org.rstudio.studio.client.application;

import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.widget.Operation;

public interface ApplicationView
{       
   // show application agreement
   void showApplicationAgreement(String title,
                             String contents, 
                             Operation doNotAcceptOperation,
                             Operation acceptOperation);
   
   // set current main view for application
   void showWorkbenchView(Widget widget);
   
   // toolbar
   void showToolbar(boolean showToolbar);
   
   // go to function
   void performGoToFunction();
   
   // application exit states
   void showApplicationQuit();
   void showApplicationSuicide(String reason);
   void showApplicationDisconnected();
   void showApplicationOffline();
   void showApplicationUpdateRequired();
   
   // error messages
   void showSessionAbendWarning();
   
   // progress
   void showSerializationProgress(String message, 
                                  boolean modal, 
                                  int delayMs, 
                                  int timeoutMs);
   void hideSerializationProgress();
   
   Widget getWidget() ;

   void showWarning(boolean severe, String message);
   void hideWarning();
}

