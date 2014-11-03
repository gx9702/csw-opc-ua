include(FindPkgConfig)

set( CMAKE_VERBOSE_MAKEFILE on )

cmake_minimum_required(VERSION 2.8.4)
project(freeopcuaExampleClient)

set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -Wall")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")

set(SOURCE_FILES main.cpp)

find_package (Threads)
set(LIBS ${LIBS} ${CMAKE_THREAD_LIBS_INIT})

set(Boost_USE_STATIC_LIBS OFF)
set(Boost_USE_MULTITHREADED ON)
set(Boost_USE_STATIC_RUNTIME OFF)

find_package(Boost 1.53.0 COMPONENTS system)
if(NOT Boost_FOUND)
  message(FATAL_ERROR "Could not find boost!")
endif()

include_directories(${Boost_INCLUDE_DIRS})
set(LIBS ${LIBS} ${Boost_LIBRARIES})

find_package(LibXml2 2.9.1 REQUIRED)

set(ENV{PKG_CONFIG_PATH} /usr/local/lib/pkgconfig)
pkg_check_modules(opcuaprotocol REQUIRED libopcuaprotocol)
pkg_check_modules(opcuacore REQUIRED libopcuacore)
pkg_check_modules(opcuaclient REQUIRED libopcuaclient)
include_directories(${opcuaclient_INCLUDE_DIRS})
link_directories(${opcuaclient_LIBRARY_DIRS})
set(LIBS ${LIBS} ${opcuaprotocol_LIBRARIES} ${opcuacore_LIBRARIES} ${opcuaclient_LIBRARIES} )
message(STATUS, "XXX got LIBS = ${LIBS}")

add_executable(freeopcuaExampleClient ${SOURCE_FILES})
target_link_libraries(freeopcuaExampleClient ${LIBS} )

