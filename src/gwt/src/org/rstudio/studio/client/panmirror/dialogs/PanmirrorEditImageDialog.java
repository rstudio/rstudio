/*
 * PanmirrorEditImageDialog.java
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


package org.rstudio.studio.client.panmirror.dialogs;

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.theme.DialogTabLayoutPanel;
import org.rstudio.core.client.theme.VerticalTabPanel;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.NumericTextBox;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorAttrProps;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorImageDimensions;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorImageProps;
import org.rstudio.studio.client.panmirror.ui.PanmirrorUIContext;
import org.rstudio.studio.client.panmirror.uitools.PanmirrorUITools;
import org.rstudio.studio.client.panmirror.uitools.PanmirrorUIToolsImage;
import org.rstudio.studio.client.rmarkdown.model.RMarkdownServerOperations;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;


public class PanmirrorEditImageDialog extends ModalDialog<PanmirrorImageProps>
{
   public PanmirrorEditImageDialog(PanmirrorImageProps props,
                                   PanmirrorImageDimensions dims,
                                   boolean editAttributes,
                                   PanmirrorUIContext uiContext,
                                   OperationWithInput<PanmirrorImageProps> operation)
   {
      super("Image", Roles.getDialogRole(), operation, () -> {
         // cancel returns null
         operation.execute(null);
      });
      
      RStudioGinjector.INSTANCE.injectMembers(this);

      // natural width, height, and containerWidth (will be null if this
      // is an insert image dialog)
      dims_ = dims;

      // size props that we are going to reflect back to the caller. the idea is that
      // if the user makes no explicit edits of size props then we just return
      // exactly what we were passed. this allows us to show a width and height
      // for images that are 'unsized' (i.e. just use natural height and width). the
      // in-editor resizing shelf implements the same behavior.
      widthProp_ = props.width;
      heightProp_ = props.height;
      unitsProp_ = props.units;

      // image tab
      VerticalTabPanel imageTab = new VerticalTabPanel(ElementIds.VISUAL_MD_IMAGE_TAB_IMAGE);
      imageTab.addStyleName(RES.styles().dialog());

      // panel for size controls (won't be added if this is an insert or !editAttributes)
      HorizontalPanel sizePanel = new HorizontalPanel();
      sizePanel.addStyleName(RES.styles().spaced());
      sizePanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);

      // image url picker
      imageTab.add(url_ = new PanmirrorImageChooser(uiContext, server_));
      url_.addStyleName(RES.styles().spaced());
      if (!StringUtil.isNullOrEmpty(props.src))
         url_.setText(props.src);
      // when the url is changed we no longer know the image dimensions. in this case
      // just wipe out those props and remove the image sizing ui. note that immediately
      // after insert the size controls will appear at the bottom of the image.
      url_.addValueChangeHandler(value -> {
         widthProp_ = null;
         heightProp_ = null;
         unitsProp_ = null;
         dims_ = null;
         imageTab.remove(sizePanel);
      });

      // width, height, units
      width_ = addSizeInput(sizePanel, ElementIds.VISUAL_MD_IMAGE_WIDTH, "Width:");
      height_ = addSizeInput(sizePanel, ElementIds.VISUAL_MD_IMAGE_HEIGHT, "Height:");
      heightAuto_ = createHorizontalLabel("(Auto)");
      heightAuto_.addStyleName(RES.styles().heightAuto());
      sizePanel.add(heightAuto_);
      units_ = addUnitsSelect(sizePanel);
      initSizeInputs();

      // lock ratio
      lockRatio_ = new CheckBox("Lock ratio");
      lockRatio_.addStyleName(RES.styles().lockRatioCheckbox());
      lockRatio_.getElement().setId(ElementIds.VISUAL_MD_IMAGE_LOCK_RATIO);
      lockRatio_.setValue(props.lockRatio);
      sizePanel.add(lockRatio_);

      // update widthProp_ and height (if lockRatio) when width text box changes
      width_.addChangeHandler(event -> {
         String width = width_.getText();
         widthProp_ = StringUtil.isNullOrEmpty(width) ? null : Double.parseDouble(width);
         if (widthProp_ != null && lockRatio_.getValue()) {
            double height = widthProp_ * (dims_.naturalHeight/dims_.naturalWidth);
            height_.setValue(uiTools_.roundUnit(height, units_.getSelectedValue()));
            heightProp_ = Double.parseDouble(height_.getValue());
         }
         unitsProp_ = units_.getSelectedValue();
      });

      // update heightProp_ and width (if lockRatio) when height text box changes
      height_.addChangeHandler(event -> {
         String height = height_.getText();
         heightProp_ = StringUtil.isNullOrEmpty(height) ? null : Double.parseDouble(height);
         if (heightProp_ != null && lockRatio_.getValue()) {
            double width = heightProp_ * (dims_.naturalWidth/dims_.naturalHeight);
            width_.setValue(uiTools_.roundUnit(width, units_.getSelectedValue()));
            widthProp_ = Double.parseDouble(width_.getValue());
         }
         unitsProp_ = units_.getSelectedValue();
      });

      // do applicable unit conversion when units change
      units_.addChangeHandler(event -> {

         String width = width_.getText();
         if (!StringUtil.isNullOrEmpty(width))
         {
            double widthPixels = uiTools_.unitToPixels(Double.parseDouble(width), prevUnits_, dims_.containerWidth);
            double widthUnit = uiTools_.pixelsToUnit(widthPixels, units_.getSelectedValue(), dims_.containerWidth);
            width_.setText(uiTools_.roundUnit(widthUnit, units_.getSelectedValue()));
            widthProp_ = Double.parseDouble(width_.getValue());
         }

         String height = height_.getText();
         if (!StringUtil.isNullOrEmpty(height))
         {
            double heightPixels = uiTools_.unitToPixels(Double.parseDouble(height), prevUnits_, dims_.containerWidth);
            double heightUnit = uiTools_.pixelsToUnit(heightPixels, units_.getSelectedValue(), dims_.containerWidth);
            height_.setText(uiTools_.roundUnit(heightUnit, units_.getSelectedValue()));
            heightProp_ = Double.parseDouble(height_.getValue());
         }

         // track previous units for subsequent conversions
         prevUnits_ = units_.getSelectedValue();

         // save units prop
         unitsProp_ = units_.getSelectedValue();

         manageUnitsUI();
      });
      manageUnitsUI();


      // only add sizing controls if we support editAttributes, dims have been provided
      // (i.e. not an insert operation) and there aren't width or height attributes
      // within props.keyvalue (which is an indicator that they use units unsupported
      // by our sizing UI (e.g. ch, em, etc.)
      if (editAttributes && dims_ != null && hasNaturalSizes(dims) && !hasSizeKeyvalue(props.keyvalue))
      {
         imageTab.add(sizePanel);
      }

      // title and alt
      title_ = PanmirrorDialogsUtil.addTextBox(imageTab, ElementIds.VISUAL_MD_IMAGE_TITLE, "Title/Tooltip:", props.title);
      alt_ = PanmirrorDialogsUtil.addTextBox(imageTab, ElementIds.VISUAL_MD_IMAGE_ALT, "Caption/Alt:", props.alt);

      // linkto
      linkTo_ = PanmirrorDialogsUtil.addTextBox(imageTab,  ElementIds.VISUAL_MD_IMAGE_LINK_TO, "Link To:", props.linkTo);

      // standard pandoc attributes
      editAttr_ =  new PanmirrorEditAttrWidget();
      editAttr_.setAttr(props, null);
      if (editAttributes)
      {
         VerticalTabPanel attributesTab = new VerticalTabPanel(ElementIds.VISUAL_MD_IMAGE_TAB_ATTRIBUTES);
         attributesTab.addStyleName(RES.styles().dialog());
         attributesTab.add(editAttr_);

         DialogTabLayoutPanel tabPanel = new DialogTabLayoutPanel("Image");
         tabPanel.addStyleName(RES.styles().imageDialogTabs());
         tabPanel.add(imageTab, "Image", imageTab.getBasePanelId());
         tabPanel.add(attributesTab, "Attributes", attributesTab.getBasePanelId());
         tabPanel.selectTab(0);

         mainWidget_ = tabPanel;
      }
      else
      {
         mainWidget_ = imageTab;
      }
   }
   
   @Inject
   void initialize(RMarkdownServerOperations server)
   {
      server_ = server;
   }

   @Override
   protected Widget createMainWidget()
   {
      return mainWidget_;
   }

   @Override
   public void focusInitialControl()
   {
      url_.getTextBox().setFocus(true);
      url_.getTextBox().setSelectionRange(0, 0);
   }

   @Override
   protected PanmirrorImageProps collectInput()
   {
      // process change event for focused size controls (typically these changes
      // only occur on the change event, which won't occur if the dialog is
      // dismissed while they are focused
      fireChangedIfFocused(width_);
      fireChangedIfFocused(height_);

      // collect and return result
      PanmirrorImageProps result = new PanmirrorImageProps();
      result.src = url_.getTextBox().getValue().trim();
      result.title = title_.getValue().trim();
      result.alt = alt_.getValue().trim();
      result.linkTo = linkTo_.getValue().trim();
      result.width = widthProp_;
      result.height = heightProp_;
      result.units = unitsProp_;
      result.lockRatio = lockRatio_.getValue();
      PanmirrorAttrProps attr = editAttr_.getAttr();
      result.id = attr.id;
      result.classes = attr.classes;
      result.keyvalue = attr.keyvalue;
      return result;
   }

   @Override
   protected boolean validate(PanmirrorImageProps result)
   {
      // width is required if height is specified
      if (height_.getText().trim().length() > 0)
      {
         GlobalDisplay globalDisplay = RStudioGinjector.INSTANCE.getGlobalDisplay();
         String width = width_.getText().trim();
         if (width.length() == 0)
         {
            globalDisplay.showErrorMessage(
               "Error", "You must provide a value for image width."
            );
            width_.setFocus(true);
            return false;
         }
         else
         {
            return true;
         }
      }
      else
      {
         return true;
      }
   }


   // set sizing UI based on passed width, height, and unit props. note that
   // these can be null (default/natural sizing) and in that case we still
   // want to dispaly pixel sizing in the UI as an FYI to the user
   private void initSizeInputs()
   {
      // only init for existing images (i.e. dims passed)
      if (dims_ == null)
         return;

      String width = null, height = null, units = "px";

      // if we have both width and height then use them
      if (widthProp_ != null && heightProp_ != null)
      {
         width = widthProp_.toString();
         height = heightProp_.toString();
         units = unitsProp_;
      }
      else if (dims_.naturalHeight != null && dims_.naturalWidth != null)
      {
         units = unitsProp_;

         // if there is width only then show computed height
         if (widthProp_ == null && heightProp_ == null)
         {
            width = dims_.naturalWidth.toString();
            height = dims_.naturalHeight.toString();
            units = "px";
         }
         else if (widthProp_ != null)
         {
            width = widthProp_.toString();
            height = uiTools_.roundUnit(widthProp_ * (dims_.naturalHeight/dims_.naturalWidth), units);
         }
         else if (heightProp_ != null)
         {
            height = heightProp_.toString();
            width = uiTools_.roundUnit(heightProp_ * (dims_.naturalWidth/dims_.naturalHeight), units);
         }
      }

      // set values into inputs
      width_.setValue(width);
      height_.setValue(height);
      for (int i = 0; i<units_.getItemCount(); i++)
      {
         if (units_.getItemText(i) == units)
         {
            units_.setSelectedIndex(i);
            // track previous units for conversions
            prevUnits_ = units;
            break;
         }
      }
   }

   // show/hide controls and enable/disable lockUnits depending on
   // whether we are using percent sizing
   private void manageUnitsUI()
   {
      boolean percentUnits = units_.getSelectedValue() == uiTools_.percentUnit();

      if (percentUnits)
      {
         lockRatio_.setValue(true);
         lockRatio_.setEnabled(false);
      }
      else
      {
         lockRatio_.setEnabled(true);
      }

      height_.setVisible(!percentUnits);
      heightAuto_.setVisible(percentUnits);
   }


   // create a numeric input
   private static NumericTextBox addSizeInput(Panel panel, String id, String labelText)
   {
      FormLabel label = createHorizontalLabel(labelText);
      NumericTextBox input = new NumericTextBox();
      input.setMin(1);
      input.setMax(10000);
      input.addStyleName(RES.styles().horizontalInput());
      input.getElement().setId(id);
      label.setFor(input);
      panel.add(label);
      panel.add(input);
      return input;
   }

   // create units select list box
   private ListBox addUnitsSelect(Panel panel)
   {
      String[] options = uiTools_.validUnits();
      ListBox units = new ListBox();
      units.addStyleName(RES.styles().horizontalInput());
      for (int i = 0; i < options.length; i++)
         units.addItem(options[i], options[i]);
      units.getElement().setId(ElementIds.VISUAL_MD_IMAGE_UNITS);
      Roles.getListboxRole().setAriaLabelProperty(units.getElement(), "Units");
      panel.add(units);
      return units;
   }

   // create a horizontal label
   private static FormLabel createHorizontalLabel(String text)
   {
      FormLabel label = new FormLabel(text);
      label.addStyleName(RES.styles().horizontalLabel());
      return label;
   }

   // fire a change event if the widget is currently focused
   private static void fireChangedIfFocused(Widget widget)
   {
      if (widget.getElement() == DomUtils.getActiveElement())
         DomEvent.fireNativeEvent(Document.get().createChangeEvent(), widget);
   }

   // check whether the passed keyvalue attributes has a size (width or height)
   private static boolean hasSizeKeyvalue(String[][] keyvalue)
   {
      for (int i=0; i<keyvalue.length; i++)
      {
         String key = keyvalue[i][0];
         if (key.equalsIgnoreCase(WIDTH) || key.equalsIgnoreCase(HEIGHT))
            return true;
      }
      return false;
   }

   private static boolean hasNaturalSizes(PanmirrorImageDimensions dims)
   {
      return dims.naturalWidth != null && dims.naturalHeight != null;
   }

   // resources
   private static PanmirrorDialogsResources RES = PanmirrorDialogsResources.INSTANCE;

   // UI utility functions from panmirror
   private final PanmirrorUIToolsImage uiTools_ = new PanmirrorUITools().image;
   
   private RMarkdownServerOperations server_;

   // original image/container dimensions
   private PanmirrorImageDimensions dims_;

   // current 'edited' values for size props
   private Double widthProp_ = null;
   private Double heightProp_ = null;
   private String unitsProp_ = null;

   // track previous units for conversions
   private String prevUnits_;

   // widgets
   private final Widget mainWidget_;
   private final PanmirrorImageChooser url_;
   private final NumericTextBox width_;
   private final NumericTextBox height_;
   private final FormLabel heightAuto_;
   private final ListBox units_;
   private final CheckBox lockRatio_;
   private final TextBox title_;
   private final TextBox alt_;
   private final TextBox linkTo_;
   private final PanmirrorEditAttrWidget editAttr_;

   private static final String WIDTH = "width";
   private static final String HEIGHT = "height";


}
