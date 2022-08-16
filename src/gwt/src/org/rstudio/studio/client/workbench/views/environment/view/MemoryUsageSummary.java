/*
 * MemoryUsageSummary.java
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.environment.view;

import com.google.gwt.aria.client.Id;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.HeadingElement;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.workbench.views.environment.ViewEnvironmentConstants;
import org.rstudio.studio.client.workbench.views.environment.model.MemoryStat;
import org.rstudio.studio.client.workbench.views.environment.model.MemoryUsage;
import org.rstudio.studio.client.workbench.views.environment.model.MemoryUsageReport;

public class MemoryUsageSummary extends Composite
{
   interface Style extends CssResource
   {
      String mbCell();
      String stats();
      String header();
      String swatch();
      String legend();
   }

   /**
    * Creates a new memory usage summary widget
    *
    * @param report The memory usage report to show in the widget.
    */
   public MemoryUsageSummary(MemoryUsageReport report)
   {
      pie_ = new MemoryUsagePieChart(report.getSystemUsage());

      initWidget(GWT.<MemoryUsageSummary.Binder>create(MemoryUsageSummary.Binder.class).createAndBindUi(this));

      MemoryUsage usage = report.getSystemUsage();

      // Size pie chart showing the percentage of memory used
      pie_.getElement().getStyle().setWidth(150, com.google.gwt.dom.client.Style.Unit.PX);
      pie_.getElement().getStyle().setHeight(150, com.google.gwt.dom.client.Style.Unit.PX);
      ElementIds.assignElementId(pie_, ElementIds.MEMORY_PIE_FULL);

      int percent = usage.getPercentUsed();
      pieLabel_.setText(percent + "%");

      Element statsTable = Document.get().createTableElement();
      ElementIds.assignElementId(statsTable, ElementIds.MEMORY_USAGE_TABLE);
      statsTable.setClassName(style.stats());

      // Create the header title for the table
      HeadingElement header = Document.get().createHElement(1);
      header.setClassName(style.header());
      header.setInnerText(constants_.memoryUsage());
      stats_.getElement().appendChild(header);
      ElementIds.assignElementId(header, ElementIds.MEMORY_TABLE_TITLE);

      // Create the header row for the table:
      //
      // Color  Statistic  Memory  Source
      // -----  ---------  ------  ------
      Element statsHeader = Document.get().createTHeadElement();
      Element statsRow = Document.get().createTRElement();
      Element colorCell = Document.get().createTHElement();

      Roles.getDialogRole().setAriaHiddenState(colorCell, true);
      colorCell.setClassName(style.legend());
      statsRow.appendChild(colorCell);

      Element statCell = Document.get().createTHElement();
      statCell.setInnerText(constants_.statisticCapitalized());
      statsRow.appendChild(statCell);
      Element memoryCell = Document.get().createTHElement();
      memoryCell.setInnerText(constants_.memoryCapitalized());
      statsRow.appendChild(memoryCell);
      Element sourceCell = Document.get().createTHElement();
      sourceCell.setInnerText(constants_.sourceCapitalized());
      statsRow.appendChild(sourceCell);
      statsHeader.appendChild(statsRow);

      statsTable.appendChild(statsHeader);

      // Create the table body
      Element statsBody = Document.get().createTBodyElement();
      statsTable.appendChild(statsBody);

      // Create a row for each statistic

      // The sum of all the objects in R (cons + vectors, as reported by gc())
      statsBody.appendChild(buildStatsRow(
         null,
         constants_.usedByRObjects(),
         report.getRUsage().getConsKb() + report.getRUsage().getVectorKb(),
         "R"
      ));

      // The size of the R session process itself, exclusive of child sessions, jobs, etc.
      statsBody.appendChild(buildStatsRow(
         MemoryUsagePieChart.getProcessColorCode(
            report.getSystemUsage().getPercentUsed()),
         constants_.usedBySession(),
         report.getSystemUsage().getProcess()));

      // The memory used by the system that isn't already accounted for in the process
      statsBody.appendChild(buildStatsRow(
         MemoryUsagePieChart.getSystemColorCode(
            report.getSystemUsage().getPercentUsed()),
         constants_.usedBySystem(),
         report.getSystemUsage().getUsed().getKb() - report.getSystemUsage().getProcess().getKb(),
         report.getSystemUsage().getUsed().getProviderName()));

      // The memory left on the system (the total less the used)
      statsBody.appendChild(buildStatsRow(
         MemUsageWidget.MEMORY_PIE_UNUSED_COLOR,
         constants_.freeSystemMemory(),
         report.getSystemUsage().getTotal().getKb() - report.getSystemUsage().getUsed().getKb(),
         report.getSystemUsage().getUsed().getProviderName()));

      // Total system memory
      statsBody.appendChild(buildStatsRow(
         null,
         constants_.totalSystemMemory(),
         report.getSystemUsage().getTotal()));

      stats_.getElement().appendChild(statsTable);
      Roles.getDialogRole().setAriaLabelledbyProperty(statsTable, Id.of(header));

      // Hack to force SVG to draw
      String html = pie_.getParent().getElement().getInnerHTML();
      pie_.getParent().getElement().setInnerHTML(html);
   }

   public interface Binder extends UiBinder<Widget, MemoryUsageSummary>
   {
   }

   /**
    * Create a row from a memory statistic
    *
    * @param color The color swatch for the statistic, or null for no swatch
    * @param name The name of the statistic
    * @param stat The statistic
    * @return A table row containing the statistic.
    */
   private TableRowElement buildStatsRow(String color, String name, MemoryStat stat)
   {
      return buildStatsRow(color, name, stat.getKb(), stat.getProviderName());
   }

   /**
    * Create a row from a memory statistic
    *
    * @param stat The name of the statistic
    * @param kb The number of kb in the statistic
    * @param source The source of the statistic
    * @return A table row containing the statistic
    */
   private TableRowElement buildStatsRow(String color, String stat, int kb, String source)
   {
      TableRowElement row = Document.get().createTRElement();

      // This cell contains a small color swatch that serves as a guide for interpreting the pie chart. The entire
      // column is hidden from the accessibility tree since it's just visual decoration for the chart values.
      TableCellElement colorCell = Document.get().createTDElement();
      if (color != null)
      {
         DivElement swatch = Document.get().createDivElement();
         swatch.getStyle().setBackgroundColor(color);
         swatch.addClassName(style.swatch());
         Roles.getDialogRole().setAriaHiddenState(swatch, true);
         colorCell.appendChild(swatch);
      }
      row.appendChild(colorCell);

      TableCellElement statCell = Document.get().createTDElement();
      statCell.setInnerText(stat);
      row.appendChild(statCell);

      TableCellElement kbCell = Document.get().createTDElement();
      Element kbVal = Document.get().createElement("strong");
      kbVal.setInnerText(StringUtil.prettyFormatNumber(kb / 1024));
      kbCell.appendChild(kbVal);
      Element kbLabel = Document.get().createSpanElement();
      kbLabel.setInnerText(" MiB");
      kbCell.appendChild(kbLabel);
      kbCell.setClassName(style.mbCell());
      row.appendChild(kbCell);

      TableCellElement sourceCell = Document.get().createTDElement();
      sourceCell.setInnerText(source);
      row.appendChild(sourceCell);

      return row;
   }
   private static final ViewEnvironmentConstants constants_ = GWT.create(ViewEnvironmentConstants.class);
   @UiField(provided = true) MemoryUsagePieChart pie_;
   @UiField Label pieLabel_;
   @UiField HTMLPanel stats_;
   @UiField Style style;
}
