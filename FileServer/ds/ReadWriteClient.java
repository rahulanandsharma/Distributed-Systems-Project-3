package ds;

import java.io.File;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.util.Random;
 
public class ReadWriteClient {
    ReadWriteInterface master ;
    RegistryServer rs ;
    String []file_servers;
    String server_ip;
    public ReadWriteClient(String args[]) throws Exception{
        server_ip=args[1];
        this.rs=(RegistryServer)Naming.lookup("//"+server_ip+"/MyRegistry_ay");
        String [] file_servers=rs.GetFileServers();
        if (file_servers.length==0){
            throw new Exception("No server is registered");
        }
        String master_link=file_servers[0];
        System.err.println("master is "+master_link);
        try{
            master = (ReadWriteInterface)Naming.lookup(master_link);
        }catch (Exception E){
            System.err.println("Could not connect to Master");
        }
    }
    public static void main(String args[]) throws Exception {
        ReadWriteClient rrc=new ReadWriteClient(args);
        rrc.writeFile(args[0]);
        rrc.readFile(args[0]);
    }
    public String getFileNameFromPath(String s){
        String []splits=s.split("/");
        return splits[splits.length-1];
    }
    /**
     * 
     * @param fname: name of file
     * @return 0 for success, 1 for failure
     * @throws FileNotFoundException 
     */
    public  int writeFile(String fname) throws FileNotFoundException, IOException{
            BufferedInputStream  br=new BufferedInputStream (new FileInputStream(fname));
            byte [] bytes=new byte[64*1024];
            int i=0;
	    int read_count;
            fname=getFileNameFromPath(fname);
            while((read_count=br.read(bytes, 0, 64*1024))>0){
		if(read_count==64*1024)
                    try{
                        master.FileWrite64K(fname, i, bytes);
                    }catch (Exception E){
                        System.err.println("Write to master failed");
                    }
                i++;
            }
            br.close();
            return 0;
    }
    public int readFile(String fname) throws Exception{
        file_servers=rs.GetFileServers();
        int n_servers=file_servers.length;
        //round robin scheduling with random start
        int start_i=randInt(0,n_servers-1);
        int server_no=start_i,i;
        
        int n_chunks=getChunksCountSafely(fname,server_no,0);
        
        //create empty file
        fname=getFileNameFromPath(fname);
	new File("output").mkdir();
        new File("output/"+fname).createNewFile();
        
        //start threaded downloading
        Thread [] downloaders=new Thread[n_chunks];
        for(i=0;i<n_chunks;i++){
            server_no+=1;
            server_no%=file_servers.length;
            downloaders[i]=new Thread(new MyChunkDownloader(server_no, fname, i,server_ip),"Task"+Integer.toString(i));
            downloaders[i].start();
        }
        for(i=0;i<n_chunks;i++){
            downloaders[i].join();
        }
        return 0;
    }
    int getChunksCountSafely(String file_name,int server_no, int depth) throws Exception{
        int ret_val=-1;
        if(depth==file_servers.length){
            throw new Error("All servers are down");
        }
        try{
            ReadWriteInterface rrc;
            String server_link=file_servers[server_no];
            rrc=(ReadWriteInterface)Naming.lookup(server_link);
            return (int)rrc.NumFileChunks(file_name);
        }catch (NotBoundException | IOException e){
            //System.err.println(e.getMessage());
            return getChunksCountSafely(file_name,(server_no+1)%file_servers.length,depth+1);
        }
    }
    public static int randInt(int min, int max) {
        // Usually this can be a field rather than a method variable
        Random rand = new Random();

        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        int randomNum = rand.nextInt((max - min) + 1) + min;

        return randomNum;
    }
}