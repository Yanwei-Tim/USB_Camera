LOCAL_PATH := $(call my-dir)  
include $(CLEAR_VARS)  

#���Ͱ�����OpenCV4Android����:��͸��Android��JNI��OpenCV�����ַ�ʽ(��OpenCVManager��������)
LIB_TYPE = STATIC
ifeq ($(LIB_TYPE), STATIC) 
#STATIC����ʱ�����Բ�����Я����������
#��JNI���������cvCaptureFromCAMȥ��AndroidNativeCamera������ҪOPENCV_CAMERA_MODULES:=on
#������libnative_camera_r4.4.0.so
	OPENCV_LIB_TYPE:=STATIC
	OPENCV_INSTALL_MODULES:=on
	OPENCV_CAMERA_MODULES:=off
else 
#SHARED����ʱ������Я���������⣬�������б���
	OPENCV_LIB_TYPE:=SHARED
	OPENCV_INSTALL_MODULES:=on
	OPENCV_CAMERA_MODULES:=on
endif 


#ԭʼopenCV4Android SDK
#include ../../openCV_2410_sdk/native/jni/OpenCV.mk

#���V4L2�����±�����openCV4Android SDK
include ../../openCV_2410_sdk_v4l2/native/jni/OpenCV.mk

$(warning "****************************************")
$(warning $(LOCAL_C_INCLUDES))

LOCAL_LDLIBS += -llog 
LOCAL_SHARED_LIBRARIES += \
						libandroid_runtime\
						liblog \
						libcutils \
						libnativehelper \
						libcore/include	

LOCAL_C_INCLUDES += $(LOCAL_PATH)
						
LOCAL_SRC_FILES  := ImageProc.cpp  
LOCAL_MODULE     := image_proc  
include $(BUILD_SHARED_LIBRARY) 