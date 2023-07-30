#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_demo_opencv_MainActivity3_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}