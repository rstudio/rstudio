package org.rstudio.studio.client.workbench.views.source.editors.text.themes;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.LinkElement;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.resources.StaticDataResource;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.events.SessionInitHandler;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

import java.util.ArrayList;
import java.util.HashMap;

@Singleton
public class AceThemes
{
   @Inject
   public AceThemes(AceThemeResources res,
                    final Provider<UIPrefs> prefs,
                    EventBus eventBus)
   {
      themes_ = new ArrayList<String>();
      themesByName_ = new HashMap<String, String>();

      addTheme("Clouds", res.clouds());
      addTheme("Clouds Midnight", res.clouds_midnight());
      addTheme("Cobalt", res.cobalt());
      addTheme("Dawn", res.dawn());
      addTheme("Eclipse", res.eclipse());
      addTheme("idle Fingers", res.idle_fingers());
      addTheme("krTheme", res.kr_theme());
      addTheme("Merbivore", res.merbivore());
      addTheme("Merbivore Soft", res.merbivore_soft());
      addTheme("Mono Industrial", res.mono_industrial());
      addTheme("Monokai", res.monokai());
      addTheme("Pastel on Dark", res.pastel_on_dark());
      addTheme("TextMate", res.textmate());
      addTheme("Twilight", res.twilight());
      addTheme("Vibrant Ink", res.vibrant_ink());

      eventBus.addHandler(SessionInitEvent.TYPE, new SessionInitHandler()
      {
         public void onSessionInit(SessionInitEvent e)
         {
            prefs.get().theme().bind(new CommandWithArg<String>()
            {
               public void execute(String theme)
               {
                  applyTheme(theme);
               }
            });
         }
      });
   }

   public String[] getThemeNames()
   {
      return themes_.toArray(new String[themes_.size()]);
   }

   public String getThemeUrl(String name)
   {
      String url = themesByName_.get(name);
      return url != null ? url : themesByName_.get(defaultThemeName_);
   }

   private void addTheme(String name, StaticDataResource resource)
   {
      themes_.add(name);
      themesByName_.put(name, resource.getUrl());
   }

   private void applyTheme(String themeName)
   {
      if (currentStyleEl_ != null)
         currentStyleEl_.removeFromParent();

      currentStyleEl_ = Document.get().createLinkElement();
      currentStyleEl_.setType("text/css");
      currentStyleEl_.setRel("stylesheet");
      currentStyleEl_.setHref(getThemeUrl(themeName));
      Document.get().getBody().appendChild(
            currentStyleEl_);
   }

   public String getEffectiveThemeName(String themeName)
   {
      return themesByName_.containsKey(themeName)
             ? themeName
             : defaultThemeName_;
   }

   private final ArrayList<String> themes_;
   private final HashMap<String, String> themesByName_;
   private final String defaultThemeName_ = "TextMate";

   private LinkElement currentStyleEl_;
}
