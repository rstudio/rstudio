/*
 * PaneConfig.java
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
package org.rstudio.studio.client.workbench.ui;

import com.google.gwt.core.client.JsArrayString;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.JsArrayUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefsAccessor;

import java.util.*;

public class PaneConfig extends UserPrefsAccessor.Panes
{
   public native static PaneConfig create(JsArrayString panes,
                                          JsArrayString tabSet1,
                                          JsArrayString tabSet2,
                                          JsArrayString hiddenTabSet,
                                          boolean consoleLeftOnTop,
                                          boolean consoleRightOnTop,
                                          int additionalSources) /*-{
      return {
         quadrants: panes,
         tabSet1: tabSet1,
         tabSet2: tabSet2,
         hiddenTabSet: hiddenTabSet,
         console_left_on_top: consoleLeftOnTop,
         console_right_on_top: consoleRightOnTop,
         additional_source_columns: additionalSources
      };
   }-*/;

   public native static void addSourcePane() /*-{
      if (this.additional_source_columns == null)
         this.additional_source_columns = 0;
      this.additional_source_columns++;
   }-*/;

   public static PaneConfig createDefault()
   {
      JsArrayString panes = createArray().cast();
      panes.push(UserPrefsAccessor.Panes.QUADRANTS_SOURCE);
      panes.push(UserPrefsAccessor.Panes.QUADRANTS_CONSOLE);
      panes.push(UserPrefsAccessor.Panes.QUADRANTS_TABSET1);
      panes.push(UserPrefsAccessor.Panes.QUADRANTS_TABSET2);
      panes.push(UserPrefsAccessor.Panes.QUADRANTS_HIDDENTABSET);

      JsArrayString tabSet1 = createArray().cast();
      tabSet1.push("Environment");
      tabSet1.push("History");
      tabSet1.push("Connections");
      tabSet1.push("Build");
      tabSet1.push("VCS");
      tabSet1.push("Tutorial");
      tabSet1.push("Presentation");

      JsArrayString tabSet2 = createArray().cast();
      tabSet2.push("Files");
      tabSet2.push("Plots");
      tabSet2.push("Packages");
      tabSet2.push("Help");
      tabSet2.push("Viewer");

      JsArrayString hiddenTabSet = createArray().cast();
      return create(panes, tabSet1, tabSet2, hiddenTabSet, false, true, 0);
   }

   public static String[] getAllPanes()
   {
      return new String[] {
         UserPrefsAccessor.Panes.QUADRANTS_SOURCE,
         UserPrefsAccessor.Panes.QUADRANTS_CONSOLE,
         UserPrefsAccessor.Panes.QUADRANTS_TABSET1,
         UserPrefsAccessor.Panes.QUADRANTS_TABSET2,
         UserPrefsAccessor.Panes.QUADRANTS_HIDDENTABSET
      };
   }

   public static String[] getVisiblePanes()
   {
      return new String[] {
         UserPrefsAccessor.Panes.QUADRANTS_SOURCE,
         UserPrefsAccessor.Panes.QUADRANTS_CONSOLE,
         UserPrefsAccessor.Panes.QUADRANTS_TABSET1,
         UserPrefsAccessor.Panes.QUADRANTS_TABSET2
      };
   }

   public static String[] getAllTabs()
   {
      // A list of all the tabs. Order matters; the Presentation tab must be the
      // last element in this array that's part of the first tabset (ts1)
      return new String[] {"Environment", "History", "Files", "Plots", "Connections",
                           "Packages", "Help", "Build", "VCS", "Tutorial", "Viewer", "Presentation"};
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
         if (StringUtil.equals(tab, replacedTabs[idx]))
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

   public final int getConsoleIndex()
   {
      JsArrayString panes = getQuadrants();
      for (int i = 0; i<panes.length(); i++)
         if (StringUtil.equals(panes.get(i), "Console"))
            return i;

      throw new IllegalStateException();
   }

   public final boolean getConsoleLeft()
   {
      JsArrayString panes = getQuadrants();
      return StringUtil.equals(panes.get(0), "Console") ||
         StringUtil.equals(panes.get(1), "Console");
   }

   public final boolean getConsoleRight()
   {
      return !getConsoleLeft();
   }

   public final boolean validateAndAutoCorrect()
   {
      JsArrayString panes = getQuadrants();
      if (panes == null)
         return false;

      JsArrayString ts1 = getTabSet1();
      JsArrayString ts2 = getTabSet2();

      // Replace any obsoleted tabs in the config
      replaceObsoleteTabs(ts1);
      replaceObsoleteTabs(ts2);

      // Presentation tab must always be at the end of the ts1 tabset (this
      // is so that activating it works even in the presence of optionally
      // visible tabs). This is normally an invariant but for a time during
      // the v0.99-1000 preview we allowed the Connections tab to be the
      // last one in the tabset.
      if (!StringUtil.equals(ts1.get(ts1.length() - 1), "Presentation"))
      {
         // 1.3 released with a bug where Tutorial would be added at the end; autocorrect
         // https://github.com/rstudio/rstudio/issues/7246
         if (StringUtil.equals(ts1.get(ts1.length() - 1), "Tutorial") &&
             StringUtil.equals(ts1.get(ts1.length() - 2), "Presentation"))
         {
            ts1.set(ts1.length() - 1, "Presentation");
            ts1.set(ts1.length() - 2, "Tutorial");
         }
         else
         {
            Debug.logToConsole("Invaliding tabset config (Presentation index)");
            return false;
         }
      }

      // Check for any unknown tabs
      Set<String> allTabs = makeSet(getAllTabs());
      if (!(isSubset(allTabs, JsUtil.asIterable(ts1)) &&
            isSubset(allTabs, JsUtil.asIterable(ts2))))
         return false;

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
      TreeSet<String> set = new TreeSet<>();
      for (String val : values)
         set.add(val);
      return set;
   }

   public final PaneConfig copy()
   {
      return create(copy(getQuadrants()),
                    copy(getTabSet1()),
                    copy(getTabSet2()),
                    copy(getHiddenTabSet()),
                    getConsoleLeftOnTop(),
                    getConsoleRightOnTop(),
                    getAdditionalSourceColumns());
   }

   public final native boolean isEqualTo(PaneConfig other)  /*-{
      return other != null &&
             this.panes.toString() == other.panes.toString() &&
             this.tabSet1.toString() == other.tabSet1.toString() &&
             this.tabSet2.toString() == other.tabSet2.toString() &&
             this.hiddenTabSet.toString() == other.hiddenTabSet.toString();
   }-*/;

   private boolean sameElements(JsArrayString a, String[] b)
   {
      if (a.length() != b.length)
         return false;

      ArrayList<String> a1 = new ArrayList<>();
      for (int i = 0; i < a.length(); i++)
         a1.add(a.get(i));
      Collections.sort(a1);

      Arrays.sort(b);

      for (int i = 0; i < b.length; i++)
         if (!StringUtil.equals(a1.get(i), b[i]))
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
      // This function was previously used to ensure tabsets didn't contain only "hideable" tabs or
      // no tabs at all. As of 1.4 any tabs can be hidden so these checks have been removed. The
      // function remains to maintain the structure if validation needs to be added in the future.
     return true;
   }
}
