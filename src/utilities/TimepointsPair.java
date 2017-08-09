package utilities;

public class TimepointsPair {
	public int t_l;
	public int t_r;
	public int intersection;

	public TimepointsPair(int t_l, int t_r, int intersection) {
		this.t_l = t_l;
		this.t_r = t_r;
		this.intersection = intersection;
	}

	public TimepointsPair(int t_l, int t_r) {
		this.t_l = t_l;
		this.t_r = t_r;
	}

	@Override
	public String toString() {
		return "[" + t_l + "-" + t_r + "]";
	}

}
