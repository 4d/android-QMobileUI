#include "exception_helper.h"

#include <string>

static std::string LogSignalInfo(siginfo_t *info) {
    std::string signalDescription;
    switch (info->si_signo) {
        case SIGILL:
            signalDescription += "signal SIGILL caught: ";
            switch (info->si_code) {
                case ILL_ILLOPC:
                    signalDescription += "illegal opcode";
                    break;
                case ILL_ILLOPN:
                    signalDescription += "illegal operand";
                    break;
                case ILL_ILLADR:
                    signalDescription += "illegal addressing mode";
                    break;
                case ILL_ILLTRP:
                    signalDescription += "illegal trap";
                    break;
                case ILL_PRVOPC:
                    signalDescription += "privileged opcode";
                    break;
                case ILL_PRVREG:
                    signalDescription += "privileged register";
                    break;
                case ILL_COPROC:
                    signalDescription += "coprocessor error";
                    break;
                case ILL_BADSTK:
                    signalDescription += "internal stack error";
                    break;
                default:
                    signalDescription += "code = %d" + std::to_string(info->si_code);
                    break;
            }
            break;
        case SIGFPE:
            signalDescription += "signal SIGFPE caught: ";
            switch (info->si_code) {
                case FPE_INTDIV:
                    signalDescription += "integer divide by zero";
                    break;
                case FPE_INTOVF:
                    signalDescription += "integer overflow";
                    break;
                case FPE_FLTDIV:
                    signalDescription += "floating-point divide by zero";
                    break;
                case FPE_FLTOVF:
                    signalDescription += "floating-point overflow";
                    break;
                case FPE_FLTUND:
                    signalDescription += "floating-point underflow";
                    break;
                case FPE_FLTRES:
                    signalDescription += "floating-point inexact result";
                    break;
                case FPE_FLTINV:
                    signalDescription += "invalid floating-point operation";
                    break;
                case FPE_FLTSUB:
                    signalDescription += "subscript out of range";
                    break;
                default:
                    signalDescription += "code = %d" + std::to_string(info->si_code);
                    break;
            }
            break;
        case SIGSEGV:
            signalDescription += "signal SIGSEGV caught: ";
            switch (info->si_code) {
                case SEGV_MAPERR:
                    signalDescription += "address not mapped to object";
                    break;
                case SEGV_ACCERR:
                    signalDescription += "invalid permissions for mapped object";
                    break;
                default:
                    signalDescription += "code = " + std::to_string(info->si_code);
                    break;
            }
            break;
        case SIGBUS:
            signalDescription += "signal SIGBUS caught: ";
            switch (info->si_code) {
                case BUS_ADRALN:
                    signalDescription += "invalid address alignment";
                    break;
                case BUS_ADRERR:
                    signalDescription += "nonexistent physical address";
                    break;
                case BUS_OBJERR:
                    signalDescription += "object-specific hardware error";
                    break;
                default:
                    signalDescription += "code = " + std::to_string(info->si_code);
                    break;
            }
            break;
        case SIGABRT:
            signalDescription += "signal SIGABRT caught";
            break;
        case SIGPIPE:
            signalDescription += "signal SIGPIPE caught";
            break;
        default:
            signalDescription += "signo %d caught", signalDescription += std::to_string(info->si_signo);
            signalDescription += "code = " + std::to_string(info->si_code);
    }
    signalDescription += "\nerrno = " + std::to_string(info->si_errno);
    signalDescription += "\n";
    return signalDescription;
}

const char *createCrashMessage(int signo, siginfo *siginfo) {
    void *current_exception = __cxxabiv1::__cxa_current_primary_exception();
    std::type_info *current_exception_type_info = __cxxabiv1::__cxa_current_exception_type();

    size_t buffer_size = 1024;
    char *abort_message = static_cast<char *>(malloc(buffer_size));

    if (current_exception) {
        try {
            // Check if we can get the message
            if (current_exception_type_info) {
                const char *exception_name = current_exception_type_info->name();

                // Try demangling exception name
                int status = -1;
                char demangled_name[buffer_size];
                __cxxabiv1::__cxa_demangle(exception_name, demangled_name, &buffer_size, &status);

                // Check demangle status
                if (status) {
                    // Couldn't demangle, go with exception_name
                    sprintf(abort_message, "Terminating with uncaught exception of type %s", exception_name);
                } else {
                    if (strstr(demangled_name, "nullptr") || strstr(demangled_name, "NULL")) {
                        // Could demangle, go with demangled_name and state that it was null
                        sprintf(abort_message, "Terminating with uncaught exception of type %s", demangled_name);
                    } else {
                        // Could demangle, go with demangled_name and exception.what() if exists
                        try {
                            __cxxabiv1::__cxa_rethrow_primary_exception(current_exception);
                        } catch (std::exception &e) {
                            // Include message from what() in the abort message
                            sprintf(abort_message, "Terminating with uncaught exception of type %s : %s",
                                    demangled_name, e.what());
                        } catch (...) {
                            // Just report the exception type since it is not an std::exception
                            sprintf(abort_message, "Terminating with uncaught exception of type %s", demangled_name);
                        }
                    }
                }

                return abort_message;
            } else {
                // Not a cpp exception, assume a custom crash and act like C
            }
        }
        catch (std::bad_cast &bc) {
            // Not a cpp exception, assume a custom crash and act like C
        }
    }

    // Assume C crash and print signal no and code
    sprintf(abort_message, "Terminating with a C crash %d : %d", signo, siginfo->si_code);
    return abort_message;
}

