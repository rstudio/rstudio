/*
 * CompilePdfErrorItemCodec.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.compilepdf;

import com.google.gwt.dom.client.*;
import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.FilePosition;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.HeaderBreaksItemCodec;
import org.rstudio.studio.client.common.compilepdf.model.CompilePdfError;

public class CompilePdfErrorItemCodec
      extends HeaderBreaksItemCodec<CompilePdfError, CodeNavigationTarget, CodeNavigationTarget>
{
   public CompilePdfErrorItemCodec(CompilePdfResources resources,
                                   boolean showFileHeaders)
   {
      resources_ = resources;
      showFileHeaders_ = showFileHeaders;
   }

   public boolean getShowFileHandlers()
   {
      return showFileHeaders_;
   }

   public void setShowFileHeaders(boolean show)
   {
      showFileHeaders_ = show;
   }

   @Override
   public TableRowElement getRowForItem(CompilePdfError entry)
   {
      TableRowElement tr = Document.get().createTRElement();
      tr.setAttribute(DATA_PATH,
                      entry.getPath());
      tr.setAttribute(DATA_LINE,
                      entry.getLine() + "");
      tr.setAttribute(LOG_PATH,
                      entry.getLogPath());
      tr.setAttribute(LOG_LINE,
                      entry.getLogLine() + "");

      TableCellElement tdIcon = Document.get().createTDElement();
      tdIcon.setClassName(resources_.styles().iconCell());
      DivElement iconDiv = Document.get().createDivElement();
      iconDiv.setClassName(
            entry.getType() == CompilePdfError.ERROR ? resources_.styles().errorIcon() :
            entry.getType() == CompilePdfError.WARNING ? resources_.styles().warningIcon() :
            resources_.styles().boxIcon());
      tdIcon.appendChild(iconDiv);
      tr.appendChild(tdIcon);

      TableCellElement tdLine = Document.get().createTDElement();
      tdLine.setClassName(resources_.styles().lineCell());
      if (entry.getLine() >= 0)
         tdLine.setInnerText("Line " + entry.getLine());
      tr.appendChild(tdLine);

      TableCellElement tdMsg = Document.get().createTDElement();
      tdMsg.setClassName(resources_.styles().messageCell());
      tdMsg.setInnerText(entry.getMessage());
      tr.appendChild(tdMsg);
      
      TableCellElement tdDiscButton = maybeCreateDisclosureButton(entry);
      if (tdDiscButton != null)
         tr.appendChild(tdDiscButton);

      return tr;

   }
   
   protected TableCellElement maybeCreateDisclosureButton(CompilePdfError entry)
   {
      if (entry.getLogLine() != -1)
      {
         TableCellElement td = Document.get().createTDElement();
         td.setClassName(resources_.styles().disclosure());
         td.setVAlign("middle");
   
         DivElement div = Document.get().createDivElement();
         div.setTitle("View error or warning within the log file");
         div.setClassName(resources_.styles().disclosure());
   
         td.appendChild(div);
         return td;
      }
      else
      {
         return null;
      }
   }

   @Override
   protected boolean needsBreak(TableRowElement prevRow, TableRowElement row)
   {
      if (!showFileHeaders_)
         return false;

      if (prevRow == null)
         return true;

      String a = StringUtil.notNull(row.getAttribute(DATA_PATH));
      String b = StringUtil.notNull(prevRow.getAttribute(DATA_PATH));
      return !a.equals(b);
   }

   @Override
   protected int addBreak(TableRowElement row)
   {
      TableRowElement headerRow = Document.get().createTRElement();
      headerRow.setClassName(resources_.styles().headerRow());

      TableCellElement cell = Document.get().createTDElement();
      cell.setColSpan(3);
      cell.setInnerText(row.getAttribute(DATA_PATH));

      headerRow.appendChild(cell);

      row.getParentElement().insertBefore(headerRow, row);

      return 1;
   }

   @Override
   public CodeNavigationTarget getOutputForRow(TableRowElement row)
   {
      String path = row.getAttribute(DATA_PATH);
      int line = Integer.parseInt(row.getAttribute(DATA_LINE));
      if (line < 0) // If we couldn't figure out the line
         line = 1;
      return new CodeNavigationTarget(path,
                                      FilePosition.create(line, 0));
   }

   @Override
   public CodeNavigationTarget getOutputForRow2(TableRowElement row)
   {
      String path = row.getAttribute(LOG_PATH);
      int line = Integer.parseInt(row.getAttribute(LOG_LINE));
      if (line < 0) // If we couldn't figure out the line
         line = 1;
      return new CodeNavigationTarget(path,
                                      FilePosition.create(line, 0));
   }

   @Override
   public boolean isValueRow(TableRowElement row)
   {
      return row.hasAttribute(DATA_LINE);
   }

   @Override
   public boolean hasNonValueRows()
   {
      return showFileHeaders_;
   }

   private final CompilePdfResources resources_;
   private boolean showFileHeaders_;

   private static final String DATA_PATH = "data-path";
   private static final String DATA_LINE = "data-line";
   private static final String LOG_PATH = "log-path";
   private static final String LOG_LINE = "log-line";
}
