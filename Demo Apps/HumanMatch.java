import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.image.*;
import javax.swing.*;
import java.lang.*;
import java.awt.event.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Vector;


import lcm.lcm.*;
import april.jcam.*;
import april.util.*;
import april.jmat.*;
import april.image.*;



public class HumanMatch implements MouseListener
{
	//Frame Variables
    JFrame jf = new JFrame("Human Detector");
    JImage jim = new JImage();
    ParameterGUI pg = new ParameterGUI();

	//Image Variables
    static ImageSource is;
	static ImageSourceFormat fmt;
	static BufferedImage im; 
	static int set[][];
	static int OFF = 3;

	//Search Variables
	static boolean getShirt, getPants, first;
	static int SHIRT, PANTS;
	static int X1, X2, Y1, Y2;

	static int stats[][][];
	static int subset1, subset2;
	static double DEFAULT_THRESHOLD = .0005F;
    static float RANGE = 1;

	//Run variables
	boolean start, exit;

    
    public HumanMatch(ImageSource _is)
    {
        is = _is;

        // Determine which slider values we want
		pg.addDoubleSlider("Step", "Step" , 0 , 1, DEFAULT_THRESHOLD);
		pg.addButtons("shirt", "Shirt Color");
        pg.addButtons("pants", "Pants Color");
		pg.addButtons("start", "Start");
        pg.addButtons("exit", "Exit");


        jim.setFit(true);

        // Setup window layout
        jf.setLayout(new BorderLayout());
        jf.add(jim, BorderLayout.CENTER);
        jf.add(pg, BorderLayout.SOUTH);
        jf.setSize(640, 626);
        jf.setVisible(true);
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jim.addMouseListener(this);

		SHIRT = 0xFFFFFFFF;
		PANTS = 0xFFFFFFFF;

		getShirt = false;
		getPants = true;
		first = true;	
		exit = false;
		start = false;	
    }

	public void mouseReleased(MouseEvent me) {}

    public void mouseClicked(MouseEvent e) {}

    public void mouseEntered(MouseEvent e) {}

    public void mouseExited(MouseEvent e) {}

    public void mousePressed(MouseEvent me) {



        if(getShirt & first){
            X1 = me.getX();
            Y1 = me.getY();

            first = false;
        }

        else if(getShirt & !first){
			X2 = me.getX();
            Y2 = me.getY();

            getShirt = false;
			first = true;

            setColor( 0 );

			printTemplates();
			jim.setImage(im);

        }

        else if(getPants & first){
            X1 = me.getX();
            Y1 = me.getY();

            first = false;
        }
        else if(getPants & !first){
            X2 = me.getX();
            Y2 = me.getY();

            getPants = false;
			first = true;

            setColor( 1 );   

			printTemplates();
			jim.setImage(im);
     
		}
    }

	static void setColor(int type){

		int R = 0, G = 0, B = 0, comp;

		for(int y = Y1; y < Y2; y++){
			for( int x = X1; x < X2; x++){

				comp = im.getRGB(x, y);

				R += (comp & 0xFF);
				G += (comp >> 8  & 0xFF);
				B += (comp >> 16 & 0xFF);
			}
		}
		
		int count = (Y2 - Y1) * (X2 - X1);

		if(count < 1)
			return;

		int meanColor = 0xFF000000 |
						((B / count) & 0xFF) << 16 |
						((G / count) & 0xFF) << 8  |
						((R / count) & 0xFF);


		if(type == 0)
			SHIRT = meanColor;
		if(type == 1)
			PANTS = meanColor;

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

	static float[] getHSV(int c)
	{
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
	
					//System.out.println(stats[group][i][0]);
					int means[] = calcMean(group, i);

					int mBounds[] = {means[0] - 1, means[1] - 1, 
				   			  		 means[0] + 1, means[1] + 1 }; 


					box( mBounds, 0xFFFF0000 );
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

				if(expand(x, y, 2))
					subset2++;
		
			}
		}
	}


	static void printTemplates(){
		for(int y = 0; y < 10; y ++){
			for(int x = 0; x < 10; x++){
		
				im.setRGB(x, y, SHIRT);
				im.setRGB(x + 10, y, PANTS);
			}
		}
	}

	


	static void print(){

		for(int y = 0; y < fmt.height; y ++){
			for(int x = 0; x < fmt.width; x++){
				int group = set[x][y];
				if(group != 0){

					if(x > 20 | y > 10)
						im.setRGB(x, y, group * 10000);
				}
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
				


    public void run()
    {
        is.start();
        fmt = is.getCurrentFormat();

        pg.addListener(new ParameterListener() {
            public void parameterChanged(ParameterGUI pg, String name)
            {
                if (name.equals("shirt")){
					getShirt = true;
					first = true;

					getNewFrame();
					printTemplates();
					jim.setImage(im);

                }

                if (name.equals("pants")){
                    getPants = true;
					first = true;

					getNewFrame();
					printTemplates();
					jim.setImage(im);
                }

                if (name.equals("start"))
                    start = true;
      
                if (name.equals("exit"))
                    exit = true;
            }
        });

		int i = 0;
		while(true){

			exit = false;
			start = false;

			while(!getShirt){
				getNewFrame();
				printTemplates();
				jim.setImage(im);
			}

			while(!start)
				System.out.println("waiting to start");	

			System.out.println("Shirt Color " + SHIRT);
			System.out.println("Pants Color " + PANTS);

			while(!exit){

				getNewFrame();

				searchImage();
			
				print();

				//System.out.println("GROUP 1 !!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
				printSets(1, subset1);

				//System.out.println("GROUP 2 !!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
				printSets(2, subset2);

				//printTemplates();

            	jim.setImage(im);

				RANGE = (float) pg.gd("Step");

				i++;
				//System.out.println("DONE LOOP " + i);
			}
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
        new HumanMatch(is).run();
    }
}
