/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ds;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.*; 
public class ReadWriteServer
    extends UnicastRemoteObject 
    implements ReadWriteInterface {
    
    public String myname; //name of server provess for Rmi registry
    boolean am_I_Master;
    RegistryServer registry_server;
    public ReadWriteServer(String []args) throws  Exception {
        super(0);    // required to avoid the 'rmic' step, see below
        System.err.println("RMI server started");
        try { //special exception handler for registry creation
            LocateRegistry.createRegistry(Integer.parseInt(args[1]));  //args[1] is port
            System.err.println("ReadWriteServer: java RMI registry created.");
        } catch (RemoteException e) {
            //do nothing, error means registry already exists
            System.err.println("ReadWriteServer: java RMI registry already exists.");
            LocateRegistry.getRegistry(Integer.parseInt(args[1]));
        }
        String mydate = java.text.DateFormat.getDateTimeInstance().format(java.util.Calendar.getInstance().getTime());
        myname="RmiServer_ay"+mydate;
        myname=myname.replace(",", "").replace(":","").replace(" ", "");
        Naming.rebind("//"+args[0]+"/"+myname, this);
        System.err.println("Server bound in registry with name "+myname);
	registry_server=(RegistryServer)Naming.lookup("//"+args[0]+"/MyRegistry_ay");
        am_I_Master=registry_server.RegisterServer(myname); 
    }
    public static void main(String args[]) throws Exception {
        //Instantiate RmiServer
        ReadWriteServer obj = new ReadWriteServer(args);
        // Bind this object instance to the name "RmiServer"
               
    }
    
    @Override
    public int FileWrite64K(String filename, long offset, byte[] data) throws IOException, RemoteException {
        if(!am_I_Master)throw new UnsupportedOperationException(myname+": Writes go to master server only!");
        else System.err.println("Writing to master server");
        
        int ret_val=FileWrite64K_internal(filename,offset,data);
        
        System.err.println("Starting replication");
        
        //start replicating
        String []replica_links=registry_server.GetFileServers();
        
        for(String replica_link:replica_links){
            if(replica_link==myname)continue;
            try{
                ReadWriteInterface rws=(ReadWriteInterface)Naming.lookup(replica_link);
                rws.FileWrite64K_internal(filename, offset, data);
            }catch (NotBoundException ne){
                System.err.print("Could not find server: "+replica_link);
                //take lite
            }
        }
        //end replicating
        return ret_val;
    }
    @Override
    public int FileWrite64K_internal(String filename, long offset, byte[] data) throws IOException, RemoteException,UnsupportedOperationException {
        //original FileWrite64K without replicas
        new File(myname).mkdir();
        new File(myname+"/"+filename).mkdir();
        FileOutputStream fos;
        String chunk_name=myname+"/"+filename+"/chunk"+Long.toString(offset);
        try{
            fos=new FileOutputStream(chunk_name);
        } catch (IOException e){
            new File(chunk_name).createNewFile();
            fos=new FileOutputStream(chunk_name);
        }
        fos.write(data);
        fos.close();
        return 0;
    }

    @Override
    public long NumFileChunks(String filename) throws IOException, RemoteException {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        filename=myname+"/"+filename;
        File folderExisting = new File(filename);
        if(!folderExisting.exists())return 0;
        int i=0;
        while(true){
            File fileExisting=new File(filename+"/chunk"+Integer.toString(i));
            if(fileExisting.exists())i++;
            else return i;
        }
    }

    @Override
    public byte[] FileRead64K(String filename, long offset) throws IOException, RemoteException {
        System.err.println("Read from Server "+myname+" for file "+filename+" and chunk number "+Long.toString(offset));
        filename=myname+"/"+filename;
        File folderExisting = new File(filename);
        if(!folderExisting.exists())return null;
        int i=0;
        FileInputStream fis;
        try{
            fis=new FileInputStream(filename+"/chunk"+Integer.toString((int) offset));
        }
        catch (FileNotFoundException ex){
            return null;
        }
        byte [] bytes=new byte[64*1024];
        int read_count=fis.read(bytes,0,64*1024);
	byte [] bytes_with_size=new byte[read_count];
	for(i=0;i<read_count;i++){
	    bytes_with_size[i]=bytes[i];
	}
	//System.err.writeln(Integer.toString(read_count));
        return bytes_with_size;
    }
    
}