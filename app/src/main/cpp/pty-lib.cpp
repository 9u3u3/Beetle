#include <jni.h>
#include <cstdlib>
#include <unistd.h>
#include <fcntl.h>
#include <sys/types.h>
#include <termios.h>
#include <sys/ioctl.h>
#include <android/log.h>

#define LOG_TAG "NativePTY"
#define ALOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C"
JNIEXPORT jint JNICALL Java_com_bsbeetle_bananaspaghetti_tenebrio_pty_Tenebrio_getAlpineFork(
        JNIEnv* env,
        jobject /*this*/
        ) {

    // Create the master file descriptor
    int masterFd = open("/dev/ptmx", O_RDWR | O_NOCTTY);
    if(masterFd < 0) {
        ALOGE("Failed to open /dev/ptmx");
        return -69;
    }

    if(grantpt(masterFd) < 0) {
        ALOGE("Failed to grantpt");
        return -69;
    }

    if(unlockpt(masterFd) < 0) {
        ALOGE("Failed to unlockpt");
        return -69;
    }

    char *slave_name = ptsname(masterFd);
    if(slave_name == NULL) {
        ALOGE("Failed ptsname");
        return -69;
    }

    pid_t pid = fork();
    if(pid < 0) {
        ALOGE("Failed fork");
        return -69;
    }

    // Child process
    if(pid == 0) {

        int slave_fd = open(slave_name, O_RDWR);
        if(slave_fd < 0) {
            ALOGE("Failed slave open");
            exit(-1);
        }

        if(setsid() < 0) {
            ALOGE("Failed setsid");
            exit(-1);
        }

        if(ioctl(slave_fd, TIOCSCTTY, 0) < 0) {
            ALOGE("Failed ioctl");
            exit(-1);
        }

        dup2(slave_fd, STDIN_FILENO);
        dup2(slave_fd, STDOUT_FILENO);
        dup2(slave_fd, STDERR_FILENO);

        if(slave_fd > STDERR_FILENO) {
            close(slave_fd);
        }

        execlp("sh", "/system/bin/sh", NULL);
        ALOGE("Failed execlp");
        exit(-1);

    }

    return masterFd;
}































