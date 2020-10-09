/*
 * DocPropMenuItem.java
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

import java.util.HashMap;

import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;

public class DocPropMenuItem extends CheckableMenuItem
{ 
   public DocPropMenuItem(String label, DocUpdateSentinel docUpdate, 
                          boolean defaultChecked, String propName, String targetValue)
   {
      this(label, false, docUpdate, defaultChecked, propName, targetValue);
   }
   
   public DocPropMenuItem(String label, boolean html, DocUpdateSentinel docUpdate, 
         boolean defaultChecked, String propName, String targetValue)
   {
      super(label, html);
      docUpdate_ = docUpdate;
      default_ = defaultChecked;
      propName_ = propName;
      targetValue_ = targetValue;
      docUpdate_.addPropertyValueChangeHandler(propName, 
            new ValueChangeHandler<String>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<String> arg0)
         {
            onStateChanged();
         }
      });
      onStateChanged();
   }

   @Override
   public String getLabel()
   {
      return getText();
   }

   @Override
   public boolean isChecked()
   {
      if (docUpdate_ == null)
         return default_;
      String val = docUpdate_.getProperty(propName_);
      if (StringUtil.isNullOrEmpty(val))
         return default_;
      else
         return val == targetValue_;
   }
   
   @Override
   public void onInvoked()
   {
      HashMap<String, String> props = new HashMap<String, String>();
      String target = targetValue_;
      
      // toggle behavior for boolean values: if our target was true but the
      // prop is already set to true, set it to false
      if (target == DocUpdateSentinel.PROPERTY_TRUE &&
          docUpdate_.getBoolProperty(propName_, default_))
         target = DocUpdateSentinel.PROPERTY_FALSE;

      props.put(propName_, target);
      docUpdate_.modifyProperties(props, new ProgressIndicator()
      {
         @Override
         public void onError(String message)
         {
            RStudioGinjector.INSTANCE.getGlobalDisplay().showErrorMessage(
                  "Could Not Change Setting", message);
         }
         
         @Override
         public void onCompleted()
         {
            onStateChanged();
            onUpdateComplete();
         }
         
         @Override
         public void clearProgress()
         {
         }

         @Override
         public void onProgress(String message, Operation onCancel)
         {
         }

         @Override
         public void onProgress(String message)
         {
         }
      });
   }
   
   protected void onUpdateComplete()
   {
   }
   
   private DocUpdateSentinel docUpdate_;
   private String propName_;
   private String targetValue_;
   private boolean default_;
}
