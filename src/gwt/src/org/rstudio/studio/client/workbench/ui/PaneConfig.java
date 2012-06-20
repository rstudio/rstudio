/*
 * PaneConfig.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.ui;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class PaneConfig extends JavaScriptObject
{
   public native static PaneConfig create(JsArrayString panes,
                                          JsArrayString tabSet1,
                                          JsArrayString tabSet2) /*-{
      return { panes: panes, tabSet1: tabSet1, tabSet2: tabSet2 };
   }-*/;


   public static PaneConfig createDefault()
   {
      JsArrayString panes = createArray().cast();
      panes.push("Source");
      panes.push("Console");
      panes.push("TabSet1");
      panes.push("TabSet2");

      JsArrayString tabSet1 = createArray().cast();
      tabSet1.push("Workspace");
      tabSet1.push("History");
      tabSet1.push("Build");
      tabSet1.push("VCS");

      JsArrayString tabSet2 = createArray().cast();
      tabSet2.push("Files");
      tabSet2.push("Plots");
      tabSet2.push("Packages");
      tabSet2.push("Help");

      return create(panes, tabSet1, tabSet2);
   }

   public static String[] getAllPanes()
   {
      return new String[] {"Source", "Console", "TabSet1", "TabSet2"};
   }

   public static String[] getAllTabs()
   {
      return new String[] {"Workspace", "History", "Files", "Plots",
                           "Packages", "Help", "VCS", "Build"};
   }

   protected PaneConfig()
   {
   }

   public native final JsArrayString getPanes() /*-{
      return this.panes;
   }-*/;

   public native final void setPanes(JsArrayString panes) /*-{
      this.panes = panes;
   }-*/;

   public native final JsArrayString getTabSet1() /*-{
      return this.tabSet1;
   }-*/;

   public native final void setTabSet1(JsArrayString tabSet) /*-{
      this.tabSet1 = tabSet;
   }-*/;

   public native final JsArrayString getTabSet2() /*-{
      return this.tabSet2;
   }-*/;

   public native final void setTabSet2(JsArrayString tabSet) /*-{
      this.tabSet2 = tabSet;
   }-*/;

   public final boolean validateAndAutoCorrect()
   {
      JsArrayString panes = getPanes();
      if (panes == null)
         return false;
      if (!sameElements(panes, new String[] {"Source", "Console", "TabSet1", "TabSet2"}))
         return false;

      JsArrayString ts1 = getTabSet1();
      JsArrayString ts2 = getTabSet2();
      if (ts1.length() == 0 || ts2.length() == 0)
         return false;

      if (!sameElements(concat(ts1, ts2), new String[] {"Workspace", "History", "Files", "Plots", "Packages", "Help", "VCS", "Build"}))
      {
         if (!sameElements(concat(ts1, ts2), new String[] {"Workspace", "History", "Files", "Plots", "Packages", "Help", "VCS"}))
         {
            if (!sameElements(concat(ts1, ts2), new String[] {"Workspace", "History", "Files", "Plots", "Packages", "Help"}))
               return false;

            // The VCS tab is missing. Add it to tabset 1.
            ts1.push("VCS");
         }
         
         // The Build tab is missing. Add it to tabset 1.
         ts1.push("Build");
      }

      // Can't have a tabset that only contains VCS and/or Build since one
      // or both can be hidden
      String[] justVCS = {"VCS"};
      if (sameElements(ts1, justVCS) || sameElements(ts2, justVCS))
         return false;
      String[] justBuild = {"Build"};
      if (sameElements(ts1, justBuild) || sameElements(ts2, justBuild))
         return false;
      String[] justVcsAndBuild = {"VCS", "Build"};
      if (sameElements(ts1, justVcsAndBuild) || 
                       sameElements(ts2, justVcsAndBuild))
         return false;

      return true;
   }

   public final PaneConfig copy()
   {
      return create(copy(getPanes()),
                    copy(getTabSet1()),
                    copy(getTabSet2()));
   }
   
   public final native boolean isEqualTo(PaneConfig other)  /*-{
      return other != null &&
             this.panes.toString() == other.panes.toString() &&
             this.tabSet1.toString() == other.tabSet1.toString() &&
             this.tabSet2.toString() == other.tabSet2.toString();
   }-*/;
  
   private boolean sameElements(JsArrayString a, String[] b)
   {
      if (a.length() != b.length)
         return false;

      ArrayList<String> a1 = new ArrayList<String>();
      for (int i = 0; i < a.length(); i++)
         a1.add(a.get(i));
      Collections.sort(a1);

      Arrays.sort(b);

      for (int i = 0; i < b.length; i++)
         if (!a1.get(i).equals(b[i]))
            return false;

      return true;
   }

   private JsArrayString concat(JsArrayString a, JsArrayString b)
   {
      JsArrayString ab = createArray().cast();
      for (int i = 0; i < a.length(); i++)
         ab.push(a.get(i));
      for (int i = 0; i < b.length(); i++)
         ab.push(b.get(i));
      return ab;
   }

   private static JsArrayString copy(JsArrayString array)
   {
      if (array == null)
         return null;

      JsArrayString copy = JsArrayString.createArray().cast();
      for (int i = 0; i < array.length(); i++)
         copy.push(array.get(i));
      return copy;
   }

   public static boolean isValidConfig(ArrayList<String> tabs)
   {
      if (tabs.size() == 0)
         return false;
      if (tabs.size() == 1 && "VCS".equals(tabs.get(0)))
         return false;
      if (tabs.size() == 1 && "Build".equals(tabs.get(0)))
         return false;
      if (isBuildAndVcs(tabs))
         return false;
      if (tabs.size() == getAllTabs().length)
         return false;
      if (tabs.size() == getAllTabs().length - 1 && !tabs.contains("VCS"))
         return false;
      if (tabs.size() == getAllTabs().length - 1 && !tabs.contains("Build"))
         return false;
      if (tabs.size() == getAllTabs().length - 2 && (!tabs.contains("Build") ||
                                                     !tabs.contains("VCS")))
         return false;
      return true;
   }
   
   private static boolean isBuildAndVcs(ArrayList<String> tabs)
   {
      return tabs.size() == 2 &&
             (tabs.get(0).equals("Build") && tabs.get(1).equals("VCS") ||
              tabs.get(0).equals("VCS") && tabs.get(1).equals("Build"));
   }
}
