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

#include <boost/process/config.hpp> 

#if defined(BOOST_POSIX_API) 
#   include <sys/wait.h> 
#elif defined(BOOST_WINDOWS_API) 
#else 
#   error "Unsupported platform." 
#endif 

#define BOOST_TEST_MAIN 
#include "util/boost.hpp" 
#include "util/use_helpers.hpp" 
#include <string> 
#include <vector> 
#include <cstdlib> 

BOOST_AUTO_TEST_CASE(test_exit_failure) 
{ 
    std::vector<std::string> args; 
    args.push_back("exit-failure"); 

    bp::child c = bp::create_child(get_helpers_path(), args); 

    int status = c.wait(); 
#if defined(BOOST_POSIX_API) 
    BOOST_REQUIRE(WIFEXITED(status)); 
    BOOST_CHECK_EQUAL(WEXITSTATUS(status), EXIT_FAILURE); 
#elif defined(BOOST_WINDOWS_API) 
    BOOST_CHECK_EQUAL(status, EXIT_FAILURE); 
#endif 
} 

BOOST_AUTO_TEST_CASE(test_exit_success) 
{ 
    std::vector<std::string> args; 
    args.push_back("exit-success"); 

    bp::child c = bp::create_child(get_helpers_path(), args); 

    int status = c.wait(); 
#if defined(BOOST_POSIX_API) 
    BOOST_REQUIRE(WIFEXITED(status)); 
    BOOST_CHECK_EQUAL(WEXITSTATUS(status), EXIT_SUCCESS); 
#elif defined(BOOST_WINDOWS_API) 
    BOOST_CHECK_EQUAL(status, EXIT_SUCCESS); 
#endif 
} 
