/*
 * HelpServerOperations.java
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
package org.rstudio.studio.client.workbench.views.help.model;

import com.google.gwt.core.client.JsArrayString;
import org.rstudio.studio.client.server.ServerRequestCallback;

public interface HelpServerOperations
{
   void suggestTopics(String prefix,
                      ServerRequestCallback<JsArrayString> requestCallback);

   void getHelp(String topic, 
                String packageName,
                int options,
                ServerRequestCallback<HelpInfo> requestCallback);
   
   String getApplicationURL(String topicURI);

   void showHelpTopic(String topic, String pkgName, int type);

   void search(String query, 
               ServerRequestCallback<JsArrayString> requestCallback);
   
   void getCustomHelp(String helpHandler,
                      String topic,
                      String source,
                      String language,
                      ServerRequestCallback<HelpInfo.Custom> requestCallback);
   
   void getCustomParameterHelp(String helpHandler,
                               String source,
                               String language,
                               ServerRequestCallback<HelpInfo.Custom> requestCallback);
   
   void showCustomHelpTopic(String helpHandler, String topic, String source);
}
