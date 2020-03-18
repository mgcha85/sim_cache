import java.util.logging.*;
import java.util.Hashtable;
import java.util.ArrayList;


public class Cache 
{
    private Set[] sets;
    private int blockSize, size, assoc, num_sets;
	private Hashtable<String, String> ht, bt;
	private Hashtable<Integer, ArrayList<String>> opt_addr;
    private int index, offset, tag;

    String address, rep, name;
    ArrayList<String> addresses;
    boolean verbal;
    Logger logger;
    int[] add_field;
    Hashtable<String, Integer> counter;
    boolean inclusive;
    
    public Cache(int blockSize, int size, int assoc, String name, String replacement, boolean inclusive, ArrayList<String> addresses, Logger logger, boolean verbal)
    {
    	if(!isPower2(blockSize)) {
//    		System.out.println("block size should be power of two!");
    		return;
    	}
    	
    	// hexadecimal to binary
    	ht = new Hashtable<String, String>() {
			private static final long serialVersionUID = 1L;

			{
    	        put("0","0000");
    	        put("1","0001");
    	        put("2","0010");
    	        put("3","0011");
    	        put("4","0100");
    	        put("5","0101");
    	        put("6","0110");
    	        put("7","0111");
    	        put("8","1000");
    	        put("9","1001");
    	        put("a","1010");
    	        put("b","1011");
    	        put("c","1100");
    	        put("d","1101");
    	        put("e","1110");
    	        put("f","1111");
    	    }
    	};

    	// binary to hexadecimal
    	bt = new Hashtable<String, String>() {
			private static final long serialVersionUID = 1L;

			{
    	        put("0000","0");
    	        put("0001","1");
    	        put("0010","2");
    	        put("0011","3");
    	        put("0100","4");
    	        put("0101","5");
    	        put("0110","6");
    	        put("0111","7");
    	        put("1000","8");
    	        put("1001","9");
    	        put("1010","a");
    	        put("1011","b");
    	        put("1100","c");
    	        put("1101","d");
    	        put("1110","e");
    	        put("1111","f");
    	    }
    	};
    	// counter table
    	counter = new Hashtable<String, Integer>() {
			private static final long serialVersionUID = 1L;

			{
    	        put("reads", 0);
    	        put("read misses", 0);
    	        put("writes", 0);
    	        put("write misses", 0);
    	        put("writebacks", 0);
    	    }
    	};
    	
    	// initiate values
    	this.blockSize = blockSize;
    	this.size = size;
    	this.assoc = assoc;
    	this.name = name;
    	this.logger = logger;
    	this.rep = replacement;
    	this.inclusive = inclusive;
    	this.verbal = verbal;
    	if(assoc == 0 || size == 0) {
//    		System.out.println("cache with zero capacity");
    		return;
    	}
    	this.num_sets = size / (blockSize * assoc);
    	if(!isPower2(num_sets)) {
    		System.out.println("num of sets should be power of two!. please reset size and associative");
    		return;
    	}
    	opt_addr = new Hashtable<Integer, ArrayList<String>>();
    	// generate optimal addresses
        if(addresses != null) {
        	for(String addr: addresses) {
        		addr = remove_offset(addr);
        		int[] addr_int = get_addr_field(addr);
        		if(opt_addr.containsKey(addr_int[1])) {
        			opt_addr.get(addr_int[1]).add(addr);
        		}
        		else {
        			ArrayList<String> strList = new ArrayList<String>();
        			strList.add(addr);
        			opt_addr.put(addr_int[1], strList);
        		}
        	}
        }
    	
        // generate sets
        this.sets = new Set[num_sets];
        for(int i = 0; i < num_sets; i++)
        {
            this.sets[i] = new Set(assoc, replacement, opt_addr, name);
        }
        this.addresses = addresses;
    }
    
    public String fillzeros(String str, int length)
    {
    	/*
    	 * add zero padding when binary conversion
    	 * */
    	StringBuilder sb = new StringBuilder();
    	for(int i=0; i<length - str.length(); i++)
    		sb.append("0");
    	sb.append(str);
    	return sb.toString();
    }
    
    // really not necessary
    private String remove_offset(String address) {
    	/*
    	 * for this simulation, offset is not used and trimmed
    	 * */
    	final int legnth = 32 / 4;
    	if(address.length() < legnth)
    		address = fillzeros(address, legnth);
    	
    	String bin_addr = "";
		for (int i=0; i<address.length(); i++) {
			char ch = address.charAt(i);
			String str = Character.toString(ch);
			bin_addr += ht.get(str);
		}
		int len_offset = (int) (Math.log(blockSize) / Math.log(2));
		int len_index = (int) (Math.log(num_sets) / Math.log(2));
		int len_tag = 32 - len_index - len_offset;
		
		// convert to hexadecimal without offset (not necessary)
		String bin_addr_sub = bin_addr.substring(0, len_tag + len_index);
		for (int i=0; i<len_offset; i++) {
			bin_addr_sub += "0";
		}
		
		String address_new = "";
		boolean add = false;
		for (int i=0; i<bin_addr_sub.length()/4; i++) {
			String hex = bt.get(bin_addr_sub.substring(4*i, 4*(i+1)));
			if(hex != "0") add = true;
			if(add) address_new += bt.get(bin_addr_sub.substring(4*i, 4*(i+1)));
		}
		return address_new;
    }
    
