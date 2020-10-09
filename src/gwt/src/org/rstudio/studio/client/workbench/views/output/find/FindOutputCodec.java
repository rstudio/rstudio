/*
 * FindOutputCodec.java
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
package org.rstudio.studio.client.workbench.views.output.find;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableRowElement;
import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.FilePosition;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.HeaderBreaksItemCodec;
import org.rstudio.studio.client.workbench.views.output.find.FindOutputResources.Styles;
import org.rstudio.studio.client.workbench.views.output.find.model.FindResult;

public class FindOutputCodec
      extends HeaderBreaksItemCodec<FindResult, CodeNavigationTarget, Object>
{
   public FindOutputCodec(FindOutputResources resources)
   {
      styles_ = resources.styles();
   }

   @Override
   public TableRowElement getRowForItem(FindResult entry)
   {
      if (entry == null)
      {
         // Overflow message
         TableRowElement tr = Document.get().createTRElement();
         TableCellElement td = Document.get().createTDElement();
         td.setClassName(styles_.overflowWarning());
         td.setColSpan(2);
         td.setInnerText("More than 1000 matching lines were found. " +
                         "Only the first 1000 lines are shown.");
         tr.appendChild(td);
         return tr;
      }

      TableRowElement tr = Document.get().createTRElement();
      tr.setAttribute(DATA_FILE, entry.getFile());
      tr.setAttribute(DATA_LINE, entry.getLine() + "");

      TableCellElement td1 = Document.get().createTDElement();
      td1.setClassName(styles_.line());
      td1.setInnerText(entry.getLine() + ":\u00A0");
      tr.appendChild(td1);

      TableCellElement td2 = Document.get().createTDElement();
      td2.setClassName(styles_.lineValue());
      if (!entry.getReplaceIndicator())
         td2.setInnerHTML(entry.getLineHTML().asString());
      else
         td2.setInnerHTML(entry.getLineReplaceHTML().asString());
      tr.appendChild(td2);

      return tr;
   }

   @Override
   protected boolean needsBreak(TableRowElement prevRow, TableRowElement row)
   {
      if (!row.hasAttribute(DATA_FILE))
         return false;

      return prevRow == null ||
             !StringUtil.equals(prevRow.getAttribute(DATA_FILE), row.getAttribute(DATA_FILE));
   }

   @Override
   protected int addBreak(TableRowElement row)
   {
      TableRowElement tr = Document.get().createTRElement();
      tr.setClassName(styles_.headerRow());

      TableCellElement td = Document.get().createTDElement();
      td.setColSpan(2);
      td.setInnerText(row.getAttribute(DATA_FILE));
      tr.appendChild(td);

      row.getParentElement().insertBefore(tr, row);
      return 1;
   }

   @Override
   public CodeNavigationTarget getOutputForRow(TableRowElement row)
   {
      String file = row.getAttribute(DATA_FILE);
      int line = Integer.parseInt(row.getAttribute(DATA_LINE));

      return new CodeNavigationTarget(file, FilePosition.create(line, 1));
   }

   @Override
   public Object getOutputForRow2(TableRowElement row)
   {
      return null;
   }

   @Override
   public boolean isValueRow(TableRowElement row)
   {
      return row.hasAttribute(DATA_FILE);
   }

   @Override
   public boolean hasNonValueRows()
   {
      return true;
   }

   private final Styles styles_;

   private static final String DATA_FILE = "data-file";
   private static final String DATA_LINE = "data-line";
}
