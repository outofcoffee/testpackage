package com.deloittedigital.testpackage.test;

import com.deloittedigital.testpackage.sequencing.TestHistoryRepository;
import com.google.common.io.Files;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static com.deloittedigital.testpackage.VisibleAssertions.assertEquals;
import static com.deloittedigital.testpackage.VisibleAssertions.assertTrue;

/**
 * Created by richardnorth on 01/01/2014.
 */
public class TestHistoryRepositoryTest {

    @Test
    public void simpleStorageTest() throws IOException {
        TestHistoryRepository repository = new TestHistoryRepository("src/test/resources/historysample1.txt");

        Map<String, Integer> runsSinceLastFailures = repository.getRunsSinceLastFailures();
        assertEquals("a just-failed test class gets marked with 0 runs since last failure", runsSinceLastFailures.get("com.deloittedigital.testpackage.runnertest.failureprioritisationtests.zzz_JustFailedTest"), 0);
        assertEquals("a just-failed test method gets marked with 0 runs since last failure", runsSinceLastFailures.get("testTrue(com.deloittedigital.testpackage.runnertest.failureprioritisationtests.zzz_JustFailedTest)"), 0);
    }

    @Test
    public void testIncrementOnSave() throws IOException {

        File tempFile = File.createTempFile("testhistory", ".txt");
        Files.copy(new File("src/test/resources/historysample2.txt"), tempFile);

        TestHistoryRepository repository = new TestHistoryRepository(tempFile.getAbsolutePath());
        Map<String, Integer> runsSinceLastFailures = repository.getRunsSinceLastFailures();
        assertEquals("a just-failed test class starts with a count of zero", runsSinceLastFailures.get("ClassName"), 0);
        assertEquals("a just-failed test method starts with a count of zero", runsSinceLastFailures.get("methodName(ClassName)"), 0);

        repository.markFailure("JustFailedClass", "justFailedMethod(JustFailedClass)");

        repository.save();

        repository = new TestHistoryRepository(tempFile.getAbsolutePath());
        runsSinceLastFailures = repository.getRunsSinceLastFailures();
        assertEquals("the test class 'runs since last failure' has been incremented if it didn't fail this time", runsSinceLastFailures.get("ClassName"), 1);
        assertEquals("the test method 'runs since last failure' has been incremented if it didn't fail this time", runsSinceLastFailures.get("methodName(ClassName)"), 1);
        assertEquals("a just-failed test class has a count of zero", runsSinceLastFailures.get("JustFailedClass"), 0);
        assertEquals("a just-failed test method has a count of zero", runsSinceLastFailures.get("justFailedMethod(JustFailedClass)"), 0);

    }

    @Test
    public void testFileCreation() throws IOException {
        File tempFile = File.createTempFile("testhistory", ".txt");
        tempFile.delete();

        TestHistoryRepository repository = new TestHistoryRepository(tempFile.getAbsolutePath());
        repository.save();

        assertTrue("repository file is always created upon save even when there are no failed tests", tempFile.exists());
        tempFile.delete();

        repository.markFailure("Foo", "bar(Foo)");
        repository.save();

        assertTrue("repository file is created upon save when there is a failed test", tempFile.exists());


    }
}
