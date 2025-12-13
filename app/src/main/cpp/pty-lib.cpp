#include <android/log.h>
#include <cstdlib>
#include <fcntl.h>
#include <jni.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <termios.h>
#include <unistd.h>

#define LOG_TAG "NativePTY"
#define ALOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jint JNICALL
Java_com_bsbeetle_bananaspaghetti_tenebrio_pty_Tenebrio_startAlpine(
    JNIEnv *env, jobject /*this*/, jstring prefix, jstring jNativeDirPath,
    jstring jProotTmpDir, jstring jProotLoader, jstring jProotLoader32,
    jstring jProot, jstring jPublic, jstring jAlpineTmp, jstring jAlpine,
    jstring jAlpineInit, jint rows, jint cols) {

  // Initializing paths
  const char *PREFIX = env->GetStringUTFChars(prefix, nullptr);
  const char *NATIVE_DIR = env->GetStringUTFChars(jNativeDirPath, nullptr);
  const char *PROOT_TMP_DIR = env->GetStringUTFChars(jProotTmpDir, nullptr);
  const char *PROOT_LOADER = env->GetStringUTFChars(jProotLoader, nullptr);
  const char *PROOT_LOADER32 = env->GetStringUTFChars(jProotLoader32, nullptr);
  const char *PROOT = env->GetStringUTFChars(jProot, nullptr);
  const char *PUBLIC = env->GetStringUTFChars(jPublic, nullptr);
  const char *ALPINE_TMP = env->GetStringUTFChars(jAlpineTmp, nullptr);
  const char *ALPINE = env->GetStringUTFChars(jAlpine, nullptr);
  // const char *INIT_ALPINE = env->GetStringUTFChars(jAlpineInit, nullptr);

  // Setting the environment variables
  setenv("PREFIX", PREFIX, 1);
  setenv("NATIVE_DIR", NATIVE_DIR, 1);
  setenv("LD_LIBRARY_PATH", PREFIX, 1);
  setenv("PROOT_TMP_DIR", PROOT_TMP_DIR, 1);
  setenv("PROOT_LOADER", PROOT_LOADER, 1);
  setenv("PROOT_LOADER32", PROOT_LOADER32, 1);
  setenv("PROOT", PROOT, 1);

  char command[] =
      ". /data/user/0/com.bsbeetle.bananaspaghetti/files/init-alpine.sh";

  // Building proot command
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
                  const_cast<char *>(ALPINE_TMP),

                  "-b",
                  "/proc/self/fd:/dev/fd",
                  "-b",
                  "/proc/self/fd/0:/dev/stdin",
                  "-b",
                  "/proc/self/fd/1:/dev/stdout",
                  "-b",
                  "/proc/self/fd/2:/dev/stderr",

                  "-r",
                  const_cast<char *>(ALPINE),

                  "-0",
                  "--link2symlink",
                  "--sysvipc",
                  "-L",
                  "-w",
                  "/home",

                  "/bin/sh",
                  "-c",
                  command,
                  NULL};

  // Create the master file descriptor
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

  // CRITICAL: Set initial window size before forking
  // This ensures child process starts with correct terminal dimensions
  struct winsize ws;
  ws.ws_row = rows; // Start with reasonable default
  ws.ws_col = cols; // Will be updated by fitTerminal() later
  ws.ws_xpixel = 0;
  ws.ws_ypixel = 0;

  if (ioctl(masterFd, TIOCSWINSZ, &ws) < 0) {
    ALOGE("Failed to set initial window size");
    // Don't fail here, continue anyway
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

  // Child process
  if (pid == 0) {

    int slave_fd = open(slave_name, O_RDWR);
    if (slave_fd < 0) {
      ALOGE("Failed slave open");
      exit(-1);
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

extern "C" JNIEXPORT jint JNICALL
Java_com_bsbeetle_bananaspaghetti_tenebrio_pty_Tenebrio_resizePty(
    JNIEnv *env, jobject /*this*/, jint masterFd, jint rows, jint cols) {
  struct winsize ws;
  ws.ws_row = rows;
  ws.ws_col = cols;
  ws.ws_xpixel = 0;
  ws.ws_ypixel = 0;

  if (ioctl(masterFd, TIOCSWINSZ, &ws) < 0) {
    ALOGE("Failed to set window size");
    return -1;
  }

  return 0;
}