    public int[] get_addr_field(String address)
    {	
    	/*
    	 * convert address to tag, index, offset
    	 * */
    	
    	int[] addr_field = new int[3];
    	
    	final int legnth = 32 / 4;
    	if(address.length() < legnth)
    		address = fillzeros(address, legnth);
    	
    	String bin_addr = "";
		for (int i=0; i<address.length(); i++) {
			char ch = address.charAt(i);
			String str = Character.toString(ch);
			bin_addr += ht.get(str);
		}
//		System.out.println(String.format("blockSize: %d, num_sets: %d", blockSize, num_sets));
		
		int len_offset = (int) (Math.log(blockSize) / Math.log(2));
		int len_index = (int) (Math.log(num_sets) / Math.log(2));
		int len_tag = 32 - len_index - len_offset;
		
		// convert to hexadecimal without offset (not necessary)
		String bin_addr_sub = bin_addr.substring(0, len_tag + len_index);
		for (int i=0; i<len_offset; i++) {
			bin_addr_sub += "0";
		}
		this.address = "";
		boolean add = false;
		for (int i=0; i<bin_addr_sub.length()/4; i++) {
			String hex = bt.get(bin_addr_sub.substring(4*i, 4*(i+1)));
			if(hex != "0") add = true;
			if(add) this.address += bt.get(bin_addr_sub.substring(4*i, 4*(i+1)));
		}
		
		tag = Integer.parseInt(bin_addr.substring(0, len_tag), 2);
		if(len_index > 0)
			index = Integer.parseInt(bin_addr.substring(len_tag, len_tag + len_index), 2);
		else
			index = 0;
		offset = Integer.parseInt(bin_addr.substring(len_tag + len_index), 2);
		
		addr_field[0] = tag;
    	addr_field[1] = index;
    	addr_field[2] = offset;    	
    	return addr_field;
    }
    
    public Block read(String address, int lineNum, Cache[] caches) {
    	/* cache read method
    	 * input: address is necessary
    	 *        lineNum is for debug, not necessary
    	 *        caches is for communication to other caches
    	 * output: victim
    	 * 
    	 * */

    	if(size == 0) return null;
		counter.put("reads", counter.get("reads")+1);
		Block victim = null;
		int hit = -1;
		String text;
		int offset = Integer.parseInt(name.substring(1));
		
		int[] addr_field = get_addr_field(address);
		if(addr_field[1] < num_sets) {
    		Set set = sets[addr_field[1]];
    		if(rep.equals("optimal"))
    			opt_addr.get(addr_field[1]).remove(0);

    		// if miss
    		hit = set.is_hit(addr_field);
    		if(hit < 0) {
    			if(verbal) {
	    			text = String.format("%s read : %s (tag %s, index %s)", name, this.address, Integer.toHexString(tag), index);
	    			logger.severe(text);
    			}
    			
    			victim = set.insert(addr_field[0], addr_field[1], this.address, false);
    			counter.put("read misses", counter.get("read misses")+1);
    			if(verbal) {
        			logger.severe(String.format("%s miss", name));    	    				
    			}

    			// something evicted
    			if(victim != null) {
    				if(verbal) {
    					logger.severe(String.format("%s victim: %s (tag %s, index %d, %s)", name, victim.address, Integer.toHexString(victim.tag), index, victim.dirty?"dirty":"clean"));
    				}
    				if (victim.dirty) {
    					counter.put("writebacks", counter.get("writebacks")+1);					
	    				for(int i=offset; i<caches.length; i++) {
							caches[i].write(victim.address, lineNum, caches);
						}	    				
    				}
	    			if (inclusive) {
    					for(int i=offset-1; i>0; i--)
    						evict(victim.address, caches[i-1]);
	    			}
    			}
    			else {
    				if(verbal) logger.severe(String.format("%s victim: none", name));
    			}
    			for(int i=offset; i<caches.length; i++) {
    				caches[i].read(address, lineNum, caches);
    			}
    		}
    		else {
    			Block block = set.blocks.get(hit);
    			text = String.format("%s read : %s (tag %s, index %s)", name, block.address, Integer.toHexString(tag), index);
    			if(verbal) {
    				logger.severe(text);
        			logger.severe(String.format("%s hit", name));
    			}
    		}
    	}
		if(verbal) logger.severe(String.format("%s update %s", name, rep));
		return victim;
    }

