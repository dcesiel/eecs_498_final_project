import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.image.*;
import javax.swing.*;
import java.math.*;

import april.jcam.*;
import april.util.*;
import april.jmat.*;
import april.image.*;


public class SwingLedTracker
{

	static int group = 0;
    static double DEFAULT_GRAY_THRESHOLD = 100000F;
    static ImageSource is;
	static ImageSourceFormat fmt;
	static BufferedImage im; 
	static int comp, current;
	static int set[][];
	static int OFF = 1;
	static double[][] stats;
	static boolean displayColors;

    JFrame jf = new JFrame("LED Tracker Demo");
    JImage jim = new JImage();

    ParameterGUI pg = new ParameterGUI();

    static float RANGE = .2F;
	static float RANGE2 = .2F;

	static int SHIFT = 0;


	static int DEFAULT_SHOWN = 0;
	static int STEP = 0;

    public SwingLedTracker(ImageSource _is)
    {
        is = _is;

        // Determine which slider values we want
		pg.addDoubleSlider("Step","Step",0, 10, DEFAULT_SHOWN);
        pg.addDoubleSlider("Step 1 Range","Range",0, 1000000,DEFAULT_GRAY_THRESHOLD);
		pg.addDoubleSlider("Step 2 Range","Range",0, 1000000,DEFAULT_GRAY_THRESHOLD);

		
        jim.setFit(true);

        // Setup window layout
        jf.setLayout(new BorderLayout());
        jf.add(jim, BorderLayout.CENTER);
        jf.add(pg, BorderLayout.SOUTH);
        jf.setSize(1024, 768);
        jf.setVisible(true);
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		displayColors = true;
    }

    static boolean sameColor(int compare, int expected){

		double maxR = 1.0 + RANGE;
		double minR = 1.0 - RANGE;

		int exp, comp;

		float total = 0;
		
		for(int i = 0; i < 3; i++){
			comp = compare & 0xFF;
			exp = expected & 0xFF;

			total += Math.pow(comp - exp, 4);

			comp = compare >> 8;
			exp = expected >> 8;
		}

		
		if(total > RANGE){
			return false;
		}
		
		return true;
   	}


	static void expand(int x, int y){

		if(x < 0 | x >= fmt.width)
			return;

		if(y < 0 | y >= fmt.height)
			return;

		if(set[x][y] != 0)
			return;

		comp = im.getRGB(x,y);
		
		if(sameColor(current, comp)){
			set[x][y] = group;
			
			stats[group][0] ++;
			stats[group][1] += (comp & 0xFF);
			stats[group][2] += (comp >> 8  & 0xFF);
			stats[group][3] += (comp >> 16 & 0xFF);

			stats[group][4] += x;
			stats[group][5] += y;

			//x min
			if(stats[group][6] > x)
				stats[group][6] = x;
			
			//x max
			if(stats[group][7] < x)
				stats[group][7] = x;
	
			//y min
			if(stats[group][8] > y)
				stats[group][8] = y;
	
			//y max
			if(stats[group][9] < y)
				stats[group][9] = y;

			expand(x + OFF, y);
			expand(x - OFF, y);
			expand(x, y + OFF);
			expand(x, y - OFF);
		}
	}

	static void box(int bounds[], int color){
	
		if(bounds[0] < 0 | bounds[2] >= fmt.width)
			return;

		if(bounds[1] < 0 | bounds[3] >= fmt.height)
			return;
			
	   	// draw the horizontal lines
        for (int y : new int[]{bounds[1], bounds[3]})
           	for (int x = bounds[0]; x <=bounds[2]; x++) {
               	im.setRGB(x,y, color); //Go Blue!
           	}

       	// draw the horizontal lines
        for (int x : new int[]{bounds[0], bounds[2]})
           	for (int y = bounds[1]; y <=bounds[3]; y++) {
               	im.setRGB(x,y, color); //Go Blue!
           	}
	}

