package framework;

import static JGL.JGL.*;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import static framework.math3d.math3d.add;
import static framework.math3d.math3d.mul;
import framework.math3d.vec3;
import framework.math3d.vec4;
import static framework.math3d.math3d.add;
import static framework.math3d.math3d.mul;
import static framework.math3d.math3d.add;
import static framework.math3d.math3d.mul;
import static framework.math3d.math3d.add;
import static framework.math3d.math3d.mul;



/**
 * Written by Jim Hudson with a few edits by us
 */

public class Mesh {
    Texture texture;
    Texture bonetex;
    Texture quattex;
    Texture mattex;
    Texture spec_texture;
    Texture emit_texture;
    Texture bump_texture;
    String filename="(none)";
    int numvertices;
    //int floats_per_vertex;
    int numindices;
    int bits_per_index;
    //int vbuff,ibuff;
    int itype;
    int vao;
    int numbones;
    int numframes=0;
    float curFrame = 0.0f;
    int maxdepth=0;
    vec4 quattex_size, bonetex_size;
    int furvbuff, furvao, numfurverts = -1;
    vec3 specular = new vec3(1,1,1);
    //vec3 bbmin,bbmax;
    //vec3 centroid;
    boolean with_adjacency;

    private String readLine(DataInputStream din) {
        String x="";
        
        while(true){
            try{
                byte b = din.readByte();
                if( b == '\n' )
                    return x;
                x += (char)b;
            }
            catch(EOFException e){
                if( x.length() == 0)
                    return null;
                else
                    return x;
            }
            catch(IOException e){
                throw new RuntimeException("IO error");
            }
        }
    }
      
    private byte[] readbytes(DataInputStream din, String[] lst ){
        int numbytes=Integer.parseInt(lst[1]);
        byte[] data = new byte[numbytes];
        try {
            din.readFully(data);
        } catch (IOException ex) {
            throw new RuntimeException("Short read "+filename);
        }
        return data;
    }
    private void makebuffer(int attribidx, int count, byte[] data){
        int[] tmp = new int[1];
        glGenBuffers(1,tmp);
        int buff = tmp[0];
        glEnableVertexAttribArray(attribidx);
        glBindBuffer( GL_ARRAY_BUFFER, buff );
        glBufferData( GL_ARRAY_BUFFER, data.length , data, GL_STATIC_DRAW );
        glVertexAttribPointer(attribidx, count, GL_FLOAT, false, count*4,0);
    }

    
    public  Mesh(String filename)
    {
        this(filename, false, false);
    }
    public  Mesh(String filename, boolean doSimplex)
    {
        this(filename, doSimplex, false);
    }
    public  Mesh(String filename, boolean doSimplex, boolean doWave)
    {
        
        texture=null;
        this.filename = filename;

        int idx = filename.lastIndexOf("/");
        String prefix="";
        if( idx != -1 )
            prefix = filename.substring(0,idx);
        
        FileInputStream fin;
        try{
            fin = new FileInputStream(filename);
        }
        catch(FileNotFoundException ex ){
            throw new RuntimeException("File not found: "+filename);
        }
        
        DataInputStream din = new DataInputStream(fin);

        String line;
        
        line = readLine(din);
        if( !line.equals("mesh_07"))
            throw new RuntimeException("Incorrect mesh format: "+line);
        
        byte[] vdata=null,ndata=null,tcdata=null,tdata=null,idata=null,wdata=null,infdata=null, furdata = null;
        
        byte[] boneheads=null,bonetails=null,matrices=null,quaternions=null;
        
        while(true){
            line = readLine(din);
            if( line == null )
                break;
            String[] lst = line.split(" ");
            //if(lst.length != 0)
            //{
            //    System.out.println(line);
            //}
            if( lst.length == 0 ){
            }
            else if(lst[0].equals("num_vertices"))
                this.numvertices = Integer.parseInt(lst[1]);
            //else if(lst[0].equals("floats_per_vertex"))
            //    this.floats_per_vertex = Integer.parseInt(lst[1]);
            else if(lst[0].equals("num_indices"))
                this.numindices = Integer.parseInt(lst[1]);
            else if(lst[0].equals("map_Kd"))
                this.texture = new ImageTexture(prefix+"/"+lst[1]);
            else if(lst[0].equals("map_Ks"))
                this.spec_texture = new ImageTexture(prefix+"/"+lst[1]);
            else if(lst[0].equals("map_Ke"))
                this.emit_texture = new ImageTexture(prefix+"/"+lst[1]);
            else if(lst[0].equals("map_Bump"))
                this.bump_texture = new ImageTexture(prefix+"/"+lst[1]);
            else if(lst[0].equals("Ks") ){
                specular = new vec3(Float.parseFloat(lst[1]),
                        Float.parseFloat(lst[2]),
                        Float.parseFloat(lst[3]));
            }
            else if(lst[0].equals("vertex_data"))
            {
                int numbytes=Integer.parseInt(lst[1]);
                vdata = readbytes(din,lst);
                 //If the flag is true, modify the plane based on Simplex Noise
                if(doSimplex)
                {
                    for(int i = 0; i < numbytes; i+=12)
                    {
                        OpenSimplexNoise noise = new OpenSimplexNoise();
                        float x = ByteBuffer.wrap(vdata, i, 4).order(ByteOrder.nativeOrder()).getFloat();
                        float z = ByteBuffer.wrap(vdata, i + 8, 4).order(ByteOrder.nativeOrder()).getFloat();
                        float y  = (float)noise.eval(x*4, z*4) * 10.0f;
                        if (y < 0.0f)
                        {
                            y = 0.0f;
                        }
                        byte[] b = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder()).putFloat(y).array();
                        vdata[i + 4] = b[0];
                        vdata[i + 5] = b[1];
                        vdata[i + 6] = b[2];
                        vdata[i + 7] = b[3];
                    }
                }
                if(doWave)
                {
                    
                }
            }
            else if(lst[0].equals("fur_data"))
            {
                furdata = readbytes(din,lst);
            }
            else if(lst[0].equals("normal_data"))
                ndata = readbytes(din,lst);
            else if(lst[0].equals("texcoord_data"))
                tcdata = readbytes(din,lst);
            else if(lst[0].equals("tangent_data"))
                tdata = readbytes(din,lst);
            else if(lst[0].equals("index_data"))
                idata = readbytes(din,lst);
            else if( lst[0].equals("weight_data"))
                wdata = readbytes(din,lst);
            else if( lst[0].equals("influence_data"))
                infdata = readbytes(din,lst);
            else if( lst[0].equals("boneheads"))
            {
                boneheads = readbytes(din,lst);
            }
            else if( lst[0].equals("bonetails"))
                bonetails = readbytes(din,lst);
            else if( lst[0].equals("matrices"))
                matrices = readbytes(din,lst);
            else if( lst[0].equals("quaternions"))
                quaternions = readbytes(din,lst);
            else if( lst[0].equals("with_adjacency")){
                if(lst[1].equals("True"))
                    with_adjacency=true;
                else if( lst[1].equals("False"))
                    with_adjacency=false;
                else
                    with_adjacency=false;
                    //throw new RuntimeException("?");
            }
            else if( lst[0].equals("numbones"))
                numbones = Integer.parseInt(lst[1]);
            else if( lst[0].equals("numframes"))
                numframes = Integer.parseInt(lst[1]);
            else if( lst[0].equals("maxdepth"))
            {
                //System.out.println(line);
                maxdepth = Integer.parseInt(lst[1]);
            }
            else if(lst[0].equals("bits_per_index"))
                this.bits_per_index = Integer.parseInt(lst[1]);
            else if(lst[0].equals("end"))
                break;
            else if( lst[0].equals("Ka") || lst[0].equals("illum") || lst[0].equals("Kd") || lst[0].equals("d") || lst[0].equals("Ns") || lst[0].equals("Ni") || lst[0].equals("Ke") )
                ;   //do nothing
            else{
                if( line.length() > 0 )
                    System.out.println("Warning: Ignoring "+line);
            }
        }
        
