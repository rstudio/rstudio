package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.inject.Inject;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.model.WorkbenchServerOperations;
import org.rstudio.studio.client.workbench.prefs.model.RPrefs;

/**
 * TODO: Apply new settings
 * TODO: Make sure onApply only does non-desktop settings if in web mode
 */
public class GeneralPreferencesPane extends PreferencesPane
{
   @Inject
   public GeneralPreferencesPane(PreferencesDialogResources res,
                                 WorkbenchServerOperations server,
                                 RemoteFileSystemContext fsContext,
                                 FileDialogs fileDialogs)
   {
      res_ = res;
      server_ = server;
      fsContext_ = fsContext;
      fileDialogs_ = fileDialogs;

      if (Desktop.isDesktop())
      {
         if (Desktop.getFrame().canChooseRVersion())
         {
            rVersion_ = new TextBoxWithButton(
                  "R Installation",
                  "Change...",
                  new ClickHandler()
                  {
                     public void onClick(ClickEvent event)
                     {
                        String ver = Desktop.getFrame().chooseRVersion();
                        if (!StringUtil.isNullOrEmpty(ver))
                           rVersion_.setText(ver);
                     }
                  });
            rVersion_.setWidth("100%");
            rVersion_.setText(Desktop.getFrame().getRVersion());
            add(rVersion_);
         }

         cranMirror_ = new TextBoxWithButton(
               "Default CRAN Mirror",
               "Change...",
               new ClickHandler()
               {
                  public void onClick(ClickEvent event)
                  {
                     String mirror = Desktop.getFrame().chooseCRANmirror();
                     if (!StringUtil.isNullOrEmpty(mirror))
                        cranMirror_.setText(mirror);
                  }
               });
         cranMirror_.setWidth("100%");
         cranMirror_.setText(Desktop.getFrame().getCRANMirror());

         add(cranMirror_);
      }
      
      saveWorkspace_ = new SelectWidget(
            "Save workspace on exit?",
            new String[] {
                  "Always",
                  "Never",
                  "Ask me every time"
            });
      add(saveWorkspace_);

      add(loadRData_ = new CheckBox("Load .RData at startup"));

      tight(radioPreserve_ = new RadioButton("workDir", "Preserve working directory"));
      tight(radioInitial_ = new RadioButton("workDir", "Always initialize working directory to:"));
      dirChooser_ = new TextBoxWithButton(null, "Browse...", new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            fileDialogs_.chooseFolder(
                  "Initial Working Directory",
                  fsContext_,
                  FileSystemItem.createDir(dirChooser_.getText()),
                  new ProgressOperationWithInput<FileSystemItem>()
                  {
                     public void execute(FileSystemItem input,
                                         ProgressIndicator indicator)
                     {
                        if (input == null)
                           return;

                        dirChooser_.setText(input.getPath());
                        radioInitial_.setValue(true, false);
                        indicator.onCompleted();
                     }
                  });
         }
      });
      dirChooser_.setWidth("80%");
      dirChooser_.addStyleName(res.styles().indent());

      add(radioPreserve_);
      add(radioInitial_);
      add(dirChooser_);

      saveWorkspace_.setEnabled(false);
      loadRData_.setEnabled(false);
      radioPreserve_.setEnabled(false);
      radioInitial_.setEnabled(false);
      dirChooser_.setEnabled(false);

      server_.getRPrefs(new SimpleRequestCallback<RPrefs>()
      {
         @Override
         public void onResponseReceived(RPrefs response)
         {
            saveWorkspace_.setEnabled(true);
            loadRData_.setEnabled(true);
            radioPreserve_.setEnabled(true);
            radioInitial_.setEnabled(true);
            dirChooser_.setEnabled(true);

            int saveWorkspaceIndex;
            switch (response.getSaveAction())
            {
               case 0: saveWorkspaceIndex = 1; break; // no
               case 1: saveWorkspaceIndex = 0; break; // yes
               default: saveWorkspaceIndex = 2; break; // ask
            }
            saveWorkspace_.getListBox().setSelectedIndex(saveWorkspaceIndex);

            loadRData_.setValue(response.getLoadRData());
            if (response.getPersistWorkingDirectory())
               radioPreserve_.setValue(true);
            else
               radioInitial_.setValue(true);
            dirChooser_.setText(response.getInitialWorkingDirectory());
         }
      });
   }

   @Override
   public ImageResource getIcon()
   {
      return res_.iconR();
   }

   @Override
   public void onApply()
   {
      super.onApply();

      if (saveWorkspace_.isEnabled())
      {
         int saveAction;
         switch (saveWorkspace_.getListBox().getSelectedIndex())
         {
            case 0: saveAction = 1; break; // yes
            case 1: saveAction = 0; break; // no
            case 2:
            default: saveAction = -1; break; // ask
         }
         if (Desktop.isDesktop())
         {
            Desktop.getFrame().setSaveAction(
                  saveWorkspace_.getListBox().getSelectedIndex());
         }

         server_.setRPrefs(saveAction,
                           loadRData_.getValue(),
                           radioPreserve_.getValue(),
                           dirChooser_.getText(),
                           new SimpleRequestCallback<org.rstudio.studio.client.server.Void>());
      }
   }

   @Override
   public String getName()
   {
      return "General";
   }

   private final PreferencesDialogResources res_;
   private final WorkbenchServerOperations server_;
   private final FileSystemContext fsContext_;
   private final FileDialogs fileDialogs_;
   private SelectWidget saveWorkspace_;
   private TextBoxWithButton rVersion_;
   private TextBoxWithButton cranMirror_;
   private RadioButton radioPreserve_;
   private RadioButton radioInitial_;
   private TextBoxWithButton dirChooser_;
   private CheckBox loadRData_;
}
