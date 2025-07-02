/**
 * FibonacciHeap
 *
 * An implementation of Fibonacci heap over positive integers.
 *
 */
public class FibonacciHeap {

	public HeapNode min;
	private int size;
	private int trees;
	private int totalLinks, totalCuts;
	private final int c;

	/**
	 *
	 * Constructor to initialize an empty heap.
	 * pre: c >= 2.
	 *
	 */
	public FibonacciHeap(int c) {
		if (c < 2)
			throw new IllegalArgumentException("c must be ≥ 2 (got " + c + ")");
		this.c = c;
		this.min = null;
		this.size = 0;
		this.trees = 0;
		this.totalLinks = 0;
		this.totalCuts  = 0;
	}

	/**
	 *
	 * pre: key > 0
	 *
	 * Insert (key,info) into the heap and return the newly generated HeapNode.
	 *
	 */
	public HeapNode insert(int key, String info) {
		if (key <= 0)
			throw new IllegalArgumentException("key must be positive");
		HeapNode x = new HeapNode(key, info);
		if (min == null) {
			min = x;
		} else {
			spliceIntoRootList(x, min);
			if (x.key < min.key)
				min = x;
		}
		size++;
		trees++;
		return x;
	}

	/**
	 *
	 * Return the minimal HeapNode, null if empty.
	 *
	 */
	public HeapNode findMin() {
		return min;
	}

	/**
	 *
	 * Delete the minimal item.
	 * Return the number of links.
	 *
	 */
	public int deleteMin() {
		if (min == null)
			return 0;

		HeapNode z = min;
		int linksPerformed = 0;

		if (z.child != null) {
			HeapNode child = z.child;
			do {
				HeapNode next = child.next;
				child.parent = null;
				child.prev = child.next = child;
				spliceIntoRootList(child, z);
				child.lost = 0;
				trees++;
				child = next;
			} while (child != z.child);
		}

		removeFromRootList(z);
		size--;
		trees--;

		linksPerformed = consolidate();

		return linksPerformed;
	}

	/**
	 *
	 * pre: 0<diff<x.key
	 *
	 * Decrease the key of x by diff and fix the heap.
	 * Return the number of cuts.
	 *
	 */
	public int decreaseKey(HeapNode x, int diff) {
		if (x == null || diff <= 0 || diff >= x.key)
			throw new IllegalArgumentException("require 0 < diff < x.key");

		x.key -= diff;
		int cuts = 0;

		HeapNode p = x.parent;
		if (p != null && x.key < p.key) {
			cut(x, p);
			cuts++;
			while (p.parent != null && p.lost >= c) {
				HeapNode gp = p.parent;
				cut(p, gp);
				cuts++;
				p = gp;
			}
		}

		if (x.key < min.key)
			min = x;

		return cuts;
	}

	/**
	 *
	 * Delete the x from the heap.
	 * Return the number of links.
	 *
	 */
	public int delete(HeapNode x) {
		if (x == null)
			return 0;
		int newKey = (min == null ? 1 : min.key) - 1;
		int diff = x.key - newKey;
		decreaseKey(x, diff);
		return deleteMin();
	}

	/**
	 *
	 * Return the total number of links.
	 *
	 */
	public int totalLinks() {
		return totalLinks;
	}

	/**
	 *
	 * Return the total number of cuts.
	 *
	 */
	public int totalCuts() {
		return totalCuts;
	}

	/**
	 *
	 * Meld the heap with heap2
	 *
	 */
	public void meld(FibonacciHeap heap2) {
		if (heap2 == null || heap2.min == null || heap2 == this)
			return;
		if (this.min == null) {
			this.min          = heap2.min;
			this.size         = heap2.size;
			this.trees        = heap2.trees;
			this.totalCuts   += heap2.totalCuts;
			this.totalLinks  += heap2.totalLinks;
			heap2.min = null;
			return;
		}
		HeapNode a = this.min;
		HeapNode b = heap2.min;
		HeapNode aNext = a.next;
		HeapNode bNext = b.next;
		a.next = bNext;
		bNext.prev = a;
		b.next = aNext;
		aNext.prev = b;
		this.size        += heap2.size;
		this.trees       += heap2.trees;
		this.totalCuts   += heap2.totalCuts;
		this.totalLinks  += heap2.totalLinks;
		if (b.key < a.key)
			this.min = b;
		heap2.min = null;
	}

	/**
	 *
	 * Return the number of elements in the heap
	 *
	 */
	public int size() {
		return size;
	}

	/**
	 *
	 * Return the number of trees in the heap.
	 *
	 */
	public int numTrees() {
		return trees;
	}

	private void spliceIntoRootList(HeapNode x, HeapNode pos) {
		if (pos == null) {
			x.next = x.prev = x;
			min = x;
			return;
		}
		x.next = pos.next;
		x.prev = pos;
		pos.next.prev = x;
		pos.next = x;
	}

	private void removeFromRootList(HeapNode x) {
		if (x.next == x) {
			min = null;
		} else {
			x.next.prev = x.prev;
			x.prev.next = x.next;
			if (min == x)
				min = x.next;
		}
		x.next = x.prev = x;
	}

	private void link(HeapNode y, HeapNode x) {
		removeFromRootList(y);
		if (x.child == null) {
			x.child = y;
			y.next = y.prev = y;
		} else {
			spliceIntoRootList(y, x.child);
		}
		y.parent = x;
		y.lost   = 0;
		x.rank++;
		totalLinks++;
		trees--;
	}

	private int consolidate() {
		if (min == null)
			return 0;
		int maxDegree = upperBoundDegree();
		HeapNode[] A = new HeapNode[maxDegree + 1];
		HeapNode start = min;
		HeapNode w = start;
		int linksHere = 0;
		do {
			HeapNode x = w;
			w = w.next;
			int d = x.rank;
			while (A[d] != null) {
				HeapNode y = A[d];
				if (y.key < x.key) {
					HeapNode tmp = x; x = y; y = tmp;
				}
				link(y, x);
				linksHere++; d = x.rank;
				A[d - 1] = null;
			}
			A[d] = x;
		} while (w != start);
		min = null;
		trees = 0;
		for (HeapNode x : A) {
			if (x != null) {
				x.next = x.prev = x;
				if (min == null) {
					min = x;
				} else {
					spliceIntoRootList(x, min);
					if (x.key < min.key)
						min = x;
				}
				trees++;
			}
		}
		totalLinks += linksHere;
		return linksHere;
	}

	private void cut(HeapNode x, HeapNode p) {
		if (x.next == x) {
			p.child = null;
		} else {
			if (p.child == x)
				p.child = x.next;
			x.next.prev = x.prev;
			x.prev.next = x.next;
		}
		p.rank--;
		x.parent = null;
		x.next = x.prev = x;
		spliceIntoRootList(x, min);
		trees++;
		x.lost = 0;
		if (p.parent != null) {
			p.lost++;
		}

		totalCuts++;

	}

	private int upperBoundDegree() {
		return (int) (Math.log(Math.max(size, 1)) / Math.log(2)) + 5;
	}

	/**
	 * Class implementing a node in a Fibonacci Heap.
	 *
	 */
	public static class HeapNode {
		public int key;
		public String info;
		public HeapNode child;
		public HeapNode next;
		public HeapNode prev;
		public HeapNode parent;
		public int rank;
		public int lost;
		private HeapNode(int key, String info) {
			this.key  = key;
			this.info = info;
			this.child = this.parent = null;
			this.rank  = 0;
			this.lost  = 0;
			this.next = this.prev = this;
		}
	}
}