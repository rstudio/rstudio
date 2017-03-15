/*
 * PaneLayoutPreferencesPane.java
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
package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.*;
import com.google.inject.Inject;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.studio.client.workbench.prefs.model.RPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.ui.PaneConfig;

import java.util.ArrayList;

public class PaneLayoutPreferencesPane extends PreferencesPane
{
   class ExclusiveSelectionMaintainer
   {
      class ListChangeHandler implements ChangeHandler
      {
         ListChangeHandler(int whichList)
         {
            whichList_ = whichList;
         }

         public void onChange(ChangeEvent event)
         {
            int selectedIndex = lists_[whichList_].getSelectedIndex();

            for (int i = 0; i < lists_.length; i++)
            {
               if (i != whichList_
                   && lists_[i].getSelectedIndex() == selectedIndex)
               {
                  lists_[i].setSelectedIndex(notSelectedIndex());
               }
            }

            updateTabSetPositions();
         }

         private Integer notSelectedIndex()
         {
            boolean[] seen = new boolean[4];
            for (ListBox listBox : lists_)
               seen[listBox.getSelectedIndex()] = true;
            for (int i = 0; i < seen.length; i++)
               if (!seen[i])
                  return i;
            return null;
         }

         private final int whichList_;
      }

      ExclusiveSelectionMaintainer(ListBox[] lists)
      {
         lists_ = lists;
         for (int i = 0; i < lists.length; i++)
            lists[i].addChangeHandler(new ListChangeHandler(i));
      }

      private final ListBox[] lists_;
   }

   class ModuleList extends Composite implements ValueChangeHandler<Boolean>,
                                                 HasValueChangeHandlers<ArrayList<Boolean>>
   {
      ModuleList()
      {
         checkBoxes_ = new ArrayList<CheckBox>();
         VerticalPanel panel = new VerticalPanel();
         for (String module : PaneConfig.getAllTabs())
         {
            CheckBox checkBox = new CheckBox(module, false);
            checkBox.addValueChangeHandler(this);
            checkBoxes_.add(checkBox);
            panel.add(checkBox);
            if (module.equals("Presentation"))
               checkBox.setVisible(false);
         }
         initWidget(panel);
      }

      public void onValueChange(ValueChangeEvent<Boolean> event)
      {
         ValueChangeEvent.fire(this, getSelectedIndices());
      }

      public ArrayList<Boolean> getSelectedIndices()
      {
         ArrayList<Boolean> results = new ArrayList<Boolean>();
         for (CheckBox checkBox : checkBoxes_)
            results.add(checkBox.getValue());
         return results;
      }

      public void setSelectedIndices(ArrayList<Boolean> selected)
      {
         for (int i = 0; i < selected.size(); i++)
            checkBoxes_.get(i).setValue(selected.get(i), false);
      }

      public ArrayList<String> getValue()
      {
         ArrayList<String> value = new ArrayList<String>();
         for (CheckBox checkBox : checkBoxes_)
         {
            if (checkBox.getValue())
               value.add(checkBox.getText());
         }
         return value;
      }

      public void setValue(ArrayList<String> tabs)
      {
         for (CheckBox checkBox : checkBoxes_)
            checkBox.setValue(tabs.contains(checkBox.getText()), false);
      }

      public HandlerRegistration addValueChangeHandler(
            ValueChangeHandler<ArrayList<Boolean>> handler)
      {
         return addHandler(handler, ValueChangeEvent.getType());
      }

      private final ArrayList<CheckBox> checkBoxes_;
   }


   @Inject
   public PaneLayoutPreferencesPane(PreferencesDialogResources res,
                                    UIPrefs uiPrefs)
   {
      res_ = res;
      uiPrefs_ = uiPrefs;

      add(new Label("Choose the layout of the panes in RStudio by selecting from the controls in each quadrant.", true));

      String[] allPanes = PaneConfig.getAllPanes();

      leftTop_ = new ListBox();
      leftBottom_ = new ListBox();
      rightTop_ = new ListBox();
      rightBottom_ = new ListBox();
      allPanes_ = new ListBox[]{leftTop_, leftBottom_, rightTop_, rightBottom_};
      for (ListBox lb : allPanes_)
      {
         for (String value : allPanes)
            lb.addItem(value);
      }

      PaneConfig value = uiPrefs.paneConfig().getGlobalValue();
      if (value == null || !value.validateAndAutoCorrect())
         uiPrefs.paneConfig().setGlobalValue(PaneConfig.createDefault(), false);

      JsArrayString origPanes = uiPrefs.paneConfig().getGlobalValue().getPanes();
      for (int i = 0; i < 4; i++)
      {
         boolean success = selectByValue(allPanes_[i], origPanes.get(i));
         if (!success)
         {
            Debug.log("Bad config! Falling back to a reasonable default");
            leftTop_.setSelectedIndex(0);
            leftBottom_.setSelectedIndex(1);
            rightTop_.setSelectedIndex(2);
            rightBottom_.setSelectedIndex(3);
            break;
         }
      }

      new ExclusiveSelectionMaintainer(allPanes_);

      for (ListBox lb : allPanes_)
         lb.addChangeHandler(new ChangeHandler()
         {
            public void onChange(ChangeEvent event)
            {
               dirty_ = true;
            }
         });

      Grid grid = new Grid(2, 2);
      grid.addStyleName(res.styles().paneLayoutTable());
      grid.setCellSpacing(8);
      grid.setCellPadding(6);
      grid.setWidget(0, 0, leftTopPanel_ = createPane(leftTop_));
      grid.setWidget(1, 0, leftBottomPanel_ = createPane(leftBottom_));
      grid.setWidget(0, 1, rightTopPanel_ = createPane(rightTop_));
      grid.setWidget(1, 1, rightBottomPanel_ = createPane(rightBottom_));
      for (int row = 0; row < 2; row++)
         for (int col = 0; col < 2; col++)
            grid.getCellFormatter().setStyleName(row, col,
                                                 res.styles().paneLayoutTable());
      add(grid);

      allPanePanels_ = new VerticalPanel[] {leftTopPanel_, leftBottomPanel_,
                                            rightTopPanel_, rightBottomPanel_};

      tabSet1ModuleList_ = new ModuleList();
      tabSet1ModuleList_.setValue(toArrayList(uiPrefs.paneConfig().getGlobalValue().getTabSet1()));
      tabSet2ModuleList_ = new ModuleList();
      tabSet2ModuleList_.setValue(toArrayList(uiPrefs.paneConfig().getGlobalValue().getTabSet2()));

      ValueChangeHandler<ArrayList<Boolean>> vch = new ValueChangeHandler<ArrayList<Boolean>>()
      {
         public void onValueChange(ValueChangeEvent<ArrayList<Boolean>> e)
         {
            dirty_ = true;

            ModuleList source = (ModuleList) e.getSource();
            ModuleList other = (source == tabSet1ModuleList_)
                               ? tabSet2ModuleList_
                               : tabSet1ModuleList_;

            if (!PaneConfig.isValidConfig(source.getValue()))
            {
               ArrayList<Boolean> indices = source.getSelectedIndices();
               ArrayList<Boolean> otherIndices = other.getSelectedIndices();
               for (int i = 0; i < indices.size(); i++)
               {
                  indices.set(i, !otherIndices.get(i));
               }
               source.setSelectedIndices(indices);
            }
            else
            {
               ArrayList<Boolean> indices = source.getSelectedIndices();
               ArrayList<Boolean> otherIndices = new ArrayList<Boolean>();
               for (Boolean b : indices)
                  otherIndices.add(!b);
               other.setSelectedIndices(otherIndices);

               updateTabSetLabels();
            }
         }
      };
      tabSet1ModuleList_.addValueChangeHandler(vch);
      tabSet2ModuleList_.addValueChangeHandler(vch);

      updateTabSetPositions();
      updateTabSetLabels();
   }

   private VerticalPanel createPane(ListBox listBox)
   {
      VerticalPanel vp = new VerticalPanel();
      vp.add(listBox);
      return vp;
   }

   private static boolean selectByValue(ListBox listBox, String value)
   {
      for (int i = 0; i < listBox.getItemCount(); i++)
      {
         if (listBox.getValue(i).equals(value))
         {
            listBox.setSelectedIndex(i);
            return true;
         }
      }

      return false;
   }

   @Override
   public ImageResource getIcon()
   {
      return new ImageResource2x(res_.iconPanes2x());
   }
   
   @Override
   protected void initialize(RPrefs prefs)
   {
   }

   @Override
   public boolean onApply(RPrefs rPrefs)
   {
      boolean restartRequired = super.onApply(rPrefs);

      if (dirty_)
      {
         JsArrayString panes = JsArrayString.createArray().cast();
         panes.push(leftTop_.getValue(leftTop_.getSelectedIndex()));
         panes.push(leftBottom_.getValue(leftBottom_.getSelectedIndex()));
         panes.push(rightTop_.getValue(rightTop_.getSelectedIndex()));
         panes.push(rightBottom_.getValue(rightBottom_.getSelectedIndex()));

         JsArrayString tabSet1 = JsArrayString.createArray().cast();
         for (String tab : tabSet1ModuleList_.getValue())
            tabSet1.push(tab);

         JsArrayString tabSet2 = JsArrayString.createArray().cast();
         for (String tab : tabSet2ModuleList_.getValue())
            tabSet2.push(tab);
         
         // Determine implicit preference for console top/bottom location
         // This needs to be saved so that when the user executes the 
         // Console on Left/Right commands we know whether to position 
         // the Console on the Top or Bottom
         PaneConfig prevConfig = uiPrefs_.paneConfig().getGlobalValue();
         boolean consoleLeftOnTop = prevConfig.getConsoleLeftOnTop();
         boolean consoleRightOnTop = prevConfig.getConsoleRightOnTop();
         final String kConsole = "Console";
         if (panes.get(0).equals(kConsole))
            consoleLeftOnTop = true;
         else if (panes.get(1).equals(kConsole))
            consoleLeftOnTop = false;
         else if (panes.get(2).equals(kConsole))
            consoleRightOnTop = true;
         else if (panes.get(3).equals(kConsole))
            consoleRightOnTop = false;
         
         uiPrefs_.paneConfig().setGlobalValue(PaneConfig.create(
               panes, tabSet1, tabSet2, consoleLeftOnTop, consoleRightOnTop));

         dirty_ = false;
      }

      return restartRequired;
   }

   @Override
   public String getName()
   {
      return "Pane Layout";
   }

   private void updateTabSetPositions()
   {
      for (int i = 0; i < allPanes_.length; i++)
      {
         String value = allPanes_[i].getValue(allPanes_[i].getSelectedIndex());
         if (value.equals("TabSet1"))
            allPanePanels_[i].add(tabSet1ModuleList_);
         else if (value.equals("TabSet2"))
            allPanePanels_[i].add(tabSet2ModuleList_);
      }
   }

   private void updateTabSetLabels()
   {
      for (ListBox pane : allPanes_)
      {
         pane.setItemText(2, StringUtil.join(tabSet1ModuleList_.getValue(), ", "));
         pane.setItemText(3, StringUtil.join(tabSet2ModuleList_.getValue(), ", "));
      }
   }

   private ArrayList<String> toArrayList(JsArrayString strings)
   {
      ArrayList<String> results = new ArrayList<String>();
      for (int i = 0; i < strings.length(); i++)
         results.add(strings.get(i));
      return results;
   }

   private final PreferencesDialogResources res_;
   private final UIPrefs uiPrefs_;
   private final ListBox leftTop_;
   private final ListBox leftBottom_;
   private final ListBox rightTop_;
   private final ListBox rightBottom_;
   private final ListBox[] allPanes_;
   private final VerticalPanel leftTopPanel_;
   private final VerticalPanel leftBottomPanel_;
   private final VerticalPanel rightTopPanel_;
   private final VerticalPanel rightBottomPanel_;
   private final VerticalPanel[] allPanePanels_;
   private final ModuleList tabSet1ModuleList_;
   private final ModuleList tabSet2ModuleList_;
   private boolean dirty_ = false;
  
}
