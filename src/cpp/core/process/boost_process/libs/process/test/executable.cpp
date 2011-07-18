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
#   include <stdlib.h> 
#elif defined(BOOST_WINDOWS_API) 
#   include <windows.h> 
#else 
#   error "Unsupported platform." 
#endif 

#define BOOST_TEST_MAIN 
#include "util/boost.hpp" 
#include "util/use_helpers.hpp" 
#include <string> 

BOOST_AUTO_TEST_CASE(test_find_default) 
{ 
    check_helpers(); 

    std::string helpersname = bfs::path(get_helpers_path()).leaf(); 

    BOOST_CHECK_THROW(bp::find_executable_in_path(helpersname), 
        bfs::filesystem_error); 
} 

BOOST_AUTO_TEST_CASE(test_find_env) 
{ 
    check_helpers(); 

    bfs::path orig = get_helpers_path(); 
    std::string helpersdir = orig.branch_path().string(); 
    std::string helpersname = orig.leaf(); 

#if defined(BOOST_POSIX_API) 
    std::string oldpath = getenv("PATH"); 
    try 
    { 
        setenv("PATH", helpersdir.c_str(), 1); 
        bfs::path found = bp::find_executable_in_path(helpersname); 
        BOOST_CHECK(bfs::equivalent(orig, found)); 
        setenv("PATH", oldpath.c_str(), 1); 
    } 
    catch (...) 
    { 
        setenv("PATH", oldpath.c_str(), 1); 
    } 
#elif defined(BOOST_WINDOWS_API) 
    char oldpath[MAX_PATH]; 
    BOOST_REQUIRE(GetEnvironmentVariableA("PATH", oldpath, MAX_PATH) != 0); 
    try 
    { 
        BOOST_REQUIRE(SetEnvironmentVariableA("PATH", 
            helpersdir.c_str()) != 0); 
        bfs::path found = bp::find_executable_in_path(helpersname); 
        BOOST_CHECK(bfs::equivalent(orig, found)); 
        BOOST_REQUIRE(SetEnvironmentVariableA("PATH", oldpath) != 0); 
    } 
    catch (...) 
    { 
        BOOST_REQUIRE(SetEnvironmentVariableA("PATH", oldpath) != 0); 
    } 
#endif 
} 

BOOST_AUTO_TEST_CASE(test_find_param) 
{ 
    check_helpers(); 

    bfs::path orig = get_helpers_path(); 
    std::string helpersdir = orig.branch_path().string(); 
    std::string helpersname = orig.leaf(); 

    bfs::path found = bp::find_executable_in_path(helpersname, helpersdir); 
    BOOST_CHECK(bfs::equivalent(orig, found)); 
} 

BOOST_AUTO_TEST_CASE(test_progname) 
{ 
    BOOST_CHECK_EQUAL(bp::executable_to_progname("foo"), "foo"); 
    BOOST_CHECK_EQUAL(bp::executable_to_progname("/foo"), "foo"); 
    BOOST_CHECK_EQUAL(bp::executable_to_progname("/foo/bar"), "bar"); 
    BOOST_CHECK_EQUAL(bp::executable_to_progname("///foo///bar"), "bar"); 
    BOOST_CHECK_EQUAL(bp::executable_to_progname("/foo/bar/baz"), "baz"); 

#if defined(BOOST_WINDOWS_API) 
    BOOST_CHECK_EQUAL(bp::executable_to_progname("f.exe"), "f"); 
    BOOST_CHECK_EQUAL(bp::executable_to_progname("f.com"), "f"); 
    BOOST_CHECK_EQUAL(bp::executable_to_progname("f.bat"), "f"); 
    BOOST_CHECK_EQUAL(bp::executable_to_progname("f.bar"), "f.bar"); 
    BOOST_CHECK_EQUAL(bp::executable_to_progname("f.bar.exe"), "f.bar"); 
    BOOST_CHECK_EQUAL(bp::executable_to_progname("f.bar.com"), "f.bar"); 
    BOOST_CHECK_EQUAL(bp::executable_to_progname("f.bar.bat"), "f.bar"); 
#endif 
} 
