package transportation;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.stream.Stream;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.config.Setting;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import utilities.CopyDirVisitor;
import utilities.Dummy.DummyFunctions;

public class TemporalSnapshotGen {

	static String completeDataGraphPath = "/Users/mnamaki/Documents/Education/PhD/Spring2017/GTAR/DATA/Transportation/TransportationBig30M.db";

	public static void main(String[] args) throws Exception {
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-completeDataGraphPath")) {
				completeDataGraphPath = args[++i];
			}
		}
		
		TemporalSnapshotGen tsg = new TemporalSnapshotGen();
		// tsg.traverseOnce();
		// tsg.addSomeNoise();
		tsg.run();

	}

	private void traverseOnce() {
		File storeDir = new File(completeDataGraphPath);

		GraphDatabaseService dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);

		Transaction tx1 = dataGraph.beginTx();
		int numOfCurrRel = 0;
		for (Relationship rel : dataGraph.getAllRelationships()) {
			numOfCurrRel++;
		}
		System.out.println("numOfCurrRel: " + numOfCurrRel);
		tx1.success();
		tx1.close();
		dataGraph.shutdown();

	}

	private void addSomeNoise() {
		File storeDir = new File(completeDataGraphPath);

		GraphDatabaseService dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.newGraphDatabase();

		DummyFunctions.registerShutdownHook(dataGraph);

		Transaction tx1 = dataGraph.beginTx();

		int numOfCurrRel = 0;
		int expectedNumOfRels = 30000000;

		for (Relationship rel : dataGraph.getAllRelationships()) {
			numOfCurrRel++;
		}

		System.out.println("numOfCurrRel: " + numOfCurrRel);

		int toBeAddedRandomly = expectedNumOfRels - numOfCurrRel;

		System.out.println("toBeAddedRandomly: " + toBeAddedRandomly);

		ArrayList<Long> autoNodeIds = new ArrayList<Long>();
		ArrayList<Long> linkNodeIds = new ArrayList<Long>();
		for (Node node : dataGraph.getAllNodes()) {
			if (node.getLabels().iterator().next().toString().toLowerCase().equals("auto")) {
				autoNodeIds.add(node.getId());
			}
			if (node.getLabels().iterator().next().toString().toLowerCase().startsWith("link")) {
				linkNodeIds.add(node.getId());
			}
		}

		System.out.println("autoNodeIds: " + autoNodeIds.size());
		System.out.println("linkNodeIds: " + linkNodeIds.size());

		Random rnd = new Random();
		for (int i = 0; i < toBeAddedRandomly; i++) {
			long selectedAutoId = autoNodeIds.get(rnd.nextInt(autoNodeIds.size()));
			long selectedLinkId = linkNodeIds.get(rnd.nextInt(linkNodeIds.size()));

			if (!existARelationship(dataGraph, selectedAutoId, selectedLinkId)) {
				Relationship newRel = dataGraph.getNodeById(selectedAutoId)
						.createRelationshipTo(dataGraph.getNodeById(selectedLinkId), RelationshipType.withName("at"));
				newRel.setProperty("timepoints", new int[] { 10, 30 });
			} else {
				i--;
			}

			if (i % 100000 == 0) {
				tx1.success();
				tx1.close();
				tx1 = dataGraph.beginTx();
			}

		}
		System.out.println("after adding");

		int finalRels = 0;
		for (Relationship rel : dataGraph.getAllRelationships()) {
			finalRels++;
		}
		System.out.println("finalRels: " + finalRels);

		tx1.success();
		tx1.close();
		dataGraph.shutdown();

	}

	private boolean existARelationship(GraphDatabaseService dataGraph, long selectedAuto, long selectedLink) {

		for (Relationship rel : dataGraph.getNodeById(selectedAuto).getRelationships(Direction.OUTGOING)) {
			if (rel.getEndNode().getId() == selectedLink) {
				return true;
			}
		}
		return false;
	}

	private void run() throws Exception {

		int relationshipsStep = 5000000;

		String parentDir = completeDataGraphPath.substring(0, completeDataGraphPath.lastIndexOf("/"));
		System.out.println("parentDir: " + parentDir);
		String newPath = parentDir + "/trans_" + 0 + ".db";
		copyGraphAndGetItsNewPath(completeDataGraphPath, newPath);

		File storeDir = new File(newPath);
		GraphDatabaseService dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
				.newGraphDatabase();
		// DummyFunctions.registerShutdownHook(dataGraph);
		Transaction tx = dataGraph.beginTx();
		int maxRelId = 0;
		for (Relationship rel : dataGraph.getAllRelationships()) {
			maxRelId = Math.max(maxRelId, (int) rel.getId());
		}
		System.out.println("maxRelId: " + maxRelId);
		// System.out.println("before success1");
		tx.success();
		// System.out.println("before close");
		tx.close();
		// System.out.println("before shutdown dataGraph");
		dataGraph.shutdown();
		// System.out.println("after shutdown dataGraph");

		Random rnd = new Random();
		for (int i = 1; i < 6; i++) {
			copyGraphAndGetItsNewPath(parentDir + "/trans_" + (i - 1) + ".db", parentDir + "/trans_" + i + ".db");

			
			File storeDirNew = new File(parentDir + "/trans_" + i + ".db");
			GraphDatabaseService dataGraphNew = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDirNew)
					.newGraphDatabase();
			// DummyFunctions.registerShutdownHook(dataGraphNew);
			System.out.println("after copying and init " + parentDir + "/trans_" + i + ".db");

			Transaction txNew = dataGraphNew.beginTx();
//			System.out.println("after transaction " + i);
			for (int j = 0; j < relationshipsStep; j++) {
				long selectedRelId = rnd.nextInt(maxRelId);
				try {
					Relationship rel = dataGraphNew.getRelationshipById(selectedRelId);
					rel.delete();
//					System.out.println("selectedRelId: " + selectedRelId + " j:" + j);
					if (j % 100000 == 0) {
						txNew.success();
						txNew.close();
						txNew = dataGraphNew.beginTx();
						System.out.println("i: " + i + " , j:" + j);
					}
				} catch (Exception exc) {
					j--;
//					System.out.println("selectedRelId: " + selectedRelId + " j:" + j);
				}
			}

			int currRel = 0;
			for (Relationship rel : dataGraphNew.getAllRelationships()) {
				currRel++;
			}
			System.out.println("currRel: " + currRel);
			txNew.success();
			txNew.close();
			dataGraphNew.shutdown();
		}
	}

	// public void copyFolder(String src, String dest) throws IOException {
	// File srcFile = new File(src);
	// File destFile = new File(dest);
	// try (Stream<Path> stream = Files.walk(srcFile.toPath())) {
	// stream.forEach(sourcePath -> {
	// try {
	// Files.copy(
	// /* Source Path */
	// sourcePath,
	// /* Destination Path */
	// srcFile.toPath().resolve(destFile.toPath().relativize(sourcePath)));
	// } catch (Exception e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// });
	// }
	// }

	public boolean deleteCompletely(Path rootPath) {
		try {
			Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					// System.out.println("delete file: " +
					// file.toString());
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					Files.delete(dir);
					// System.out.println("delete dir: " + dir.toString());
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	public Path copyGraphAndGetItsNewPath(String dataGraphPath, String newGraphPath) throws Exception {

		System.out.println("copying is started..." + new Date());

		Path sourcePath = Paths.get(dataGraphPath);
		Path destinationPath = Paths.get(newGraphPath);

		if (Files.exists(destinationPath)) {
			deleteCompletely(destinationPath);
		}

		Files.walkFileTree(sourcePath,
				new CopyDirVisitor(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING));
		System.out.println("copying is finished..." + new Date());
		return destinationPath;

	}

}
