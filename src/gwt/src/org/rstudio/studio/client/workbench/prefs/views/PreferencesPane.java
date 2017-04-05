/*
 * PreferencesPane.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.prefs.views;


import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Label;

import org.rstudio.core.client.prefs.PreferencesDialogPaneBase;
import org.rstudio.core.client.widget.NumericValueWidget;
import org.rstudio.studio.client.workbench.prefs.model.Prefs.PrefValue;
import org.rstudio.studio.client.workbench.prefs.model.RPrefs;

import java.util.ArrayList;

public abstract class PreferencesPane extends PreferencesDialogPaneBase<RPrefs>
{ 
   @Override
   public boolean onApply(RPrefs rPrefs)
   {
      for (Command cmd : onApplyCommands_)
         cmd.execute();
      return false;
   }
   
   /**
    * Create a checkbox bound to a user preference with `lessSpaced` margin style.
    * @param label checkbox label
    * @param prefValue boolean preference to bind to
    * @return CheckBox
    */
   protected CheckBox checkboxPref(String label,
         final PrefValue<Boolean> prefValue)
   {
      return checkboxPref(label, prefValue, null /*title*/, true /*defaultSpaced*/);
   }

   /**
    * Create a checkbox bound to a user preference.
    * @param label checkbox label
    * @param prefValue bound boolean preference
    * @param defaultSpaced if true apply `lessSpaced style`, otherwise no spacing
    * style will be applied
    * @return CheckBox
    */
   protected CheckBox checkboxPref(String label,
                                   final PrefValue<Boolean> prefValue,
                                   boolean defaultSpaced)
   {
      return checkboxPref(label, prefValue, null /*title*/, defaultSpaced);
   }
    
   /**
    * Create a checkbox bound to a user preference with `lessSpaced` margin style.
    * @param label checkbox label
    * @param prefValue bound boolean preference
    * @param title checkbox title; typically shown via tooltip
    * @return CheckBox
    */
   protected CheckBox checkboxPref(String label,
                                   final PrefValue<Boolean> prefValue,
                                   String title)
   {
      return checkboxPref(label, prefValue, title, true /*defaultSpaced*/);
   }
  
   /**
    * Create a checkbox bound to a user preference.
    * @param label checkbox label
    * @param prefValue bound boolean preference
    * @param title checkbox title; typically shown via tooltip
    * @param defaultSpaced if true apply `lessSpaced` style, otherwise no spacing
    * style will be applied
    * @return CheckBox
    */
   protected CheckBox checkboxPref(String label,
                                   final PrefValue<Boolean> prefValue,
                                   String title,
                                   boolean defaultSpaced)
   {
      final CheckBox checkBox = new CheckBox(label, false);
      if (defaultSpaced)
         lessSpaced(checkBox);
      checkBox.setValue(prefValue.getGlobalValue());
      if (title != null)
         checkBox.setTitle(title);
      onApplyCommands_.add(new Command()
      {
         public void execute()
         {
            prefValue.setGlobalValue(checkBox.getValue());
         }
      });
      return checkBox;
   }

   protected NumericValueWidget numericPref(String label,
                                            final PrefValue<Integer> prefValue)
   {
      final NumericValueWidget widget = new NumericValueWidget(label);
      lessSpaced(widget);
      registerEnsureVisibleHandler(widget);
      widget.setValue(prefValue.getGlobalValue() + "");
      onApplyCommands_.add(new Command()
      {
         public void execute()
         {
            try
            {
               prefValue.setGlobalValue(Integer.parseInt(widget.getValue()));
            }
            catch (Exception e)
            {
               // It's OK for this to be invalid if we got past validation--
               // that means the associated checkbox wasn't checked
            }
         }
      });
      return widget;
   }
   
   protected Label headerLabel(String caption) 
   {
      Label headerLabel = new Label(caption);
      headerLabel.addStyleName(res().styles().headerLabel());
      nudgeRight(headerLabel);
      return headerLabel;
   }

   protected final ArrayList<Command> onApplyCommands_ = new ArrayList<Command>();
}
