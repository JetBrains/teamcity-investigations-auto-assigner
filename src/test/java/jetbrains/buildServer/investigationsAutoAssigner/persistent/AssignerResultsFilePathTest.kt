package jetbrains.buildServer.investigationsAutoAssigner.persistent

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants
import jetbrains.buildServer.investigationsAutoAssigner.persistent.AssignerResultsFilePath
import jetbrains.buildServer.serverSide.SBuild
import jetbrains.buildServer.serverSide.STestRun
import org.mockito.Mockito.*
import org.testng.Assert.*
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.io.IOException
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path

class AssignerResultsFilePathTest {

    private lateinit var fs: FileSystem
    private lateinit var build: SBuild
    private lateinit var testRun: STestRun
    private lateinit var assignerPath: AssignerResultsFilePath
    private lateinit var artifactsDir: Path

    @BeforeMethod
    fun setUp() {
        fs = Jimfs.newFileSystem(Configuration.unix())
        artifactsDir = fs.getPath("/artifacts")
        Files.createDirectory(artifactsDir)

        build = mock(SBuild::class.java)
        `when`(build.artifactsDirectory).thenReturn(artifactsDir.toFile())

        testRun = mock(STestRun::class.java)

        assignerPath = AssignerResultsFilePath()
    }

    @Test
    fun testGetCreatesMissingStructure() {
        val path = assignerPath.get(build)

        assertTrue(Files.exists(path), "Expected artifact file to be created")
        assertTrue(path.toString().endsWith(Constants.ARTIFACT_FILENAME))
        assertTrue(Files.exists(path.parent), "Expected parent directory to be created")
    }

    @Test(expectedExceptions = [IllegalStateException::class])
    fun testGetThrowsWhenTeamCityDirMissing() {
        // Simulate missing `.teamcity` dir
        val brokenDir = fs.getPath("/empty")
        Files.createDirectory(brokenDir)
        `when`(build.artifactsDirectory).thenReturn(brokenDir.toFile())

        assignerPath.get(build)
    }

    @Test
    fun testGetIfExistReturnsNullWhenMissingAndNoCreate() {
        val result = assignerPath.getIfExist(build, testRun)
        assertNull(result, "Expected null when artifact structure is missing and creation is off")
    }

    @Test
    fun testGetIfExistReturnsPathIfExists() {
        // Manually create full structure
        val teamcityDir = artifactsDir.resolve(Constants.TEAMCITY_DIRECTORY)
        Files.createDirectory(teamcityDir)

        val pluginDir = teamcityDir.resolve(Constants.ARTIFACT_DIRECTORY)
        Files.createDirectory(pluginDir)

        val file = pluginDir.resolve(Constants.ARTIFACT_FILENAME)
        Files.createFile(file)

        val result = assignerPath.getIfExist(build, testRun)

        assertNotNull(result, "Expected result when file structure already exists")
        assertEquals(result, file)
    }
}
