/*
 * OutputCapture.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef CORE_SYSTEM_OUTPUT_CAPTURE_HPP
#define CORE_SYSTEM_OUTPUT_CAPTURE_HPP

#include <string>

#include <boost/function.hpp>

namespace core {

class Error;

namespace system {

Error captureStandardStreams(
         const boost::function<void(const std::string&)>& stdoutHandler,
         const boost::function<void(const std::string&)>& stderrHandler);

} // namespace system
} // namespace core

#endif // CORE_SYSTEM_OUTPUT_CAPTURE_HPP

