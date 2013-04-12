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


public class TagTest
{
    static final double KP_Y = 0.013;
    static final double KP_X = 0.01733;
    static final double KP_Z = 0.01733;

    static final double CODE_FLAT = 0.0;
    static final double APRIL_CODE_PIXEL_HEIGHT = 244;
    static final double APRIL_CODE_PIXEL_WIDTH = 350;
    
    static final double CODE_ANGLE_RANGE = 20;
    static final double PIXEL_RANGE = 40;
    static final double PIXEL_RANGE_WIDTH = 55;

    ImageSource is;

    TagFamily tf;
    TagDetector detector;

    // usage: TagTest <imagesource> [tagfamily class]
    public static void main(String args[])
    {
        GetOpt opts  = new GetOpt();
        opts.addBoolean('h',"help",false,"See this help screen");
        opts.addString('u',"url","","Camera url");
        opts.addString('t',"tagfamily","april.tag.Tag36h11","Tag family");

        if (!opts.parse(args)) {
            System.out.println("option error: "+opts.getReason());
        }

        String url = opts.getString("url");
        String tagfamily = opts.getString("tagfamily");

        if (opts.getBoolean("help") || url.isEmpty()){
            System.out.println("Usage:");
            opts.doHelp();
            System.exit(1);
        }

        try {
            ImageSource is = ImageSource.make(url);
            TagFamily tf = (TagFamily) ReflectUtil.createObject(tagfamily);

            TagTest tt = new TagTest(is, tf);

        } catch (IOException ex) {
            System.out.println("Ex: "+ex);
        }
    }

    public TagTest(ImageSource is, TagFamily tf)
    {
        this.is = is;
        this.tf = tf;

        detector = new TagDetector(tf);

        ImageSourceFormat ifmt = is.getCurrentFormat();

        new RunThread().start();
        new RunThread().start();
        new RunThread().start();

    }


    class RunThread extends Thread
    {
        public void run()
        {
            is.start();
            ImageSourceFormat fmt = is.getCurrentFormat();

            detector = new TagDetector(tf);

            //These parameters are set based on April TagDetector documentation
            //and tweaked with a little experimentation

            //The two values below are zero because segDecimate is enabled
            //Both should be 0.7 if segDecimate is false
            detector.segSigma = 0.0;
            detector.sigma = 0.0;
            //minMag can go from 0.001 to 0.01 the higher the number the faster
            //the image processes
            detector.minMag = 0.01;
            detector.maxEdgeCost = 0.52359;
            detector.magThresh = 12000.0;
            detector.thetaThresh = 100.0;
            //Lower values are faster
            detector.WEIGHT_SCALE = 60;
            detector.segDecimate = true;
            detector.debug = false;

            //TODO: Figure out of these are needed
            /*detector.debugSegments  = vw.getBuffer("segments");
            detector.debugQuads     = vw.getBuffer("quads");
            detector.debugSamples   = vw.getBuffer("samples");
            detector.debugLabels    = vw.getBuffer("labels");*/

            MotorPublisher mp = new MotorPublisher();
            MotorSpeed ms = new MotorSpeed();

            FrameData frmd;
            BufferedImage im;
            double angle = 0;
            double prev_angle = 0;
            int empCounter = 0;
            while (true) {
                frmd = null;
                for (int i = 0; i < 3; i++){
                    frmd = is.getFrame();
                }
                if (frmd == null)
                    continue;

                im = ImageConvert.convertToImage(frmd);

                //Not sure what this does but this is the number set in the
                //test program and it works so I'm going to use it
                tf.setErrorRecoveryBits(1);


                //TODO: Use this detection array to get location of tags
                //Import TagDetection class fields include:
                //cxy: (center of tag in pixel in tag coordinates)
                //rotation: (How many 90 degree rotations to align with code)
                ArrayList<TagDetection> detections = detector.process(im, new double[] {im.getWidth()/2.0, im.getHeight()/2.0});
                
                //If can't find a tag stop the robot before it kills someone
                if (detections.isEmpty()){
                    empCounter++;
                    if (empCounter > 3){
                        ms.frontMotor = 0;
                        ms.backMotor = 0;
                        ms.rightMotor = 0;
                        ms.leftMotor = 0;
                        mp.publish(ms);
                        mp.publish(ms);
                        empCounter = 0;
                    }
                }

                for (TagDetection d : detections) {
                    double yPix = d.cxy[1];
                    double xPix = d.cxy[0];
                    //5.2 inches to meters
                    double tagsize_m = 0.132;
                    //Need to figure out the proper focal length
                    double f = 485.6;
                    double M[][] = CameraUtil.homographyToPose(f, f, im.getWidth()/2, im.getHeight()/2, d.homography);
                    prev_angle = angle;
                    angle = -Math.asin(M[2][0]) * 100;
                    System.out.print("Angle: " + angle);
                    
                    double errorY = 0;
                    //Check to see if robot is in decent range
                    if ((yPix < (APRIL_CODE_PIXEL_HEIGHT - PIXEL_RANGE)) || 
                        (yPix > (APRIL_CODE_PIXEL_HEIGHT + PIXEL_RANGE))){
                        errorY = yPix - im.getHeight()/2;
                    }
                    else{
                        errorY = 0;
                    }

                    ms.rightMotor = KP_Y * errorY;
                    ms.leftMotor = KP_Y * errorY;

                    double errorX = 0;
                    //Check to see if robot is in decent range
                    if ((xPix < (APRIL_CODE_PIXEL_WIDTH - PIXEL_RANGE_WIDTH)) || 
                        (xPix > (APRIL_CODE_PIXEL_WIDTH + PIXEL_RANGE_WIDTH))){
                        errorX = xPix - im.getWidth()/2;
                    }
                    else{
                        errorX = 0;
                    }

                    double errorZ = 0;
                    //Check to see if robot is in decent range
                    if ((angle > 100) || (angle < -100)){
                        System.out.println("Invalid Angle");
                        errorZ = 0;
                    }
                    else{
                        if ((angle < (CODE_FLAT - CODE_ANGLE_RANGE)) || 
                            (angle > (CODE_FLAT + CODE_ANGLE_RANGE))){
                            errorZ = angle;
                        }
                        else{
                            errorZ = 0;
                        }
                    }

                    ms.backMotor = KP_X * errorX + KP_Z * errorZ;
                    ms.frontMotor = KP_X * errorX - KP_Z * errorZ;

                    
                    //Check to max sure ms values are within range
                    if (ms.rightMotor > 1.0)
                        ms.rightMotor = 1.0;
                    if (ms.rightMotor < -1.0)
                        ms.rightMotor = -1.0;
                    if (ms.leftMotor > 1.0)
                        ms.leftMotor = 1.0;
                    if (ms.leftMotor < -1.0)
                        ms.leftMotor = -1.0;
                    if (ms.frontMotor > 1.0)
                        ms.frontMotor = 1.0;
                    if (ms.frontMotor < -1.0)
                        ms.frontMotor = -1.0;
                    if (ms.backMotor > 1.0)
                        ms.backMotor = 1.0;
                    if (ms.backMotor < -1.0)
                        ms.backMotor = -1.0;

                    System.out.println("Error: " + errorY + "  Right: " + ms.rightMotor + "  Left: " + ms.leftMotor); 
                    mp.publish(ms);
                    mp.publish(ms);

                }
            }
        }
    }
}
