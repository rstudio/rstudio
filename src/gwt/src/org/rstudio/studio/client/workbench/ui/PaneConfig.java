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

      JsArrayString tabSet2 = createArray().cast();
      tabSet2.push("Files");
      tabSet2.push("Plots");
      tabSet2.push("Packages");
      tabSet2.push("Help");

      return create(panes, tabSet1, tabSet2);
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

   public final boolean isValid()
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

      if (!sameElements(concat(ts1, ts2), new String[] {"Workspace", "History", "Files", "Plots", "Packages", "Help"}))
         return false;

      return true;
   }

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
}
