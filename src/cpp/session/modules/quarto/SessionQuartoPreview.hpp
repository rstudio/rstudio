/*
 * SessionQuartoPreview.hpp
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

#ifndef SESSION_QUARTO_PREVIEW_HPP
#define SESSION_QUARTO_PREVIEW_HPP

#include <string>

namespace rstudio {
namespace core {
   class Error;
   class FilePath;
}
}

namespace rstudio {
namespace session {
namespace modules {
namespace quarto {
namespace preview {

core::Error initialize();

// Decide whether the Quarto project config (_quarto.yml / _quarto.yaml) that
// governs previewTarget has changed relative to a previously captured config
// file and its contents. Used to determine whether a running 'quarto preview'
// server can be reused for an in-place re-render, or must be restarted so it
// re-reads project metadata. Contents (rather than timestamps) are compared so
// that an edit made within the same second as the captured startup state is
// still detected. See https://github.com/rstudio/rstudio/issues/17874.
bool projectConfigChanged(const core::FilePath& previewTarget,
                          const core::FilePath& priorConfigFile,
                          const std::string& priorConfigContents);

} // namespace preview
} // namespace quarto
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_QUARTO_PREVIEW_HPP
