/*
 * CRANMirror.java
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
package org.rstudio.studio.client.common.mirrors.model;

import java.util.ArrayList;

import org.rstudio.core.client.StringUtil;

import com.google.gwt.core.client.JavaScriptObject;


public class CRANMirror extends JavaScriptObject
{
   protected CRANMirror()
   {
   }
   
   public final static native CRANMirror empty() /*-{
      var cranMirror = new Object();
      cranMirror.name = "";
      cranMirror.host = "";
      cranMirror.url = "";
      cranMirror.country = "";
      cranMirror.changed = false;

      return cranMirror;
   }-*/;
   
   public final boolean isEmpty()
   {
      return getName() == null || getName().length() == 0;
   }
   
   public final native String getName() /*-{
      return this.name;
   }-*/;

   public final native void setName(String name) /*-{
      this.name = name;
   }-*/;

   public final native String getHost() /*-{
      return this.host;
   }-*/;

   public final native void setHost(String host) /*-{
      this.host = host;
   }-*/;

   private final native String getRawURL() /*-{
      return this.url;
   }-*/;

   private final native void setRawURL(String url) /*-{
      this.url = url;
   }-*/;

   private final native String getError() /*-{
      return this.error;
   }-*/;

   public final native boolean getChanged() /*-{
      return this.changed;
   }-*/;

   public final native void setChanged(boolean changed) /*-{
      this.changed = changed;
   }-*/;

   public final String getURL()
   {
      String rawUrl = getRawURL();

      if (rawUrl.startsWith("CRAN|"))
         return rawUrl.split("\\|")[1];
      else
         return rawUrl;
   }

   public final void setURL(String url)
   {
      if (getRawURL().startsWith("CRAN|"))
         setSecondaryRepos(url, getSecondaryRepos());
      else
         setRawURL(url);
   }

   private final void setSecondaryRepos(String cran, ArrayList<CRANMirror> repos)
   {
      ArrayList<String> entries = new ArrayList<String>();
      entries.add("CRAN|" + cran);

      for (CRANMirror repo : repos)
      {
         if (!repo.getName().toLowerCase().equals("cran"))
         {
            entries.add(repo.getName() + "|" + repo.getURL());
         }
      }
      
      setRawURL(StringUtil.join(entries, "|"));
   }

   public final void setSecondaryRepos(ArrayList<CRANMirror> repos)
   {
      setSecondaryRepos(getURL(), repos);
   }

   public final ArrayList<CRANMirror> getSecondaryRepos()
   {
      ArrayList<CRANMirror> repos = new ArrayList<CRANMirror>();

      if (getRawURL().startsWith("CRAN|"))
      {
         String[] entries = getRawURL().split("\\|");
         if (entries.length > 0)
         {
            for (int i = 1; i < entries.length / 2; i++)
            {
               CRANMirror repo = CRANMirror.empty();
               repo.setName(entries[2 * i]);
               repo.setURL(entries[2 * i + 1]);

               if (!repo.getName().toLowerCase().equals("cran"))
               {
                  repos.add(repo);
               }
            }
         }
      }
      
      return repos;
   }

   public final native String getCountry() /*-{
      return this.country;
   }-*/;

   public final String getDisplay()
   {
      return getName() + " - " + getHost();
   }
}