        /*
        float[] mins = new float[]{Float.MAX_VALUE,Float.MAX_VALUE,Float.MAX_VALUE};
        float[] maxs = new float[]{-Float.MAX_VALUE,-Float.MAX_VALUE,-Float.MAX_VALUE};
        ByteBuffer tmpb = ByteBuffer.wrap(vdata);
        tmpb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = tmpb.asFloatBuffer();
        int i=0;
        while(i<fb.limit()){
            for(int j=0;j<3;++j){
                float tmp1 = fb.get(i+j);
                mins[j] = Float.min(mins[j],tmp1);
                maxs[j] = Float.max(maxs[j],tmp1);
            }
            i+=floats_per_vertex;
        }
        bbmin = new vec3(mins[0],mins[1],mins[2]);
        bbmax = new vec3(maxs[0],maxs[1],maxs[2]);
        centroid = mul( 0.5f, add(bbmin,bbmax) );
        */
        
        switch (bits_per_index) {
            case 8:
                this.itype = GL_UNSIGNED_BYTE;
                break;
            case 16:
                this.itype = GL_UNSIGNED_SHORT;
                break;
            case 32:
                this.itype = GL_UNSIGNED_INT;
                break;
            default:
                throw new RuntimeException("Bad bits: "+bits_per_index);
        }

        int[] tmp = new int[1];
        glGenVertexArrays(1,tmp);
        this.vao = tmp[0];
        glBindVertexArray(vao);      //do it here so it captures the bindbuffer's below
        
