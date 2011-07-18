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
 * \file boost/process/stream_id.hpp
 *
 * Includes the declaration of the stream_id type.
 */

#ifndef BOOST_PROCESS_STREAM_ID_HPP
#define BOOST_PROCESS_STREAM_ID_HPP

#include <boost/process/config.hpp>

namespace boost {
namespace process {

/**
 * Standard stream id to refer to standard streams in a cross-platform manner.
 */
enum std_stream_id { stdin_id, stdout_id, stderr_id };

#if defined(BOOST_PROCESS_DOXYGEN)
/**
 * Stream id type.
 *
 * Depending on the platform the stream id type is defined to refer to standard
 * streams only or to support more streams.
 */
typedef NativeStreamId stream_id;
#elif defined(BOOST_POSIX_API)
typedef int stream_id;
#elif defined(BOOST_WINDOWS_API)
typedef std_stream_id stream_id;
#endif

}
}

#endif
