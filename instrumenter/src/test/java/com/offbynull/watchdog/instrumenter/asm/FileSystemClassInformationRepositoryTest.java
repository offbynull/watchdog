package com.offbynull.watchdog.instrumenter.asm;

import com.offbynull.watchdog.instrumenter.testhelpers.TestUtils;
import java.io.File;
import static java.util.Arrays.asList;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

public final class FileSystemClassInformationRepositoryTest {
    
    private static File jarFile;
    private static FileSystemClassInformationRepository repo;

    @BeforeClass
    public static void beforeClass() throws Exception {
        byte[] data = TestUtils.getResource("FakeJVMClasses.jar");
        
        jarFile = File.createTempFile(TestUtils.class.getSimpleName(), ".jar");
        jarFile.deleteOnExit();
        FileUtils.writeByteArrayToFile(jarFile, data);
        
        repo = FileSystemClassInformationRepository.create(asList(jarFile));
    }
    
    @AfterClass
    public static void afterClass() {
        jarFile.delete();
    }

    @Test
    public void mustGetClassInformationForInteger() {
        ClassInformation info = repo.getInformation("fake/java/lang/Integer");
        
        assertEquals("fake/java/lang/Number", info.getSuperClassName());
        assertEquals(1, info.getInterfaces().size());
        assertTrue(info.getInterfaces().contains("fake/java/lang/Comparable"));
        assertFalse(info.isInterface());
    }
    
    @Test
    public void mustGetClassInformationForBoolean() {
        ClassInformation info = repo.getInformation("fake/java/lang/Boolean");
        
        assertEquals("java/lang/Object", info.getSuperClassName());
        assertEquals(2, info.getInterfaces().size());
        assertTrue(info.getInterfaces().contains("fake/java/lang/Comparable"));
        assertTrue(info.getInterfaces().contains("fake/java/io/Serializable"));
        assertFalse(info.isInterface());
    }

    @Test
    public void mustGetClassInformationForRunnableFuture() {
        ClassInformation info = repo.getInformation("fake/java/util/concurrent/RunnableFuture");
        
        assertEquals("java/lang/Object", info.getSuperClassName());
        assertEquals(2, info.getInterfaces().size());
        assertTrue(info.getInterfaces().contains("fake/java/lang/Runnable"));
        assertTrue(info.getInterfaces().contains("fake/java/util/concurrent/Future"));
        assertTrue(info.isInterface());
    }

    @Test
    public void mustFailToGetClassInformationForUnknownClass() {
        ClassInformation info = repo.getInformation("2huowhf9w37fy9fhnwfwfwefasef");
        
        assertNull(info);
    }
    
}
