/*
 * PaneConfig.java
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
package org.rstudio.studio.client.workbench.ui;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.JsArrayUtil;
import org.rstudio.core.client.js.JsUtil;

import java.util.*;

public class PaneConfig extends JavaScriptObject
{
   public final static String SOURCE = "Source";
   public final static String CONSOLE = "Console";
   
   
   public native static PaneConfig create(JsArrayString panes,
                                          JsArrayString tabSet1,
                                          JsArrayString tabSet2,
                                          boolean consoleLeftOnTop,
                                          boolean consoleRightOnTop) /*-{
      return { 
         panes: panes, 
         tabSet1: tabSet1, 
         tabSet2: tabSet2,
         consoleLeftOnTop: consoleLeftOnTop,
         consoleRightOnTop: consoleRightOnTop 
      };
   }-*/;

   public static PaneConfig createDefault()
   {
      JsArrayString panes = createArray().cast();
      panes.push(SOURCE);
      panes.push(CONSOLE);
      panes.push("TabSet1");
      panes.push("TabSet2");

      JsArrayString tabSet1 = createArray().cast();
      tabSet1.push("Environment");
      tabSet1.push("History");
      tabSet1.push("Connections");
      tabSet1.push("Build");
      tabSet1.push("VCS");
      tabSet1.push("Presentation");
    

      JsArrayString tabSet2 = createArray().cast();
      tabSet2.push("Files");
      tabSet2.push("Plots");
      tabSet2.push("Packages");
      tabSet2.push("Help");
      tabSet2.push("Viewer");

      return create(panes, tabSet1, tabSet2, false, true);
   }

   public static String[] getAllPanes()
   {
      return new String[] {SOURCE, CONSOLE, "TabSet1", "TabSet2"};
   }

   public static String[] getAllTabs()
   {
      return new String[] {"Environment", "History", "Files", "Plots", "Connections",
                           "Packages", "Help", "Build", "VCS", "Presentation",
                           "Viewer"};
   }

   public static String[] getAlwaysVisibleTabs()
   {
      return new String[] {"Environment", "History", "Files", "Plots",
                           "Help", "Viewer"};
   }

   public static String[] getHideableTabs()
   {
      return new String[] {"Build", "VCS", "Presentation", "Connections", "Packages" };
   }

   // Any tabs that were added after our first public release.
   public static String[] getAddableTabs()
   {
      return new String[] {"Build", "VCS", "Presentation", "Connections", "Viewer" };
   }

   // Tabs that have been replaced by newer versions/replaceable supersets
   public static String[] getReplacedTabs()
   {
      return new String[] {"Workspace"};
   }

   // The tabs that replace those in getReplacedTabs(), order-matched
   public static String[] getReplacementTabs()
   {
      return new String[] {"Environment"};
   }

   // Given the name of a tab, return the index of the tab that it should
   // replace it, or -1 if the tab doesn't have a replacement
   public static int indexOfReplacedTab(String tab)
   {
      String[] replacedTabs = getReplacedTabs();
      int idx;
      for (idx = 0; idx < replacedTabs.length; idx++)
      {
         if (tab.equals(replacedTabs[idx]))
         {
            return idx;
         }
      }
      return -1;
   }

   // Given an array of tabs, replace any obsolete entries with their
   // replacements
   public static void replaceObsoleteTabs(JsArrayString tabs)
   {
      for (int idx = 0; idx < tabs.length(); idx++)
      {
         if (indexOfReplacedTab(tabs.get(idx)) >= 0)
         {
            tabs.set(idx, getReplacementTabs()[idx]);
         }
      }
   }

   protected PaneConfig()
   {
   }

   public native final JsArrayString getPanes() /*-{
      return this.panes;
   }-*/;

   public native final JsArrayString getTabSet1() /*-{
      return this.tabSet1;
   }-*/;

   public native final JsArrayString getTabSet2() /*-{
      return this.tabSet2;
   }-*/;
   
   public final int getConsoleIndex()
   {
      JsArrayString panes = getPanes();
      for (int i = 0; i<panes.length(); i++)
         if (panes.get(i).equals("Console"))
            return i;
      
      throw new IllegalStateException();
   }
   
   public final boolean getConsoleLeft()
   {
      JsArrayString panes = getPanes();
      return panes.get(0).equals("Console") || panes.get(1).equals("Console");
   }
   
   public final boolean getConsoleRight()
   {
      return !getConsoleLeft();
   }
   
   public native final boolean getConsoleLeftOnTop() /*-{
      // return default if the existing object doesn't have this property
      if (this.hasOwnProperty("consoleLeftOnTop"))
         return this.consoleLeftOnTop;
      else
         return false;
   }-*/;
   
   public native final boolean getConsoleRightOnTop() /*-{
      // return default if the existing object doesn't have this property
      if (this.hasOwnProperty("consoleRightOnTop"))
         return this.consoleRightOnTop;
      else
         return true;
   }-*/;

   public final boolean validateAndAutoCorrect()
   {
      JsArrayString panes = getPanes();
      if (panes == null)
         return false;
      if (!sameElements(panes, new String[] {SOURCE, CONSOLE, "TabSet1", "TabSet2"}))
         return false;

      JsArrayString ts1 = getTabSet1();
      JsArrayString ts2 = getTabSet2();
      if (ts1.length() == 0 || ts2.length() == 0)
         return false;

      // Replace any obsoleted tabs in the config
      replaceObsoleteTabs(ts1);
      replaceObsoleteTabs(ts2);

      // Presentation tab must always be at the end of the ts1 tabset (this 
      // is so that activating it works even in the presense of optionally
      // visible tabs). This is normally an invariant but for a time during 
      // the v0.99-1000 preview we allowed the Connections tab to be the 
      // last one in the tabset.
      if (!ts1.get(ts1.length() - 1).equals("Presentation"))
      {
         Debug.logToConsole("Invaliding tabset config (Presentation index)");
         return false;
      }
      
      // If any of these tabs are missing, then they can be added
      Set<String> addableTabs = makeSet(getAddableTabs());

      // If any of these tabs are missing, then the whole config is invalid
      Set<String> baseTabs = makeSet(getAllTabs());
      baseTabs.removeAll(addableTabs);

      for (String tab : JsUtil.asIterable(concat(ts1, ts2)))
      {
         if (!baseTabs.remove(tab) && !addableTabs.remove(tab))
            return false; // unknown tab
      }

      // If any baseTabs are still present, they weren't part of the tabsets
      if (baseTabs.size() > 0)
         return false;

      // Were any addable tabs missing? Add them the appropriate tabset
      // (Iterate over original array instead of addableTabs set so that order
      // is well-defined)
      for (String tab : getAddableTabs())
         if (addableTabs.contains(tab))
            if (tab.equals("Viewer"))
               ts2.push(tab);
            else
               ts1.push(tab);

      // These tabs can be hidden sometimes; they can't stand alone in a tabset
      Set<String> hideableTabs = makeSet(getHideableTabs());
      if (isSubset(hideableTabs, JsUtil.asIterable(ts1))
          || isSubset(hideableTabs, JsUtil.asIterable(ts2)))
      {
         return false;
      }

      return true;
   }

   private static boolean isSubset(Set<String> set, Iterable<String> possibleSubset)
   {
      for (String el : possibleSubset)
         if (!set.contains(el))
            return false;
      return true;
   }

   private static Set<String> makeSet(String... values)
   {
      TreeSet<String> set = new TreeSet<String>();
      for (String val : values)
         set.add(val);
      return set;
   }

   public final PaneConfig copy()
   {
      return create(copy(getPanes()),
                    copy(getTabSet1()),
                    copy(getTabSet2()),
                    getConsoleLeftOnTop(),
                    getConsoleRightOnTop());
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
      return JsArrayUtil.concat(a, b);
   }

   private static JsArrayString copy(JsArrayString array)
   {
      return JsArrayUtil.copy(array);
   }

   public static boolean isValidConfig(ArrayList<String> tabs)
   {
      if (isSubset(makeSet(getHideableTabs()), tabs))
      {
         // The proposed tab config only contains hideable tabs (or possibly
         // no tabs at all). Reject.
         return false;
      }
      else if (isSubset(makeSet(tabs.toArray(new String[tabs.size()])),
                        makeSet(getAlwaysVisibleTabs())))
      {
         // The proposed tab config contains all the always-visible tabs,
         // which implies that the other tab config only contains hideable
         // tabs (or possibly no tabs at all). Reject.
         return false;
      }
      else
         return true;
   }
}
