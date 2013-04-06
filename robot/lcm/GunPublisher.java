package robot.lcm;

import java.awt.*;
import java.io.*;
import static java.lang.System.out;

import javax.swing.*;

import lcm.lcm.*;
import april.jmat.*;
import april.util.*;
import april.vis.*;
import lcmtypes.*;
import april.*;

public class GunPublisher
{
    LCM lcm;

    public GunPublisher(){
        try{
            lcm = new LCM();
        } catch (Exception e){

        }
    }

    void publish(boolean fire){
        long now = TimeUtil.utime();
        gun_t cmd = new gun_t();
        cmd.timestamp = now;
        cmd.fire = fire;
        lcm.publish("GUN", cmd);
    }

}
