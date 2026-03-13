/*
 * TrustRequestDialog.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
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
package org.rstudio.studio.client.workbench;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ThemedButton;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style.FontWeight;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class TrustRequestDialog extends ModalDialogBase
{
   public TrustRequestDialog(String directory,
                             JsArrayString riskyFiles,
                             Operation trustOperation,
                             Operation dontTrustOperation)
   {
      super(Roles.getAlertdialogRole());
      directory_ = directory;
      riskyFiles_ = riskyFiles;
      trustOperation_ = trustOperation;
      dontTrustOperation_ = dontTrustOperation;

      setEscapeDisabled(false);
      setThemeAware(true);
      hideButtons();
   }

   @Override
   protected Widget createMainWidget()
   {
      HorizontalPanel outerPanel = new HorizontalPanel();
      outerPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
      outerPanel.getElement().getStyle().setPadding(8, Unit.PX);

      // lock icon column
      String lockSvg =
         "<svg xmlns='http://www.w3.org/2000/svg' width='64' height='64' " +
         "viewBox='0 0 24 24' fill='none' stroke='currentColor' " +
         "stroke-linecap='round' stroke-linejoin='round'>" +
         "<path d='M7 9V7a5 5 0 0 1 10 0v2' stroke-width='2.5'/>" +
         "<rect x='4' y='9' width='16' height='14' rx='3' ry='3' " +
              "fill='currentColor' stroke='none'/>" +
         "<circle class='rstudio-lock-keyhole' cx='12' cy='14.8' r='1.6' " +
              "fill='#F3F4F4' stroke='none'/>" +
         "<rect class='rstudio-lock-keyhole' x='11.2' y='14.8' width='1.6' height='3.8' " +
              "rx='0.5' fill='#F3F4F4' stroke='none'/>" +
         "</svg>";
      HTML lockIcon = new HTML(lockSvg);
      lockIcon.getElement().getStyle().setPaddingRight(20, Unit.PX);
      lockIcon.getElement().getStyle().setPaddingTop(2, Unit.PX);
      lockIcon.getElement().getStyle().setProperty("minWidth", "68px");
      lockIcon.getElement().getStyle().setProperty("textAlign", "center");
      outerPanel.add(lockIcon);

      // content column
      FlowPanel panel = new FlowPanel();
      panel.setWidth("460px");

      // header
      Label header = new Label(constants_.trustDialogHeader());
      header.getElement().getStyle().setFontSize(16, Unit.PX);
      header.getElement().getStyle().setFontWeight(FontWeight.BOLD);
      header.getElement().getStyle().setMarginBottom(12, Unit.PX);
      panel.add(header);

      // description
      HTML description = new HTML(constants_.trustDialogDescription());
      description.getElement().getStyle().setMarginBottom(12, Unit.PX);
      panel.add(description);

      // project path
      Label pathLabel = new Label(directory_);
      pathLabel.getElement().getStyle().setProperty("fontFamily", "monospace");
      pathLabel.getElement().getStyle().setMarginBottom(12, Unit.PX);
      pathLabel.getElement().getStyle().setProperty("wordBreak", "break-all");
      panel.add(pathLabel);

      // risky files list
      if (riskyFiles_ != null && riskyFiles_.length() > 0)
      {
         FlowPanel filesRow = new FlowPanel();
         filesRow.getElement().getStyle().setMarginBottom(16, Unit.PX);

         InlineLabel prefix = new InlineLabel(constants_.trustDialogFilesDetected() + " ");
         filesRow.add(prefix);

         for (int i = 0; i < riskyFiles_.length(); i++)
         {
            if (i > 0)
            {
               filesRow.add(new InlineLabel(", "));
            }

            final String filename = riskyFiles_.get(i);
            final String filePath = FileSystemItem.createDir(directory_).completePath(filename);

            // .RData is binary; only make text files clickable
            if (filename.equals(".RData"))
            {
               InlineLabel code = new InlineLabel(filename);
               code.getElement().getStyle().setProperty("fontFamily", "monospace");
               filesRow.add(code);
            }
            else
            {
               Anchor link = new Anchor(filename);
               link.setHref("javascript:void(0)");
               link.getElement().getStyle().setProperty("fontFamily", "monospace");
               link.addClickHandler(new ClickHandler()
               {
                  @Override
                  public void onClick(ClickEvent event)
                  {
                     event.preventDefault();
                     ViewFileDialog dialog = new ViewFileDialog(filePath);
                     dialog.showModal();
                  }
               });
               filesRow.add(link);
            }
         }

         panel.add(filesRow);
      }

      // buttons row
      HorizontalPanel buttonsRow = new HorizontalPanel();
      buttonsRow.setWidth("100%");
      buttonsRow.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
      buttonsRow.getElement().getStyle().setProperty("tableLayout", "fixed");

      // trust column
      FlowPanel trustColumn = new FlowPanel();

      ThemedButton trustButton = new ThemedButton(
         constants_.trustDialogTrustButton(),
         new ClickHandler()
         {
            @Override
            public void onClick(ClickEvent event)
            {
               closeDialog();
               if (trustOperation_ != null)
                  trustOperation_.execute();
            }
         });
      trustButton.setWidth("100%");
      trustButton.setWrapperWidth("100%");
      trustButton.getElement().getStyle().setMarginLeft(0, Unit.PX);
      trustButton.getElement().getStyle().setMarginBottom(4, Unit.PX);
      trustColumn.add(trustButton);

      Label trustExplanation = new Label(constants_.trustDialogTrustExplanation());
      trustExplanation.addStyleName("rstudio-trust-detail");
      trustExplanation.getElement().getStyle().setFontSize(11, Unit.PX);
      trustExplanation.getElement().getStyle().setPaddingLeft(2, Unit.PX);
      trustColumn.add(trustExplanation);

      buttonsRow.add(trustColumn);
      buttonsRow.setCellWidth(trustColumn, "50%");

      // don't trust column
      FlowPanel dontTrustColumn = new FlowPanel();
      dontTrustColumn.getElement().getStyle().setPaddingLeft(8, Unit.PX);

      ThemedButton dontTrustButton = new ThemedButton(
         constants_.trustDialogDontTrustButton(),
         new ClickHandler()
         {
            @Override
            public void onClick(ClickEvent event)
            {
               closeDialog();
               if (dontTrustOperation_ != null)
                  dontTrustOperation_.execute();
            }
         });
      dontTrustButton.setWidth("100%");
      dontTrustButton.setWrapperWidth("100%");
      dontTrustButton.getElement().getStyle().setMarginLeft(0, Unit.PX);
      dontTrustButton.getElement().getStyle().setMarginBottom(4, Unit.PX);
      dontTrustButton.getElement().getStyle().setFontWeight(FontWeight.BOLD);
      dontTrustButton_ = dontTrustButton;
      dontTrustColumn.add(dontTrustButton);

      Label dontTrustExplanation = new Label(constants_.trustDialogDontTrustExplanation());
      dontTrustExplanation.addStyleName("rstudio-trust-detail");
      dontTrustExplanation.getElement().getStyle().setFontSize(11, Unit.PX);
      dontTrustExplanation.getElement().getStyle().setPaddingLeft(2, Unit.PX);
      dontTrustColumn.add(dontTrustExplanation);

      buttonsRow.add(dontTrustColumn);
      buttonsRow.setCellWidth(dontTrustColumn, "50%");

      panel.add(buttonsRow);

      outerPanel.add(panel);

      setARIADescribedBy(header.getElement());

      return outerPanel;
   }

   @Override
   protected void onEscapeKeyDown(NativePreviewEvent preview)
   {
      NativeEvent nativeEvent = preview.getNativeEvent();
      nativeEvent.preventDefault();
      nativeEvent.stopPropagation();
      preview.cancel();
      closeDialog();
   }

   @Override
   protected void onDialogShown()
   {
      super.onDialogShown();
      dontTrustButton_.setFocus(true);
   }

   private final String directory_;
   private final JsArrayString riskyFiles_;
   private final Operation trustOperation_;
   private final Operation dontTrustOperation_;
   private ThemedButton dontTrustButton_;

   private static final ClientWorkbenchConstants constants_ = GWT.create(ClientWorkbenchConstants.class);
}
