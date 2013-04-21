import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.image.*;
import javax.swing.*;
import java.lang.*;
import java.awt.event.*;

import lcm.lcm.*;
import april.jcam.*;
import april.util.*;
import april.jmat.*;
import april.image.*;



public class HumanMatchBot{

    //Image Variables
    static ImageSource is;
    static ImageSourceFormat fmt;
    static BufferedImage im; 
    static int set[][];
    static int OFF = 3;

    //Search Variables
    static int SHIRT, PANTS;
    static int stats[][][];
    static int subset1, subset2;
    static float RANGE = .0005F;

    //Targeting Variables
    static int goodFrames, badFrames, found;
    static int TargetX;
    static boolean target;
    static int wait;
    
    
    public HumanMatchBot(ImageSource _is){
        is = _is;
    	wait = 0;
    }


    static float[] getHSV(int c){

        int red = c & 0xff;
        int green = (c >> 8) & 0xff;
        int blue = (c >> 16) & 0xff;
        float[] hsv = new float[3];
        Color.RGBtoHSB(red, green, blue, hsv);

        return hsv;
    }

    static boolean colorMatch(int compare, int expected){

    	int exp, comp;
    	float total = 0;

    	float hsvC[] = getHSV(compare);
    	float hsvE[] = getHSV(expected);
    	
    	for(int i = 0; i < 1; i++){

    		total += Math.pow(hsvC[i] - hsvE[i], 2);
    	}

    	if(total > RANGE){
    		return false;
    	}
    	
    	return true;
       }




    static int[] calcMean(int group, int sub){

    	int count = (int)stats[group][sub][0];

    	if(count == 0)
			count = 1;

		int meanX = (int) stats[group][sub][1] / count;

		int meanY = (int) stats[group][sub][2] / count;

		int means[] = { meanX, meanY };
		
		return means;
	}

	static void printSets(int group, int sub){

		group --;
		for(int i = 0; i < sub; i++ ){

			//System.out.println("Count " + stats[group][i][0]);

			if(stats[group][i][0] > 2000/OFF){

				//System.out.println("Count " + stats[group][i][0]);

				int area = (stats[group][i][4] - stats[group][i][3]) *
							(stats[group][i][6] - stats[group][i][5]);

				//System.out.println("Area " + .5 * area);

				
				if( stats[group][i][0] > (.3/(OFF* OFF)) * area){

					stats[group][i][7] = 1;
	
					int means[] = calcMean(group, i);
			
					TargetX = means[0];

					System.out.println(means[0] + "  " + means[1]);

					found++;

				}
			}
		}
	}



	static boolean expand(int x, int y, int group){

		if(x < 0 | x >= fmt.width)
			return false;

		if(y < 0 | y >= fmt.height)
			return false;

		if(set[x][y] != 0)
			return false;


		int comp = im.getRGB(x,y);

		int sub = 0, current = 0;
		if(group == 1){
			sub = subset1;
			current = SHIRT;
		}
	
		if(group == 2){
			sub = subset2;
			current = PANTS;
		}

		
		if(colorMatch(current, comp)){


			set[x][y] = group;
			
			group--;

			stats[group][sub][0] ++;
			stats[group][sub][1] += x;
			stats[group][sub][2] += y;

			//x min
			if(stats[group][sub][3] > x)
				stats[group][sub][3] = x;
			
			//x max
			if(stats[group][sub][4] < x)
				stats[group][sub][4] = x;
	
			//y min
			if(stats[group][sub][5] > y)
				stats[group][sub][5] = y;
	
			//y max
			if(stats[group][sub][6] < y)
				stats[group][sub][6] = y;

			expand(x + OFF, y, group + 1);
			expand(x - OFF, y, group + 1);
			expand(x, y + OFF, group + 1);
			expand(x, y - OFF, group + 1);

			return true;
		}
		
		return false;
	}


	static void searchImage(){

		int current, group;
		set = new int[fmt.width][fmt.height];
		stats = new int[3][1000][8];

		
		//min values
		for(int i = 0; i < 2; i++ ){
			for(int j = 0; j < 1000; j++){
				stats[i][j][3] = 10000;
				stats[i][j][5] = 10000;
			}
		}

		subset1 = 0;
		subset2 = 0;
		
		for(int y = 0; y < fmt.height; y += 60){
			for(int x = 0; x < fmt.width; x += 60){

				if(expand(x, y, 1))
					subset1++;

				//if(expand(x, y, 2))
				//	subset2++;	
			}
		}
	}


	public void getNewFrame(){

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

	}					
	
	static void fireOn(){
		
		int center = fmt.width / 2;

		TargetX -= center;

		if(TargetX < 20 & TargetX > -20){
			System.out.println("FIRE!!!!!!");

			target = false;
			goodFrames = 0;
			badFrames = 0;
			wait = 100;

			return;
		}

		System.out.println("DAVID PID");
	}

		
			


    public void run()
    {
        is.start();
        fmt = is.getCurrentFormat();

		while(true){

			found = 0;

			getNewFrame();

			searchImage();

			printSets(1, subset1);

			if( wait < 0){
				if(badFrames > 2){
					goodFrames = 0;
					target = false;
				}

				if(found == 1 & ! target){
					goodFrames++;
					badFrames = 0;
				}

				if(found != 1)
					badFrames++;

				if(goodFrames > 10){
					target = true;
					goodFrames = 0;
				}

				if(target)		
					fireOn();
			}
			wait--;
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

		if(args.length > 1)
			SHIRT = Integer.parseInt(args[1]);

		if(args.length > 2)
			PANTS = Integer.parseInt(args[2]);

        if (url == null) {
            System.out.printf("Cameras found:\n");
            for (String u : urls)
                System.out.printf("  %s\n", u);
            System.out.printf("Please specify one on the command line.\n");
            return;
        }

        ImageSource is = ImageSource.make(url);
        new HumanMatchBot(is).run();
    }
}
