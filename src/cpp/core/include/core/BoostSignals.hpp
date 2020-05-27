/*
 * BoostSignals.hpp
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

#ifdef RSTUDIO_BOOST_SIGNALS_VERSION_USED
# if RSTUDIO_BOOST_SIGNALS_VERSION_USED != RSTUDIO_BOOST_SIGNALS_VERSION
#  error "Mismatch in Boost.Signals versions used in this translation unit"
# endif
#endif

#ifndef CORE_RSTUDIO_BOOST_SIGNALS_HPP
#define CORE_RSTUDIO_BOOST_SIGNALS_HPP

#define RSTUDIO_BOOST_SIGNALS_VERSION_USED RSTUDIO_BOOST_SIGNALS_VERSION

#if RSTUDIO_BOOST_SIGNALS_VERSION == 1

# include <boost/signals.hpp>
# define RSTUDIO_BOOST_SIGNAL boost::signal
# define RSTUDIO_BOOST_CONNECTION boost::signals::connection
# define RSTUDIO_BOOST_SCOPED_CONNECTION boost::signals::scoped_connection
# define RSTUDIO_BOOST_LAST_VALUE boost::last_value

#elif RSTUDIO_BOOST_SIGNALS_VERSION == 2

# include <boost/signals2.hpp>
# define RSTUDIO_BOOST_SIGNAL boost::signals2::signal
# define RSTUDIO_BOOST_CONNECTION boost::signals2::connection
# define RSTUDIO_BOOST_SCOPED_CONNECTION boost::signals2::scoped_connection
# define RSTUDIO_BOOST_LAST_VALUE boost::signals2::last_value

#else
# error "Unrecognized RSTUDIO_BOOST_SIGNALS_VERSION"
#endif

#endif // CORE_RSTUDIO_BOOST_SIGNALS_HPP

