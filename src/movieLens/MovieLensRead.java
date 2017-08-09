package movieLens;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import utilities.Dummy.DummyFunctions;

public class MovieLensRead {

	public static void main(String[] args) throws Exception {
		MovieLensRead mlr = new MovieLensRead();
		mlr.read();
	}

	private String newDataGraphPath = "/Users/mnamaki/Documents/Education/PhD/Spring2017/GTAR/DATA/MovieLens/movielens.db";
	private GraphDatabaseService dataGraph;

	private void read() throws Exception {
		String base = "/Users/mnamaki/Documents/Education/PhD/Spring2017/GTAR/DATA/MovieLens/ml-10M100K/";

		HashMap<Integer, User> usersOfId = new HashMap<Integer, User>();
		HashMap<Integer, Movie> moviesId = new HashMap<Integer, Movie>();

		// User
		// FileInputStream fisUser = new FileInputStream(base + "users.txt");
		// BufferedReader brUser = new BufferedReader(new
		// InputStreamReader(fisUser));
		// String line = null;
		// while ((line = brUser.readLine()) != null) {
		// // 1::F::1::10::48067
		// String[] userSplit = line.split("::");
		// usersOfId.put(Integer.parseInt(userSplit[0]), new
		// User(Integer.parseInt(userSplit[0]), userSplit[1],
		// Integer.parseInt(userSplit[2]), Integer.parseInt(userSplit[3]),
		// userSplit[4]));
		// }
		// brUser.close();

		// System.out.println("usersOfId size:" + usersOfId.size());

		// Movie
		FileInputStream fisMovie = new FileInputStream(base + "movies.txt");
		BufferedReader brMovie = new BufferedReader(new InputStreamReader(fisMovie));
		String line = null;
		while ((line = brMovie.readLine()) != null) {
			// 3910::Dancer in the Dark (2000)::Drama|Musical
			String[] movieSplit = line.split("::");
			ArrayList<String> categories = new ArrayList<String>();
			String[] categoriesSplit = movieSplit[2].split("\\|");
			for (String s : categoriesSplit) {
				categories.add(s);
			}
			moviesId.put(Integer.parseInt(movieSplit[0]),
					new Movie(Integer.parseInt(movieSplit[0]), movieSplit[1], categories));

		}
		brMovie.close();

		System.out.println("moviesId size:" + moviesId.size());

		// Users
		FileInputStream fisUser = new FileInputStream(base + "ratings.txt");
		BufferedReader brUser = new BufferedReader(new InputStreamReader(fisUser));
		line = null;
		while ((line = brUser.readLine()) != null) {
			// 1::1193::5::978300760
			String[] ratingSplit = line.split("::");
			usersOfId.put(Integer.parseInt(ratingSplit[0]),
					new User(Integer.parseInt(ratingSplit[0]), null, null, null, null));
		}
		brUser.close();

		int minTimeStamp = Integer.MAX_VALUE;
		int maxTimeStamp = Integer.MIN_VALUE;
		// Rating
		ArrayList<Rating> ratings = new ArrayList<Rating>();
		FileInputStream fisRating = new FileInputStream(base + "ratings.txt");
		BufferedReader brRating = new BufferedReader(new InputStreamReader(fisRating));
		line = null;
		while ((line = brRating.readLine()) != null) {
			// 1::1193::5::978300760
			String[] ratingSplit = line.split("::");
			ratings.add(new Rating(Integer.parseInt(ratingSplit[0]), Integer.parseInt(ratingSplit[1]),
					Double.parseDouble(ratingSplit[2]), Integer.parseInt(ratingSplit[3])));

			minTimeStamp = Math.min(Integer.parseInt(ratingSplit[3]), minTimeStamp);
			maxTimeStamp = Math.max(Integer.parseInt(ratingSplit[3]), maxTimeStamp);
		}
		brRating.close();

		// data graph new graph
		File storeDir = new File(newDataGraphPath);
		dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir).newGraphDatabase();
		DummyFunctions.registerShutdownHook(dataGraph);

