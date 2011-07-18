//
// Boost.Process
// ~~~~~~~~~~~~~
//
// Copyright (c) 2006, 2007 Julio M. Merino Vidal
// Copyright (c) 2008 Ilya Sokolov, Boris Schaeling
// Copyright (c) 2009 Boris Schaeling
// Copyright (c) 2010 Felipe Tanus, Boris Schaeling
//
// Distributed under the Boost Software License, Version 1.0. (See accompanying
// file LICENSE_1_0.txt or copy at http://www.boost.org/LICENSE_1_0.txt)
//

/**
 * \file boost/process/pipe.hpp
 *
 * Includes the declaration of the pipe class.
 */

#ifndef BOOST_PROCESS_PIPE_HPP
#define BOOST_PROCESS_PIPE_HPP

#include <boost/process/config.hpp>
#include <boost/asio.hpp>

namespace boost {
namespace process {

#if defined(BOOST_PROCESS_DOXYGEN)
/**
 * The pipe class is a type definition for stream-based classes defined by
 * Boost.Asio.
 *
 * The type definition is provided for convenience. You can also use Boost.Asio
 * classes directly for asynchronous I/O operations.
 */
typedef BoostAsioPipe pipe;
#elif defined(BOOST_POSIX_API)
typedef boost::asio::posix::stream_descriptor pipe;
#elif defined(BOOST_WINDOWS_API)
typedef boost::asio::windows::stream_handle pipe;
#else
#   error "Unsupported platform."
#endif

}
}

#endif
