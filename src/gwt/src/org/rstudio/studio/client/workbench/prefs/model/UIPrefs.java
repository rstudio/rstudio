package org.rstudio.studio.client.workbench.prefs.model;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.ui.PaneConfig;

@Singleton
public class UIPrefs extends Prefs
{
   @Inject
   public UIPrefs(Session session)
   {
      super(session.getSessionInfo().getUiPrefs());
   }

   public PrefValue<Boolean> showLineNumbers()
   {
      return bool("show_line_numbers", true);
   }

   public PrefValue<Boolean> highlightSelectedWord()
   {
      return bool("highlight_selected_word", true);
   }

   public PrefValue<Boolean> highlightSelectedLine()
   {
      return bool("highlight_selected_line", false);
   }

   public PrefValue<PaneConfig> paneConfig()
   {
      return object("pane_config");
   }

   public PrefValue<Boolean> useSpacesForTab()
   {
      return bool("use_spaces_for_tab", true);
   }

   public PrefValue<Integer> numSpacesForTab()
   {
      return integer("num_spaces_for_tab", 2);
   }

   public PrefValue<Boolean> showMargin()
   {
      return bool("show_margin", false);
   }

   public PrefValue<Integer> printMarginColumn()
   {
      return integer("print_margin_column", 80);
   }

   public PrefValue<Boolean> insertMatching()
   {
      return bool("insert_matching", true);
   }

   public PrefValue<Boolean> softWrapRFiles()
   {
      return bool("soft_wrap_r_files", false);
   }
}
