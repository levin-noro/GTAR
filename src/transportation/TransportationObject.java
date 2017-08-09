package transportation;

public class TransportationObject {
	int autoId;
	String color;
	double speed;
	int link;
	int lane;
	int nextLink;
	double lecoordx;
	double lecoordy;
	double lecoordz;
	int timestamp;

	public TransportationObject(int autoId, String color, double speed, int link, int lane, int nextLink,
			double lecoordx, double lecoordy, double lecoordz, int timestamp) {

		this.autoId = autoId;
		this.color = color;
		this.speed = speed;
		this.link = link;
		this.lane = lane;
		this.nextLink = nextLink;
		this.lecoordx = lecoordx;
		this.lecoordy = lecoordy;
		this.lecoordz = lecoordz;
		this.timestamp = timestamp;
	}

}
