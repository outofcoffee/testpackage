/*
 * Copyright 2013 Deloitte Digital and Richard North
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.testpackage;

import org.testpackage.streams.StreamCapture;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import org.fusesource.jansi.Ansi;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import java.util.List;

import static org.testpackage.AnsiSupport.ansiPrintf;

/**
 * A JUnit run listener which generates user-facing output on System.out to indicate progress of a test run.
 *
 * @author rnorth
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class ColouredOutputRunListener extends RunListener {

    private static final String TICK_MARK = "\u2714";
    private static final String CROSS_MARK = "\u2718";

    private final boolean failFast;
    private StreamCapture streamCapture;
    private Description currentDescription;
    private long currentTestStartTime;
    private boolean currentTestDidFail = false;

    public ColouredOutputRunListener(boolean failFast) {
        this.failFast = failFast;
    }

    @Override
    public void testStarted(Description description) throws Exception {
        System.out.print(Ansi.ansi().saveCursorPosition());
        System.out.print(">>  " + description.getTestClass().getSimpleName() + "." + description.getMethodName() + ":");
        System.out.flush();

        currentTestStartTime = System.currentTimeMillis();
        currentTestDidFail = false;

        streamCapture = StreamCapture.grabStreams(false);
        currentDescription = description;
    }

    @Override
    public void testFailure(Failure failure) throws Exception {

        currentTestDidFail = true;

        streamCapture.restore();

        replaceTestMethodPlaceholder(false);

        if (streamCapture.getStdOut().length() > 0) {
            System.out.println("   STDOUT:");
            System.out.print(streamCapture.getStdOut());
        }

        if (streamCapture.getStdErr().length() > 0) {
            System.out.println("\n   STDERR:");
            System.out.print(streamCapture.getStdErr());
        }


        if (failFast) {
            System.out.flush();
            System.out.println();
            System.out.println();
            System.out.println("*** TESTS ABORTED");
            ansiPrintf("*** @|bg_red Fail-fast triggered by test failure:|@\n");

            reportFailure(failure);
        }
    }

    @Override
    public void testFinished(Description description) throws Exception {

        streamCapture.restore();
        if (!currentTestDidFail) {
            replaceTestMethodPlaceholder(true);
        }
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    @Override
    public void testRunFinished(Result result) throws Exception {

        int failureCount = result.getFailureCount();
        int testCount = result.getRunCount();
        int ignoredCount = result.getIgnoreCount();
        int passed = testCount - failureCount;
        List<Failure> failures = Lists.newArrayList();

        failures.addAll(result.getFailures());

        System.out.flush();
        System.out.println();
        System.out.println();
        System.out.println("*** TESTS COMPLETE");

        String passedStatement;
        if (passed > 0 && failureCount == 0) {
            passedStatement = "@|bg_green %d passed|@";
        } else {
            passedStatement = "%d passed";
        }
        String failedStatement;
        if (failureCount > 0) {
            failedStatement = "@|bg_red %d failed|@";
        } else {
            failedStatement = "0 failed";
        }
        String ignoredStatement;
        if (ignoredCount > 0 && ignoredCount > passed) {
            ignoredStatement = "@|bg_red %d ignored|@";
        } else if (ignoredCount > 0) {
            ignoredStatement = "@|bg_yellow %d ignored|@";
        } else {
            ignoredStatement = "0 ignored";
        }

        ansiPrintf("*** " + passedStatement + ", " + failedStatement + ", " + ignoredStatement, passed, failureCount, ignoredCount);

        if (failureCount > 0) {
            System.out.println();
            System.out.println();
            System.out.println("Failures:");
            for (Failure failure : failures) {
                reportFailure(failure);
            }
        }
        System.out.flush();
    }

    private void reportFailure(Failure failure) {
        ansiPrintf("    @|red %s|@:\n", failure.getDescription());
        ansiPrintf("      @|yellow %s: %s|@\n", failure.getException().getClass().getSimpleName(), indentNewlines(failure.getMessage()));
        Throwable exception = failure.getException();
        Throwable rootCause = Throwables.getRootCause(exception);

        if (exception.equals(rootCause)) {
            System.out.printf("        At %s\n\n", rootCause.getStackTrace()[0]);
        } else {
            System.out.printf("        At %s\n", exception.getStackTrace()[0]);
            ansiPrintf("      Root cause: @|yellow %s: %s|@\n", rootCause.getClass().getSimpleName(), indentNewlines(rootCause.getMessage()));
            System.out.printf("        At %s\n\n", rootCause.getStackTrace()[0]);
        }
        System.out.flush();
    }

    private static String indentNewlines(String textWithPossibleNewlines) {

        if (textWithPossibleNewlines == null) {
            return "";
        }

        return textWithPossibleNewlines.replaceAll("\\n", "\n      ");
    }

    private void replaceTestMethodPlaceholder(boolean success) {
        long elapsedTime = System.currentTimeMillis() - currentTestStartTime;
        System.out.print(Ansi.ansi().eraseLine(Ansi.Erase.ALL).restorCursorPosition());
        String colour;
        String symbol;
        if (success) {
            colour = "green";
            symbol = TICK_MARK;
        } else {
            colour = "red";
            symbol = CROSS_MARK;
        }
        ansiPrintf(" @|"+colour+" %s  %s.%s|@ @|blue (%d ms)|@\n", symbol, currentDescription.getTestClass().getSimpleName(), currentDescription.getMethodName(), elapsedTime);
    }
}
