import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.*;
import java.util.ArrayList;


public class sim_cache {
	private static class MyCustomFormatter extends Formatter {
		 
        @Override
        public String format(LogRecord record) {
            StringBuffer sb = new StringBuffer();
            sb.append(record.getMessage());
            sb.append("\n");
            return sb.toString();
        }
    }
	
	public static void writeBack(Cache cache, String address, int lineNum, Cache[] caches) {
		if(cache != null && cache.getSize() > 0)	
			cache.write(address, lineNum, caches);
	}
	
	public static ArrayList<String> preLoadAddresses(String fpath) {
		// if replacement is optimal, pre-load
        ArrayList<String> addresses = new ArrayList<String>();

        try {
	    	File f = new File(fpath);
	    	@SuppressWarnings("resource")
			BufferedReader b = new BufferedReader(new FileReader(f));
	        String readLine = "";
	        
	        while ((readLine = b.readLine()) != null) {
	        	String[] arrStr = readLine.split(" ", 2);
	        	addresses.add(arrStr[1]);
	        }
		} catch (IOException e) {
            e.printStackTrace();
        }
		return addresses;
	}
	
    public static void main(String[] args) {
    	/* Main:
    	 * input is from user command only 
    	 * 
    	 * */
    	
    	if(args.length < 8) {
    		System.out.println("At least 8 inputs are required [<BLOCKSIZE> <L1_SIZE> <L1_ASSOC> <L2_SIZE> <L2_ASSOC> <REPLACEMENT_POLICY> <INCLUSION_PROPERTY> <trace_file>]");
    		return;
    	}
    	
    	// logger
    	boolean verbal = false;
    	Logger logger = null;
    	if(verbal) {
	    	logger = Logger.getLogger(sim_cache.class.getName());
	    	FileHandler fh;
	    	try {
	    		String fpath = "debug.txt";
	    		fh = new FileHandler(fpath);
	    		logger.addHandler(fh);
	    		fh.setFormatter(new MyCustomFormatter());
	    		logger.setLevel(Level.SEVERE);
	
	    		logger.severe("===== Simulator configuration =====");
	    	} catch (SecurityException e) {
	    		e.printStackTrace();
	    	} catch (IOException e) {
	    		e.printStackTrace();
	    	}
    	}

    	System.out.println("===== Simulator configuration =====");

    	// set caches from user input
    	int num_cache = (args.length - 4) / 2;
    	Cache[] caches = new Cache[num_cache];
    	int blocksize = Integer.parseInt(args[0]);
    	int size, assoc;
    	
    	boolean inclusive;
    	if(args[args.length-2].equals("inclusive"))
    		inclusive = true;
    	else
    		inclusive = false;

    	String replacement = args[args.length-3];
    	String fpath = args[args.length-1];
    	String trace_file = fpath.substring(fpath.lastIndexOf("/")+1);

    	// if replacement is optimal, the file is loaded in advance
    	ArrayList<String> addresses = null;
		if(replacement.equals("optimal")) {
			addresses = preLoadAddresses(fpath);
		}

		// set caches
    	int cnt = 0;
    	for(int i=1; i<args.length-4; i+=2) {
    		size = Integer.parseInt(args[i]);
    		assoc = Integer.parseInt(args[i+1]);
    		caches[cnt] = new Cache(blocksize, size, assoc, String.format("L%d", cnt+1), replacement, inclusive, addresses, logger, verbal);
    		cnt++;
    	}
    	
		// header of log
		if(verbal) {
			logger.severe(String.format("BLOCKSIZE:             %d", blocksize));
			for(int i=0; i<num_cache; i++) {
				logger.severe(String.format("L%d_SIZE:               %d", i+1, caches[i].getSize()));
				logger.severe(String.format("L%d_ASSOC:              %d", i+1, caches[i].getSetAssoc()));			
			}
			logger.severe(String.format("REPLACEMENT POLICY:    %s", replacement));
			logger.severe(String.format("INCLUSION PROPERTY:    %s", (inclusive?"inclusive":"non-inclusive")));
			logger.severe(String.format("trace_file:            %s", trace_file));
		}
		else {
			System.out.println(String.format("BLOCKSIZE:             %d", blocksize));
			for(int i=0; i<num_cache; i++) {
				System.out.println(String.format("L%d_SIZE:               %d", i+1, caches[i].getSize()));
				System.out.println(String.format("L%d_ASSOC:              %d", i+1, caches[i].getSetAssoc()));			
			}
			System.out.println(String.format("REPLACEMENT POLICY:    %s", replacement));
			System.out.println(String.format("INCLUSION PROPERTY:    %s", (inclusive?"inclusive":"non-inclusive")));
			System.out.println(String.format("trace_file:            %s", trace_file));			
		}
		
		// simulation starts
    	try {
	    	File f = new File(fpath);
	    	@SuppressWarnings("resource")
			BufferedReader b = new BufferedReader(new FileReader(f));
	        String readLine = "";
	        cnt = 1;
	        
	        while ((readLine = b.readLine()) != null) {
	        	String[] arrStr = readLine.split(" ", 2);
	        	
	        	// read
	        	if(arrStr[0].equals("r")) {
	        		if(verbal) {
		        		logger.severe("----------------------------------------");
		        		logger.severe(String.format("# %d : read %s", cnt, arrStr[1]));
	        		}
	        		caches[0].read(arrStr[1], cnt-1, caches);
	        	}
	        	// write
        		else if(arrStr[0].equals("w")) {
        			if(verbal) {
	        			logger.severe("----------------------------------------");
	    	        	logger.severe(String.format("# %d : write %s", cnt, arrStr[1]));
        			}
	        		caches[0].write(arrStr[1], cnt-1, caches);
        		}
        		else {
        			System.out.println("wrong option [w|r]");
	        		continue;
        		}
	        	cnt++;
	        }
	        
    	}	catch (IOException e) {
            e.printStackTrace();
        }

    	// ====== add footer of log ======
    	// cache contents
    	StringBuffer sb_contents = new StringBuffer();
    	for(Cache cache:caches) {
    		sb_contents.append(cache.toString());
    	}
    	String msg = sb_contents.toString();
    	if(verbal)
    		logger.severe(msg.substring(0, msg.length() - 2));
    	else
    		System.out.println(msg.substring(0, msg.length() - 2));

    	// report
    	ArrayList<String> keys = new ArrayList<String>() {
			private static final long serialVersionUID = 1L;

			{
    			add("reads");
    			add("read misses");
    			add("writes");
    			add("write misses");
    			add("miss rate");
    			add("writebacks");
    		}
    	};
    	
		char ch = 'a';
		int memTraffic = 0;
    	StringBuffer sb_report = new StringBuffer("===== Simulation results (raw) =====\n");
    	for(Cache cache:caches) {
    		// for the same format
    		int i = 0;
    		String padding = null;
    		for(String s: keys) {
    			switch(i) {
    			case 0:
    				padding = "        ";
    				break;
    			case 1:
    				padding = "  ";
    				break;
    			case 2:
    				padding = "       ";
    				break;
    			case 3:
    				padding = " ";
    				break;
    			case 4:
    				padding = "              ";
    				break;
    			case 5:
    				padding = "   ";
    				break;
    			}
    			if(i != 4)
    				sb_report.append(String.format("%c. number of %s %s:%s%s", ch++, cache.getName(), s, padding, cache.counter.get(s)));
    			else
    	    		sb_report.append(String.format("%c. %s miss rate:%s%f", ++ch, cache.getName(), padding, cache.getMissRate()));
    			sb_report.append("\n");
    			i++;
    		}
    		if(cache.getSize() > 0)
    			memTraffic = cache.getMemoryTraffic();
    	}
    	sb_report.append(String.format("m. total memory traffic:      %d", memTraffic));
    	if(verbal)	logger.severe(sb_report.toString());
    	else		System.out.println(sb_report.toString());
	}
}
