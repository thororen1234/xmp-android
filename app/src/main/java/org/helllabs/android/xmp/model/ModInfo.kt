package org.helllabs.android.xmp.model

// Same as libxmp/include/xmp.h -> xmp_test_info
// jni testModule() must point to this package
// Keep JvmField annotation for C compat in release mode.
data class ModInfo(var name: String = "", var type: String = "")
