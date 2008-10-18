package com.google.gwt.examples.i18n;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.ui.Image;

public class ThemeDictionaryExample implements EntryPoint {

  public void onModuleLoad() {
    useThemeDictionary();
  }

  public void useThemeDictionary() {
    Dictionary theme = Dictionary.getDictionary("CurrentTheme");

    String highlightColor = theme.get("highlightColor");
    String shadowColor = theme.get("shadowColor");
    applyShadowStyle(highlightColor, shadowColor);

    String errorColor = theme.get("errorColor");
    String errorIconSrc = theme.get("errorIconSrc");
    Image errorImg = new Image(errorIconSrc);
    showError(errorColor, errorImg);
  }

  private void showError(String errorColor, Image errorImg) {
    // bogus
  }

  private void applyShadowStyle(String highlightColor, String shadowColor) {
    // bogus
  }
}
