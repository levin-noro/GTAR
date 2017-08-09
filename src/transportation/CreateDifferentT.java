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
import java.util.Date;
import java.util.Random;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

import utilities.CopyDirVisitor;

public class CreateDifferentT {

	private static String dataGraphPath = "/Users/mnamaki/Documents/Education/PhD/Spring2017/GTAR/Synthetics/TransVaryingT/trans_3.db";
	private static double currentMaxT = 91614d;

	public static void main(String[] args) throws Exception {

		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-dataGraphPath")) {
				dataGraphPath = args[++i];
			} else if (args[i].equals("-currentMaxT")) {
				currentMaxT = Double.parseDouble(args[++i]);
			}
		}

		Double[] divid = new Double[] { 10d, 20d, 30d };

		for (int d = 0; d < divid.length; d++) {
			String parentDir = dataGraphPath.substring(0, dataGraphPath.lastIndexOf("/"));
			System.out.println("parentDir: " + parentDir);
			String newPath = parentDir + "/trans_T_" + divid[d].intValue() + ".db";
			copyGraphAndGetItsNewPath(dataGraphPath, newPath);

			File storeDir = new File(newPath);
			GraphDatabaseService dataGraph = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir)
					.setConfig(GraphDatabaseSettings.pagecache_memory, "8g")
					.setConfig(GraphDatabaseSettings.allow_store_upgrade, "true").newGraphDatabase();

			System.out.println("newPath: " + dataGraphPath);
			Transaction tx1 = dataGraph.beginTx();
			int cnt = 0;
			Random rnd = new Random();
			for (Relationship rel : dataGraph.getAllRelationships()) {
				if (rel.hasProperty("timepoints")) {
					cnt++;
					int[] timepoints = (int[]) rel.getProperty("timepoints");
					for (int t = 0; t < timepoints.length; t++) {
						timepoints[t] /= (int) Math.ceil(currentMaxT / divid[d]);
					}

					int interval = (int) (divid[d] / 10);
					if (interval > 1) {
						int middle =  rnd.nextInt(timepoints[1]) + timepoints[0];
					}

					rel.setProperty("timepoints", timepoints);
					if (cnt % 1000000 == 0) {
						tx1.success();
						tx1.close();
						tx1 = dataGraph.beginTx();
						System.out.println("cnt: " + cnt);
					}
				}
			}

			tx1.success();
			tx1.close();
			dataGraph.shutdown();

		}

	}

	public static Path copyGraphAndGetItsNewPath(String dataGraphPath, String newGraphPath) throws Exception {

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

	public static boolean deleteCompletely(Path rootPath) {
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

}
