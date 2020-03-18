
public class Block {
	boolean dirty = false;
	boolean valid;
	int tag;
	int num;
	String address;	// not necessary
	
	Block(int tag, int num, String address, boolean dirty) {
		this.tag = tag;
		this.num = num;
		this.address = address;	// for debug only
		this.dirty = dirty;
	}
}
