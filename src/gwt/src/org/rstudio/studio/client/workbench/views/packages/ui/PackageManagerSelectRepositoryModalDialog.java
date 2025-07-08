/*
 * PackageManagerSelectRepositoryModalDialog.java
 *
 * Copyright (C) 2025 by Posit Software, PBC
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

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.RowTable;
import org.rstudio.studio.client.workbench.views.packages.model.PackageManagerRepository;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.FontStyle;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

import jsinterop.base.Js;

public class PackageManagerSelectRepositoryModalDialog extends ModalDialog<PackageManagerRepository>
{
   private static class Header extends Composite
   {
      public Header(String label)
      {
         FlowPanel panel = new FlowPanel("h2");
         panel.getElement().setInnerText(label);
         initWidget(panel);
      }
   }

   public PackageManagerSelectRepositoryModalDialog(
      JsArray<PackageManagerRepository> ppmRepos,
      OperationWithInput<PackageManagerRepository> onSelected)
   {
      super("Select Repository", Roles.getDialogRole(), onSelected);

      panel_ = new FlowPanel();
      panel_.setSize("500px", "400px");

      tableContainer_ = new ScrollPanel();
      tableContainer_.setHeight("200px");

      table_ = new RowTable<PackageManagerRepository>("Repositories")
      {
         @Override
         public void drawRowImpl(PackageManagerRepository object, TableRowElement rowEl)
         {
            TableCellElement cellEl;

            cellEl = Document.get().createTDElement();
            rowEl.appendChild(cellEl);

            cellEl = Document.get().createTDElement();
            cellEl.setInnerText("Repository: " + object.getName());
            rowEl.appendChild(cellEl);

            String description = object.getDescription();
            if (Js.isTruthy(description))
            {
               cellEl = Document.get().createTDElement();
               cellEl.setInnerText(description);
               rowEl.appendChild(cellEl);
            }
            else
            {
               Element cellContentsEl = Document.get().createElement("span");
               cellContentsEl.getStyle().setFontStyle(FontStyle.ITALIC);
               cellContentsEl.setInnerText("(No description available)");

               cellEl = Document.get().createTDElement();
               cellEl.appendChild(cellContentsEl);
               rowEl.appendChild(cellEl);
            }

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
            return new int[] { 10, 100, 220, 160 };
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

      table_.getElement().getStyle().setBorderWidth(1, Unit.PX);
      table_.getElement().getStyle().setBackgroundColor("white");
      tableContainer_.add(table_);

      useSnapshotCheckbox_ = new CheckBox("Use repository snapshot from specific date");

      dateInputPanel_ = new FlowPanel("input");
      dateInputPanel_.getElement().setAttribute("type", "date");
      dateInputPanel_.setVisible(false);

      useSnapshotCheckbox_.getElement().getStyle().setDisplay(Display.BLOCK);
      useSnapshotCheckbox_.addValueChangeHandler(new ValueChangeHandler<Boolean>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            dateInputPanel_.setVisible(event.getValue());
         }
      });

      panel_.add(new Header("Repository"));
      panel_.add(tableContainer_);

      panel_.add(new Header("Snapshot"));
      panel_.add(useSnapshotCheckbox_);
      panel_.add(dateInputPanel_);
   }

   @Override
   protected PackageManagerRepository collectInput()
   {
      PackageManagerRepository repository = table_.getSelectedItem();
      if (repository == null)
         return null;

      if (useSnapshotCheckbox_.getValue())
      {
         String date = (String) dateInputPanel_.getElement().getPropertyObject("value");
         if (!StringUtil.isNullOrEmpty(date))
            repository.setSnapshot(date);
      }
      else
      {
         repository.setSnapshot("latest");
      }

      return repository;
   }

   @Override
   protected Widget createMainWidget()
   {
      return panel_;
   }

   private final FlowPanel panel_;
   private final ScrollPanel tableContainer_;
   private final RowTable<PackageManagerRepository> table_;
   private final CheckBox useSnapshotCheckbox_;
   private final FlowPanel dateInputPanel_;
}