    public Block write(String address, int lineNum, Cache[] caches) {
    	/* cache write method
    	 * input: address is necessary
    	 *        lineNum is for debug, not necessary
    	 *        caches is for communication to other caches
    	 * output: victim
    	 * 
    	 * */
    	
    	if(size == 0) return null;
    	counter.put("writes", counter.get("writes")+1);
		Block victim = null;
		int hit = -1;
		String text;
		int offset = Integer.parseInt(name.substring(1));
				
		int[] addr_field = get_addr_field(address);
		if(addr_field[1] < num_sets) {
    		Set set = sets[addr_field[1]];
    		if(rep.equals("optimal"))
    			opt_addr.get(addr_field[1]).remove(0);
    		
    		// if miss
    		hit = set.is_hit(addr_field);
    		if(hit < 0) {
    			text = String.format("%s write : %s (tag %s, index %s)", name, this.address, Integer.toHexString(tag), index);
    			if(verbal) logger.severe(text);    

    			victim = set.insert(addr_field[0], addr_field[1], this.address, true);
    			counter.put("write misses", counter.get("write misses")+1);
    			if(verbal) logger.severe(String.format("%s miss", name));

    			// something evicted
    			if(victim != null) {
    				if(verbal) 
    					logger.severe(String.format("%s victim: %s (tag %s, index %d, %s)", name, victim.address, Integer.toHexString(victim.tag), index, victim.dirty?"dirty":"clean"));
    				
    				if (victim.dirty) {
    					counter.put("writebacks", counter.get("writebacks")+1);
	    				for(int i=offset; i<caches.length; i++) {
							caches[i].write(victim.address, lineNum, caches);
						}
    				}
    				if (inclusive) {
    					for(int i=offset-1; i>0; i--)
    						evict(victim.address, caches[i-1]);
	    			}
    			}
    			else {
    				if(verbal) logger.severe(String.format("%s victim: none", name));
    			}
    			
    			for(int i=offset; i<caches.length; i++) {
    				caches[i].read(address, lineNum, caches);
    			}
    		}
    		else {
    			Block block = set.blocks.get(hit);
    			block.dirty = true;
    			text = String.format("%s write : %s (tag %s, index %s)", name, this.address, Integer.toHexString(tag), index);
    			if(verbal) {
    				logger.severe(text);
        			logger.severe(String.format("%s hit", name));
    			}
    		}
    	}
		if(verbal) {
			logger.severe(String.format("%s update %s", name, rep));
			logger.severe(String.format("%s set dirty", name));
		}
		return victim;
    }
    
    private void evict(String address, Cache cache) {
    	/*	for the inclusive property when L2 or higher cache have victim, L1 or lower cache is remove 
    	 * */
    	
    	int index = 0;
		for(Set set: cache.sets) {
			for(int i=0; i<set.blocks.size(); i++) {
				if(set.blocks.get(i).address.equals(address)) {
					if(set.blocks.get(i).dirty) {
						if(verbal) {
							logger.severe(String.format("%s invalidated: %s (tag %s, index %d, dirty)", cache.name, set.blocks.get(i).address, Integer.toHexString(set.blocks.get(i).tag), index));
							logger.severe(String.format("%s writeback to main memory directly", name));
						}
					}
					// index update
					int num = set.blocks.get(i).num;
					for(int j=0; j<set.blocks.size(); j++) {
						if(j == i) continue;
						if(set.blocks.get(j).num > num) set.blocks.get(j).num--; 
					}
					set.blocks.get(i).num = -1;
				}
			}
			index++;
		}
    }
    
    private boolean isPower2(int n) {
    	return n>0 && (n&n-1)==0;
    }
    
    public int getSetAssoc() {
        return assoc;
    }

    public int getSize() {
        return size;
    }
    
    public String getName() {
    	return name;
    }
    
    public double getMissRate() {
    	double tot = 0.0;
    	double misses = 0.0;
    	if(Integer.parseInt(name.substring(1)) == 1) {
        	tot += counter.get("reads");
        	tot += counter.get("writes");
	    	misses += counter.get("read misses");
	    	misses += counter.get("write misses");
    	}
    	else {
        	tot += counter.get("reads");    		
	    	misses += counter.get("read misses");
    	}
    	if(tot == 0) return 0;
    	return misses / tot;
    }
    
    public int getMemoryTraffic() {
    	int traffic = 0;
    	traffic += counter.get("read misses");
    	traffic += counter.get("write misses");
    	traffic += counter.get("writebacks");
    	return traffic;
    }
    
    public String toString() {
    	/*
    	 * to string all contents of each cache
    	 * */
    	
        StringBuffer sb = new StringBuffer(String.format("===== %s contents =====\n", name));
    	if(sets == null) return "";
    	
    	int cnt = 0;
    	for(Set set:sets) {
    		sb.append(set.toString(String.format("Set\t%d:\t", cnt)));
    		sb.append("\n");
    		cnt++;
    	}
    	return sb.toString();
    }
}
