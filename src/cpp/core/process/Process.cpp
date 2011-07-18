/*
 * Process.cpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <core/process/Process.hpp>

#include <core/Thread.hpp>

// NOTE: Boost.Process is a proposed boost library for process management
//
// We have an emdedded version of the library which was download on 7/18/2011
// from this location (the file stamps embedded in the archive indicate
// that the code within was last updated on 2/11/2001):
//
//   http://www.highscore.de/boost/gsoc2010/process.zip
//
// Documentation for the library can be found at:
//
//   http://www.highscore.de/boost/gsoc2010/
//
// The following thread includes additional discussion about the
// project and its implementation (this thread was in response to the
// original posting of the code from gsoc2010):
//
//   http://thread.gmane.org/gmane.comp.lib.boost.devel/207594/
//
#include <boost/process/all.hpp>


namespace core {
namespace supervisor {




} // namespace supervisor
} // namespace core
