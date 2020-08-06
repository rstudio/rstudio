package org.rstudio.studio.client.workbench.prefs.views.python;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.studio.client.common.icons.code.CodeIcons;
import org.rstudio.studio.client.workbench.prefs.views.PythonInterpreter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class PythonInterpreterListEntryUi extends Composite
{
   public PythonInterpreterListEntryUi(PythonInterpreter interpreter)
   {
      interpreter_ = interpreter;
      
      uiIcon_    = createUiIcon();
      uiVersion_ = createUiVersion();
      uiPath_    = createUiPath();
      
      initWidget(uiBinder.createAndBindUi(this));
   }
   
   public final PythonInterpreter getInterpreter()
   {
      return interpreter_;
   }
   
   private final Image createUiIcon()
   {
      ImageResource resource;
      String altText;
      
      String type = interpreter_.getType();
      if (StringUtil.equals(type, "conda"))
      {
         resource = new ImageResource2x(CodeIcons.INSTANCE.conda2x());
         altText = "Conda Environment";
      }
      else if (StringUtil.equals(type, "virtualenv"))
      {
         resource = new ImageResource2x(CodeIcons.INSTANCE.virtualenv2x());
         altText = "Virtual Environment";
      }
      else
      {
         resource = new ImageResource2x(CodeIcons.INSTANCE.python2x());
         altText = "Python Interpreter";
      }
      
      Image image = new Image(resource);
      image.setAltText(altText);
      return image;
   }
   
   private final Label createUiVersion()
   {
      return new Label("Python " + interpreter_.getVersion());
   }
   
   private final Label createUiPath()
   {
      return new Label("[" + interpreter_.getPath() + "]");
   }
   
   public final Image getIcon()
   {
      return uiIcon_;
   }
   
   public final Label getVersion()
   {
      return uiVersion_;
   }
   
   public final Label getPath()
   {
      return uiPath_;
   }

   private static PythonInterpreterListEntryUiUiBinder uiBinder =
         GWT.create(PythonInterpreterListEntryUiUiBinder.class);

   interface PythonInterpreterListEntryUiUiBinder
         extends UiBinder<Widget, PythonInterpreterListEntryUi>
   {
   }
   
   private final PythonInterpreter interpreter_;

   @UiField(provided = true) final Image uiIcon_;
   @UiField(provided = true) final Label uiVersion_;
   @UiField(provided = true) final Label uiPath_;
}
