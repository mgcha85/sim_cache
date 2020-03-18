import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList; 

public class Replacement {
	private int csize;
	protected String rep;
	
	LinkedList<Block> q;
	String name;
	Hashtable<Integer, ArrayList<String>> opt_addr;
	
    Replacement(int csize, String rep, Hashtable<Integer, ArrayList<String>> opt_addr, String name)
    {
        this.csize = csize;
        this.rep = rep;
        this.name = name;
        this.opt_addr = opt_addr;
    }
    
    public void set_blocks(LinkedList<Block> q) {
    	this.q = q;
    }
    
    int get_size()
    {
    	return q.size();
    }

    private int search_furthest(int index) {
    	/*
    	 * for optimal replacement, find the farthest referred one
    	 * */
    	int cnt = 0;
    	int max_idx = -1;
    	int max_dist = -1;
    	ArrayList<String> addresses = opt_addr.get(index);
    	boolean found = false;
    	
    	for(Block c: q) {	
        	int distance = -1;
	        for (int i=0; i<addresses.size(); i++) {
	        	if(addresses.get(i).equals(c.address)) {
	        		distance = i;
		        	found = true;
	        		break;
	        	}
	        }
	        if(distance > max_dist)	{
	        	max_dist = distance;
	        	max_idx = cnt;
	        }
	        if(distance < 0) {
	        	max_dist = (1 << 31) - 1;
	        	max_idx = cnt;
	        }
	        cnt++;
    	}
    	
    	if(!found) 	return 0;
    	else		return max_idx;
    }
    
    public void update(int index, int num)
    {
    	/*
    	 * update index when replacement or referred
    	 * */
    	
    	if(index < get_size()) {    	
	    	for(int i=0; i<get_size(); i++) {
	    		if(i == index) continue;
	    		Block cc = q.get(i);
	    		if(cc.num > num) {
	    			cc.num--;
	    		}
	    	}
    	}
    }
    
    // insert value to queue: if queue is full, remove oldest one and append the value as most recent one
    public Block insert(int tag, int index, String address, boolean dirty)
    {
    	Block victim = null;	// victim is whether null or tag
		int idx = 0;

    	if(get_size() == csize) {
			switch(rep) {
    		case "LRU":
    			for(Block c:q) {
    				if(c.num == -1) break;
    				else if(c.num == 0) break;
    				idx++;
    			}
	    		victim = q.get(idx);
	    		break;
    		case "FIFO":
    			for(Block c:q) {
    				if(c.num == -1) break;
    				else if(c.num == 0) break;
    				idx++;
    			}
	    		victim = q.get(idx);
	    		break;
    		case "optimal":
    			idx = search_furthest(index);
	    		victim = q.get(idx);
	    		break;
    		}
    	}

    	int size = get_size();
    	int block_num = (size >= csize ? csize-1 : size);
    	Block block = new Block(tag, block_num, address, dirty);
    	
    	if(victim != null) {
    		switch(rep) {
    		case "LRU":
            	q.set(idx, block);
            	update(idx, victim.num);
            	break;
    		case "FIFO":
            	q.set(idx, block);
            	update(idx, victim.num);
            	break;
    		case "optimal":
            	q.set(idx, block);
            	update(idx, victim.num);
            	break;
    		}
    	}
    	else {
    		q.add(block);
    	}
    	if(victim != null)	return victim;
    	else				return null;
    }
}
