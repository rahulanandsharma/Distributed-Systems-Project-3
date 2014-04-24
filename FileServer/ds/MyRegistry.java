/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ds;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * @author asgardian07
 */
public class MyRegistry
    extends UnicastRemoteObject implements RegistryServer{

    static private ArrayList<String> FileServers;
    static private boolean master_exists;
    static private MyRegistry myregistry_obj=null;
    public MyRegistry(String host, String port) throws RemoteException, MalformedURLException{
        FileServers=new ArrayList<>();
        master_exists=false;
    }
    public static void main(String [] args) throws RemoteException,MalformedURLException{
        myregistry_obj=new MyRegistry(args[0],args[1]);
        bindRegistry(args[0], args[1]);
    }
    public static MyRegistry getMyRegistryObject(){
        return myregistry_obj;
    }
    @Override
    public boolean RegisterServer(String name) throws RemoteException {
        FileServers.add(name);
        if(!master_exists){
            master_exists=true;
            return true;
        }
        return false;
    }

    @Override
    public String[] GetFileServers() throws RemoteException {
        String [] ret=new String[FileServers.size()];
        Iterator<String> it;
        int i=0;
        for(String s:FileServers){
            ret[i]=s;
            i++;
        }
        return ret;
    }
    
    static void bindRegistry(String host, String port) throws RemoteException, MalformedURLException{
        System.err.print("port no in MyRegistry is : "+port);
        try { //special exception handler for registry creation
            LocateRegistry.createRegistry(Integer.parseInt(port));  //args[0] is port
            System.err.println("MyRegistry: java RMI registry created.");
        } catch (RemoteException e) {
            //do nothing, error means registry already exists
            System.err.println("MyRegistry: java RMI registry already exists.");
            LocateRegistry.getRegistry(Integer.parseInt(port));
        } 
        //Instantiate MyRegistry_ay
        Naming.rebind("//"+host+"/MyRegistry_ay", myregistry_obj);
        
    }
}
