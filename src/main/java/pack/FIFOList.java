package pack;

import java.util.ArrayList;

public class FIFOList {
	private static final int DEFAULT_SIZE = 10;
	private int maxSize;
	private int size;
	private int addPtr;
	private ArrayList<Double> list;
	
	public void add(double value) {
		if (size < maxSize) {
			list.add(value);
			++size;
		} else {
			list.set(addPtr, value);
			updateAddPtr();
		}
	}
	
	public double getSum() {
		double sum = 0;
		for (Double value : list) {
			sum += value.doubleValue();
		}
		return sum;
	}

	private void updateAddPtr() {
		++addPtr;
		if (addPtr >= size) {
			addPtr = 0;
		}
	}

	public ArrayList<Double> getList() {
		return list;
	}

	/**
	 * Constructor with no size parameter
	 */
	public FIFOList() {
		this(DEFAULT_SIZE);
	}

	/**
	 * Constructor with specified size parameter
	 * 
	 * @param size
	 */
	public FIFOList(int maxSize) {
		this.maxSize = maxSize;
		list = new ArrayList<>();
		addPtr = 0;
	}

}
