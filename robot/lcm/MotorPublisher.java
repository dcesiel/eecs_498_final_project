package robot.lcm;
import java.awt.*;
import java.io.*;
import static java.lang.System.out;

import javax.swing.*;

import lcm.lcm.*;
import april.jmat.*;
import april.util.*;
import april.vis.*;
import lcm.lcmtypes.*;
import april.*;

public class MotorPublisher
{
    public class MotorSpeed
    {
        //All values are between -1.0 and 1.0 with 0 = stop
        double rightMotor;
        double leftMotor;
        double frontMotor;
        double backMotor;
    }

    LCM lcm;

    public MotorPublisher(){
        try{
            lcm = new LCM();
        } catch (Exception e){

        }
    }

    void publish(MotorSpeed motorSpeed){
	    long now = TimeUtil.utime();
        drive_t cmd = new drive_t();
        cmd.timestamp = now;
        cmd.front_motor = motorSpeed.frontMotor;
        cmd.back_motor = motorSpeed.backMotor;
        cmd.right_motor = motorSpeed.rightMotor;
        cmd.left_motor = motorSpeed.leftMotor;

        lcm.publish("MOTOR", cmd);
    }

}