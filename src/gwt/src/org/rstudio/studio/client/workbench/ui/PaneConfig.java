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
import org.rstudio.core.client.js.JsUtil;

import java.util.*;

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
      tabSet1.push("Environment");
      tabSet1.push("History");
      tabSet1.push("Build");
      tabSet1.push("VCS");
      tabSet1.push("Presentation");

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
      return new String[] {"Environment", "History", "Files", "Plots",
                           "Packages", "Help", "Build", "VCS", "Presentation"};
   }

   public static String[] getAlwaysVisibleTabs()
   {
      return new String[] {"Environment", "History", "Files", "Plots",
                           "Packages", "Help"};
   }

   public static String[] getHideableTabs()
   {
      return new String[] {"Build", "VCS", "Presentation" };
   }

   // Any tabs that were added after our first public release.
   public static String[] getAddableTabs()
   {
      return new String[] {"Build", "VCS", "Presentation" };
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

      // Replace any obsoleted tabs in the config
      replaceObsoleteTabs(ts1);
      replaceObsoleteTabs(ts2);

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

      // Were any addable tabs missing? Add them to ts1.
      // (Iterate over original array instead of addableTabs set so that order
      // is well-defined)
      for (String tab : getAddableTabs())
         if (addableTabs.contains(tab))
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
