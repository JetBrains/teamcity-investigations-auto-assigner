

package jetbrains.buildServer.investigationsAutoAssigner.persistent

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants
import jetbrains.buildServer.serverSide.SBuild
import org.mockito.Mockito
import org.testng.Assert
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

import org.mockito.Mockito.`when`
import java.lang.IllegalStateException

class AssignerResultsFilePathTest {
    private lateinit var myInstance: AssignerResultsFilePath
    private lateinit var mySBuild: SBuild
    private lateinit var myTeamCityDir: Path
    private lateinit var myAutoAssignerArtifactDir: Path
    private lateinit var myAutoAssignerArtifactFile: Path

    @BeforeMethod
    fun setUp() {
        mySBuild = Mockito.mock<SBuild>(SBuild::class.java)
        val artifactFile = Mockito.mock<File>(File::class.java)
        `when`(mySBuild.artifactsDirectory).thenReturn(artifactFile)

        val fs = Jimfs.newFileSystem(Configuration.unix())
        val artifactsDir = fs.getPath("/artifactsDir")
        Files.createDirectory(artifactsDir)
        `when`(artifactFile.toPath()).thenReturn(artifactsDir)

        myTeamCityDir = artifactsDir.resolve(Constants.TEAMCITY_DIRECTORY)
        myAutoAssignerArtifactDir = myTeamCityDir.resolve(Constants.ARTIFACT_DIRECTORY)
        myAutoAssignerArtifactFile = myAutoAssignerArtifactDir.resolve(Constants.ARTIFACT_FILENAME)

        myInstance = AssignerResultsFilePath()
    }

    @Test(expectedExceptions = [IllegalStateException::class])
    fun testGetNoTeamCityDir() {
        myInstance.get(mySBuild)
    }

    @Test
    fun testGetIfExistNoTeamCityDir() {
        Assert.assertNull(myInstance.getIfExist(mySBuild, null))
    }

    @Test
    fun testGetNoAutoAssignerDir() {
        Files.createDirectory(myTeamCityDir)

        val result = myInstance.get(mySBuild)

        Assert.assertNotNull(result)
        Assert.assertTrue(Files.exists(myAutoAssignerArtifactDir))
    }

    @Test
    fun testGetIfExistNoAutoAssignerDir() {
        Files.createDirectory(myTeamCityDir)

        val result = myInstance.getIfExist(mySBuild, null)

        Assert.assertNull(result)
        Assert.assertFalse(Files.exists(myAutoAssignerArtifactDir))
    }

    @Test
    fun testGetNoAutoAssignerFile() {
        Files.createDirectory(myTeamCityDir)
        Files.createDirectories(myAutoAssignerArtifactDir)

        val result = myInstance.get(mySBuild)

        Assert.assertNotNull(result)
        Assert.assertTrue(Files.exists(myAutoAssignerArtifactFile))
    }

    @Test
    fun testGetIfExistNoAutoAssignerFile() {
        Files.createDirectory(myTeamCityDir)
        Files.createDirectories(myAutoAssignerArtifactDir)

        val result = myInstance.getIfExist(mySBuild, null)

        Assert.assertNull(result)
        Assert.assertFalse(Files.exists(myAutoAssignerArtifactFile))
    }

    @Test
    fun testGetAutoAssignerFileExist() {
        Files.createDirectory(myTeamCityDir)
        Files.createDirectories(myAutoAssignerArtifactDir)
        Files.createFile(myAutoAssignerArtifactFile)

        val result = myInstance.get(mySBuild)

        Assert.assertNotNull(result)
    }

    @Test
    fun testGetIfExistAutoAssignerFileExist() {
        Files.createDirectory(myTeamCityDir)
        Files.createDirectories(myAutoAssignerArtifactDir)
        Files.createFile(myAutoAssignerArtifactFile)

        val result = myInstance.getIfExist(mySBuild, null)

        Assert.assertNotNull(result)
    }
}