package com.termux.terminal;

import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;

import java.io.*;
import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class TerminalSynchronousSessionHandler implements TerminalSessionHandler {

    public final String mHandle = UUID.randomUUID().toString();

    // stateLock: guards mShellPid, mShellExitStatus, mTerminalFileDescriptor, mPtyOut
    private final Object stateLock = new Object();

    private int mShellPid;
    private int mShellExitStatus;
    private int mTerminalFileDescriptor;

    private final String mShellPath;
    private final String mCwd;
    private final String[] mArgs;
    private final String[] mEnv;

    // syncLock: guards commandOutput, commandFinished, currentMarker
    private final Object syncLock = new Object();
    // commandLock: ensures only one executeCommandSync runs at a time
    private final Object commandLock = new Object();

    private boolean commandFinished = true;
    private String currentMarker;
    private final StringBuilder commandOutput = new StringBuilder();

    // Single persistent output stream — opened once, never closed between writes
    private FileOutputStream mPtyOut;

    // Guard against initializeEmulator() being called more than once
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private static final String LOG_TAG = "TerminalSession";

    // PTY column width — must be wide enough that no command line wraps
    private static final int PTY_COLS = 1000;
    private static final int PTY_ROWS = 50;

    public TerminalSynchronousSessionHandler(String shellPath, String cwd, String[] args, String[] env, Integer transcriptRows) {
        this.mShellPath = shellPath;
        this.mCwd = cwd;
        this.mArgs = args;
        this.mEnv = env;
    }

    // ============================================================
    // Alpine initialization (legacy / simple mode)
    // ============================================================

    /** Convenience overload — no log callback. */
    public void initializeAlpine() {
        initializeAlpine(null);
    }

    /**
     * Initializes Alpine synchronously.
     * @param logListener optional callback invoked on the calling thread for each log line;
     *                    pass {@code null} to skip.
     */
    public void initializeAlpine(Consumer<String> logListener) {

        int[] processId = new int[1];

        mTerminalFileDescriptor = JNI.createSubprocess(
                mShellPath, mCwd, mArgs, mEnv,
                processId, 80, 24, 1, 1
        );

        mShellPid = processId[0];

        final FileDescriptor fd = wrapFileDescriptor(mTerminalFileDescriptor);

        String startMsg = "Starting Alpine initialization…";
        Log.d("INIT", startMsg);
        if (logListener != null) logListener.accept(startMsg);

        boolean isInitialized = false;

        try (
                InputStream termIn = new FileInputStream(fd);
                FileOutputStream termOut = new FileOutputStream(fd)
        ) {

            byte[] buffer = new byte[4096];

            while (!isInitialized) {

                int read = termIn.read(buffer);
                if (read == -1) break;

                String text = new String(buffer, 0, read);
                String cleaned = text.replaceAll("\u001B\\[[;\\d]*[ -/]*[@-~]", "").trim();

                if (!cleaned.isEmpty()) {
                    Log.d("INIT_LOG", cleaned);
                    if (logListener != null) logListener.accept(cleaned);
                }

                // Detect shell prompt
                if (cleaned.endsWith("#") || cleaned.endsWith("$")) {
                    String promptMsg = "Shell prompt detected — sending READY probe…";
                    Log.d("INIT", promptMsg);
                    if (logListener != null) logListener.accept(promptMsg);
                    termOut.write("echo READY\n".getBytes());
                    termOut.flush();
                }

                // Final confirmation
                if (cleaned.contains("READY")) {
                    isInitialized = true;
                    String doneMsg = "✓ Alpine initialization complete.";
                    Log.d("INIT", doneMsg);
                    if (logListener != null) logListener.accept(doneMsg);
                }
            }

        } catch (Exception e) {
            String errMsg = "Error during initialization: " + e;
            Log.e("INIT_ERROR", errMsg);
            if (logListener != null) logListener.accept(errMsg);
        }
    }

    // ============================================================
    // Main emulator initialization — call once
    // ============================================================
    public void initializeEmulator() {
        // Prevent duplicate initialization
        if (!initialized.compareAndSet(false, true)) {
            Log.w(LOG_TAG, "initializeEmulator() called more than once — ignoring");
            return;
        }

        int[] processId = new int[1];
        int fd = JNI.createSubprocess(
                mShellPath, mCwd, mArgs, mEnv, processId, PTY_COLS, PTY_ROWS, 1, 1);

        FileDescriptor wrappedFd = wrapFileDescriptor(fd);

        synchronized (stateLock) {
            mTerminalFileDescriptor = fd;
            mShellPid = processId[0];
            // Open ONE persistent write stream — never close between writes
            try {
                mPtyOut = new FileOutputStream(wrappedFd);
            } catch (Exception e) {
                throw new RuntimeException("Failed to open PTY output stream", e);
            }
        }

        // --------------------------------------------------------
        // INPUT READER THREAD
        // --------------------------------------------------------
        new Thread(() -> {
            try (InputStream termIn = new FileInputStream(wrappedFd)) {
                byte[] buffer = new byte[8192];
                while (true) {
                    int read = termIn.read(buffer);
                    if (read == -1) return;

                    String text = new String(buffer, 0, read);

                    // Strip ANSI/VT escape sequences and carriage returns
                    String cleaned = text
                            .replaceAll("\u001B\\[[;\\d]*[ -/]*[@-~]", "")           // CSI sequences
                            .replaceAll("\u001B\\][^\u0007]*(\u0007|\u001B\\\\)", "") // OSC sequences
                            .replaceAll("\u001B[()][AB012]", "")                      // Character set
                            .replaceAll("\u001B[=>]", "")                             // Keypad mode
                            .replaceAll("\r", "");                                    // CR

                    // Strip shell prompt lines e.g. "root@reterm ~ # "
                    cleaned = cleaned.replaceAll("(?m)^[^\\n]*@[^\\n]*#[^\\n]*$\\n?", "");

                    Log.d("PTY_OUTPUT", "|" + cleaned + "|");

                    synchronized (syncLock) {
                        commandOutput.append(cleaned);

                        if (currentMarker == null) continue;

                        // ----------------------------------------
                        // FIX Bug 3: derive both markers here
                        // ----------------------------------------
                        String startMarker = currentMarker + "start__";
                        String endMarker   = currentMarker + "end__";

                        // ----------------------------------------
                        // FIX Bug 3: strip everything before and
                        // including the start marker so the echo
                        // of the marker itself never leaks into the result
                        // ----------------------------------------
                        int startIdx = commandOutput.indexOf(startMarker);
                        if (startIdx != -1) {
                            commandOutput.delete(0, startIdx + startMarker.length());
                        }

                        int markerIndex = commandOutput.indexOf(endMarker);
                        if (markerIndex == -1) continue;

                        // ----------------------------------------
                        // FIX Bug 1: tail is the raw text that
                        // follows the end marker — parse it directly.
                        // The old code called tail.split(endMarker)[1]
                        // but endMarker was already consumed by indexOf,
                        // so split always returned ["tail"] (length 1)
                        // and index [1] threw → NumberFormatException → continue loop forever.
                        // ----------------------------------------
                        String tail = commandOutput
                                .substring(markerIndex + endMarker.length())
                                .trim();

                        // Exit code not yet arrived — wait for next chunk
                        if (tail.isEmpty()) continue;

                        int exitCode;
                        try {
                            // Take the first whitespace-separated token in case
                            // there's trailing noise (e.g. a newline with a prompt)
                            exitCode = Integer.parseInt(tail.split("\\s+")[0]);
                        } catch (NumberFormatException ignored) {
                            // Partial data — wait for more
                            continue;
                        }

                        Log.d(LOG_TAG, "Exit code: " + exitCode);

                        // Write exit status under stateLock
                        synchronized (stateLock) {
                            mShellExitStatus = exitCode;
                        }

                        // ----------------------------------------
                        // FIX Bug 2: the old code did
                        //   setLength(commandOutput.length() - markerIndex)
                        // which subtracts the marker *position* from the total
                        // length — keeping the wrong tail instead of trimming at
                        // the marker position.
                        // Correct: keep only the output before the end marker.
                        // ----------------------------------------
                        commandOutput.setLength(markerIndex);

                        commandFinished = true;
                        syncLock.notifyAll();
                    }
                }
            } catch (Exception e) {
                Log.e("PTY_ERROR", e.toString());
            }
        }, "pty-reader").start();

        // --------------------------------------------------------
        // PROCESS WAITER THREAD
        // --------------------------------------------------------
        new Thread(() -> {
            // Capture pid safely before blocking
            int pid;
            synchronized (stateLock) {
                pid = mShellPid;
            }
            int exitCode = JNI.waitFor(pid);
            cleanupResources(exitCode);
            // Unblock any waiting executeCommandSync
            synchronized (syncLock) {
                commandFinished = true;
                syncLock.notifyAll();
            }
            Log.d("PTY_EXIT", "Process exited: " + exitCode);
        }, "pty-waiter").start();
    }

    // ============================================================
    // Execute command synchronously — 30s default timeout
    // ============================================================
    public String executeCommandSync(String command) {
        return executeCommandSync(command, 30_000);
    }

    public String executeCommandSync(String command, long timeoutMs) {
        // commandLock serialises callers so only one command runs at a time
        synchronized (commandLock) {

            final String marker;

            synchronized (syncLock) {
                commandFinished = false;
                commandOutput.setLength(0);

                // Short marker to avoid any chance of PTY line-wrapping
                marker = "__" + UUID.randomUUID().toString().replace("-", "") + "__";
                currentMarker = marker;
            }

            // Build the full command:
            //   1. echo startMarker   — lets the reader know real output begins
            //   2. the actual command
            //   3. printf endMarker + exit code on its own line
            String startMarker = marker + "start__";
            String endMarker   = marker + "end__";

            String fullCommand =
                    "echo " + startMarker + "\n" +
                            command + "\n" +
                            "printf '" + endMarker + "%s\\n' \"$?\"\n";

            Log.d(LOG_TAG, "Executing: " + command);
            write(fullCommand.getBytes());

            // Wait for the reader thread to signal completion
            String result;
            synchronized (syncLock) {
                long deadline = System.currentTimeMillis() + timeoutMs;
                while (!commandFinished) {
                    long remaining = deadline - System.currentTimeMillis();
                    if (remaining <= 0) {
                        Log.w(LOG_TAG, "Command timed out after " + timeoutMs + "ms: " + command);
                        break;
                    }
                    try {
                        syncLock.wait(remaining);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                // Capture result while still holding syncLock to prevent
                // the reader racing in and overwriting commandOutput
                result = commandOutput.toString().trim();
                currentMarker = null;
            }

            return result;
        }
    }

    // ============================================================
    // Write to shell
    // ============================================================
    public void write(byte[] data) {
        write(data, 0, data.length);
    }

    public void write(byte[] data, int offset, int count) {
        // Capture stream reference under stateLock, then do I/O outside it
        FileOutputStream out;
        synchronized (stateLock) {
            if (mShellPid <= 0 || mPtyOut == null) return;
            out = mPtyOut;
        }
        // Synchronize on the stream itself to prevent interleaved writes
        // from concurrent callers (shouldn't happen with commandLock, but defensive)
        try {
            synchronized (out) {
                out.write(data, offset, count);
                out.flush();
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Write error: " + e.getMessage());
        }
    }

    // ============================================================
    // Shutdown
    // ============================================================
    public void finishIfRunning() {
        synchronized (stateLock) {
            if (mShellPid > 0) {
                try {
                    Os.kill(mShellPid, OsConstants.SIGKILL);
                } catch (ErrnoException ignored) {}
            }
            if (mPtyOut != null) {
                try {
                    mPtyOut.close();
                } catch (IOException ignored) {}
                mPtyOut = null;
            }
        }
    }

    void cleanupResources(int exitStatus) {
        int fdToClose;
        synchronized (stateLock) {
            mShellPid = -1;
            mShellExitStatus = exitStatus;
            fdToClose = mTerminalFileDescriptor;
        }
        JNI.close(fdToClose);
    }

    public boolean isRunning() {
        synchronized (stateLock) {
            return mShellPid > 0;
        }
    }

    public int getLastCommandExitCode() {
        synchronized (stateLock) {
            return mShellExitStatus;
        }
    }

    public int getPid() {
        synchronized (stateLock) {
            return mShellPid;
        }
    }

    // ============================================================
    // Wrap raw int fd into Java FileDescriptor via reflection
    // ============================================================
    private static FileDescriptor wrapFileDescriptor(int fd) {
        FileDescriptor result = new FileDescriptor();
        try {
            Field field;
            try {
                field = FileDescriptor.class.getDeclaredField("descriptor");
            } catch (NoSuchFieldException e) {
                field = FileDescriptor.class.getDeclaredField("fd");
            }
            field.setAccessible(true);
            field.set(result, fd);
        } catch (Exception e) {
            throw new RuntimeException("Failed to wrap file descriptor", e);
        }
        return result;
    }
}