/*
 * RestartRequired.java
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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
package org.rstudio.core.client.prefs;

public class RestartRequirement
{
   public RestartRequirement()
   {
      this(false, false);
   }

   public RestartRequirement(boolean uiReloadRequired, boolean desktopRestartRequired)
   {
      uiReloadRequired_ = uiReloadRequired;
      desktopRestartRequired_ = desktopRestartRequired;
   }

   /**
    * @param required true if a change requires reloading the web app
    */
   public void setUiReloadRequired(boolean required)
   {
      uiReloadRequired_ = required;
   }

   /**
    * @return true if a change requires reloading the web app
    */
   public boolean getUiReloadRequired()
   {
      return uiReloadRequired_;
   }

   /**
    * @param required true if a change requires restarting desktop app
    */
   public void setDesktopRestartRequired(boolean required)
   {
      desktopRestartRequired_ = required;
   }

   /**
    * @return true if a change requires restarting the desktop app
    */
   public boolean getDesktopRestartRequired()
   {
      return desktopRestartRequired_;
   }

   public void mergeRequirements(RestartRequirement requirement)
   {
      if (requirement.getDesktopRestartRequired())
         setDesktopRestartRequired(true);
      if (requirement.getUiReloadRequired())
         setUiReloadRequired(true);
   }

   private boolean uiReloadRequired_;
   private boolean desktopRestartRequired_;
}
