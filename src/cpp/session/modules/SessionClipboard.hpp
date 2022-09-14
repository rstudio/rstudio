/*
 * SessionClipboard.hpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef SESSION_CLIPBOARD_HPP
#define SESSION_CLIPBOARD_HPP

namespace rstudio {
namespace core {
class Error;
} // end namespace core
} // end namespace rstudio


namespace rstudio {
namespace session {
namespace modules {
namespace clipboard {

core::Error initialize();

} // end namespace clipboard
} // end namespace modules
} // end namespace session
} // end namespace rstudio

#endif /* SESSION_CLIPBOARD_HPP */
#ifndef SESSION_CLIPBOARD_HPP
#define SESSION_CLIPBOARD_HPP

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#endif
