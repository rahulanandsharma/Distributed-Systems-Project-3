/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ds;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 *
 * @author asgardian
 */
public interface RegistryServer extends Remote {
    public boolean RegisterServer(String name) throws RemoteException;
    public String[] GetFileServers()  throws RemoteException;
    
}
