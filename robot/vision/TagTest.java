import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

import april.jmat.*;
import april.jmat.geom.*;

import april.jcam.*;

import april.util.*;

public class TagTest
{
    JFrame jf;

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

        //TODO: Eliminate when done testing/////////////////////////////////////
        jf = new JFrame("TagTest");
        jf.setLayout(new BorderLayout());

        jf.setSize(800,600);
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jf.setVisible(true);
        /////////////////////////////////////////////////////////////////////////

        ImageSourceFormat ifmt = is.getCurrentFormat();

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
            //Both should be 0.8 if segDecimate is false
            detector.segSigma = 0.0;
            detector.sigma = 0.0;
            //minMag can go from 0.001 to 0.01 the higher the number the faster
            //the image processes
            detector.minMag = 0.01;
            detector.maxEdgeCost = 0.52359;
            detector.magThresh = 12000.0;
            detector.thetaThresh = 100.0;
            //Lower values are faster
            detector.WEIGHT_SCALE = 30;
            detector.segDecimate = true;
            detector.debug = false;

            //TODO: Figure out of these are needed
            /*detector.debugSegments  = vw.getBuffer("segments");
            detector.debugQuads     = vw.getBuffer("quads");
            detector.debugSamples   = vw.getBuffer("samples");
            detector.debugLabels    = vw.getBuffer("labels");*/

            while (true) {
                FrameData frmd = is.getFrame();
                if (frmd == null)
                    continue;

                BufferedImage im = ImageConvert.convertToImage(frmd);

                //Not sure what this does but this is the number set in the
                //test program and it works so I'm going to use it
                tf.setErrorRecoveryBits(1);

                //TODO: Move this into a debug check (measures execution time)
                Tic tic = new Tic();

                //TODO: Use this detection array to get location of tags
                //Import TagDetection class fields include:
                //cxy: (center of tag in pixel in tag coordinates)
                //rotation: (How many 90 degree rotations to align with code)
                ArrayList<TagDetection> detections = detector.process(im, new double[] {im.getWidth()/2.0, im.getHeight()/2.0});
                double dt = tic.toc();

                if (detector.debugInput!=null)
                    vbInput.addBack(new VisDepthTest(false, new VisLighting(false, new VzImage(detector.debugInput, VzImage.FLIP))));
                vbInput.swap();

                if (detector.debugSegmentation!=null)
                    vbSegmentation.addBack(new VisDepthTest(false, new VisLighting(false, new VzImage(detector.debugSegmentation, VzImage.FLIP))));
                vbSegmentation.swap();


                vbOriginal.addBack(new VisDepthTest(false, new VisLighting(false, new VzImage(im, VzImage.FLIP))));
                vbOriginal.swap();

                if (detector.debugTheta != null)
                    vbThetas.addBack(new VisDepthTest(false, new VisLighting(false, new VzImage(detector.debugTheta, VzImage.FLIP))));
                vbThetas.swap();

                if (detector.debugMag != null)
                    vbMag.addBack(new VisDepthTest(false, new VisLighting(false, new VzImage(detector.debugMag, VzImage.FLIP))));
                vbMag.swap();

                vbClock.addBack(new VisPixCoords(VisPixCoords.ORIGIN.BOTTOM_RIGHT,
                                                        new VzText(VzText.ANCHOR.BOTTOM_RIGHT,
                                                                    String.format("<<cyan>>%8.2f ms", dt*1000))));
                vbClock.swap();

                for (TagDetection d : detections) {
                    double p0[] = d.interpolate(-1,-1);
                    double p1[] = d.interpolate(1,-1);
                    double p2[] = d.interpolate(1,1);
                    double p3[] = d.interpolate(-1,1);

                    double ymax = Math.max(Math.max(p0[1], p1[1]), Math.max(p2[1], p3[1]));

                    vbDetections.addBack(new VisChain(LinAlg.translate(0, im.getHeight(), 0),
                                                      LinAlg.scale(1, -1, 1),
                                                      new VzLines(new VisVertexData(p0, p1, p2, p3, p0),
                                                                  VzLines.LINE_STRIP,
                                                                  new VzLines.Style(Color.blue, 4)),
                                                      new VzLines(new VisVertexData(p0,p1),
                                                                  VzLines.LINE_STRIP,
                                                                  new VzLines.Style(Color.green, 4)),
                                                      new VzLines(new VisVertexData(p0, p3),
                                                                  VzLines.LINE_STRIP,
                                                                  new VzLines.Style(Color.red, 4)),
                                                      new VisChain(LinAlg.translate(d.cxy[0], ymax + 20, 0), //LinAlg.translate(d.cxy[0],d.cxy[1],0),
                                                                   LinAlg.scale(1, -1, 1),
                                                                   LinAlg.scale(.25, .25, .25),
                                                                   new VzText(VzText.ANCHOR.CENTER,
                                                                              String.format("<<sansserif-48,center,yellow,dropshadow=#88000000>>id %3d\n(err=%d)\n", d.id, d.hammingDistance)))));

                    // You need to adjust the tag size (measured
                    // across the whole tag in meters and the focal
                    // length.
                    double tagsize_m = 0.216;
                    double f = 485.6;
                    double aspect = 752.0 / 480.0;
//                    double M[][] = CameraUtil.homographyToPose(f, f, tagsize_m, d.homography);
                    double M[][] = CameraUtil.homographyToPose(f, f, im.getWidth()/2, im.getHeight()/2, d.homography);
                    M = CameraUtil.scalePose(M, 2.0, tagsize_m);

                    BufferedImage tfimg = tf.makeImage(d.id);
                    double vertices[][] = {{ -tagsize_m/2, -tagsize_m/2, 0},
                                           { tagsize_m/2, -tagsize_m/2, 0},
                                           { tagsize_m/2,  tagsize_m/2, 0},
                                           { -tagsize_m/2,  tagsize_m/2, 0}};


                    // same order as in vertices, but remember y flip.
                    double texcoords [][] = { { 0, 1},
                                              { 1, 1},
                                              { 1, 0},
                                              { 0, 0 } };

                    vbTag3D.addBack(new VisChain(LinAlg.rotateX(Math.PI/2),
                                                 M,
                                                 new VzImage(new VisTexture(tfimg, VisTexture.NO_MIN_FILTER),
                                                             vertices, texcoords, null)));
                }

                vbTag3D.addBack(new VisChain(LinAlg.rotateX(Math.PI/2),
                                             new VzAxes()));
                vbTag3D.addBack(new VisChain(LinAlg.rotateZ(Math.PI/2),
                                             LinAlg.scale(.25, .25, .25),
                                             new VzCamera()));
                vbTag3D.swap();

                vbDetections.swap();
            }
        }
    }
}