		int cnt = 1;
		Transaction tx1 = dataGraph.beginTx();

		HashMap<Integer, Long> nodeOfUserId = new HashMap<Integer, Long>();

		Random random = new Random();
		for (Integer userID : usersOfId.keySet()) {
			Node userNode = dataGraph.createNode();
			userNode.addLabel(Label.label("User"));
			// userNode.setProperty("gender", usersOfId.get(userID).gender);
			// userNode.setProperty("age", usersOfId.get(userID).age);
			// userNode.setProperty("occupation",
			// usersOfId.get(userID).occupation);
			// userNode.setProperty("zip_code", usersOfId.get(userID).zip_code);
			userNode.setProperty("group", random.nextInt(1500));
			nodeOfUserId.put(userID, userNode.getId());
			cnt++;
			if (cnt % 100000 == 0) {
				tx1.success();
				tx1.close();
				tx1 = dataGraph.beginTx();
			}
		}

		System.out.println("after users");

		HashMap<Integer, Long> nodeOfMovieId = new HashMap<Integer, Long>();
		for (Integer movieID : moviesId.keySet()) {
			Node movieNode = dataGraph.createNode();
			movieNode.addLabel(Label.label("Movie"));
			// String title;
			// ArrayList<String> category;
			movieNode.setProperty("title", moviesId.get(movieID).title);
			String catValue = "";
			for (String cat : moviesId.get(movieID).category) {
				catValue += cat + ",";

				Node catNode = dataGraph.createNode();
				catNode.addLabel(Label.label(cat));
				Relationship rel = movieNode.createRelationshipTo(catNode, RelationshipType.withName("cat"));
				rel.setProperty("timepoints", new int[] { 0, maxTimeStamp - minTimeStamp });

			}
			movieNode.setProperty("category", catValue);
			nodeOfMovieId.put(movieID, movieNode.getId());
			cnt++;
			if (cnt % 500000 == 0) {
				tx1.success();
				tx1.close();
				tx1 = dataGraph.beginTx();
			}
		}

		System.out.println("after movies");

		SimpleDateFormat f = new SimpleDateFormat("HH:mm");
		for (Rating r : ratings) {
			Relationship rel = dataGraph.getNodeById(usersOfId.get(r.userId).userId).createRelationshipTo(
					dataGraph.getNodeById(moviesId.get(r.movieId).movieId), RelationshipType.withName("rate"));
			rel.setProperty("rating", r.rating);

			// HH:mm
			Date date1 = new Date(r.timestamp);
			rel.setProperty("time", f.format(date1));

			cnt++;
			if (cnt % 500000 == 0) {
				tx1.success();
				tx1.close();
				tx1 = dataGraph.beginTx();
				System.out.println("after inseting " + cnt + "'s");
			}
		}

		tx1.success();
		tx1.close();
		dataGraph.shutdown();
	}
}

class User {
	// UserID::Gender::Age::Occupation::Zip-code
	Integer userId;
	String gender;
	Integer age;
	Integer occupation;
	String zip_code;

	public User(Integer userId, String gender, Integer age, Integer occupation, String zip_code) {
		this.userId = userId;
		this.gender = gender;
		this.age = age;
		this.occupation = occupation;
		this.zip_code = zip_code;

	}

}

class Movie {
	Integer movieId;
	String title;
	ArrayList<String> category;

	public Movie(Integer movieId, String title, ArrayList<String> category) {
		this.title = title;
		this.category = category;
		this.movieId = movieId;
	}
}

class Rating {
	Integer userId;
	Integer movieId;
	Double rating;
	Integer timestamp;

	public Rating(Integer userId, Integer movieId, Double rating, Integer timestamp) {
		this.userId = userId;
		this.movieId = movieId;
		this.rating = rating;
		this.timestamp = timestamp;
	}

}