/*
 * DocShadowPropMenuItem.java
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
package org.rstudio.core.client.widget;

import org.rstudio.studio.client.workbench.prefs.model.Prefs.PrefValue;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;

/**
 * DocShadowPropMenuItem is a menu item representing a local document property
 * that shadows (overrides) a global property. 
 */
public class DocShadowPropMenuItem extends DocPropMenuItem
{
   /**
    * Creates a new shadowed document property menu item.
    * 
    * @param label The label for the menu item
    * @param docUpdate The update sentinel 
    * @param pref The global preference to track
    * @param propName The name of the document property to track
    * @param targetValue The value of the document property when the menu item
    *   is checked.
    */
   public DocShadowPropMenuItem(String label, DocUpdateSentinel docUpdate,
         PrefValue<String> pref, String propName, String targetValue)
   {
      super(label, 
            docUpdate, 
            docUpdate.getProperty(propName, pref.getValue()) == targetValue,
            propName,
            targetValue);
      
      propName_ = propName;
      sentinel_ = docUpdate;
      pref_ = pref;
      target_ = targetValue;
      
      pref_.addValueChangeHandler(new ValueChangeHandler<String>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<String> event)
         {
            onStateChanged();
         }
      });
      onStateChanged();
   }
   
   @Override
   public boolean isChecked()
   {
      if (sentinel_ == null)
         return false;
      else if (sentinel_.hasProperty(propName_))
         return super.isChecked();
      else
         return pref_.getValue() == target_;
   }
   
   private final String propName_;
   private final String target_;
   private final DocUpdateSentinel sentinel_;
   private final PrefValue<String> pref_;
}
