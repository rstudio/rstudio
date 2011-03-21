/*
 * HelpServerOperations.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.help.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import org.rstudio.studio.client.server.ServerRequestCallback;

import java.util.ArrayList;

public interface HelpServerOperations 
{
   public static final String HISTORY = "history" ;
   public static final String FAVORITES = "favorites" ;
   
   public class LinksList extends JavaScriptObject
   {
      protected LinksList()
      {
      }
      
      public final ArrayList<Link> getLinks()
      {
         ArrayList<Link> results = new ArrayList<Link>() ;
         for (int i = 0; i < getUrls().length(); i++)
            results.add(new Link(getUrls().get(i), getTitles().get(i))) ;
         return results ;
      }
      
      private final native JsArrayString getUrls() /*-{
         return this.url ;
      }-*/;
      
      private final native JsArrayString getTitles() /*-{
         return this.title ;
      }-*/;
   }

   void suggestTopics(String prefix,
                      ServerRequestCallback<JsArrayString> requestCallback);

   void getHelp(String topic, 
                String packageName,
                int options,
                ServerRequestCallback<HelpInfo> requestCallback);
   
   String getApplicationURL(String topicURI);

   void showHelpTopic(String topic, String pkgName) ;

   void search(String query, 
               ServerRequestCallback<JsArrayString> requestCallback) ;
   
   void getHelpLinks(String setName, ServerRequestCallback<LinksList> requestCallback) ;

   void setHelpLinks(String setName, ArrayList<Link> links) ;
}