        if( vdata != null )
        {
            makebuffer(Program.POSITION_INDEX,3,vdata);
        }
        if( tcdata != null )
        {
            makebuffer(Program.TEXCOORD_INDEX,2,tcdata);
        }
        if( ndata != null )
        {
            makebuffer(Program.NORMAL_INDEX,3,ndata);
        }
        if( tdata != null )
        {
            makebuffer( Program.TANGENT_INDEX,3,tdata);
        }
        if( wdata != null )
        {
            System.out.println("WEIGHT!!");
            makebuffer( Program.WEIGHT_INDEX,4,wdata);
        }
        if( infdata != null )
        {
            System.out.println("INF: " + infdata.length);
            makebuffer( Program.INFLUENCE_INDEX,4,infdata);
            ByteBuffer tmpx = ByteBuffer.allocate(infdata.length );
            tmpx.order(ByteOrder.nativeOrder());
            FloatBuffer tmpf = tmpx.asFloatBuffer();
            tmpx.put(infdata);
            float[] F = new float[infdata.length/4];
            tmpf.get(F);
            //System.out.println(numbones + " " + F.length);
            //for(int i = 0; i < infdata.length/4;i++)
            //{
            //    System.out.println(F[i * 4] + " " + F[i * 4 + 1]+ " " + F[i * 4 + 2]+ " " + F[i * 4 + 3]);
            //}
        }
        glGenBuffers(1,tmp);
        int ibuff = tmp[0];
        glBindBuffer( GL_ELEMENT_ARRAY_BUFFER, ibuff );
        glBufferData( GL_ELEMENT_ARRAY_BUFFER, idata.length, idata, GL_STATIC_DRAW );
        
        glBindVertexArray(0);    //so no one else interferes with us...
        if( boneheads != null ){
            /*ByteBuffer tmpx = ByteBuffer.allocate(boneheads.length );
            tmpx.order(ByteOrder.nativeOrder());
            FloatBuffer tmpf = tmpx.asFloatBuffer();
            tmpx.put(boneheads);
            float[] F = new float[boneheads.length/4];
            tmpf.get(F);
            System.out.println(numbones + " " + F.length);
            for(int i = 0; i < numbones;i++)
            {
                System.out.println(F[i * 4] + " " + F[i * 4 + 1]+ " " + F[i * 4 + 2]+ " " + F[i * 4 + 3]);
            }*/
            //System.out.println(tmpf.toString());
            bonetex = new DataTexture(numbones,1,boneheads);
            bonetex_size = new vec4(0,0,numbones,1);
        }
        if(quaternions != null)
        {
            
            quattex = new DataTexture(numbones,numframes, quaternions);
            quattex_size = new vec4(0,0,numbones, numframes);
        }
        if( matrices != null )
            mattex = new DataTexture( numbones*4, numframes, matrices );
        if(furdata != null)
        {
            numfurverts = furdata.length / 8 / 4;
            glGenVertexArrays(1,tmp);
            furvao = tmp[0];
            glBindVertexArray(furvao);
            glGenBuffers(1,tmp);
            furvbuff = tmp[0];
            glBindBuffer( GL_ARRAY_BUFFER, furvbuff );
            glBufferData( GL_ARRAY_BUFFER, furdata.length , furdata, GL_STATIC_DRAW );
            glEnableVertexAttribArray(Program.POSITION_INDEX);
            glVertexAttribPointer(Program.POSITION_INDEX, 3, GL_FLOAT, false, 9*4,0);
            glEnableVertexAttribArray(Program.NORMAL_INDEX);
            glVertexAttribPointer(Program.NORMAL_INDEX, 3, GL_FLOAT, false, 9*4,3*4);
            glEnableVertexAttribArray(Program.TEXCOORD_INDEX);
            //this is a vec3!
            glVertexAttribPointer(Program.TEXCOORD_INDEX, 3, GL_FLOAT, false, 9*4,6*4);
            glBindVertexArray(0);
        }
    }
public void drawFur(Program prog)
{
    if(this.furvao != 0)
    {
        if(this.texture != null )
            prog.setUniform("diffuse_texture",this.texture);
        glBindVertexArray(furvao);
        glDrawArrays(GL_POINTS, 0, numfurverts);
    }
}
public void draw(Program prog){
        if(this.texture != null )
            prog.setUniform("diffuse_texture",this.texture);
        //if(this.emit_texture != null )
        //    prog.setUniform("emit_texture",this.texture);
        //if( this.spec_texture != null )
        //    prog.setUniform("spec_texture",this.texture);
        //if( this.bump_texture != null )
        //    prog.setUniform("bump_texture",this.bump_texture);
        if( bonetex != null )
            prog.setUniform("bonetex",this.bonetex);
        if(quattex != null)
        {
            prog.setUniform("quattex", this.quattex);
            prog.setUniform("quattex_size", this.quattex_size);
            prog.setUniform("bonetex_size", this.bonetex_size);
            prog.setUniform("curFrame", this.curFrame);
        }
        if( mattex != null )
            prog.setUniform("mattex", this.mattex );
        
        prog.setUniform("specular",specular);
        
        glBindVertexArray(vao);
        if(with_adjacency)
            glDrawElements(GL_TRIANGLES_ADJACENCY,this.numindices,this.itype,0);
        else
            glDrawElements(GL_TRIANGLES,this.numindices,this.itype,0);

    }
    public void setTexture(Texture2D tex)
    {
        this.texture = tex;
    }
}

                