void nativeCrashSignalHandler(int signo, siginfo *siginfo, void *ctxvoid) {
    // Restoring an old handler to make built-in Android crash mechanism work.
    sigaction(signo, &crashInContext->old_handlers[signo], nullptr);
    auto *ctx = (sigcontext *) ctxvoid;
    // Get more contexts.
    const ucontext_t *signal_ucontext = (ucontext_t *) ctxvoid;
    assert(signal_ucontext);
    const sigcontext *signal_mcontext = (sigcontext *) &(signal_ucontext->uc_mcontext);
    assert(signal_mcontext);

    // Log crash message
    std::string log_msg = LogSignalInfo(siginfo) + createCrashMessage(signo, siginfo);
    __android_log_print(ANDROID_LOG_WARN, "exception_helper::", "App last msg: %s", log_msg.c_str());
    ofs.open(log_path, std::ios::out);
    ofs << log_msg;
    ofs.close();

    // In some cases we need to re-send a signal to run standard bionic handler.
    if (siginfo->si_code <= 0 || signo == SIGABRT) {
        if (syscall(__NR_tgkill, getpid(), gettid(), signo) < 0) {
            _exit(1);
        }
    }
}

bool registerSignalHandler(CrashSignalHandler handler, struct sigaction old_handlers[NSIG]) {
    struct sigaction sigactionstruct;
    memset(&sigactionstruct, 0, sizeof(sigactionstruct));
    sigactionstruct.sa_flags = SA_SIGINFO;
    sigactionstruct.sa_sigaction = handler;

    // Register new handlers for all signals
    for (int signo: SIGNALS_TO_CATCH) {
        if (sigaction(signo, &sigactionstruct, &old_handlers[signo])) {
            return false;
        }
    }

    return true;
}

void unregisterSignalHandler(struct sigaction old_handlers[NSIG]) {
    // Recover old handler for all signals
    for (int signo = 0; signo < NSIG; ++signo) {
        const struct sigaction *old_handler = &old_handlers[signo];

        if (!old_handler->sa_handler) {
            continue;
        }

        sigaction(signo, old_handler, nullptr);
    }
}

void registerNativeCrashHandler() {
    // Check if already initialized
    if (crashInContext) {
        __android_log_print(ANDROID_LOG_INFO, "NDK Playground", "%s", "Native crash handler is already initialized.");
        return;
    }

    // Initialize singleton crash handler context
    crashInContext = static_cast<CrashInContext *>(malloc(sizeof(CrashInContext)));
    memset(crashInContext, 0, sizeof(CrashInContext));

    // Trying to register signal handler.
    if (!registerSignalHandler(&nativeCrashSignalHandler, crashInContext->old_handlers)) {
        unregisterNativeCrashHandler();
        __android_log_print(ANDROID_LOG_ERROR, "NDK Playground", "%s", "Native crash handler initialization failed.");
        return;
    }

    __android_log_print(ANDROID_LOG_INFO, "NDK Playground", "%s", "Native crash handler successfully initialized.");
}

bool unregisterNativeCrashHandler() {
    // Check if already cleared
    if (!crashInContext) return false;

    // Unregister signal handlers
    unregisterSignalHandler(crashInContext->old_handlers);

    // Free singleton crash handler context
    free(crashInContext);
    crashInContext = nullptr;

    __android_log_print(ANDROID_LOG_INFO, "NDK Playground", "%s", "Native crash handler successfully deinitialized.");

    return true;
}

extern "C" JNIEXPORT void JNICALL
Java_com_qmobile_qmobileui_crash_SignalHandler_nativeRegisterSignalHandler(JNIEnv *env,
                                                                           jobject /* this */) {
    registerNativeCrashHandler();
}

extern "C" JNIEXPORT void JNICALL
Java_com_qmobile_qmobileui_crash_SignalHandler_nativeUnregisterSignalHandler(JNIEnv *env,
                                                                             jobject /* this */) {
    unregisterNativeCrashHandler();
}

extern "C" JNIEXPORT void JNICALL
Java_com_qmobile_qmobileui_crash_SignalHandler_nativeCreateLogFile(JNIEnv *env,
                                                                   jobject /* this */,
                                                                   jstring cache_path,
                                                                   jstring log_file_name) {
    assert(cache_path != nullptr && "isLogsDirPathProvided");
    assert(log_file_name != nullptr && "isLogFileNameProvided");
    crash_absolute_path = env->GetStringUTFChars(cache_path, 0);
    log_path = crash_absolute_path + "/", log_path += env->GetStringUTFChars(log_file_name, 0);
    __android_log_print(ANDROID_LOG_WARN, "exception_helper::", "Init log.txt at path: %s", log_path.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_qmobile_qmobileui_crash_SignalHandler_crashAndGetExceptionMessage(JNIEnv *env,
                                                                           jobject thiz,
                                                                           jthrowable exception) {
    //env->ThrowNew(env->GetObjectClass(exception),"Oh my pony");
    int *a = nullptr;
    a[10] = 6;
    env->ExceptionCheck();
    //throw MyException(); // This can be replaced with any foreign function call that throws.
}