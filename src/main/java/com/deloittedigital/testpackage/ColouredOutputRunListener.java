package com.deloittedigital.testpackage;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import java.util.List;
import java.util.Map;

import static com.deloittedigital.testpackage.AnsiSupport.ansiPrintf;

/**
 *
 * @author rnorth
 */
public class ColouredOutputRunListener extends RunListener {

    @Override
    public void testStarted(Description description) throws Exception {
        System.out.println(">> " + description.getTestClass().getSimpleName() + "." + description.getMethodName() + ":");
    }

    @Override
    public void testRunFinished(Result result) throws Exception {

        int failureCount = result.getFailureCount();
        int testCount = result.getRunCount();
        int ignoredCount = result.getIgnoreCount();
        int passed = testCount - failureCount;
        List<Failure> failures = Lists.newArrayList();
        Map<String, Long> runTimings = Maps.newHashMap();

        failures.addAll(result.getFailures());

        System.out.flush();
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
            System.out.println("Failures:");
            for (Failure failure : failures) {
                ansiPrintf("    %s: @|bold,red %s|@", failure.getDescription(), indentNewlines(failure.getMessage()));
                Throwable exception = failure.getException();
                Throwable rootCause = Throwables.getRootCause(exception);

                if (exception.equals(rootCause)) {
                    System.out.printf("        At %s\n\n", rootCause.getStackTrace()[0]);
                } else {
                    System.out.printf("        At %s\n", exception.getStackTrace()[0]);
                    ansiPrintf(       "      Root cause: @|bold,red %s|@", indentNewlines(rootCause.getMessage()));
                    System.out.printf("        At %s\n\n", rootCause.getStackTrace()[0]);
                }
            }
        }
        System.out.flush();
    }

    private static String indentNewlines(String textWithPossibleNewlines) {
        return textWithPossibleNewlines.replaceAll("\\n", "\n      ");
    }
}