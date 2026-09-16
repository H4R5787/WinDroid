cmake_minimum_required(VERSION 3.22.1)

project("windroid_native")

add_library(
    windroid_native
    SHARED
    windroid_native.cpp
)

if(ANDROID)
    find_library(log-lib log)
    find_library(android-lib android)
    target_link_libraries(
        windroid_native
        ${log-lib}
        ${android-lib}
        dl
    )
else()
    target_link_libraries(
        windroid_native
        dl
        util
    )
endif()
