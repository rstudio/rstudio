package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ImageResource;
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
import org.rstudio.studio.client.common.Value;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.model.WorkbenchServerOperations;

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
      if (Desktop.isDesktop())
      {
         saveWorkspace_.getListBox().setSelectedIndex(
               Desktop.getFrame().getSaveAction());
      }
      else
      {
         // TODO
      }
      add(saveWorkspace_);


      add(checkboxPref("Load .RData at startup", new Value<Boolean>(false)));


      tight(radioPreserve_ = new RadioButton("workDir", "Preserve working directory"));
      tight(radioInitial_ = new RadioButton("workDir", "Always initialize working directory to:"));
      radioInitial_.addValueChangeHandler(new ValueChangeHandler<Boolean>()
      {
         public void onValueChange(ValueChangeEvent<Boolean> evt)
         {
            if (evt.getValue() && initialDir_ == null)
               dirChooser_.click();
         }
      });
      dirChooser_ = new TextBoxWithButton(null, "Browse...", new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            fileDialogs_.chooseFolder(
                  "Initial Working Directory",
                  fsContext_,
                  initialDir_,
                  new ProgressOperationWithInput<FileSystemItem>()
                  {
                     public void execute(FileSystemItem input,
                                         ProgressIndicator indicator)
                     {
                        if (input == null)
                        {
                           if (initialDir_ == null)
                              radioPreserve_.setValue(true);
                           return;
                        }

                        initialDir_ = input;
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

      radioPreserve_.setValue(true);
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
      
      String saveAction;
      int index = saveWorkspace_.getListBox().getSelectedIndex();
      switch (index)
      {
         case 0:
            saveAction = "yes";
            break;
         case 1:
            saveAction = "no";
            break;
         case 2:
         default:
            saveAction = "ask";
            break;
      }

      Desktop.getFrame().setSaveAction(index);
      server_.setSaveAction(
            saveAction,
            new SimpleRequestCallback<org.rstudio.studio.client.server.Void>());
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
   private FileSystemItem initialDir_;
}
