/*
 * RBusy.hpp
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

#ifndef R_SESSION_BUSY_HPP
#define R_SESSION_BUSY_HPP

namespace rstudio {
namespace r {
namespace session {

// is the R session busy?
// here, busy implies that the R session is executing some R code,
// even if that wasn't explicitly executed by the user
// (e.g. this captured code executed in the background by RStudio)
bool isBusy();

} // namespace session
} // namespace r
} // namespace rstudio

#endif // R_SESSION_BUSY_HPP
