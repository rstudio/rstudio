/*
 * SessionAutomation.hpp
 *
 * Copyright (C) 2024 by Posit Software, PBC
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

#ifndef SESSION_AUTOMATION_HPP
#define SESSION_AUTOMATION_HPP

namespace rstudio {
namespace core {
class Error;
} // end namespace core
} // end namespace rstudio

namespace rstudio {
namespace session {
namespace modules {
namespace automation {

core::Error initialize();

} // namespace source_control
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_AUTOMATION_HPP
