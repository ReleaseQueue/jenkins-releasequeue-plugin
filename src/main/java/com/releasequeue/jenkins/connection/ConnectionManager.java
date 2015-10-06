/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.releasequeue.jenkins.connection;

import com.releasequeue.jenkins.descriptor.ReleaseQueueGlobalDescriptor;
import com.releasequeue.server.ReleaseQueueServer;
import com.releasequeue.server.ServerConnection;
import java.io.IOException;
import java.net.MalformedURLException;
import jenkins.model.Jenkins;

/**
 *
 * @author adrian
 */
public class ConnectionManager {
    public static ServerConnection getConnection(){
        ReleaseQueueGlobalDescriptor.DescriptorImpl globalDescriptor =
                (ReleaseQueueGlobalDescriptor.DescriptorImpl)Jenkins.getInstance().getDescriptor(ReleaseQueueGlobalDescriptor.class);

        String serverUrl = globalDescriptor.getServerUrl(),
               email = globalDescriptor.getEmail(),
               password = globalDescriptor.getPassword();

        ReleaseQueueServer server = null;
        try{
            server = new ReleaseQueueServer(serverUrl, email, password);
        }
        catch(MalformedURLException e){
            throw new RuntimeException(e);
        }
        catch(IOException e){
            throw new RuntimeException(e);
        }

        return server;
    }

}
