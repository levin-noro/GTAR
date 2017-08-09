package movieLens;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DameDasti {

	public static void main(String[] args) {
		SimpleDateFormat f= new SimpleDateFormat("HH:mm");
		
		Date date1 = new Date(978300760);
		System.out.println(date1.toString());
		System.out.println(f.format(date1));
		

		Date date2 = new Date(969659932);
		System.out.println(date2.toString());
		System.out.println(f.format(date2));

		Date date3 = new Date(956715569);
		System.out.println(date3.toString());
		System.out.println(f.format(date3));

	}

}
