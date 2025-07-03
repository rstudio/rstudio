/*
 * PackageManagerSelectRepositoryModalDialog.java
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
package org.rstudio.studio.client.workbench.views.packages.ui;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.RowTable;
import org.rstudio.studio.client.workbench.views.packages.model.PackageManagerRepository;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class PackageManagerSelectRepositoryModalDialog extends ModalDialog<PackageManagerRepository>
{
   public PackageManagerSelectRepositoryModalDialog(
      JsArray<PackageManagerRepository> ppmRepos,
      OperationWithInput<PackageManagerRepository> onSelected)
   {
      super("Select Repository", Roles.getDialogRole(), onSelected);

      ppmRepos_ = ppmRepos;
      setSize("500px", "400px");

      panel_ = new VerticalPanel();

      table_ = new RowTable<PackageManagerRepository>("Goober")
      {
         @Override
         public void drawRowImpl(PackageManagerRepository object, TableRowElement rowEl)
         {
            TableCellElement cellEl;

            cellEl = Document.get().createTDElement();
            cellEl.setInnerText(Integer.toString(object.getId()));
            rowEl.appendChild(cellEl);

            cellEl = Document.get().createTDElement();
            cellEl.setInnerText(object.getName());
            rowEl.appendChild(cellEl);

            cellEl = Document.get().createTDElement();
            cellEl.setInnerText(object.getDescription());
            rowEl.appendChild(cellEl);

            cellEl = Document.get().createTDElement();
            cellEl.setInnerText(object.getCreated());
            rowEl.appendChild(cellEl);
         }

         @Override
         public double getRowHeight()
         {
            return 24;
         }

         @Override
         public int[] getColumnWidths()
         {
            return new int[] { 30, 70, 300, 80 };
         }

         @Override
         public String getKey(PackageManagerRepository object)
         {
            return object.getName();
         }
      };

      List<PackageManagerRepository> tableData = new ArrayList<>();
      for (int i = 0, n = ppmRepos.length(); i < n; i++)
      {
         PackageManagerRepository ppmRepo = ppmRepos.get(i);
         if (ppmRepo.isHidden())
            continue;
         
         String type = ppmRepo.getType();
         if (!type.equals(PackageManagerRepository.TYPE_R))
            continue;

         tableData.add(ppmRepo);
      }
      table_.draw(tableData);

      dateInputPanel_ = new FlowPanel("input");
      dateInputPanel_.getElement().setAttribute("type", "date");

      panel_.add(table_);
      panel_.add(dateInputPanel_);
   }

   @Override
   protected PackageManagerRepository collectInput()
   {
      throw new UnsupportedOperationException("Unimplemented method 'collectInput'");
   }

   @Override
   protected Widget createMainWidget()
   {
      return panel_;
   }

   private final VerticalPanel panel_;
   private final RowTable<PackageManagerRepository> table_;
   private final FlowPanel dateInputPanel_;
   
   private final JsArray<PackageManagerRepository> ppmRepos_;
}
