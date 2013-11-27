package org.rstudio.core.client;

import com.google.gwt.dom.client.Element;

public class ElementIds
{
   public static void assignElementId(Element ele, String id)
   {
      ele.setId(ID_PREFIX + id);
   }
   
   public static String getElementId(String id)
   {
      return ID_PREFIX + id;
   }
   
   private final static String ID_PREFIX = "rstudio_";
   
   public final static String CONSOLE_INPUT = "console_input";
   public final static String CONSOLE_OUTPUT = "console_output";
   public final static String LOADING_SPINNER = "loading_image";
   public final static String SHELL_WIDGET = "shell_widget";
   public final static String POPUP_COMPLETIONS = "popup_completions";
   public final static String FIND_REPLACE_BAR = "find_replace_bar";
   public final static String SOURCE_TEXT_EDITOR = "source_text_editor";
   public final static String PLOT_IMAGE_FRAME = "plot_image_frame";
   public final static String HELP_FRAME = "help_frame";
}
