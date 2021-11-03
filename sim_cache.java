class sim_cache {
	public static void main(String[] args) throws Exception {
		if (args.length != 8){
			throw new Exception("Exception : Insufficient/Too many arguments passed");
		}
		int block_size = Integer.parseInt(args[0]);
		int l1_size = Integer.parseInt(args[1]);
		int l1_assoc = Integer.parseInt(args[2]);
		int l2_size = Integer.parseInt(args[3]);
		int l2_assoc = Integer.parseInt(args[4]);
		int replacement_policy = Integer.parseInt(args[5]);
		int inclusion_property = Integer.parseInt(args[6]);
		String trace_file = args[7];

		System.out.println("===== Simulator configuration =====");
		System.out.println("BLOCK SIZE          : " + block_size);
		System.out.println("L1 SIZE             : " + l1_size);
		System.out.println("L1 ASSOC            : " + l1_assoc);
		System.out.println("L2 SIZE             : " + l2_size);
		System.out.println("L2 ASSOC            : " + l2_assoc);
		if (replacement_policy == 0){
			System.out.println("REPLACEMENT POLICY  : LRU");
		}else if (replacement_policy == 1){
			System.out.println("REPLACEMENT POLICY  : Pseudo-LRU");
		}else {
			System.out.println("REPLACEMENT POLICY  : Optimal");
		}
		if (inclusion_property == 0){
			System.out.println("INCLUSION PROPERTY  : non-inclusive");
		}else {
			System.out.println("INCLUSION PROPERTY  : inclusive");
		}
		String[] tr = trace_file.split("/");
		System.out.println("trace_file          : " + tr[tr.length-1]);

		//Create L1 and L2 Cache
		cache l1_cache = new cache(block_size, l1_size, l1_assoc, replacement_policy, trace_file);
		cache l2_cache = new cache(0, 0, 0, replacement_policy, trace_file);
		if (l2_size != 0) {
			l2_cache = new cache(block_size, l2_size, l2_assoc, replacement_policy, trace_file);
		}
		//Start cache simulator
		simulator s = new simulator(l1_cache, l2_cache, trace_file, l2_size);
		s.begin(inclusion_property);
		s.print_results();
	}
}

