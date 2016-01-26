package framework;

import static JGL.JGL.*;
import JSDL.JSDL;
import static JSDL.JSDL.*;
import TextRendering.*;
import static framework.math3d.math3d.*;
import framework.math3d.vec3;
import framework.math3d.vec4;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GameLoop 
{
    //<editor-fold defaultstate="collapsed" desc="Application Variables">
    Program prog;
    Set<Integer> keys = new TreeSet<>();
    long win;
    JSDL.SDL_Event ev;
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Initialize Animals, Pins, and Obstacles">
        ArrayList<Animal> animalList = new ArrayList();
        ArrayList<Obstacle> obstacleList = new ArrayList();
        ArrayList<Pin> pinList = new ArrayList();
//</editor-fold> 
    //<editor-fold defaultstate="collapsed" desc="Loop Variables">
    int numLaunches = 0;
    int numHits = 0;
    String console = "";
    float prev;
    float elapsed;
    int animalSelected = 0;
    Camera cam = new Camera();
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Mesh variables">
    Mesh rockMesh = new Mesh("assets/toonRocks.obj.mesh");
    Mesh treeMesh = new Mesh("assets/bobbleTree.obj.mesh");
    Mesh giraffeMesh = new Mesh("assets/giraffe.obj.mesh");
    Mesh pigMesh = new Mesh("assets/goodPig.obj.mesh");
    Mesh zomMesh = new Mesh("assets/basicZombie.obj.mesh");
    Mesh wallMesh = new Mesh("assets/RockWall.obj.mesh");
    Mesh pinMesh = zomMesh;
    Mesh planeMesh = new Mesh("assets/grassPlane.obj.mesh");
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Fonts">
    Font testFont = null;
    ImageTexture alphabet = new ImageTexture("assets/cooperBlack.png");
//</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Future code provided by Jim?">

    UnitSquare usq = new UnitSquare();
    vec3 skyColor = new vec3(0.5,0.5,0.5);
    //fbo1 = new Framebuffer(512,512);
    //fbo2 = new Framebuffer(512,512);

    //blurprog = new Program("blurvs.txt","blurfs.txt");
//</editor-fold>

    public GameLoop(long w)
    {
        win = w;
        try {
        testFont = new Font("assets/CooperBlack.fnt");
        } 
        catch (IOException ex) 
        {
        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        prog = new Program("vs.txt","fs.txt");
        animalList.add(new Animal(pigMesh,new vec4(-30,0,0,1), 3.0f));
        animalList.add(new Animal(giraffeMesh,new vec4(0,0,0,1), 3.0f));
        animalList.add(new Animal(zomMesh,new vec4(30,1000,0,1), 0.0f));
        
        
        obstacleList.add(new Obstacle(wallMesh, new vec4(0,-1,-20,1), 0.0f));
        
        pinList.add(new Pin(pinMesh, new vec4(0,-1,-30,1), 3.0f));
        pinList.add(new Pin(pinMesh, new vec4(30,-1,-30,1), 3.0f));
        pinList.add(new Pin(pinMesh, new vec4(-30,-1,-30,1), 3.0f));
        
        cam.lookAt( new vec3(0,2,3), animalList.get(animalSelected).mPos.xyz(), new vec3(0,1,0) );
        cam.mFollowTarget = animalList.get(0);
    }
    public void runLoop()
    {
        prev = (float)(System.nanoTime()*1E-9);
        ev=new JSDL.SDL_Event();
        while(true)
        {
            HandleEvents();
            float now = (float)(System.nanoTime()*1E-9);
            elapsed = now-prev;
            prev=now;
            UpdateAnimals();
            cam.update();
            UpdatePins();
            CullDeadObjects();
            HandleInput();       
            Render();
        }
    }
    private void HandleEvents()
    {
        while(true){
                int rv = SDL_PollEvent(ev);
                if( rv == 0 )
                    break;
                //System.out.println("Event "+ev.type);
                if( ev.type == SDL_QUIT )
                    System.exit(0);
                if( ev.type == SDL_MOUSEMOTION ){
                    //System.out.println("Mouse motion "+ev.motion.x+" "+ev.motion.y+" "+ev.motion.xrel+" "+ev.motion.yrel);
                }
                if( ev.type == SDL_KEYDOWN ){
                    //System.out.println("Key press "+ev.key.keysym.sym+" "+ev.key.keysym.sym);
                    keys.add(ev.key.keysym.sym);
                    console += (char)ev.key.keysym.sym;
                }
                if( ev.type == SDL_KEYUP ){
                    keys.remove(ev.key.keysym.sym);
                }
            }
    }
    private void UpdateAnimals()
    {
        for(Animal a: animalList)
        {
            a.update(elapsed);
        }
    }
    private void UpdatePins()
    {
        for (Pin p : pinList)
        {
            for(Animal a: animalList)
            {
                if(p.checkCollision(a.mPos, a.mRad,a.mMoving))
                {
                    if(a.equals(animalList.get(animalSelected)))
                        animalSelected = 0;
                    animalList.get(animalSelected).mAlive = false;
                }

                p.checkAnimalPosition(a.mPos);
            }
            p.update(elapsed);
        }
    }
    private void CullDeadObjects()
    {
        for(int i = 0; i < pinList.size(); i++)
        {
            if(!pinList.get(i).mAlive)
            {
                pinList.remove(i);
                numHits++;
            }
        }
        for(int i = 0; i < animalList.size(); i++)
        {
            if(!animalList.get(i).mAlive)
                animalList.remove(i);
        }
    }
    private void HandleInput()
    {
        if( keys.contains(SDLK_z))
            {
                animalSelected--;
                if(animalSelected < 0)
                    animalSelected = animalList.size()-1;
                cam.follow(animalList.get(animalSelected),false);
                cam.mFollowTarget = animalList.get(animalSelected);
                if(animalList.get(animalSelected).mMoving)
                {
                    cam.follow(animalList.get(animalSelected), true);
                }
                else
                {
                    cam.mFollowing = false;
                }
                keys.remove(SDLK_z);
            }
            
            if( keys.contains(SDLK_x))
            {
                animalSelected++;
                if(animalSelected > animalList.size()-1)
                    animalSelected = 0;
                cam.follow(animalList.get(animalSelected),false);
                cam.mFollowTarget = animalList.get(animalSelected);
                if(animalList.get(animalSelected).mMoving)
                {
                    cam.follow(animalList.get(animalSelected), true);
                }
                else
                {
                    cam.mFollowing = false;
                }
                keys.remove(SDLK_x);
            }
                
            if( keys.contains(SDLK_w ))
                cam.walk(3f*elapsed);
            if( keys.contains(SDLK_s))
                cam.walk(-3f*elapsed);
            if( keys.contains(SDLK_a))
            {
                if(!cam.mFollowTarget.mMoving)
                {
                    animalList.get(animalSelected).rotate(2 * elapsed);
                    cam.follow(animalList.get(animalSelected),false);
                }
            }
            if( keys.contains(SDLK_d))
            {
                if(!cam.mFollowTarget.mMoving)
                {
                    animalList.get(animalSelected).rotate(-2 * elapsed);
                    cam.follow(animalList.get(animalSelected), false);
                }
            }
            if( keys.contains(SDLK_r))
                cam.tilt(0.4f*elapsed);
            if( keys.contains(SDLK_t))
                cam.tilt(-0.4f*elapsed);
            if( keys.contains(SDLK_SPACE))
            {
                if(!cam.mFollowTarget.mMoving)
                {
                    animalList.get(animalSelected).takeoff();
                    cam.follow(animalList.get(animalSelected), true);
                    numLaunches++;
                }
                keys.remove(SDLK_SPACE);
            }
            if(keys.contains(SDLK_ESCAPE))
            {
                System.exit(0);
            }
            if(keys.contains(SDLK_RETURN))
            {
                System.out.println("Number of launches: " + numLaunches);
                System.out.println("Number of hits: " + numHits);
                keys.remove(SDLK_RETURN);
            }
    }
    private void Render()
    {
        DrawableString scoreText = new DrawableString("Enemies hit: " + numHits, 10, 20, 20, testFont);
        DrawableString launchText = new DrawableString("Launches: " + numLaunches, 10, 40, 20, testFont);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        prog.use();
        prog.setUniform("mode",0.1);
        prog.setUniform("skyColor",skyColor);
        prog.setUniform("lights[0].position",new vec3(cam.eye.x, cam.eye.y, cam.eye.z));
        prog.setUniform("lights[0].color",new vec3(1,1,1));
        for(int i =1;i<8;i++)
        {
            prog.setUniform("lights["+i+"].position",new vec3(0,0,0));
            prog.setUniform("lights["+i+"].color",new vec3(0,0,0));
        }
        cam.draw(prog);
        prog.setUniform("worldMatrix", mul(scaling(new vec3(100,1,100)),translation(new vec3(0,-1.0f,0))));
        planeMesh.draw(prog);
        for(Animal a: animalList)
        {
            a.draw(prog);
        }
        for(Pin p : pinList)
        {
            p.draw(prog);
        }
        for(Obstacle o : obstacleList)
        {
            o.draw(prog);
        }
        //fbo1.unbind();

        //this is also for later...
/*
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        blurprog.use();
        blurprog.setUniform("diffuse_texture",fbo1.texture);
        usq.draw(blurprog);
        blurprog.setUniform("diffuse_texture",dummytex);*/
        prog.setUniform("diffuse_texture",alphabet);
        prog.setUniform("unitSquare", 1.0f);
        scoreText.draw(prog);
        launchText.draw(prog);
        prog.setUniform("unitSquare", 0.0f);
        SDL_GL_SwapWindow(win);
    }
}
