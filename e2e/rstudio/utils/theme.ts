/**
 * Shared helpers for specs that assert on the active editor theme.
 *
 * Theming is asserted the same way across several specs -- apply a known theme,
 * wait for its stylesheet to be the live one, then measure a background color --
 * so the theme names, the stylesheet selector and the luminance math live here
 * rather than being copied per spec.
 */
import { expect, type Page } from '@playwright/test';

/**
 * 'Cobalt' ships with RStudio and is dark; 'Textmate (default)' is the light
 * default. A theme's stylesheet href contains a lowercased, underscored slug of
 * its name rather than the name itself -- 'Textmate (default)' is served as
 * textmate.rstheme, 'Crimson Editor' as crimson_editor.rstheme -- so the marker
 * below is written out per theme rather than derived, and
 * {@link expectThemeStylesheet} matches on it as a substring.
 */
export const DARK_THEME = 'Cobalt';
export const LIGHT_THEME = 'Textmate (default)';

/** Marker contained in the stylesheet href of each theme above. */
export const DARK_THEME_HREF = 'cobalt';
export const LIGHT_THEME_HREF = 'textmate';

/**
 * Id on the <link> element AceThemes.java injects to apply the theme CSS -- in
 * top-level windows, in themed iframes (via RStudioThemedFrame), and in
 * satellites that opt in to theming with supportsThemes().
 */
export const ACE_THEME_LINK = '#rstudio-acethemes-linkelement';

/**
 * Id RStudioThemes.initializeThemes() assigns to the themed container; it also
 * carries rstudio-themes-dark for dark themes.
 */
export const THEME_CONTAINER = '#rstudio_container';

/**
 * Applying a theme takes a server round trip before the client swaps the
 * stylesheet, so these waits need more headroom than the 5s expect default --
 * especially just after a session restart.
 */
export const THEME_POLL = { timeout: 15000, intervals: [200, 500, 1000] };

/** Relative luminance (0 = black, 255 = white) of a CSS rgb()/rgba() color. */
export function luminance(cssColor: string): number {
  const match = cssColor.match(/rgba?\((\d+),\s*(\d+),\s*(\d+)(?:,\s*([\d.]+))?\)/);
  if (!match)
    throw new Error(`unexpected color format: ${cssColor}`);

  // a fully transparent background means nothing was actually painted; return
  // NaN so both the light and the dark comparison fail rather than letting
  // rgba(0, 0, 0, 0) read as black
  if (match[4] !== undefined && Number(match[4]) === 0)
    return Number.NaN;

  const [r, g, b] = [Number(match[1]), Number(match[2]), Number(match[3])];
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}

/** Href of the theme stylesheet currently applied to `page`'s document. */
export async function getThemeStylesheetHref(page: Page): Promise<string> {
  return page.evaluate(
    (id) => document.querySelector(id)?.getAttribute('href') ?? '',
    ACE_THEME_LINK,
  );
}

/** Wait until `page`'s applied theme stylesheet is the one named by `href`. */
export async function expectThemeStylesheet(page: Page, href: string): Promise<void> {
  await expect.poll(() => getThemeStylesheetHref(page), THEME_POLL).toContain(href);
}
