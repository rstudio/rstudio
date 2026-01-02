/*
 * PaneLayoutPreferencesPane.java
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
package org.rstudio.studio.client.workbench.prefs.views;

import java.util.ArrayList;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.FormCheckBox;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.ScrollPanelWithClick;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.studio.client.workbench.prefs.PrefsConstants;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefsAccessor;
import org.rstudio.studio.client.workbench.ui.PaneConfig;
import org.rstudio.studio.client.workbench.ui.PaneManager;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class PaneLayoutPreferencesPane extends PreferencesPane {
   class ExclusiveSelectionMaintainer {
      class ListChangeHandler implements ChangeHandler {
         ListChangeHandler(int whichList) {
            whichList_ = whichList;
         }

         public void onChange(ChangeEvent event) {
            int selectedIndex = lists_[whichList_].getSelectedIndex();

            for (int i = 0; i < lists_.length; i++) {
               if (i != whichList_
                     && lists_[i].getSelectedIndex() == selectedIndex) {
                  lists_[i].setSelectedIndex(notSelectedIndex());
               }
            }

            updateTabSetPositions();
         }

         private Integer notSelectedIndex() {
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

      ExclusiveSelectionMaintainer(ListBox[] lists) {
         lists_ = lists;
         for (int i = 0; i < lists.length; i++)
            lists[i].addChangeHandler(new ListChangeHandler(i));
      }

      private final ListBox[] lists_;
   }

   class ModuleList extends Composite implements ValueChangeHandler<Boolean>,
         HasValueChangeHandlers<ArrayList<Boolean>> {
      ModuleList(String width) {
         this(width, SCROLL_PANEL_HEIGHT);
      }

      ModuleList(String width, int height) {
         checkBoxes_ = new ArrayList<>();
         moduleIds_ = new ArrayList<>();
         FlowPanel flowPanel = new FlowPanel();
         for (String module : PaneConfig.getAllTabs()) {
            CheckBox checkBox = new CheckBox(PaneConfig.getPaneDisplayLabel(module), false);
            checkBox.addValueChangeHandler(this);
            checkBoxes_.add(checkBox);
            moduleIds_.add(module);
            flowPanel.add(checkBox);
            if (StringUtil.equals(module, PaneManager.PRESENTATION_PANE))
               checkBox.setVisible(false);
            if (StringUtil.equals(module, PaneManager.CHAT_PANE) &&
                  !userPrefs_.pai().getGlobalValue())
               checkBox.setVisible(false);
         }

         ScrollPanel scrollPanel = new ScrollPanelWithClick();
         scrollPanel.setStyleName(res_.styles().paneLayoutTable());
         scrollPanel.setWidth(width);
         scrollPanel.setHeight(height + "px");
         scrollPanel.add(flowPanel);
         initWidget(scrollPanel);
      }

      public void onValueChange(ValueChangeEvent<Boolean> event) {
         ValueChangeEvent.fire(this, getSelectedIndices());
      }

      public ArrayList<Boolean> getSelectedIndices() {
         ArrayList<Boolean> results = new ArrayList<>();
         for (CheckBox checkBox : checkBoxes_)
            results.add(checkBox.getValue());
         return results;
      }

      public void setSelectedIndices(ArrayList<Boolean> selected) {
         for (int i = 0; i < selected.size(); i++)
            checkBoxes_.get(i).setValue(selected.get(i), false);
      }

      public ArrayList<String> getValue() {
         ArrayList<String> value = new ArrayList<>();
         for (int i = 0; i < checkBoxes_.size(); i++) {
            if (checkBoxes_.get(i).getValue())
               value.add(moduleIds_.get(i));
         }
         return value;
      }

      public ArrayList<String> getVisibleValue() {
         ArrayList<String> value = new ArrayList<>();
         for (int i = 0; i < checkBoxes_.size(); i++) {
            if (checkBoxes_.get(i).getValue() && checkBoxes_.get(i).isVisible())
               value.add(moduleIds_.get(i));
         }
         return value;
      }

      public void setValue(ArrayList<String> tabs) {
         for (int i = 0; i < checkBoxes_.size(); i++)
            checkBoxes_.get(i).setValue(tabs.contains(moduleIds_.get(i)), false);
      }

      public boolean presentationVisible() {
         if (checkBoxes_.size() <= 0)
            return false;

         int lastIndex = checkBoxes_.size() - 1;
         CheckBox lastCheckBox = checkBoxes_.get(lastIndex);
         return StringUtil.equals(moduleIds_.get(lastIndex), "Presentation") &&
               lastCheckBox.isVisible();
      }

      public HandlerRegistration addValueChangeHandler(
            ValueChangeHandler<ArrayList<Boolean>> handler) {
         return addHandler(handler, ValueChangeEvent.getType());
      }

      private final ArrayList<CheckBox> checkBoxes_;
      private final ArrayList<String> moduleIds_;
   }

   @Inject
   public PaneLayoutPreferencesPane(PreferencesDialogResources res,
         UserPrefs userPrefs,
         Provider<PaneManager> pPaneManager) {
      res_ = res;
      userPrefs_ = userPrefs;
      paneManager_ = pPaneManager.get();

      PaneConfig paneConfig = userPrefs.panes().getGlobalValue().cast();

      additionalColumnCount_ = paneConfig.getAdditionalSourceColumns();

      add(new Label(constants_.paneLayoutText(),
            true));

      columnToolbar_ = new Toolbar(constants_.columnToolbarLabel());
      columnToolbar_.setStyleName(res_.styles().newSection());
      columnToolbar_.setHeight("20px");

      ToolbarButton addButton = new ToolbarButton(
            constants_.addButtonText(),
            constants_.addButtonLabel(),
            res_.iconAddSourcePane());
      if (displayColumnCount_ > PaneManager.MAX_COLUMN_COUNT - 1 ||
            !userPrefs.allowSourceColumns().getGlobalValue())
         addButton.setEnabled(false);

      ToolbarButton removeButton = new ToolbarButton(
            constants_.removeButtonText(), constants_.removeButtonLabel(),
            res_.iconRemoveSourcePane());
      removeButton.setEnabled(additionalColumnCount_ > 0);

      addButton.addClickHandler(event -> {
         dirty_ = true;
         updateTable(displayColumnCount_ + 1);

         if (displayColumnCount_ > PaneManager.MAX_COLUMN_COUNT - 1)
            addButton.setEnabled(false);
         if (!removeButton.isEnabled())
            removeButton.setEnabled(true);
      });

      removeButton.addClickHandler(event -> {
         dirty_ = true;
         updateTable(displayColumnCount_ - 1);

         if (displayColumnCount_ < 1)
            removeButton.setEnabled(false);
         if (!addButton.isEnabled())
            addButton.setEnabled(true);
      });

      columnToolbar_.addLeftWidget(addButton);
      columnToolbar_.addLeftSeparator();
      columnToolbar_.addLeftWidget(removeButton);
      columnToolbar_.addLeftSeparator();

      // Create a wrapper panel for the toolbar row
      FlowPanel toolbarWrapper = new FlowPanel();
      toolbarWrapper.getElement().getStyle().setProperty("position", "relative");
      toolbarWrapper.getElement().getStyle().setProperty("height", "20px");

      toolbarWrapper.add(columnToolbar_);

      // Create and position Sidebar Visible checkbox
      sidebarVisibleCheckbox_ = new FormCheckBox(
            constants_.sidebarVisible(),
            ElementIds.getUniqueElementId(ElementIds.PANE_LAYOUT_SIDEBAR_VISIBLE));
      sidebarVisibleCheckbox_.getElement().getStyle().setProperty("position", "absolute");
      sidebarVisibleCheckbox_.getElement().getStyle().setProperty("top", "0");
      toolbarWrapper.add(sidebarVisibleCheckbox_);

      add(toolbarWrapper);

      String[] visiblePanes = PaneConfig.getVisiblePanes();

      leftTop_ = new ListBox();
      Roles.getListboxRole().setAriaLabelProperty(leftTop_.getElement(), "Top left panel");
      ElementIds.assignElementId(leftTop_.getElement(), ElementIds.PANE_LAYOUT_LEFT_TOP_SELECT);

      leftBottom_ = new ListBox();
      Roles.getListboxRole().setAriaLabelProperty(leftBottom_.getElement(), "Bottom left panel");
      ElementIds.assignElementId(leftBottom_.getElement(), ElementIds.PANE_LAYOUT_LEFT_BOTTOM_SELECT);

      rightTop_ = new ListBox();
      Roles.getListboxRole().setAriaLabelProperty(rightTop_.getElement(), "Top right panel");
      ElementIds.assignElementId(rightTop_.getElement(), ElementIds.PANE_LAYOUT_RIGHT_TOP_SELECT);

      rightBottom_ = new ListBox();
      Roles.getListboxRole().setAriaLabelProperty(rightBottom_.getElement(), "Bottom right panel");
      ElementIds.assignElementId(rightBottom_.getElement(), ElementIds.PANE_LAYOUT_RIGHT_BOTTOM_SELECT);

      visiblePanes_ = new ListBox[] { leftTop_, leftBottom_, rightTop_, rightBottom_ };
      for (ListBox lb : visiblePanes_) {
         for (String value : visiblePanes)
            lb.addItem(value);
      }

      if (paneConfig == null || !paneConfig.validateAndAutoCorrect())
         userPrefs.panes().setGlobalValue(PaneConfig.createDefault(), false);

      JsArrayString origPanes = userPrefs.panes().getGlobalValue().getQuadrants();
      for (int i = 0; i < 4; i++) {
         boolean success = selectByValue(visiblePanes_[i], origPanes.get(i));
         if (!success) {
            Debug.log("Bad config! Falling back to a reasonable default");
            leftTop_.setSelectedIndex(0);
            leftBottom_.setSelectedIndex(1);
            rightTop_.setSelectedIndex(2);
            rightBottom_.setSelectedIndex(3);
            break;
         }
      }

      new ExclusiveSelectionMaintainer(visiblePanes_);

      for (ListBox lb : visiblePanes_)
         lb.addChangeHandler(event -> dirty_ = true);

      // Create module lists first with a default width
      String defaultPaneWidth = "200px";
      tabSet1ModuleList_ = new ModuleList(defaultPaneWidth);
      tabSet1ModuleList_.setValue(toArrayList(userPrefs.panes().getGlobalValue().getTabSet1()));
      tabSet2ModuleList_ = new ModuleList(defaultPaneWidth);
      tabSet2ModuleList_.setValue(toArrayList(userPrefs.panes().getGlobalValue().getTabSet2()));
      hiddenTabSetModuleList_ = new ModuleList(defaultPaneWidth);
      hiddenTabSetModuleList_.setValue(toArrayList(
            userPrefs.panes().getGlobalValue().getHiddenTabSet()));
      sidebarModuleList_ = new ModuleList(defaultPaneWidth, TABLE_HEIGHT * 2 - 55);
      sidebarModuleList_.setValue(toArrayList(userPrefs.panes().getGlobalValue().getSidebar()));

      // Get current config for initializing sidebar preferences
      PaneConfig currentConfig = userPrefs.panes().getGlobalValue().cast();

      // Initialize sidebar visible checkbox with preference value
      sidebarVisibleCheckbox_.setValue(currentConfig.getSidebarVisible());
      sidebarVisibleCheckbox_.addValueChangeHandler(event -> dirty_ = true);

      // Create sidebar location dropdown
      sidebarLocation_ = new ListBox();
      ElementIds.assignElementId(sidebarLocation_.getElement(), ElementIds.PANE_LAYOUT_SIDEBAR_SELECT);
      sidebarLocation_.addItem(constants_.sidebarLocationLeft());
      sidebarLocation_.addItem(constants_.sidebarLocationRight());

      // Set initial selection based on current preference
      String currentLocation = currentConfig.getSidebarLocation();
      if ("left".equals(currentLocation))
         sidebarLocation_.setSelectedIndex(0);
      else
         sidebarLocation_.setSelectedIndex(1); // default to right

      // Add change handler to track changes and rebuild grid
      sidebarLocation_.addChangeHandler(event -> {
         dirty_ = true;
         // Force complete grid rebuild to reposition sidebar
         if (grid_ != null) {
            remove(grid_);
            grid_ = null;
         }
         updateTable(displayColumnCount_);

         // Ensure reset panel stays at the bottom after grid rebuild
         if (resetPanel_ != null) {
            remove(resetPanel_);
            add(resetPanel_);
         }
      });

      // Now update the table which will set the correct widths
      updateTable(additionalColumnCount_);

      visiblePanePanels_ = new VerticalPanel[] { leftTopPanel_, leftBottomPanel_,
            rightTopPanel_, rightBottomPanel_ };

      ValueChangeHandler<ArrayList<Boolean>> vch = new ValueChangeHandler<ArrayList<Boolean>>() {
         public void onValueChange(ValueChangeEvent<ArrayList<Boolean>> e) {
            dirty_ = true;

            ModuleList source = (ModuleList) e.getSource();
            ModuleList other = (source == tabSet1ModuleList_)
                  ? tabSet2ModuleList_
                  : tabSet1ModuleList_;

            // an index should only be on for one of these lists,
            ArrayList<Boolean> indices = source.getSelectedIndices();
            ArrayList<Boolean> otherIndices = other.getSelectedIndices();
            ArrayList<Boolean> hiddenIndices = hiddenTabSetModuleList_.getSelectedIndices();
            ArrayList<Boolean> sidebarIndices = sidebarModuleList_.getSelectedIndices();
            if (!PaneConfig.isValidConfig(source.getValue())) {
               // when the configuration is invalid, we must reset sources to the prior valid
               // configuration based on the values of the other lists
               for (int i = 0; i < indices.size(); i++)
                  indices.set(i, !(otherIndices.get(i) || hiddenIndices.get(i) || sidebarIndices.get(i)));
               source.setSelectedIndices(indices);
            } else {
               for (int i = 0; i < indices.size(); i++) {
                  if (indices.get(i)) {
                     otherIndices.set(i, false);
                     hiddenIndices.set(i, false);
                     sidebarIndices.set(i, false);
                  } else if (!otherIndices.get(i) && !sidebarIndices.get(i))
                     hiddenIndices.set(i, true);
               }
               other.setSelectedIndices(otherIndices);
               hiddenTabSetModuleList_.setSelectedIndices(hiddenIndices);
               sidebarModuleList_.setSelectedIndices(sidebarIndices);

               updateTabSetLabels();
               updateSidebarVisibilityCheckbox();
            }
         }
      };
      tabSet1ModuleList_.addValueChangeHandler(vch);
      tabSet2ModuleList_.addValueChangeHandler(vch);

      // Add value change handler for sidebar
      sidebarModuleList_.addValueChangeHandler(new ValueChangeHandler<ArrayList<Boolean>>() {
         public void onValueChange(ValueChangeEvent<ArrayList<Boolean>> e) {
            dirty_ = true;

            ArrayList<Boolean> sidebarIndices = sidebarModuleList_.getSelectedIndices();
            ArrayList<Boolean> tabSet1Indices = tabSet1ModuleList_.getSelectedIndices();
            ArrayList<Boolean> tabSet2Indices = tabSet2ModuleList_.getSelectedIndices();
            ArrayList<Boolean> hiddenIndices = hiddenTabSetModuleList_.getSelectedIndices();

            // Ensure mutual exclusivity
            for (int i = 0; i < sidebarIndices.size(); i++) {
               if (sidebarIndices.get(i)) {
                  tabSet1Indices.set(i, false);
                  tabSet2Indices.set(i, false);
                  hiddenIndices.set(i, false);
               } else if (!tabSet1Indices.get(i) && !tabSet2Indices.get(i))
                  hiddenIndices.set(i, true);
            }

            tabSet1ModuleList_.setSelectedIndices(tabSet1Indices);
            tabSet2ModuleList_.setSelectedIndices(tabSet2Indices);
            hiddenTabSetModuleList_.setSelectedIndices(hiddenIndices);

            updateTabSetLabels();
            updateSidebarVisibilityCheckbox();
         }
      });

      updateTabSetPositions();
      updateTabSetLabels();

      // Add reset link below the grid, right-justified
      resetPanel_ = new FlowPanel();
      resetPanel_.getElement().getStyle().setProperty("textAlign", "right");
      resetPanel_.getElement().getStyle().setProperty("marginRight", "4px");

      Anchor resetLink = new Anchor(constants_.resetPaneLayoutToDefaults());
      ElementIds.assignElementId(resetLink.getElement(), ElementIds.PANE_LAYOUT_RESET_LINK);
      resetLink.addStyleName("rstudio-themes-flat");
      resetLink.addClickHandler(event -> {
         event.preventDefault();
         resetToDefaults();
      });

      resetPanel_.add(resetLink);
      add(resetPanel_);
   }

   private void resetToDefaults() {
      // Get default configuration
      PaneConfig defaultConfig = PaneConfig.createDefault();

      // Reset quadrant selections
      JsArrayString defaultPanes = defaultConfig.getQuadrants();
      for (int i = 0; i < 4; i++)
         selectByValue(visiblePanes_[i], defaultPanes.get(i));

      // Reset tab assignments
      tabSet1ModuleList_.setValue(toArrayList(defaultConfig.getTabSet1()));
      tabSet2ModuleList_.setValue(toArrayList(defaultConfig.getTabSet2()));
      hiddenTabSetModuleList_.setValue(toArrayList(defaultConfig.getHiddenTabSet()));
      sidebarModuleList_.setValue(toArrayList(defaultConfig.getSidebar()));

      // Reset sidebar preferences
      sidebarVisibleCheckbox_.setValue(defaultConfig.getSidebarVisible());
      sidebarLocation_.setSelectedIndex("left".equals(defaultConfig.getSidebarLocation()) ? 0 : 1);

      // Force complete grid rebuild to reposition sidebar if needed
      if (grid_ != null) {
         remove(grid_);
         grid_ = null;
      }

      // Reset column count to 0 (default has no additional columns)
      updateTable(0);

      // Update labels to reflect new configuration
      updateTabSetPositions();
      updateTabSetLabels();
      updateSidebarVisibilityCheckbox();

      // Ensure reset panel stays at the bottom after grid rebuild
      if (resetPanel_ != null) {
         remove(resetPanel_);
         add(resetPanel_);
      }

      // Mark as dirty so changes apply on OK/Apply
      dirty_ = true;
   }

   private String updateTable(int newCount) {
      // nothing has changed since the last update
      if (grid_ != null && displayColumnCount_ == newCount)
         return "";

      // Check if sidebar should be on the left
      boolean sidebarOnLeft = (sidebarLocation_ != null && sidebarLocation_.getSelectedIndex() == 0);

      // Calculate total column units: source columns + 2 quadrant pairs + 1 sidebar
      // (same size as quadrant pair)
      // Each quadrant pair takes 2 units, sidebar takes 2 units
      double columnCount = newCount + (2 * GRID_PANE_COUNT) + GRID_PANE_COUNT;
      double columnWidthValue = (double) TABLE_WIDTH / columnCount;
      double cellWidthValue = columnWidthValue * GRID_PANE_COUNT;
      double sidebarWidthValue = cellWidthValue; // Sidebar same width as quadrants

      // If the column width is bigger than MAX_COLUMN_WIDTH, give space back to the
      // panes
      if (newCount > 0 && Math.min(columnWidthValue, MAX_COLUMN_WIDTH) != columnWidthValue) {
         double extra = (newCount * (columnWidthValue - MAX_COLUMN_WIDTH)) / GRID_PANE_COUNT;
         cellWidthValue += extra;
         columnWidthValue = MAX_COLUMN_WIDTH;
      }
      cellWidthValue -= (GRID_CELL_SPACING + GRID_CELL_PADDING);
      columnWidthValue -= (GRID_CELL_SPACING + GRID_CELL_PADDING);
      sidebarWidthValue -= (GRID_CELL_SPACING + GRID_CELL_PADDING);

      final String columnWidth = columnWidthValue + "px";
      final String cellWidth = cellWidthValue + "px";
      final String sidebarWidth = sidebarWidthValue + "px";
      final String selectWidth = (cellWidthValue - GRID_SELECT_PADDING) + "px";
      leftTop_.setWidth(selectWidth);
      leftBottom_.setWidth(selectWidth);
      rightTop_.setWidth(selectWidth);
      rightBottom_.setWidth(selectWidth);

      // create grid
      if (grid_ == null) {
         grid_ = new FlexTable();
         grid_.addStyleName(res_.styles().paneLayoutTable());
         grid_.setCellSpacing(GRID_CELL_SPACING);
         grid_.setCellPadding(GRID_CELL_PADDING);
         grid_.setWidth(TABLE_WIDTH + "px");
         grid_.setHeight(TABLE_HEIGHT + "px");
         Roles.getGridRole().setAriaLabelProperty(grid_.getElement(), constants_.createGridLabel());

         int topColumn = 0;

         // If sidebar is on the left, add it first
         if (sidebarOnLeft) {
            grid_.setWidget(0, topColumn, createSidebarPane());
            grid_.getFlexCellFormatter().setRowSpan(0, topColumn, 2);
            grid_.getCellFormatter().setStyleName(0, topColumn, res_.styles().paneLayoutTable());
            grid_.getCellFormatter().setWidth(0, topColumn, sidebarWidth);
            topColumn++;
         }

         // Add source columns
         for (int i = 0; i < newCount; i++, topColumn++) {
            ScrollPanel sp = createColumn();
            grid_.setWidget(0, topColumn, sp);
            grid_.getFlexCellFormatter().setRowSpan(0, topColumn, 2);
            grid_.getCellFormatter().setStyleName(0, topColumn, res_.styles().paneLayoutTable());
            grid_.getColumnFormatter().setWidth(topColumn, columnWidth);
         }

         // Add quadrants
         grid_.setWidget(0, topColumn, leftTopPanel_ = createPane(leftTop_, ElementIds.PANE_LAYOUT_LEFT_TOP));
         grid_.getCellFormatter().setStyleName(0, topColumn, res_.styles().paneLayoutTable());
         grid_.getCellFormatter().setWidth(0, topColumn, cellWidth);

         grid_.setWidget(0, ++topColumn, rightTopPanel_ = createPane(rightTop_, ElementIds.PANE_LAYOUT_RIGHT_TOP));
         grid_.getCellFormatter().setStyleName(0, topColumn, res_.styles().paneLayoutTable());
         grid_.getCellFormatter().setWidth(0, topColumn, cellWidth);

         // If sidebar is on the right, add it after the quadrants
         if (!sidebarOnLeft) {
            grid_.setWidget(0, ++topColumn, createSidebarPane());
            grid_.getFlexCellFormatter().setRowSpan(0, topColumn, 2);
            grid_.getCellFormatter().setStyleName(0, topColumn, res_.styles().paneLayoutTable());
            grid_.getCellFormatter().setWidth(0, topColumn, sidebarWidth);
         }

         int bottomColumn = 0;
         grid_.setWidget(1, bottomColumn,
               leftBottomPanel_ = createPane(leftBottom_, ElementIds.PANE_LAYOUT_LEFT_BOTTOM));
         grid_.getCellFormatter().setStyleName(1, bottomColumn, res_.styles().paneLayoutTable());
         grid_.getCellFormatter().setWidth(1, bottomColumn, cellWidth);

         grid_.setWidget(1, ++bottomColumn,
               rightBottomPanel_ = createPane(rightBottom_, ElementIds.PANE_LAYOUT_RIGHT_BOTTOM));
         grid_.getCellFormatter().setStyleName(1, bottomColumn, res_.styles().paneLayoutTable());
         grid_.getCellFormatter().setWidth(1, bottomColumn, cellWidth);

         add(grid_);
         displayColumnCount_ = newCount;

         // Update the array to reference the new panels
         visiblePanePanels_ = new VerticalPanel[] { leftTopPanel_, leftBottomPanel_,
               rightTopPanel_, rightBottomPanel_ };

         // Re-attach module lists to the new panels
         updateTabSetPositions();

         // Update module list widths and heights after grid creation
         tabSet1ModuleList_.setWidth(cellWidth);
         tabSet2ModuleList_.setWidth(cellWidth);
         sidebarModuleList_.setWidth(sidebarWidth);

         // Update sidebar location dropdown width
         if (sidebarLocation_ != null) {
            String dropdownWidth = (Double.parseDouble(sidebarWidth.replace("px", "")) - GRID_SELECT_PADDING) + "px";
            sidebarLocation_.setWidth(dropdownWidth);
         }

         // Position toolbar to align with source columns or first quadrant
         if (columnToolbar_ != null) {
            double leftMargin = 0;
            if (sidebarOnLeft) {
               // If sidebar is on left, shift toolbar to start after it
               leftMargin = sidebarWidthValue + GRID_CELL_SPACING + GRID_CELL_PADDING;
            }
            columnToolbar_.getElement().getStyle().setProperty("marginLeft", leftMargin + "px");
         }

         // Position checkbox to align with sidebar column (using absolute positioning)
         if (sidebarVisibleCheckbox_ != null) {
            double checkboxLeft = 0;
            if (sidebarOnLeft) {
               // Sidebar is on left, checkbox should be at the start
               checkboxLeft = 0;
            } else {
               // Sidebar is on right, shift checkbox to start after source columns and
               // quadrants
               checkboxLeft = (newCount * columnWidthValue) + (2 * cellWidthValue) +
                     ((newCount + 2) * (GRID_CELL_SPACING + GRID_CELL_PADDING));
            }
            sidebarVisibleCheckbox_.getElement().getStyle().setProperty("left", checkboxLeft + "px");
         }

         return cellWidth;
      }

      // adjust existing grid
      int difference = newCount - displayColumnCount_;
      displayColumnCount_ = newCount;

      // Source columns start at position 1 if sidebar is on left, 0 if on right
      int sourceColumnStart = sidebarOnLeft ? 1 : 0;

      // when the number of columns has decreased, remove columns
      for (int i = 0; i > difference; i--)
         grid_.removeCell(0, sourceColumnStart);

      // when the number of columns has increased, add columns
      for (int i = 0; i < difference; i++) {
         ScrollPanel sp = createColumn();
         grid_.insertCell(0, sourceColumnStart);
         grid_.setWidget(0, sourceColumnStart, sp);
         grid_.getFlexCellFormatter().setRowSpan(0, sourceColumnStart, 2);
         grid_.getCellFormatter().setStyleName(0, sourceColumnStart, res_.styles().paneLayoutTable());
      }

      // update the widths for source columns
      for (int i = 0; i < newCount; i++)
         grid_.getCellFormatter().setWidth(0, sourceColumnStart + i, columnWidth);
      tabSet1ModuleList_.setWidth(cellWidth);
      tabSet2ModuleList_.setWidth(cellWidth);
      sidebarModuleList_.setWidth(sidebarWidth);

      // Update sidebar location dropdown width
      if (sidebarLocation_ != null) {
         String dropdownWidth = (Double.parseDouble(sidebarWidth.replace("px", "")) - GRID_SELECT_PADDING) + "px";
         sidebarLocation_.setWidth(dropdownWidth);
      }

      // Update sidebar column width
      int sidebarCol = sidebarOnLeft ? 0 : (newCount + 2); // If left: first column, if right: after source columns +
                                                           // quadrants
      grid_.getCellFormatter().setWidth(0, sidebarCol, sidebarWidth);

      // ensure grid maintains proper dimensions
      grid_.setWidth(TABLE_WIDTH + "px");
      grid_.setHeight(TABLE_HEIGHT + "px");

      // Position toolbar to align with source columns or first quadrant
      if (columnToolbar_ != null) {
         double leftMargin = 0;
         if (sidebarOnLeft) {
            // If sidebar is on left, shift toolbar to start after it
            leftMargin = Double.parseDouble(sidebarWidth.replace("px", "")) + GRID_CELL_SPACING + GRID_CELL_PADDING;
         }
         columnToolbar_.getElement().getStyle().setProperty("marginLeft", leftMargin + "px");
      }

      // Position checkbox to align with sidebar column (using absolute positioning)
      if (sidebarVisibleCheckbox_ != null) {
         double checkboxLeft = 0;
         if (sidebarOnLeft) {
            // Sidebar is on left, checkbox should be at the start
            checkboxLeft = 0;
         } else {
            // Sidebar is on right, shift checkbox to start after source columns and
            // quadrants
            double colWidth = Double.parseDouble(columnWidth.replace("px", ""));
            double cellW = Double.parseDouble(cellWidth.replace("px", ""));
            checkboxLeft = (newCount * colWidth) + (2 * cellW) +
                  ((newCount + 2) * (GRID_CELL_SPACING + GRID_CELL_PADDING));
         }
         sidebarVisibleCheckbox_.getElement().getStyle().setProperty("left", checkboxLeft + "px");
      }

      return cellWidth;
   }

   private VerticalPanel createPane(ListBox listBox, String paneId) {
      VerticalPanel vp = new VerticalPanel();
      vp.add(listBox);
      ElementIds.assignElementId(vp.getElement(), paneId);
      return vp;
   }

   private ScrollPanel createColumn() {
      VerticalPanel verticalPanel = new VerticalPanel();
      FormLabel label = new FormLabel();
      label.setText(UserPrefsAccessor.Panes.QUADRANTS_SOURCE);
      label.setStyleName(res_.styles().label());
      verticalPanel.add(label);

      ScrollPanel sp = new ScrollPanel();
      sp.add(verticalPanel);
      Roles.getTextboxRole().setAriaLabelProperty(sp.getElement(), constants_.createColumnLabel());

      return sp;
   }

   private VerticalPanel createSidebarPane() {
      VerticalPanel vp = new VerticalPanel();

      // Add the dropdown for sidebar location
      if (sidebarLocation_ != null) {
         vp.add(sidebarLocation_);
         // Set width to match other dropdowns
         String selectWidth = (sidebarLocation_.getOffsetWidth() > 0) ? sidebarLocation_.getOffsetWidth() + "px"
               : "100%";
         sidebarLocation_.setWidth(selectWidth);
      }

      // Add the module list
      if (sidebarModuleList_ != null)
         vp.add(sidebarModuleList_);

      ElementIds.assignElementId(vp.getElement(), ElementIds.PANE_LAYOUT_SIDEBAR);
      return vp;
   }

   private static boolean selectByValue(ListBox listBox, String value) {
      for (int i = 0; i < listBox.getItemCount(); i++) {
         if (listBox.getValue(i) == value) {
            listBox.setSelectedIndex(i);
            return true;
         }
      }

      return false;
   }

   @Override
   public ImageResource getIcon() {
      return new ImageResource2x(res_.iconPanes2x());
   }

   @Override
   protected void initialize(UserPrefs prefs) {
   }

   @Override
   public RestartRequirement onApply(UserPrefs rPrefs) {
      RestartRequirement restartRequirement = super.onApply(rPrefs);

      if (dirty_) {
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

         JsArrayString sidebar = JsArrayString.createArray().cast();
         for (String tab : sidebarModuleList_.getValue())
            sidebar.push(tab);

         JsArrayString hiddenTabSet = JsArrayString.createArray().cast();
         for (String tab : hiddenTabSetModuleList_.getValue())
            hiddenTabSet.push(tab);

         // Determine implicit preference for console top/bottom location
         // This needs to be saved so that when the user executes the
         // Console on Left/Right commands we know whether to position
         // the Console on the Top or Bottom
         PaneConfig prevConfig = userPrefs_.panes().getGlobalValue().cast();
         boolean consoleLeftOnTop = prevConfig.getConsoleLeftOnTop();
         boolean consoleRightOnTop = prevConfig.getConsoleRightOnTop();
         if (panes.get(0).equals(PaneManager.CONSOLE_PANE))
            consoleLeftOnTop = true;
         else if (panes.get(1).equals(PaneManager.CONSOLE_PANE))
            consoleLeftOnTop = false;
         else if (panes.get(2).equals(PaneManager.CONSOLE_PANE))
            consoleRightOnTop = true;
         else if (panes.get(3).equals(PaneManager.CONSOLE_PANE))
            consoleRightOnTop = false;

         if (displayColumnCount_ != additionalColumnCount_)
            additionalColumnCount_ = paneManager_.syncAdditionalColumnCount(displayColumnCount_, true);

         // Get sidebar visibility from checkbox
         boolean sidebarVisible = sidebarVisibleCheckbox_.getValue();

         // Get the selected sidebar location from dropdown
         String sidebarLocation = "right"; // default
         if (sidebarLocation_ != null) {
            int selectedIndex = sidebarLocation_.getSelectedIndex();
            sidebarLocation = (selectedIndex == 0) ? "left" : "right";
         }

         userPrefs_.panes().setGlobalValue(PaneConfig.create(
               panes, tabSet1, tabSet2, hiddenTabSet,
               consoleLeftOnTop, consoleRightOnTop, additionalColumnCount_,
               sidebar, sidebarVisible, sidebarLocation));

         // Clear sidebar cache and refresh it to show new tabs immediately
         paneManager_.clearSidebarCache();
         paneManager_.refreshSidebar();

         dirty_ = false;
      }

      return restartRequirement;
   }

   @Override
   public String getName() {
      return constants_.paneLayoutLabel();
   }

   private void updateTabSetPositions() {
      for (int i = 0; i < visiblePanes_.length; i++) {
         String value = visiblePanes_[i].getValue(visiblePanes_[i].getSelectedIndex());
         if (StringUtil.equals(value, UserPrefsAccessor.Panes.QUADRANTS_TABSET1))
            visiblePanePanels_[i].add(tabSet1ModuleList_);
         else if (StringUtil.equals(value, UserPrefsAccessor.Panes.QUADRANTS_TABSET2))
            visiblePanePanels_[i].add(tabSet2ModuleList_);
      }
   }

   private void updateTabSetLabels() {
      // If no tabs are values in a tabset pane, give the pane a generic name,
      // otherwise the name is created from the selected values
      String itemText1 = tabSet1ModuleList_.getValue().isEmpty() ? "TabSet"
            : StringUtil.join(tabSet1ModuleList_.getValue(), ", ");
      String itemText2 = tabSet2ModuleList_.getValue().isEmpty() ? "TabSet"
            : StringUtil.join(tabSet2ModuleList_.getValue(), ", ");
      if (StringUtil.equals(itemText1, "Presentation") && !tabSet1ModuleList_.presentationVisible())
         itemText1 = "TabSet";

      for (ListBox pane : visiblePanes_) {
         pane.setItemText(2, itemText1);
         pane.setItemText(3, itemText2);
      }
   }

   private boolean hasSidebarTabs() {
      return !sidebarModuleList_.getVisibleValue().isEmpty();
   }

   private void updateSidebarVisibilityCheckbox() {
      boolean hasTabs = hasSidebarTabs();
      boolean currentlyVisible = sidebarVisibleCheckbox_.getValue();

      // Auto-check when adding first tab to hidden sidebar
      if (hasTabs && !currentlyVisible) {
         sidebarVisibleCheckbox_.setValue(true, false);
      }
      // Auto-uncheck when removing last tab from visible sidebar
      else if (!hasTabs && currentlyVisible) {
         sidebarVisibleCheckbox_.setValue(false, false);
      }
   }

   private ArrayList<String> toArrayList(JsArrayString strings) {
      ArrayList<String> results = new ArrayList<>();
      for (int i = 0; strings != null && i < strings.length(); i++)
         results.add(strings.get(i));
      return results;
   }

   private final PreferencesDialogResources res_;
   private final UserPrefs userPrefs_;
   private final ListBox leftTop_;
   private final ListBox leftBottom_;
   private final ListBox rightTop_;
   private final ListBox rightBottom_;
   private final ListBox[] visiblePanes_;
   private VerticalPanel[] visiblePanePanels_;
   private final ModuleList tabSet1ModuleList_;
   private final ModuleList tabSet2ModuleList_;
   private final ModuleList hiddenTabSetModuleList_;
   private final ModuleList sidebarModuleList_;
   private final ListBox sidebarLocation_;
   private final CheckBox sidebarVisibleCheckbox_;
   private final PaneManager paneManager_;
   private boolean dirty_ = false;
   private Toolbar columnToolbar_;

   private VerticalPanel leftTopPanel_;
   private VerticalPanel leftBottomPanel_;
   private VerticalPanel rightTopPanel_;
   private VerticalPanel rightBottomPanel_;
   private FlowPanel resetPanel_;

   private int additionalColumnCount_ = 0;
   private int displayColumnCount_ = 0;
   private FlexTable grid_;

   private final static int GRID_CELL_SPACING = 8;
   private final static int GRID_CELL_PADDING = 6;
   private final static int MAX_COLUMN_WIDTH = 50 + GRID_CELL_PADDING + GRID_CELL_SPACING;

   private final static int TABLE_HEIGHT = PreferencesDialogConstants.PANEL_CONTAINER_HEIGHT - 355;
   private final static int TABLE_WIDTH = PreferencesDialogConstants.PANE_CONTAINER_WIDTH - 8;
   private final static int SCROLL_PANEL_HEIGHT = TABLE_HEIGHT - 40;

   private final static int GRID_PANE_COUNT = 2;
   private final static int GRID_SELECT_PADDING = 10; // must match CSS file
   private final static PrefsConstants constants_ = GWT.create(PrefsConstants.class);
}
