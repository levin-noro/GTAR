package test;

import java.util.ArrayList;

public class RenewalTest {

	public static void main(String[] args) {

		int bound = 1000000;
		for (int i = 0; i < bound; i++) {
			ArrayList<Integer> myInts = new ArrayList<Integer>(1000000);
			for (int j = 0; j < bound; j++) {
				myInts.add(j);
			}
		}

	}

}
