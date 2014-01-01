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

package com.deloittedigital.testpackage;

import com.deloittedigital.testpackage.failfast.FailFastRunListener;
import com.deloittedigital.testpackage.junitcore.FailFastSupportCore;
import com.deloittedigital.testpackage.sequencing.TestHistoryRepository;
import com.deloittedigital.testpackage.sequencing.TestHistoryRunListener;
import com.google.common.collect.Lists;
import com.twitter.common.testing.runner.AntJunitXmlReportListener;
import com.twitter.common.testing.runner.StreamSource;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.StoppedByUserException;
import org.kohsuke.args4j.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Manifest;

import static com.deloittedigital.testpackage.AnsiSupport.ansiPrintf;
import static com.deloittedigital.testpackage.AnsiSupport.initialize;

/**
 * @author rnorth
 */
public class TestPackage {

    protected TestSequencer testSequencer = new TestSequencer();

    @Option(name = "--failfast", aliases = "-ff", usage = "Fail Fast: Causes test run to be aborted at the first test failure")
    public boolean failFast = false;

    @Argument
    private List<String> testPackageNames = Lists.newArrayList();

    public static void main(String[] args) throws IOException {

        initialize();

        int exitCode = new TestPackage().doMain(args);

        System.exit(exitCode);
    }

    private int doMain(String[] args) throws IOException {

        CmdLineParser cmdLineParser = new CmdLineParser(this);
        try {
            cmdLineParser.parseArgument(args);
        } catch (CmdLineException e) {
            // if there's a problem in the command line,
            // you'll get this exception. this will report
            // an error message.
            System.err.println(e.getMessage());
            System.err.println("java -jar JARFILE [options...] packagenames...");
            // print the list of available options
            cmdLineParser.printUsage(System.err);
            System.err.println();

            // print option sample. This is useful some time
            System.err.println("  Example: java -jar JARFILE" + cmdLineParser.printExample(ExampleMode.ALL) + " packagenames");

            return -1;
        }

        return run();
    }

    public int run() throws IOException {

        new File(".testpackage").mkdir();
        TestHistoryRepository testHistoryRepository = new TestHistoryRepository(".testpackage/history.txt");
        TestHistoryRunListener testHistoryRunListener = new TestHistoryRunListener(testHistoryRepository);

        getTestPackage();

        Request request = testSequencer.sequenceTests(testHistoryRepository.getRunsSinceLastFailures(), testPackageNames.toArray(new String[testPackageNames.size()]));

        FailFastSupportCore core = new FailFastSupportCore();

        File targetDir = new File("target");
        boolean mkdirs = targetDir.mkdirs();
        if (!(targetDir.exists() || mkdirs)) {
            throw new TestPackageException("Could not create target directory: " + targetDir.getAbsolutePath());
        }
        RunListener antXmlRunListener = new AntJunitXmlReportListener(targetDir, new StreamSource() {
            @Override
            public byte[] readOut(Class<?> testClass) throws IOException {
                return new byte[0];
            }

            @Override
            public byte[] readErr(Class<?> testClass) throws IOException {
                return new byte[0];
            }
        });

        RunListener colouredOutputRunListener = new ColouredOutputRunListener(failFast);

        core.addListener(antXmlRunListener);
        core.addListener(colouredOutputRunListener);
        core.addListener(testHistoryRunListener);

        if (failFast) {
            core.addListener(new FailFastRunListener(core.getNotifier()));
        }

        Result result;
        try {
            result = core.run(request);
        } catch (StoppedByUserException e) {
            ansiPrintf("@|red FAILED|@");
            return 1;
        } finally {
            testHistoryRepository.save();
        }

        int failureCount = result.getFailureCount();
        int testCount = result.getRunCount();
        int passed = testCount - failureCount;
        if (failureCount > 0 || passed == 0) {
            ansiPrintf("@|red FAILED|@");
            return 1;
        } else {
            ansiPrintf("@|green OK|@");
            return 0;
        }
    }


    private void getTestPackage() {

        if (testPackageNames.size() != 0) {
            // command-line arguments always preferred
            return;
        }

        // Check the JAR manifest
        try {
            Enumeration<URL> resources = TestPackage.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                Manifest manifest = new Manifest(url.openStream());
                String attributes = manifest.getMainAttributes().getValue("TestPackage-Package");
                if (attributes != null) {
                    testPackageNames.add(attributes);
                    return;
                }
            }

        } catch (IOException e) {
            throw new TestPackageException("Error loading MANIFEST.MF", e);
        }

        // Fall back to system property
        testPackageNames.add(System.getProperty("package"));
    }

}
