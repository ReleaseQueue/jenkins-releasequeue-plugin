package com.releasequeue.jenkins.trigger;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import hudson.model.Cause;


/**
 *
 * @author adrian
 */
public class ReleaseQueueBuildCause extends Cause {

    public ReleaseQueueBuildCause() {

    }

    @Override
    public String getShortDescription() {
        return "Triggered by Release Queue Build Trigger";
    }

}
