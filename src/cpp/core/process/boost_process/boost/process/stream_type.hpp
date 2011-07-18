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
 * \file boost/process/stream_type.hpp
 *
 * Includes the declaration of the stream_type enumeration.
 */

#ifndef BOOST_PROCESS_STREAM_TYPE_HPP
#define BOOST_PROCESS_STREAM_TYPE_HPP

#include <boost/process/config.hpp>

namespace boost {
namespace process {

/**
 * Stream type to differentiate between input and output streams.
 *
 * On POSIX systems another value unknown_stream is defined. It is passed
 * to stream behaviors for file descriptors greater than 2.
 */
enum stream_type {
    input_stream,
    output_stream
#if defined(BOOST_POSIX_API)
    , unknown_stream
#endif
};

}
}

#endif
