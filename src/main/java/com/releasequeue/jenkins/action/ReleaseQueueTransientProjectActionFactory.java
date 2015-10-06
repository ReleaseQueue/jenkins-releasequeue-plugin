/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.releasequeue.jenkins.action;

import com.releasequeue.jenkins.trigger.ReleaseQueueWebHookTrigger;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.TransientProjectActionFactory;
import hudson.triggers.Trigger;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author adrian
 */
@Extension
public class ReleaseQueueTransientProjectActionFactory extends TransientProjectActionFactory{

    @Override
     public Collection<? extends Action> createFor(AbstractProject target){
         Trigger trigger = target.getTrigger(ReleaseQueueWebHookTrigger.class);
         if(trigger != null){
             ArrayList<Action> ta = new ArrayList<Action>();
             ta.add(new ReleaseQueueWebHookAction());
             return ta;
         }

         return null;
     }
}
