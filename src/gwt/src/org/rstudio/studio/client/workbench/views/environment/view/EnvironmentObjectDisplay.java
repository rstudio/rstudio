/*
 * EnvironmentObjectDisplay.java
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

package org.rstudio.studio.client.workbench.views.environment.view;

import java.util.List;

import org.rstudio.core.client.cellview.ScrollingDataGrid;
import org.rstudio.studio.client.workbench.views.environment.EnvironmentPane;

import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.text.shared.AbstractSafeHtmlRenderer;
import com.google.gwt.user.cellview.client.Column;

public abstract class EnvironmentObjectDisplay 
                      extends ScrollingDataGrid<RObjectEntry>
{
   public interface Host
   {
      public boolean enableClickableObjects();
      public boolean useStatePersistence();
      public String getFilterText();
      public int getSortColumn();
      public void setSortColumn(int col);
      public void toggleAscendingSort();
      boolean getAscendingSort();
      boolean getShowInternalFunctions();
      void setShowInternalFunctions(boolean hide);
      public void fillEntryContents(RObjectEntry entry, int idx, 
                                    boolean drawProgress);
   }

   public EnvironmentObjectDisplay(Host host, 
                                   EnvironmentObjectsObserver observer,
                                   String environmentName)
   {
      super(EnvironmentObjects.MAX_ENVIRONMENT_OBJECTS, 
            RObjectEntry.KEY_PROVIDER);

      observer_ = observer;
      host_ = host;
      environmentStyle_ = EnvironmentResources.INSTANCE.environmentStyle();
      environmentStyle_.ensureInjected();
      environmentName_ = environmentName;
      filterRenderer_ = new AbstractSafeHtmlRenderer<String>()
      {
         @Override
         public SafeHtml render(String str)
         {
            SafeHtmlBuilder sb = new SafeHtmlBuilder();
            boolean hasMatch = false;
            String filterText = host_.getFilterText();
            if (filterText.length() > 0)
            {
               int idx = str.toLowerCase().indexOf(filterText);
               if (idx >= 0)
               {
                  hasMatch = true;
                  sb.appendEscaped(str.substring(0, idx));
                  sb.appendHtmlConstant(
                        "<span class=\"" + 
                        environmentStyle_.filterMatch() + 
                        "\">");
                  sb.appendEscaped(str.substring(idx, 
                        idx + filterText.length()));
                  sb.appendHtmlConstant("</span>");
                  sb.appendEscaped(str.substring(idx + filterText.length(), 
                        str.length()));
               }
            }
            if (!hasMatch)
               sb.appendEscaped(str);
            return sb.toSafeHtml();
         }
      };
   }
   
   public abstract List<String> getSelectedObjects();
   public abstract void clearSelection();

   public void setEnvironmentName(String environmentName)
   {
      environmentName_ = environmentName;
   }
   
   // attaches a handler to a column that invokes the associated object
   protected void attachClickToInvoke(Column<RObjectEntry, String> column)
   {
      column.setFieldUpdater(new FieldUpdater<RObjectEntry, String>()
      {
         @Override
         public void update(int index, RObjectEntry object, String value)
         {
            boolean isClickable =
                  host_.enableClickableObjects() &&
                  object.getCategory() != RObjectEntry.Categories.Value;
            
            if (isClickable)
               observer_.viewObject(object.rObject.getName());
         }
      });
   }
   
   protected boolean selectionEnabled()
   {
      return environmentName_.equals(EnvironmentPane.GLOBAL_ENVIRONMENT_NAME);
   }

   protected AbstractSafeHtmlRenderer<String> filterRenderer_;
   protected EnvironmentObjectsObserver observer_;
   protected Host host_;
   protected EnvironmentStyle environmentStyle_;
   protected String environmentName_ = "";
}
