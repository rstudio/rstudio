/*
 * CRANMirror.java
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
package org.rstudio.studio.client.common.mirrors.model;

import java.util.ArrayList;

import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

public class CRANMirror extends UserPrefs.CranMirror
{
   protected CRANMirror()
   {
   }
   
   public final static native CRANMirror empty() /*-{
      var cranMirror = new Object();
      cranMirror.name = "";
      cranMirror.host = "";
      cranMirror.url = "";
      cranMirror.secondary = "";
      cranMirror.country = "";
      cranMirror.changed = false;

      return cranMirror;
   }-*/;
   
   public final boolean isEmpty()
   {
      return getName() == null || getName().length() == 0;
   }
   
   public final native void setName(String name) /*-{
      this.name = name;
   }-*/;

   public final native void setHost(String host) /*-{
      this.host = host;
   }-*/;

   public final native String getURL() /*-{
      return this.url;
   }-*/;

   public final native void setURL(String url) /*-{
      this.url = url;
   }-*/;

   private final native void setSecondary(String secondary) /*-{
      this.secondary = secondary;
   }-*/;

   private final native String getError() /*-{
      return this.error;
   }-*/;

   private final void setSecondaryRepos(String cran, ArrayList<CRANMirror> repos)
   {
      setURL(cran);

      ArrayList<String> entries = new ArrayList<String>();
      for (CRANMirror repo : repos)
      {
         if (!repo.getName().toLowerCase().equals("cran"))
         {
            entries.add(repo.getName() + "|" + repo.getURL());
         }
      }
      
      setSecondary(StringUtil.join(entries, "|"));
   }

   public final void setSecondaryRepos(ArrayList<CRANMirror> repos)
   {
      setSecondaryRepos(getURL(), repos);
   }

   public final ArrayList<CRANMirror> getSecondaryRepos()
   {
      ArrayList<CRANMirror> repos = new ArrayList<CRANMirror>();

      String secondary = getSecondary();
      if (StringUtil.isNullOrEmpty(secondary))
      {
         // Return empty list of secondary repos if none were defined.
         return repos;
      }
      
      String[] entries = secondary.split("\\|");
      for (int i = 0; i < entries.length / 2; i++)
      {
         CRANMirror repo = CRANMirror.empty();
         repo.setName(entries[2 * i]);
         repo.setURL(entries[2 * i + 1]);

         repos.add(repo);
      }
      
      return repos;
   }

   public final String getDisplay()
   {
      return getName() + " - " + getHost();
   }
}
