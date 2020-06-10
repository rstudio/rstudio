/*
 * DesktopProgressActivator.hpp
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

#ifndef DESKTOP_PROGRESS_ACTIVATOR
#define DESKTOP_PROGRESS_ACTIVATOR

#include <boost/noncopyable.hpp>
#include <boost/scoped_ptr.hpp>

namespace rstudio {
namespace desktop {

class ProgressActivator : boost::noncopyable
{
public:
   ProgressActivator();
   ~ProgressActivator();

private:
   struct Impl;
   boost::scoped_ptr<Impl> pImpl_;
};

#ifndef _WIN32

struct ProgressActivator::Impl
{

};

inline ProgressActivator::ProgressActivator()
{

}

inline ProgressActivator::~ProgressActivator()
{

}

#endif


} // namespace desktop
} // namespace rstudio

#endif // DESKTOP_PROGRESS_ACTIVATOR
