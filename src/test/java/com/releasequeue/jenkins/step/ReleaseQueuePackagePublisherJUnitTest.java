/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.releasequeue.jenkins.step;

import com.releasequeue.server.ReleaseQueueServer;
import hudson.FilePath;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;
import org.mockito.MockitoAnnotations;

/**
 *
 * @author adrian
 */

public class ReleaseQueuePackagePublisherJUnitTest{

    public ReleaseQueuePackagePublisherJUnitTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() {
    }

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void test_package_uploaded()
    throws Exception, InterruptedException, IOException, ExecutionException
    {
        String dist = "dist";
        String comp = "comp";
        String pattern = "*.deb";
        FreeStyleProject project = j.createFreeStyleProject();
        ReleaseQueueServer server = mock(ReleaseQueueServer.class);

        ReleaseQueuePackagePublisher testPublisher = new ReleaseQueuePackagePublisher(dist, comp, pattern, server);
        project.getPublishersList().add(testPublisher);

        FilePath ws = j.jenkins.getWorkspaceFor(project);
        ws.mkdirs();
        FilePath pkg = new FilePath(new File(ws.toString() + "/test.deb"));
        pkg.touch(0);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.SUCCESS, build);
        Mockito.verify(server).uploadPackage(pkg, dist, comp);
    }

    @Test
    public void test_with_no_package()
    throws Exception, InterruptedException, IOException, ExecutionException
    {
        String dist = "dist";
        String comp = "comp";
        String pattern = "*.deb";
        FreeStyleProject project = j.createFreeStyleProject();
        ReleaseQueueServer server = mock(ReleaseQueueServer.class);

        ReleaseQueuePackagePublisher testPublisher = new ReleaseQueuePackagePublisher(dist, comp, pattern, server);
        project.getPublishersList().add(testPublisher);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.SUCCESS, build);
    }

    @Test
    public void test_specific_package_uploaded()
    throws Exception, InterruptedException, IOException, ExecutionException
    {
        String dist = "dist";
        String comp = "comp";
        String pattern = "test.deb";
        FreeStyleProject project = j.createFreeStyleProject();
        ReleaseQueueServer server = mock(ReleaseQueueServer.class);

        ReleaseQueuePackagePublisher testPublisher = new ReleaseQueuePackagePublisher(dist, comp, pattern, server);
        project.getPublishersList().add(testPublisher);

        FilePath ws = j.jenkins.getWorkspaceFor(project);
        ws.mkdirs();
        FilePath pkg1 = new FilePath(new File(ws.toString() + "/test.deb"));
        pkg1.touch(0);
        FilePath pkg2 = new FilePath(new File(ws.toString() + "/dont.deb"));
        pkg2.touch(0);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.SUCCESS, build);
        Mockito.verify(server).uploadPackage(pkg1, dist, comp);
        Mockito.verify(server, times(0)).uploadPackage(pkg2, dist, comp);
    }


}
