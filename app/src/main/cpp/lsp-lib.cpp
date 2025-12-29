//
// Created by harsh on 10/19/25.
//

#include <android/log.h>
#include <cstdlib>
#include <errno.h>
#include <fcntl.h>
#include <jni.h>
#include <string>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <termios.h>
#include <unistd.h>

#define LOG_TAG "NativeLSP"
#define ALOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jint JNICALL
Java_com_bsbeetle_bananaspaghetti_utils_languageHandling_LspServer_startNativeLsp(
    JNIEnv *env, jobject /*thizz nutz*/, jstring prefix, jstring jNativeDirPath,
    jstring jProotTmpDir, jstring jProotLoader, jstring jProotLoader32,
    jstring jProot, jstring jPublic, jstring jUbuntuTmp, jstring jUbuntu,
    jstring jScriptName) {

  ALOGE("Called getCppLsp");

  const char *PREFIX = env->GetStringUTFChars(prefix, nullptr);
  const char *NATIVE_DIR = env->GetStringUTFChars(jNativeDirPath, nullptr);
  const char *PROOT_TMP_DIR = env->GetStringUTFChars(jProotTmpDir, nullptr);
  const char *PROOT_LOADER = env->GetStringUTFChars(jProotLoader, nullptr);
  const char *PROOT_LOADER32 = env->GetStringUTFChars(jProotLoader32, nullptr);
  const char *PROOT = env->GetStringUTFChars(jProot, nullptr);
  const char *PUBLIC = env->GetStringUTFChars(jPublic, nullptr);
  const char *UBUNTU_TMP = env->GetStringUTFChars(jUbuntuTmp, nullptr);
  const char *UBUNTU = env->GetStringUTFChars(jUbuntu, nullptr);
  const char *SCRIPT_NAME = env->GetStringUTFChars(jScriptName, nullptr);

  setenv("PREFIX", PREFIX, 1);
  setenv("NATIVE_DIR", NATIVE_DIR, 1);
  setenv("LD_LIBRARY_PATH", PREFIX, 1);
  setenv("PROOT_TMP_DIR", PROOT_TMP_DIR, 1);
  setenv("PROOT_LOADER", PROOT_LOADER, 1);
  setenv("PROOT_LOADER32", PROOT_LOADER32, 1);
  setenv("PROOT", PROOT, 1);

  // Construct command dynamically using PREFIX and SCRIPT_NAME
  std::string cmd = std::string(". ") + PREFIX + "/" + SCRIPT_NAME;

  char *argv[] = {const_cast<char *>(PROOT),

                  "--kill-on-exit",

                  "-b",
                  "/apex",
                  "-b",
                  "/odm",
                  "-b",
                  "/product",
                  "-b",
                  "/system",
                  "-b",
                  "/system_ext",
                  "-b",
                  "/vendor",
                  "-b",
                  "/linkerconfig/ld.config.txt",
                  "-b",
                  "/linkerconfig/com.android.art/ld.config.txt",

                  "-b",
                  "/sdcard",
                  "-b",
                  "/storage",
                  "-b",
                  "/dev",
                  "-b",
                  "/data",
                  "-b",
                  "/dev/urandom:/dev/random",
                  "-b",
                  "/proc",
                  "-b",
                  "/sys",

                  "-b",
                  const_cast<char *>(PREFIX),
                  "-b",
                  const_cast<char *>(PUBLIC),
                  "-b",
                  const_cast<char *>(UBUNTU_TMP),

                  "-b",
                  "/proc/self/fd:/dev/fd",
                  "-b",
                  "/proc/self/fd/0:/dev/stdin",
                  "-b",
                  "/proc/self/fd/1:/dev/stdout",
                  "-b",
                  "/proc/self/fd/2:/dev/stderr",

                  "-r",
                  const_cast<char *>(UBUNTU),

                  "-0",
                  "--link2symlink",
                  "--sysvipc",
                  "-L",

                  "/bin/sh",
                  "-c",
                  const_cast<char *>(cmd.c_str()),
                  NULL};

  int masterFd = open("/dev/ptmx", O_RDWR | O_NOCTTY);
  if (masterFd < 0) {
    ALOGE("Failed to open /dev/ptmx");
    return -69;
  }

  if (grantpt(masterFd) < 0) {
    ALOGE("Failed to grantpt");
    close(masterFd);
    return -69;
  }

  if (unlockpt(masterFd) < 0) {
    ALOGE("Failed to unlockpt");
    close(masterFd);
    return -69;
  }

  char *slave_name = ptsname(masterFd);
  if (slave_name == NULL) {
    ALOGE("Failed ptsname");
    close(masterFd);
    return -69;
  }

  pid_t pid = fork();
  if (pid < 0) {
    ALOGE("Failed fork");
    close(masterFd);
    return -69;
  }

  if (pid == 0) {

    int slave_fd = open(slave_name, O_RDWR);
    if (slave_fd < 0) {
      ALOGE("Failed slave open");
      exit(-1);
    }

    struct termios tio;
    if (tcgetattr(slave_fd, &tio) == -1) {
      ALOGE("tcgetattr failed: %d", errno);
    } else {
      // Prefer cfmakeraw if available:
#ifdef __GLIBC__
      cfmakeraw(&tio);
#else
      // manual raw flags fallback
      tio.c_iflag &=
          ~(IGNBRK | BRKINT | PARMRK | ISTRIP | INLCR | IGNCR | ICRNL | IXON);
      tio.c_oflag &= ~OPOST;
      tio.c_lflag &= ~(ECHO | ECHONL | ICANON | ISIG | IEXTEN);
      tio.c_cflag &= ~(CSIZE | PARENB);
      tio.c_cflag |= CS8;
#endif

      // disable ECHO explicitly
      tio.c_lflag &= ~ECHO;
      // set attributes immediately
      if (tcsetattr(slave_fd, TCSANOW, &tio) == -1) {
        ALOGE("tcsetattr failed: %d", errno);
      } else {
        ALOGE("Slave pty set to raw / no-echo mode");
      }
    }

    if (setsid() < 0) {
      ALOGE("Failed setsid");
      exit(-1);
    }

    if (ioctl(slave_fd, TIOCSCTTY, 0) < 0) {
      ALOGE("Failed ioctl");
      exit(-1);
    }

    dup2(slave_fd, STDIN_FILENO);
    dup2(slave_fd, STDOUT_FILENO);
    dup2(slave_fd, STDERR_FILENO);

    if (slave_fd > STDERR_FILENO) {
      close(slave_fd);
    }

    execv(PROOT, argv);
    ALOGE("Failed execv");
    exit(-1);
  }

  return masterFd;
}