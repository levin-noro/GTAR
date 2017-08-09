package utilities;

import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.plaf.synth.SynthSpinnerUI;

public class TimeIntervalsOperation {

	public static ArrayList<Integer> unionOfTimeIntervals(int startTime1, int endTime1, int startTime2, int endTime2) {
		ArrayList<Integer> result = new ArrayList<Integer>();
		if (startTime1 > endTime2) {
			updateResult(result, startTime2, endTime2);
			updateResult(result, startTime1, endTime1);
		} else if (startTime2 > endTime1) {
			updateResult(result, startTime1, endTime1);
			updateResult(result, startTime2, endTime2);
		} else {
			int newStartTime = Math.min(startTime1, startTime2);
			int newEndTime = Math.max(endTime1, endTime2);
			updateResult(result, newStartTime, newEndTime);

		}
		return result;
	}

	public static void updateResult(ArrayList<Integer> result, int s, int e) {
		if (result.size() > 0 && result.get(result.size() - 1) >= s) {
			result.set(result.size() - 1, e);
		} else {
			result.add(s);
			result.add(e);
		}
	}

	public static ArrayList<Integer> unionOfTimeIntervals(ArrayList<Integer> set1, ArrayList<Integer> set2) {
		ArrayList<Integer> result = new ArrayList<Integer>();
		int s1 = 0, e1 = 1, s2 = 0, e2 = 1;

		while (e1 <= set1.size() || e2 <= set2.size()) {
			if (e1 > set1.size()) {
				if (result.size() == 0 || result.get(result.size() - 1) < set2.get(s2)) {
					updateResult(result, set2.get(s2), set2.get(e2));
				}
				s2 += 2;
				e2 += 2;
			} else if (e2 > set2.size()) {
				if (result.size() == 0 || result.get(result.size() - 1) < set1.get(s1)) {
					updateResult(result, set1.get(s1), set1.get(e1));
				}
				s1 += 2;
				e1 += 2;
			} else if (set1.get(s1) > set2.get(e2)) {
				updateResult(result, set2.get(s2), set2.get(e2));
				s2 += 2;
				e2 += 2;
			} else if (set2.get(s2) > set1.get(e1)) {
				updateResult(result, set1.get(s1), set1.get(e1));
				s1 += 2;
				e1 += 2;
			} else {
				int newStartTime = Math.min(set1.get(s1), set2.get(s2));
				int newEndTime = Math.max(set1.get(e1), set2.get(e2));
				updateResult(result, newStartTime, newEndTime);
				s1 += 2;
				e1 += 2;
				s2 += 2;
				e2 += 2;
			}
		}

		// just for testing:
		// if (result.size() % 2 != 0) {
		// System.err.println("not even");
		// }
		// for (int i = 1; i < result.size(); i++) {
		// if (result.get(i - 1) >= result.get(i)) {
		// System.err.println("not sorted or overlapped");
		// }
		// }

		return result;

	}

	public static ArrayList<Integer> intersectionOfTimeIntervals(ArrayList<Integer> set1, ArrayList<Integer> set2) {
		// an empty set means -inf, +inf but a null result means that no
		// intersection.

		if (set1 == null || set2 == null) {
			System.err.println();
		}

		ArrayList<Integer> result = new ArrayList<Integer>();

		if (set1.size() == 0 && set2.size() == 0) {
			return result;
		}
		if (set1.size() == 0 && set2.size() > 0) {
			result.addAll(set2);
			return result;
		}
		if (set2.size() == 0 && set1.size() > 0) {
			result.addAll(set1);
			return result;
		}

		int s1 = 0, e1 = 1, s2 = 0, e2 = 1;

		while (e1 <= set1.size() && e2 <= set2.size()) {
			if (set1.get(s1) <= set2.get(e2) && set2.get(s2) <= set1.get(e1)) {
				int newStartTime = Math.max(set1.get(s1), set2.get(s2));
				int newEndTime = Math.min(set1.get(e1), set2.get(e2));
				result.add(newStartTime);
				result.add(newEndTime);
				if (newEndTime >= set1.get(e1)) {
					s1 += 2;
					e1 += 2;
				}
				if (newEndTime >= set2.get(e2)) {
					s2 += 2;
					e2 += 2;
				}

			} else if (set1.get(s1) < set2.get(s2)) {
				s1 += 2;
				e1 += 2;
			} else if (set2.get(s2) < set1.get(s1)) {
				s2 += 2;
				e2 += 2;
			}
		}
		if (result.size() == 0)
			return null;

		return result;
	}

	public static ArrayList<Integer> intersectionOfTimeIntervals(int startTime1, int endTime1, int startTime2,
			int endTime2) {
		ArrayList<Integer> result = new ArrayList<Integer>();
		if (startTime1 <= endTime2 && startTime2 <= endTime1) {
			int newStartTime = Math.max(startTime1, startTime2);
			int newEndTime = Math.min(endTime1, endTime2);
			result.add(newStartTime);
			result.add(newEndTime);

		}

		return result;
	}

