import java.util.*;


public class Set extends Replacement {
    LinkedList<Block> blocks; 	//Data contained in the set
    String hit, name;
    boolean dirty;
    
    public Set(int setLength, String rep, Hashtable<Integer, ArrayList<String>> opt_addr, String name)
    {
    	super(setLength, rep, opt_addr, name);
        this.name = name;
        blocks = new LinkedList<Block>();
        set_blocks(blocks);
    }

    public int is_hit(int[] addr_field) {
    	/*
    	 * check if hit or not
    	 * if not, return -1, otherwise, return its index
    	 * */
    	
    	int i=-1;
    	int tag = addr_field[0];

    	for(Block c: blocks) {
    		i++;
            if(tag == c.tag) {
            	if(super.rep.equals("LRU")) {
            		int num = c.num;
            		c.num = get_size() - 1;
        			update(i, num);	// for replacement
            	}
            	return i;
            }
        }
    	return -1;
    }
    
    public String toString(String prefix) {
    	/*
    	 * to string all contents of each set
    	 * */
        StringBuffer sb = new StringBuffer(prefix);
    	Iterator<Block> queueIterator = blocks.iterator();
    	
    	while(queueIterator.hasNext()) {
    		Block c = queueIterator.next();
    		sb.append(Integer.toHexString(c.tag));
    		if(c.dirty)	sb.append(" D");
    		else		sb.append("  ");
    		sb.append("   ");
    	}
    	return sb.toString();
    }
}