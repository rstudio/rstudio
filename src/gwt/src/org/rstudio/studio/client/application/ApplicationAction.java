/*
 * ApplicationAction.java
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

package org.rstudio.studio.client.application;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.URIUtils;

import com.google.gwt.user.client.Window;

public class ApplicationAction
{
   public static final String QUIT = "quit";
   public static final String QUIT_TO_HOME = "quit_to_home";
   public static final String NEW_PROJECT = "new_project";
   public static final String OPEN_PROJECT = "open_project";
   public static final String SWITCH_PROJECT = "switch_project";
   
   
   public static String addAction(String url, String action)
   {
      return URIUtils.addQueryParam(url, ACTION_PARAMETER, action);  
   }
   
   public static String addLauncherFlag(String url)
   {
      return URIUtils.addQueryParam(url, LAUNCHER_PARAMETER, "1");
   }
   
   public static boolean hasAction()
   {
      return getAction().length() > 0;
   }
   
   public static boolean isQuit()
   {
      return isAction(QUIT) || isQuitToHome();
   }
   
   public static boolean isQuitToHome()
   {
      return isAction(QUIT_TO_HOME);
   }
   
   public static boolean isNewProject()
   {
      return isAction(NEW_PROJECT);
   }
   
   public static boolean isOpenProject()
   {
      return isAction(OPEN_PROJECT);
   }
   
   public static boolean isSwitchProject()
   {
      return isAction(SWITCH_PROJECT);
   }
   
   public static String getId()
   {
      return StringUtil.notNull(
          Window.Location.getParameter(ID_PARAMETER));
   }

   public static boolean isLauncherSession()
   {
      String flag = StringUtil.notNull(
            Window.Location.getParameter(LAUNCHER_PARAMETER));
      return flag.equals("1");
   }
  
   public static String getQueryStringWithoutAction()
   {
      return ApplicationUtils.getRemainingQueryString(getActionParameters());
   }
   
   public static void removeActionFromUrl()
   {
      ApplicationUtils.removeQueryParams(getActionParameters());
   }
    
   private static boolean isAction(String action)
   {
      return action == getAction();
   }
   
   private static String getAction()
   {
      return StringUtil.notNull(
          Window.Location.getParameter(ACTION_PARAMETER));
   }
   
   private static List<String> getActionParameters()
   {
      ArrayList<String> params = new ArrayList<>();
      params.add(ACTION_PARAMETER);
      params.add(ID_PARAMETER);
      params.add(LAUNCHER_PARAMETER);
      return params;
   }
   
   private static final String ACTION_PARAMETER = "action";
   private static final String ID_PARAMETER = "id";
   private static final String LAUNCHER_PARAMETER = "launcher";
}