	public static ArrayList<Integer> getArrayListOfArray(int[] intArray) {
		ArrayList<Integer> intList = new ArrayList<Integer>(intArray.length);

		for (int i = 0; i < intArray.length; i++) {
			intList.add(intArray[i]);
		}

		return intList;

	}

	// TEST
	public static void main(String[] args) {
		Integer[] set1 = new Integer[] { 0, 216 };
		Integer[] set2 = new Integer[] { 24, 37, 100, 123, 168, 171, 172, 201 };
		// System.out.println("union of { 3, 4, 5, 7, 10, 11, 15, 16 } and { 1,
		// 2, 5, 8, 11, 12 }");
		// print(TimeIntervalsOperation.unionOfTimeIntervals(new
		// ArrayList<Integer>(Arrays.asList(set1)),
		// new ArrayList<Integer>(Arrays.asList(set2))));
		//
		// System.out.println("intersection of { 3, 4, 5, 7, 10, 11, 15, 16 }
		// and { 1, 2, 5, 8, 11, 12 }");
		// print(TimeIntervalsOperation.intersectionOfTimeIntervals(new
		// ArrayList<Integer>(Arrays.asList(set1)),
		// new ArrayList<Integer>(Arrays.asList(set2))));

		//////
		/////
		// set1 = new Integer[] { 134,191,195,209 };
		// set2 = new Integer[] { 37,66};
		//
		// System.out.println("union of " + Arrays.toString(set1) + " and " +
		// Arrays.toString(set2));
		// print(TimeIntervalsOperation.unionOfTimeIntervals(new
		// ArrayList<Integer>(Arrays.asList(set1)),
		// new ArrayList<Integer>(Arrays.asList(set2))));
		//
		// System.out.println("intersection of " +Arrays.toString(set1)+ " and "
		// + Arrays.toString(set2));
		// print(TimeIntervalsOperation.intersectionOfTimeIntervals(new
		// ArrayList<Integer>(Arrays.asList(set1)),
		// new ArrayList<Integer>(Arrays.asList(set2))));

		// checkOccurrenceOfNodeIdAtT(new
		// ArrayList<Integer>(Arrays.asList(set2)), 167);

		int[] set22 = new int[] { 1, 3, 6, 8, 12, 16, 20, 21 };
		int[] set11 = new int[] { 2, 4, 7, 8, 10, 12, 18, 20 };
		int minOcc = getMinOccurrencesOfTwoFocusMatches(getArrayListOfArray(set11), getArrayListOfArray(set22), 2, 1,
				20);
		System.out.println("min occ:" + minOcc);

	}

	private static void print(ArrayList<Integer> result) {
		for (Integer i : result) {
			System.out.print(i + ",");
		}
		System.out.println();
		System.out.println();
	}

	public static int checkOccurrenceOfNodeIdAtT(ArrayList<Integer> timePointsOfAMatch, int t) {

		int left = -1;
		int right = timePointsOfAMatch.size();

		while (right > left + 1) {
			int middle = (left + right) / 2;
			if (timePointsOfAMatch.get(middle) >= t)
				right = middle;
			else
				left = middle;
		}

		if (right < timePointsOfAMatch.size() && (timePointsOfAMatch.get(right) == t
				|| ((right % 2) == 1 && timePointsOfAMatch.get(right - 1) <= t) && timePointsOfAMatch.get(right) >= t))
			return right;

		return -1;

	}

	public static int getMinOccurrencesOfTwoFocusMatches(ArrayList<Integer> lhsTimepoints,
			ArrayList<Integer> rhsTimepoints, int deltaT, int startTime, int endTime) {
		// an empty set means -inf, +inf but a null result means that no
		// intersection.

		if (lhsTimepoints == null || rhsTimepoints == null) {
			System.err.println("why null?!");
		}

		ArrayList<Integer> result = new ArrayList<Integer>();

		if (lhsTimepoints.size() == 0 && rhsTimepoints.size() == 0) {
			return (endTime - startTime);
		}
		if (lhsTimepoints.size() == 0 && rhsTimepoints.size() > 0) {
			return TimeIntervalsOperation.getTotalTimesOfOccurrences(rhsTimepoints);
		}
		if (rhsTimepoints.size() == 0 && lhsTimepoints.size() > 0) {
			return TimeIntervalsOperation.getTotalTimesOfOccurrences(lhsTimepoints);
		}

		int s1 = 0, e1 = 1, s2 = 0, e2 = 1;

		while (e1 <= lhsTimepoints.size() && e2 <= rhsTimepoints.size()) {
			if (lhsTimepoints.get(s1) <= rhsTimepoints.get(e2) && rhsTimepoints.get(s2) <= lhsTimepoints.get(e1)) {
				int newStartTime = Math.max(lhsTimepoints.get(s1), rhsTimepoints.get(s2));
				int newEndTime = Math.min(lhsTimepoints.get(e1), rhsTimepoints.get(e2));
				result.add(newStartTime);
				result.add(newEndTime);
				if (newEndTime >= lhsTimepoints.get(e1)) {
					s1 += 2;
					e1 += 2;
				}
				if (newEndTime >= rhsTimepoints.get(e2)) {
					s2 += 2;
					e2 += 2;
				}

			} else if (rhsTimepoints.get(s2) < lhsTimepoints.get(s1)) {
				s2 += 2;
				e2 += 2;
			} else if (lhsTimepoints.get(s1) < rhsTimepoints.get(s2)) {

				// new for minimal occurrence computation:
				if ((result.isEmpty() || (lhsTimepoints.get(e1) > result.get(result.size() - 1)))
						&& (lhsTimepoints.get(e1) + deltaT) >= rhsTimepoints.get(s2)
						// This condition is for the following example:
						// LHS:[1-4][6-8][10-10][12-13]
						// RHS:[2-7][10-11][14-20]
						// (10,10) will count two times
						&& ((lhsTimepoints.size() < (s1 + 3)) || (lhsTimepoints.get(s1 + 2) > rhsTimepoints.get(s2)))) {
					result.add(lhsTimepoints.get(e1));
					result.add(lhsTimepoints.get(e1));
				}

				s1 += 2;
				e1 += 2;
			}
		}

		int retValue = 0;
		for (int i = 1; i < result.size(); i += 2) {
			retValue += result.get(i) - result.get(i - 1) + 1;
		}

		// System.out.println(Arrays.toString(result.toArray()));

		return retValue;
	}