	static int[] calcMean(int group){

		int count = (int)stats[group][0];

		int meanColor = (int) 0xFF000000 |
						(int) (stats[group][3] / count) & 0xFF << 16 |
						(int) (stats[group][2] / count) & 0xFF << 8  |
						(int) (stats[group][1] / count) & 0xFF;

		int meanX = (int) stats[group][4] / count;

		int meanY = (int) stats[group][5] / count;

		int means[] = { meanColor, meanX, meanY };
		
		return means;
	}

	static int meanColor(int g){

		int count = (int)stats[g][0];

		int meanColor = 0xFF000000 |
						((int)(stats[g][3] / count) & 0xFF) << 16 |
						((int)(stats[g][2] / count) & 0xFF) << 8  |
						((int)(stats[g][1] / count) & 0xFF);

		return meanColor;
	}


	static void printSets(){

		for(int i = 1; i <= group; i++ ){

			int means[] = calcMean(i);

			int mBounds[] = {means[1] - 1, means[2] - 1, 
			   		  		 means[1] + 1, means[2] + 1 }; 

			box( mBounds, 0xFFFF0000 );

		
			if(stats[i][0] > 50){
				int gBounds[] = { (int) stats[i][6], (int) stats[i][8], 
						   		  (int) stats[i][7], (int) stats[i][9] };

				box( gBounds, 0xFF00FF00 );
			}
		}
	}

	static float[] getHSV(int c)
	{
    	int red = c & 0xff;
    	int green = (c >> 8) & 0xff;
    	int blue = (c >> 16) & 0xff;
    	float[] hsv = new float[3];
    	Color.RGBtoHSB(red, green, blue, hsv);
    	return hsv;
	}
		

	static void print(){

		for(int y = 0; y < fmt.height; y ++){
			for(int x = 0; x < fmt.width; x++){
				int group = set[x][y];
				if(group != 0)
					im.setRGB(x, y, group * 10000);
			}
		}
	}
					
				
		

    public void run()
    {
        is.start();
        fmt = is.getCurrentFormat();

        // Initialize visualization environment now that we know the image dimensions

        while(true) {
            // read a frame

			byte buf[] = null;
			for(int i = 0; i < 2; i++){			
            	buf = is.getFrame().data;
            	if (buf == null)
                	continue;
			}

            // Grab the image, and convert it to gray scale immediately
            im = ImageConvert.convertToImage(fmt.format, fmt.width, fmt.height, buf);
			
			float[] gaus = SigProc.makeGaussianFilter(.8, 5);
			FloatImage bim = new FloatImage(im, 0);
			FloatImage gim = new FloatImage(im, 8);
			FloatImage rim = new FloatImage(im, 16);

			bim = bim.filterFactoredCentered(gaus, gaus);
			gim = gim.filterFactoredCentered(gaus, gaus);
			rim = rim.filterFactoredCentered(gaus, gaus);

            im = FloatImage.makeImage(rim, gim, bim);


			set = new int[fmt.width][fmt.height];

			int size = (fmt.height/(10)) * fmt.width/(10);
			stats = new double[size][10];
				
			for(int y = 0; y < fmt.height; y += OFF + 10){
				for(int x = 0; x < fmt.width; x+= OFF + 10){

					if(set[x][y] == 0){
						group++;
						//min values
						stats[group][6] = 10000;
						stats[group][8] = 10000;

						current = im.getRGB(x, y);
						expand(x, y);
					}
				}
			}

			if(STEP == 3){
				printSets();
			}


			print();

 			group = 0;
            jim.setImage(im);
			RANGE = (float) pg.gd("Step 1 Range");
			RANGE2 = (float) pg.gd("Step 2 Range");
			STEP = (int) pg.gd("Step");
        }
    }

    public static void main(String args[]) throws IOException
    {
        ArrayList<String> urls = ImageSource.getCameraURLs();

        String url = null;
        if (urls.size()==1)
            url = urls.get(0);

        if (args.length > 0)
            url = args[0];

        if (url == null) {
            System.out.printf("Cameras found:\n");
            for (String u : urls)
                System.out.printf("  %s\n", u);
            System.out.printf("Please specify one on the command line.\n");
            return;
        }

        ImageSource is = ImageSource.make(url);
        new SwingLedTracker(is).run();
    }
}
