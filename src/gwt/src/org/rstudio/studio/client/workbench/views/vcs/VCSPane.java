package org.rstudio.studio.client.workbench.views.vcs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.CellTable.Style;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionModel;
import com.google.inject.Inject;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.vcs.VCS.Display;

import java.util.ArrayList;

public class VCSPane extends WorkbenchPane implements Display
{
   protected interface CellTableResources extends CellTable.Resources
   {
      @Override
      @Source("VCSPaneCellTableStyle.css")
      Style cellTableStyle();
   }

   @Inject
   public VCSPane(Session session, Commands commands)
   {
      super(session.getSessionInfo().getVcsName());
      commands_ = commands;
   }

   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar();
      toolbar.addLeftWidget(commands_.vcsDiff().createToolbarButton());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(commands_.vcsStage().createToolbarButton());
      toolbar.addLeftWidget(commands_.vcsUnstage().createToolbarButton());
      toolbar.addLeftWidget(commands_.vcsRevert().createToolbarButton());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(commands_.vcsCommit().createToolbarButton());

      toolbar.addRightWidget(commands_.vcsRefresh().createToolbarButton());
      return toolbar;
   }

   @Override
   protected Widget createMainWidget()
   {
      table_ = new CellTable<StatusAndPath>(
            100, (CellTable.Resources) GWT.create(CellTableResources.class));

      TextColumn<StatusAndPath> statusColumn = new TextColumn<StatusAndPath>()
      {
         @Override
         public String getValue(StatusAndPath object)
         {
            return object.getStatus().replaceAll(" ", "\u00A0");
         }
      };
      table_.addColumn(statusColumn);

      TextColumn<StatusAndPath> pathColumn = new TextColumn<StatusAndPath>()
      {
         @Override
         public String getValue(StatusAndPath object)
         {
            return object.getPath();
         }
      };
      table_.addColumn(pathColumn);

      table_.setSelectionModel(new MultiSelectionModel<StatusAndPath>(
            new ProvidesKey<StatusAndPath>()
            {
               @Override
               public Object getKey(StatusAndPath item)
               {
                  return item.getPath();
               }
            }));

      table_.setSize("100%", "auto");

      ScrollPanel scrollPanel = new ScrollPanel(table_);

      return scrollPanel;
   }

   @Override
   public void setItems(ArrayList<StatusAndPath> items)
   {
      items_ = items;
      table_.setPageSize(items.size());
      table_.setRowData(items);
   }

   @Override
   public ArrayList<String> getSelectedPaths()
   {
      SelectionModel<? super StatusAndPath> selectionModel = table_.getSelectionModel();

      ArrayList<String> results = new ArrayList<String>();
      for (StatusAndPath item : items_)
      {
         if (selectionModel.isSelected(item))
            results.add(item.getRawPath());
      }
      return results;
   }

   private final Commands commands_;
   private CellTable<StatusAndPath> table_;
   private ArrayList<StatusAndPath> items_;
}
