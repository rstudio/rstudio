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
#   include <utility> 
#   include <cstddef> 
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
#include <cstring> 
#include <cstdlib> 

std::string get_argument(const std::string &word) 
{ 
    std::vector<std::string> args; 
    args.push_back("echo-quoted"); 
    args.push_back(word); 

    bp::context ctx; 
    ctx.streams[bp::stdout_id] = bpb::pipe(); 

    bp::child c = bp::create_child(get_helpers_path(), args, ctx); 
    bp::pistream is(c.get_handle(bp::stdout_id)); 

    std::string result; 
    std::getline(is, result); 
    std::string::size_type pos = result.rfind('\r'); 
    if (pos != std::string::npos) 
        result.erase(pos); 

    int status = c.wait(); 
#if defined(BOOST_POSIX_API) 
    BOOST_REQUIRE(WIFEXITED(status)); 
    BOOST_CHECK_EQUAL(WEXITSTATUS(status), EXIT_SUCCESS); 
#elif defined(BOOST_WINDOWS_API) 
    BOOST_CHECK_EQUAL(status, EXIT_SUCCESS); 
#endif 

    return result; 
} 

BOOST_AUTO_TEST_CASE(test_quoting) 
{ 
    check_helpers(); 

    BOOST_CHECK_EQUAL(get_argument("foo"), ">>>foo<<<"); 
    BOOST_CHECK_EQUAL(get_argument("foo "), ">>>foo <<<"); 
    BOOST_CHECK_EQUAL(get_argument(" foo"), ">>> foo<<<"); 
    BOOST_CHECK_EQUAL(get_argument("foo bar"), ">>>foo bar<<<"); 

    BOOST_CHECK_EQUAL(get_argument("foo\"bar"), ">>>foo\"bar<<<"); 
    BOOST_CHECK_EQUAL(get_argument("foo\"bar\""), ">>>foo\"bar\"<<<"); 
    BOOST_CHECK_EQUAL(get_argument("\"foo\"bar"), ">>>\"foo\"bar<<<"); 
    BOOST_CHECK_EQUAL(get_argument("\"foo bar\""), ">>>\"foo bar\"<<<"); 

    BOOST_CHECK_EQUAL(get_argument("*"), ">>>*<<<"); 
    BOOST_CHECK_EQUAL(get_argument("?*"), ">>>?*<<<"); 
    BOOST_CHECK_EQUAL(get_argument("[a-z]*"), ">>>[a-z]*<<<"); 
} 

#if defined(BOOST_POSIX_API) 
BOOST_AUTO_TEST_CASE(test_collection_to_posix_argv) 
{ 
    std::vector<std::string> args; 
    args.push_back("program"); 
    args.push_back("arg1"); 
    args.push_back("arg2"); 
    args.push_back("arg3"); 

    std::pair<std::size_t, char**> p = bpd::collection_to_argv(args); 
    std::size_t argc = p.first; 
    char **argv = p.second; 

    BOOST_REQUIRE_EQUAL(argc, static_cast<std::size_t>(4)); 

    BOOST_REQUIRE(std::strcmp(argv[0], "program") == 0); 
    BOOST_REQUIRE(std::strcmp(argv[1], "arg1") == 0); 
    BOOST_REQUIRE(std::strcmp(argv[2], "arg2") == 0); 
    BOOST_REQUIRE(std::strcmp(argv[3], "arg3") == 0); 
    BOOST_REQUIRE(argv[4] == 0); 

    delete[] argv[0]; 
    delete[] argv[1]; 
    delete[] argv[2]; 
    delete[] argv[3]; 
    delete[] argv; 
} 
#endif 

#if defined(BOOST_WINDOWS_API) 
BOOST_AUTO_TEST_CASE(test_collection_to_windows_cmdline) 
{ 
    std::vector<std::string> args; 
    args.push_back("program"); 
    args.push_back("arg1"); 
    args.push_back("arg2"); 
    args.push_back("arg3"); 

    boost::shared_array<char> cmdline = bpd::collection_to_windows_cmdline(args); 
    BOOST_REQUIRE(std::strcmp(cmdline.get(), "program arg1 arg2 arg3") == 0); 
} 
#endif 
