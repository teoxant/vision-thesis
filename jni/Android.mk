LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

#OPENCV_CAMERA_MODULES:=off
OPENCV_INSTALL_MODULES:=on
OPENCV_LIB_TYPE:=STATIC
include C:\Development/OpenCV4Android/OpenCV-2.4.3.2-android-sdk/sdk/native/jni/OpenCV.mk

LOCAL_MODULE     := detection_based_tracker
LOCAL_LDLIBS     += -llog -ldl

LOCAL_SRC_FILES  := DetectionBasedTracker_jni.cpp
LOCAL_SRC_FILES  += /vision_lib/ImageUtils_0.7.cpp
LOCAL_SRC_FILES  += /vision_lib/detectObject.cpp
LOCAL_SRC_FILES  += /vision_lib/preprocessFace.cpp
LOCAL_SRC_FILES  += /vision_lib/recognition.cpp
LOCAL_C_INCLUDES += $(LOCAL_PATH)/vision_lib

include $(BUILD_SHARED_LIBRARY)