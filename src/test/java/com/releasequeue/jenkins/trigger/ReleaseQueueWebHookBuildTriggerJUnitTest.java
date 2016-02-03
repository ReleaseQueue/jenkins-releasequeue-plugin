/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.releasequeue.jenkins.trigger;

import com.releasequeue.jenkins.action.ReleaseQueueWebHookAction;
import com.releasequeue.server.ReleaseQueueServer;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.FreeStyleProject;
import hudson.triggers.Trigger;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mockito;
import static org.mockito.Mockito.mock;
import org.mockito.MockitoAnnotations;


/**
 *
 * @author adrian
 */
public class ReleaseQueueWebHookBuildTriggerJUnitTest {

    public ReleaseQueueWebHookBuildTriggerJUnitTest() {
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
    public void addWebHookTest()
    throws Exception, InterruptedException, IOException, ExecutionException{

        String applicationName = "application_a";
        String eventName = "application_version.create";
        ReleaseQueueServer server = mock(ReleaseQueueServer.class);

        FreeStyleProject p = j.createFreeStyleProject();
        ReleaseQueueWebHookTrigger t = new ReleaseQueueWebHookTrigger(applicationName, eventName, server);
        t.start(p, true);

        t = p.getTrigger(ReleaseQueueWebHookTrigger.class);
        Trigger<AbstractProject<?, ?>> tt = p.getTrigger(t.getClass());
        assertEquals(t, tt);

        URL absoluteUrl = new URL(p.getAbsoluteUrl());
        String triggerUrl = (new URL(absoluteUrl, "rqhook/")).toString();
        String webhookName = "jenkins_" + p.getFullName();
        Mockito.verify(server).addWebHookSubscription(applicationName, eventName, webhookName, triggerUrl);

        Action action = p.getAction(ReleaseQueueWebHookAction.class);
        assertNotNull(action);
        assertEquals("rqhook", action.getUrlName());

        t.stop();
        Mockito.verify(server).removeWebHookSubscription(applicationName, webhookName);
    }

}
