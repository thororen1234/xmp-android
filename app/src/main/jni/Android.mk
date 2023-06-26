# Clone libxmp into `src\main\jni` and enter the directory `libxmp` directory.
# Note: You may need to install autoconf and gcc
# sudo apt install autoconf build-essential -y
#
# Run the following command to build:
# autoconf && ./configure && make && make check && (cd test-dev; autoconf && ./configure && make)
#
# References:
# Reference: https://github.com/libxmp/libxmp/
# Reference: https://github.com/libxmp/libxmp/blob/master/INSTALL
# Reference: https://github.com/libxmp/libxmp/blob/master/jni/Android.mk
# Reference: https://github.com/libxmp/libxmp/blob/ab70ec9f3a5c9052e022a66338343f8ea87a4220/.travis.yml#L13

# TODO cmake is available now

LOCAL_PATH := $(call my-dir)/libxmp

include $(CLEAR_VARS)

include $(LOCAL_PATH)/src/Makefile
include $(LOCAL_PATH)/src/loaders/Makefile
include $(LOCAL_PATH)/src/loaders/prowizard/Makefile
include $(LOCAL_PATH)/src/depackers/Makefile

SRC_SOURCES := $(addprefix src/,$(SRC_OBJS))
LOADERS_SOURCES := $(addprefix src/loaders/,$(LOADERS_OBJS))
PROWIZ_SOURCES := $(addprefix src/loaders/prowizard/,$(PROWIZ_OBJS))
DEPACKERS_SOURCES := $(addprefix src/depackers/,$(DEPACKERS_OBJS))

LOCAL_MODULE := libxmp-jni

LOCAL_CFLAGS := -O3 -DHAVE_MKSTEMP -DHAVE_FNMATCH -DHAVE_ROUND -I$(LOCAL_PATH)/include \
		   -I$(LOCAL_PATH)/src

LOCAL_SRC_FILES := $(SRC_SOURCES:.o=.c) \
		   $(LOADERS_SOURCES:.o=.c) \
		   $(PROWIZ_SOURCES:.o=.c) \
		   $(DEPACKERS_SOURCES:.o=.c) \
		   $(LOCAL_PATH)/../xmp-jni.c \
		   $(LOCAL_PATH)/../opensl.c

# for native audio
LOCAL_LDLIBS := -lOpenSLES

include $(BUILD_SHARED_LIBRARY)
