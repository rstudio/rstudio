/*
 * SessionThemes.hpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

#ifndef SESSION_THEMES_HPP
#define SESSION_THEMES_HPP

#include <functional>
#include <set>
#include <string>

#include <boost/optional.hpp>

namespace rstudio {
namespace core {
   class Error;
}
}

namespace rstudio {
namespace session {
namespace modules {
namespace themes {

struct ThemeColors
{
   std::string foreground;
   std::string background;
   bool isDark;
};

/**
 * Get the foreground and background colors of the active editor theme.
 * Colors are retrieved from client state; defaults to black on white if unavailable.
 */
ThemeColors getThemeColors();

// Resolves the global (non-project) editor theme name by consulting the layers in
// precedence order: user, system, computed, default. Returns the first present,
// non-empty value, or "" if none (a present-but-empty layer value falls through).
// readLayer is injected for testability.
std::string resolveGlobalThemeName(
   const std::function<boost::optional<std::string>(const std::string& layer)>& readLayer);

// Returns the theme name to apply: effectiveName if installed, else globalName if
// installed, else defaultName.
std::string chooseAppliedThemeName(const std::string& effectiveName,
                                   const std::string& globalName,
                                   const std::set<std::string>& availableThemes,
                                   const std::string& defaultName);

core::Error initialize();

} // namespace themes
} // namespace modules
} // namespace session
} // namespace rstudio

#endif /* SESSION_THEMES_HPP */