	public static int getMinOccurrencesOfTwoFocusMatchesExcludingCoOcc(ArrayList<Integer> lhsTimepoints,
			ArrayList<Integer> rhsTimepoints, int deltaT, int startTime, int endTime) {
		// an empty set means -inf, +inf but a null result means that no
		// intersection.

		if (lhsTimepoints == null || rhsTimepoints == null) {
			System.err.println("why null?!");
		}

		ArrayList<Integer> result = new ArrayList<Integer>();
		int lastItemInResult = -1;

		ArrayList<Integer> notCoOccurredResult = new ArrayList<Integer>();

		if (lhsTimepoints.size() == 0 && rhsTimepoints.size() == 0) {
			return 0;
		}
		if (lhsTimepoints.size() == 0 && rhsTimepoints.size() > 0) {
			return 0;
		}
		if (rhsTimepoints.size() == 0 && lhsTimepoints.size() > 0) {
			return 0;
		}

		int s1 = 0, e1 = 1, s2 = 0, e2 = 1;

		while (e1 <= lhsTimepoints.size() && e2 <= rhsTimepoints.size()) {
			if (lhsTimepoints.get(s1) <= rhsTimepoints.get(e2) && rhsTimepoints.get(s2) <= lhsTimepoints.get(e1)) {
				int newStartTime = Math.max(lhsTimepoints.get(s1), rhsTimepoints.get(s2));
				int newEndTime = Math.min(lhsTimepoints.get(e1), rhsTimepoints.get(e2));
				result.add(newStartTime);
				result.add(newEndTime);
				if (newEndTime >= lhsTimepoints.get(e1)) {
					s1 += 2;
					e1 += 2;
				}
				if (newEndTime >= rhsTimepoints.get(e2)) {
					s2 += 2;
					e2 += 2;
				}

			} else if (rhsTimepoints.get(s2) < lhsTimepoints.get(s1)) {
				s2 += 2;
				e2 += 2;
			} else if (lhsTimepoints.get(s1) < rhsTimepoints.get(s2)) {

				// new for minimal occurrence computation:
				if (lhsTimepoints.get(e1) > lastItemInResult
						&& (lhsTimepoints.get(e1) + deltaT) >= rhsTimepoints.get(s2)
						// This condition is for the following example:
						// LHS:[1-4][6-8][10-10][12-13]
						// RHS:[2-7][10-11][14-20]
						// (10,10) will count two times
						&& ((lhsTimepoints.size() < (s1 + 3)) || (lhsTimepoints.get(s1 + 2) > rhsTimepoints.get(s2)))) {
					result.add(lhsTimepoints.get(e1));
					result.add(lhsTimepoints.get(e1));
					lastItemInResult = lhsTimepoints.get(e1);

					// notOccurredResult
					notCoOccurredResult.add(lhsTimepoints.get(e1));
				}

				s1 += 2;
				e1 += 2;
			}
		}

		int retValue = notCoOccurredResult.size();

		// for (int i = 1; i < result.size(); i += 2) {
		// retValue += result.get(i) - result.get(i - 1) + 1;
		// }

		// System.out.println(Arrays.toString(result.toArray()));

		return retValue;
	}

	private static int getTotalTimesOfOccurrences(ArrayList<Integer> rhsTimepoints) {

		int ret = 0;
		for (int i = 1; i < rhsTimepoints.size(); i += 2) {
			ret += rhsTimepoints.get(i) - rhsTimepoints.get(i - 1) + 1;
		}

		return ret;
	}
}
