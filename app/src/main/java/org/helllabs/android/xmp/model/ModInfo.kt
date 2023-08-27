package org.helllabs.android.xmp.model

// Same as libxmp/include/xmp.h -> xmp_test_info
// jni testModule() must point to this package
// Keep JvmField annotation for C compat in release mode.
class ModInfo {
    @JvmField
    var name: String = ""
    @JvmField
    var type: String = ""
}
