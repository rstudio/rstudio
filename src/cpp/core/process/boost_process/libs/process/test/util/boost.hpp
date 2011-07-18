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

#ifndef BOOST_PROCESS_TEST_UTIL_BOOST_HPP 
#define BOOST_PROCESS_TEST_UTIL_BOOST_HPP 

#include <boost/process/all.hpp> 
#define BOOST_TEST_IGNORE_SIGCHLD 
#include <boost/test/included/unit_test.hpp> 
#include <boost/filesystem.hpp> 
#include <boost/asio.hpp> 
#include <boost/thread.hpp> 
#include <boost/iostreams/device/file_descriptor.hpp> 
#include <boost/bind.hpp> 
#include <boost/ref.hpp> 
#include <boost/lexical_cast.hpp> 
#include <boost/uuid/uuid.hpp> 
#include <boost/uuid/random_generator.hpp> 
#include <boost/uuid/uuid_io.hpp> 

namespace bp = boost::process; 
namespace bpb = boost::process::behavior; 
namespace bpd = boost::process::detail; 
namespace but = boost::unit_test; 
namespace butf = boost::unit_test::framework; 
namespace bfs = boost::filesystem; 
namespace ba = boost::asio; 
namespace bio = boost::iostreams; 
namespace bu = boost::uuids; 

#endif 
