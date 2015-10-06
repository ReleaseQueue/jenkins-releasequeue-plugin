/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.releasequeue.jenkins.action;

import com.releasequeue.jenkins.trigger.ReleaseQueueBuildCause;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.CauseAction;
import hudson.model.ChoiceParameterDefinition;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.StringParameterDefinition;
import hudson.model.TextParameterDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.servlet.ServletInputStream;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.kohsuke.stapler.Ancestor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 *
 * @author adrian
 */
@Extension
public class ReleaseQueueWebHookAction implements Action {
    static final String URL = "rqhook";

    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return null;
    }

    public String getUrlName() {
        return URL;
    }

    private static final Logger LOGGER =  Logger.getLogger(ReleaseQueueWebHookAction.class.getName());

    public void doIndex(StaplerRequest req, StaplerResponse resp) throws Exception {
        List<Ancestor> ancestors = req.getAncestors();
        AbstractProject project = (AbstractProject)ancestors.get(ancestors.size() - 2).getObject();

        byte[] data      = new byte[req.getContentLength()];
        ServletInputStream inputStream = req.getInputStream();
        inputStream.read(data);
        JSONParser parser = new JSONParser();
        JSONObject jsonObj = JSONObject.fromObject(parser.parse(new String(data)));

        List<ParameterValue> values = new ArrayList<ParameterValue>();
        ParametersDefinitionProperty property = (ParametersDefinitionProperty)project.getProperty(ParametersDefinitionProperty.class);

        if (property != null){
            for (ParameterDefinition parameterDefinition : property.getParameterDefinitions()) {
                String parameterName = parameterDefinition.getName();
                ParameterValue parameterValue = null;

                if(jsonObj.containsKey(parameterName)){
                    String strValue = jsonObj.get(parameterName).toString();
                    if (parameterDefinition.getClass() == StringParameterDefinition.class){
                        parameterValue = ((StringParameterDefinition) parameterDefinition).createValue(strValue);
                    }
                    else if (parameterDefinition.getClass() == TextParameterDefinition.class){
                        parameterValue = ((TextParameterDefinition) parameterDefinition).createValue(strValue);
                    }
                    else if (parameterDefinition.getClass() == ChoiceParameterDefinition.class){
                        if (((ChoiceParameterDefinition)parameterDefinition).getChoices().contains(strValue)){
                            parameterValue = ((ChoiceParameterDefinition)parameterDefinition).createValue(strValue);
                        }
                    }
                }
                if (parameterValue == null) {
                    parameterValue = parameterDefinition.getDefaultParameterValue();
                }

                values.add(parameterValue);
            }
        }

        Jenkins.getInstance().getQueue().schedule(project, 0, new ParametersAction(values), new CauseAction(new ReleaseQueueBuildCause()));
    }


}