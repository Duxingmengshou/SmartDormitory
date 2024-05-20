cmake_minimum_required(VERSION 3.5)

project(SmartDormitoryQt VERSION 0.1 LANGUAGES CXX)

set(CMAKE_AUTOUIC ON)
set(CMAKE_AUTOMOC ON)
set(CMAKE_AUTORCC ON)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(QT NAMES Qt6 Qt5 REQUIRED COMPONENTS Widgets)
find_package(Qt${QT_VERSION_MAJOR} REQUIRED COMPONENTS Widgets)


set(OpenCV_DIR H:/Programming/C++/OpenCV/OpenCV490/opencv/build)
find_package(OpenCV REQUIRED)
find_package(Qt6 REQUIRED COMPONENTS Multimedia)
find_package(Qt6 REQUIRED COMPONENTS MultimediaWidgets)
find_package(Qt6 REQUIRED COMPONENTS Network)




include_directories(${OpenCV_INCLUDE_DIRS})
link_directories(${OpenCV_LIBRARY_DIRS})


set(PROJECT_SOURCES
        main.cpp
        SmartDormitoryQt.cpp
        SmartDormitoryQt.h
        SmartDormitoryQt.ui
        opencv_camera.hpp
        img.qrc
        ico.rc
)

if(${QT_VERSION_MAJOR} GREATER_EQUAL 6)
    qt_add_executable(SmartDormitoryQt
        MANUAL_FINALIZATION
        ${PROJECT_SOURCES}
    )
# Define target properties for Android with Qt 6 as:
#    set_property(TARGET SmartDormitoryQt APPEND PROPERTY QT_ANDROID_PACKAGE_SOURCE_DIR
#                 ${CMAKE_CURRENT_SOURCE_DIR}/android)
# For more information, see https://doc.qt.io/qt-6/qt-add-executable.html#target-creation
else()
    if(ANDROID)
        add_library(SmartDormitoryQt SHARED
            ${PROJECT_SOURCES}
        )
# Define properties for Android with Qt 5 after find_package() calls as:
#    set(ANDROID_PACKAGE_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/android")
    else()
        add_executable(SmartDormitoryQt
            ${PROJECT_SOURCES}
        )
    endif()
endif()

target_link_libraries(SmartDormitoryQt PRIVATE Qt${QT_VERSION_MAJOR}::Widgets)
target_link_libraries(SmartDormitoryQt PRIVATE ${OpenCV_LIBS})
target_link_libraries(SmartDormitoryQt PRIVATE Qt6::Multimedia)
target_link_libraries(SmartDormitoryQt PRIVATE Qt6::MultimediaWidgets)
target_link_libraries(SmartDormitoryQt PRIVATE Qt6::Network)
set_target_properties(SmartDormitoryQt PROPERTIES
    MACOSX_BUNDLE_GUI_IDENTIFIER my.example.com
    MACOSX_BUNDLE_BUNDLE_VERSION ${PROJECT_VERSION}
    MACOSX_BUNDLE_SHORT_VERSION_STRING ${PROJECT_VERSION_MAJOR}.${PROJECT_VERSION_MINOR}
    MACOSX_BUNDLE TRUE
    WIN32_EXECUTABLE TRUE
)

install(TARGETS SmartDormitoryQt
    BUNDLE DESTINATION .
    LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR})

if(QT_VERSION_MAJOR EQUAL 6)
    qt_finalize_executable(SmartDormitoryQt)
endif()
