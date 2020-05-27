/* UserPrefValuesNative.hpp
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
 
#ifndef SESSION_USER_PREF_VALUES_NATIVE_HPP
#define SESSION_USER_PREF_VALUES_NATIVE_HPP

#include <core/StringUtils.hpp>

#include <session/SessionTerminalShell.hpp>

#include "UserPrefValues.hpp"

namespace rstudio {
namespace session {
namespace prefs {

struct CRANMirror
{
   std::string name;
   std::string host;
   std::string url;
   std::string country;
   std::string secondary;
};

struct BioconductorMirror
{
   std::string name;
   std::string url;
};

class UserPrefValuesNative: public UserPrefValues
{
public:
   core::string_utils::LineEnding lineEndings();
   CRANMirror getCRANMirror();
   core::Error setCRANMirror(const struct CRANMirror& mirror, bool update);
   console_process::TerminalShell::ShellType defaultTerminalShellValue();
};

}
}
}

#endif
