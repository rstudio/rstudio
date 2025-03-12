/*
 * PreferencesPane.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.prefs.views;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rstudio.core.client.prefs.PreferencesDialogPaneBase;
import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.core.client.widget.NumericValueWidget;
import org.rstudio.studio.client.workbench.prefs.model.Prefs.PrefValue;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.CheckBox;

public abstract class PreferencesPane extends PreferencesDialogPaneBase<UserPrefs>
{
   @Override
   public RestartRequirement onApply(UserPrefs rPrefs)
   {
      for (Command cmd : onApplyCommands_)
         cmd.execute();
      return new RestartRequirement();
   }

   /**
    * Create a checkbox that infers label/title from the PrefValue and uses the `lessSpaced` margin style
    *
    * Provides a convenience to reduce boilerplate when parsing PrefValues
    *
    * @param prefValue boolean preference to bind to
    * @return CheckBox
    */
   protected CheckBox checkboxPref(final PrefValue<Boolean> prefValue)
   {
      return checkboxPref(prefValue, true /*defaultSpaced*/);
   }

   /**
    * Create a checkbox that infers label/title from the PrefValue
    *
    * Provides a convenience to reduce boilerplate when parsing PrefValues
    *
    * @param prefValue boolean preference to bind to
    * @return CheckBox
    */
   protected CheckBox checkboxPref(final PrefValue<Boolean> prefValue,
                                   boolean defaultSpaced)
   {
      // Note that properties map such that:
      //    prefValue.title --> checkboxPref.label
      //    prefValue.description --> checkboxPref.title
      return checkboxPref(prefValue.getTitle(), prefValue, prefValue.getDescription() /*title*/, defaultSpaced /*defaultSpaced*/, false /*asHtml*/);
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
      return checkboxPref(label, prefValue, null /*title*/, true /*defaultSpaced*/, false /*asHtml*/);
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
      return checkboxPref(label, prefValue, null /*title*/, defaultSpaced, false /*asHtml*/);
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
                                   boolean defaultSpaced,
                                   boolean asHtml)
   {
      return checkboxPref(label, prefValue, null /*title*/, defaultSpaced, asHtml);
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
      return checkboxPref(label, prefValue, title, true /*defaultSpaced*/, false /*asHtml*/);
   }

   /**
    * Create a checkbox bound to a user preference.
    * @param label checkbox label
    * @param prefValue bound boolean preference
    * @param title checkbox title; typically shown via tooltip
    * @param defaultSpaced if true apply `lessSpaced` style, otherwise no spacing
    * @param asHtml if true, interpret String label as HTML
    * style will be applied
    * @return CheckBox
    */
   protected CheckBox checkboxPref(String label,
                                   final PrefValue<Boolean> prefValue,
                                   String title,
                                   boolean defaultSpaced,
                                   boolean asHtml)
   {
      final CheckBox checkBox = new CheckBox(label, asHtml);
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
      
      // Keep a list of all checkboxes with the same label, so that
      // changing one checkbox can signal a change and synchronize
      // any other checkboxes associated with the same state.
      if (!cbMap_.containsKey(label))
      {
         cbMap_.put(label, new ArrayList<>());
      }
      
      List<CheckBox> cbList = cbMap_.get(label);
      cbList.add(checkBox);
      
      checkBox.addValueChangeHandler(new ValueChangeHandler<Boolean>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            for (CheckBox cb : cbMap_.get(label))
            {
               cb.setValue(checkBox.getValue(), false);
            }
         }
      });
      
      return checkBox;
   }

   /**
    * Prompt for integer preference value in range [0 - maxint] with label inferred from prefValue
    */
   protected NumericValueWidget numericPref(final PrefValue<Integer> prefValue)
   {
      return numericPref(NumericValueWidget.ZeroMinimum,
         NumericValueWidget.NoMaximum,
         prefValue);
   }

   /**
    * Prompt for integer preference value in range [0 - maxint]
    */
   protected NumericValueWidget numericPref(String label,
                                            final PrefValue<Integer> prefValue)
   {
      return numericPref(label, NumericValueWidget.ZeroMinimum,
            NumericValueWidget.NoMaximum,
            prefValue);
   }

   protected NumericValueWidget numericPref(Integer minValue,
                                            Integer maxValue,
                                            final PrefValue<Integer> prefValue)
   {
      return numericPref(minValue, maxValue, prefValue, true);
   }

   protected NumericValueWidget numericPref(Integer minValue,
                                            Integer maxValue,
                                            final PrefValue<Integer> prefValue,
                                            boolean defaultSpaced)
   {
      return numericPref(prefValue.getTitle(), prefValue.getDescription(), minValue, maxValue, prefValue, defaultSpaced);
   }

   protected NumericValueWidget numericPref(String label,
                                            Integer minValue,
                                            Integer maxValue,
                                            final PrefValue<Integer> prefValue)
   {
      return numericPref(label, "", minValue, maxValue, prefValue, true);
   }

   protected NumericValueWidget numericPref(String label,
                                            Integer minValue,
                                            Integer maxValue,
                                            final PrefValue<Integer> prefValue,
                                            boolean defaultSpaced)
   {
      return numericPref(label, "", minValue, maxValue, prefValue, defaultSpaced);
   }

   protected NumericValueWidget numericPref(String label,
                                            String tooltip,
                                            Integer minValue,
                                            Integer maxValue,
                                            final PrefValue<Integer> prefValue)
   {
      return numericPref(label, tooltip, minValue, maxValue, prefValue, true);
   }

   /**
    * Prompt for integer preference value in range [min, max]
    *
    * @param label
    * @param minValue minimum value or NumericValueWidget.ZeroMinimum
    * @param maxValue maximum value or NumericValueWidget.NoMaximum
    * @param prefValue
    * @param defaultSpaced
    * @return
    */
   protected NumericValueWidget numericPref(String label,
                                            String tooltip,
                                            Integer minValue,
                                            Integer maxValue,
                                            final PrefValue<Integer> prefValue,
                                            boolean defaultSpaced)
   {
      final NumericValueWidget widget = new NumericValueWidget(label, tooltip, minValue, maxValue);
      if (defaultSpaced)
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

   protected final ArrayList<Command> onApplyCommands_ = new ArrayList<>();
   protected final Map<String, List<CheckBox>> cbMap_ = new HashMap<>();
}
