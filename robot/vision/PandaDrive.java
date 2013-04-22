package robot.vision;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import java.lang.Math;

import april.jmat.*;
import april.jmat.geom.*;

import april.jcam.*;

import april.util.*;
import april.tag.*;
import robot.lcm.*;


public class Drive 
{
    static final boolean DEBUG = true;

    static final double KP_Y = 0.013;
    static final double KP_X = 0.01733;
    static final double KP_Z = 0.01783; //0.01743

    static final double STOP = 0.0;
    
	LCM lcm;
    MotorPublisher mp;
    MotorSpeed ms;

    public Drive(){
        try{
			// Get an LCM Object
            lcm = LCM.getSingleton();

            mp = new MotorPublisher();
            ms = new MotorSpeed(); 
		}
		catch(Throwable t) {
			System.out.println("Error: Exception thrown");
		}
    }

    public void Lock(){

    }

    public void Unlock(){

    }


    public void Stop(){
        ms.frontMotor = STOP;
        ms.backMotor = STOP;
        ms.rightMotor = STOP;
        ms.leftMotor = STOP;
        mp.publish(ms);
    }

    public void Drive(double Xprop, double Yprop){
        
    }

}
