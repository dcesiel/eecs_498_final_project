package Panda.sensors;

import java.io.*;
import lcm.lcm.*;
import java.lang.Math;
import lcmtypes.*;

public class MotorSubscriber implements LCMSubscriber
{
    LCM lcm;

	//pimu_t variables
   	drive_t msg;


    public MotorSubscriber() throws IOException
    {
        this.lcm = new LCM();
        this.lcm.subscribe("MOTOR_MSG", this);
    }

    public void messageReceived(LCM lcm, String channel, LCMDataInputStream ins)
    {
		//System.out.println("Received message on channel " + channel);
		try {
			if (channel.equals ("MOTOR_MSG")) {
                this.msg = new drive_t(ins);
            }

        } catch (IOException ex) {
            System.out.println("Exception: " + ex);
        }
	}

	public drive_t getMessage() {
		while(this.msg == null)
			{System.out.println("waiting");}
		return this.msg;
	}

}
