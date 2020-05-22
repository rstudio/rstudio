/*
 * PackageActionConfirmationDialog.java
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
package org.rstudio.studio.client.workbench.views.packages.ui;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.aria.client.DialogRole;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.cellview.AriaLabeledCheckboxCell;
import org.rstudio.core.client.cellview.LabeledBoolean;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.server.ServerDataSource;
import org.rstudio.studio.client.server.ServerError;

import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.NoSelectionModel;

public abstract class PackageActionConfirmationDialog<T extends JavaScriptObject> extends ModalDialog<ArrayList<T>>
{
   public PackageActionConfirmationDialog(
         String caption,
         String okCaption,
         DialogRole role,
         ServerDataSource<JsArray<T>> actionsDS,
         OperationWithInput<ArrayList<T>> checkOperation,
         Operation cancelOperation)
   {
      super(caption, role, checkOperation, cancelOperation);
      actionsDS_ = actionsDS;
      
      setOkButtonCaption(okCaption);
    
      addLeftButton(selectAllButton_ = new ThemedButton("Select All",
         event -> setGlobalPerformAction("Select All", true)), ElementIds.SELECT_ALL_BUTTON);
      
      selectAllButton_.getElement().getStyle().setMarginRight(10, Unit.PX);
     
      addLeftButton(selectNoneButton_ = new ThemedButton("Select None",
         event -> setGlobalPerformAction("Select None", false)), ElementIds.SELECT_NONE_BUTTON);
      
      enableOkButton(false);
      selectAllButton_.setEnabled(false);
      selectNoneButton_.setEnabled(false);
   }

   @Override
   protected ArrayList<T> collectInput()
   {
      ArrayList<T> actions = new ArrayList<T>();
      for (PendingAction action : actionsDataProvider_.getList())
      {
         if (action.getPerformAction().getBool())
            actions.add(action.getActionInfo());
      }
      return actions;
   }

   @Override
   protected boolean validate(ArrayList<T> input)
   {
      return input.size() > 0;
   }

   @Override
   protected Widget createMainWidget()
   {
      FlowPanel flowPanel = new FlowPanel();
      String explanatoryText = getExplanatoryText();
      if (explanatoryText.length() > 0)
      {
         Label text = new Label(explanatoryText);
         text.setStylePrimaryName(RESOURCES.styles().explanatoryText());
         flowPanel.add(text);
      }
      
      actionsTable_ = new CellTable<>(
            15,
            GWT.<PackagesCellTableResources> create(PackagesCellTableResources.class));
      actionsTable_.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
      actionsTable_.setSelectionModel(new NoSelectionModel<>());
      actionsTable_.setWidth("100%", true);
      
      ActionColumn actionColumn = new ActionColumn();
      actionsTable_.addColumn(actionColumn);
      actionsTable_.setColumnWidth(actionColumn, 30, Unit.PX);
      
      addTableColumns(actionsTable_);

      ScrollPanel scrollPanel = new ScrollPanel();
      scrollPanel.setWidget(actionsTable_);
      scrollPanel.setStylePrimaryName(RESOURCES.styles().mainWidget());
      flowPanel.add(scrollPanel);
      
      // query for updates
      actionsDS_.requestData(new SimpleRequestCallback<JsArray<T>>() {

         @Override
         public void onResponseReceived(JsArray<T> actions)
         {
            if (actions != null && actions.length() > 0)
            {
               ArrayList<PendingAction> pendingActions = new ArrayList<>();
               for (int i=0; i<actions.length(); i++)
                  pendingActions.add(new PendingAction(actions.get(i),
                     new LabeledBoolean(getActionName(actions.get(i)), false)));
               actionsTable_.setPageSize(pendingActions.size());
               actionsDataProvider_ = new ListDataProvider<>();
               actionsDataProvider_.setList(pendingActions);
               actionsDataProvider_.addDataDisplay(actionsTable_);
               
               selectAllButton_.setEnabled(true);
               selectNoneButton_.setEnabled(true);
               refreshFocusableElements();
               focusInitialControl();
            }
            else
            {
               closeDialog();
               showNoActionsRequired();
            }
         }
         
         @Override
         public void onError(ServerError error)
         {
            closeDialog();
            super.onError(error);
         }
      });

      return flowPanel;
   }

   protected String getExplanatoryText()
   {
      return "";
   }
   
   protected abstract void showNoActionsRequired(); 
   protected abstract void addTableColumns(CellTable<PendingAction> table);
   protected abstract String getActionName(T action);
  
   class ActionColumn extends Column<PendingAction, LabeledBoolean>
   {
      public ActionColumn()
      {
         super(new AriaLabeledCheckboxCell(false, false));
         
         setFieldUpdater(new FieldUpdater<PendingAction, LabeledBoolean>() {
            public void update(int index, PendingAction action, LabeledBoolean value)
            {
               List<PendingAction> actions = actionsDataProvider_.getList();
               actions.set(actions.indexOf(action), new PendingAction(action.getActionInfo(), value));
               manageUIState();
            }
         });
      }

      @Override
      public LabeledBoolean getValue(PendingAction update)
      {
         return update.getPerformAction();
      }
   }
   
   protected class PendingAction
   {
      public PendingAction(T actionInfo, LabeledBoolean performAction)
      {
         actionInfo_ = actionInfo;
         performAction_ = performAction;
      }
      
      public T getActionInfo()
      {
         return actionInfo_;
      }
      
      public LabeledBoolean getPerformAction()
      {
         return performAction_;
      }

      private final T actionInfo_;
      private final LabeledBoolean performAction_;
   }
   
   private void setGlobalPerformAction(String label, Boolean performAction)
   {
      List<PendingAction> actions = actionsDataProvider_.getList();
      ArrayList<PendingAction> newActions = new ArrayList<PendingAction>();
      for(PendingAction action : actions)
         newActions.add(new PendingAction(action.getActionInfo(), new LabeledBoolean(label, performAction)));
      actionsDataProvider_.setList(newActions);
      manageUIState();
   }
   
   private void manageUIState()
   {
      enableOkButton(collectInput().size() > 0);
   }
   
   interface Styles extends CssResource
   {
      String mainWidget();
      String explanatoryText();
   }

   interface Resources extends ClientBundle
   {
      @Source("PackageActionConfirmationDialog.css")
      Styles styles();
   }

   static Resources RESOURCES = GWT.create(Resources.class);

   public static void ensureStylesInjected()
   {
      RESOURCES.styles().ensureInjected();
   }

   private CellTable<PendingAction> actionsTable_;
   private ServerDataSource<JsArray<T>> actionsDS_;
   private ListDataProvider<PendingAction> actionsDataProvider_;
   private ThemedButton selectAllButton_;
   private ThemedButton selectNoneButton_;

}
