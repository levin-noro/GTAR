package test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

import utilities.Dummy.DummyFunctions;

public class SimpleTest {

	public static void main(String[] args) throws Exception {

		// String timeFormat = "dd-MMM-yyyy";
		String timeFormat = "yyyy-MM-dd";
		// String timeFormat = "yyyy";
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(timeFormat);
		Date currentDate = simpleDateFormat.parse("1999-12-31");
		int i = 0;
		File foutG = new File(String.format("deltaEsEnron/G%03d.txt", i));
		FileOutputStream fosG = new FileOutputStream(foutG);
		BufferedWriter bwG = new BufferedWriter(new OutputStreamWriter(fosG));
		HashSet<String> soFar = new HashSet<String>();

		for (File file : DummyFunctions.getFilesInTheDirfinder(
				"/Users/mnamaki/Documents/Education/PhD/Spring2017/networkScience/data/enron/deltaE/")) {

			if (file.getPath().contains("idLabelMap.txt"))
				continue;

			FileInputStream fis = new FileInputStream(file);

			// Construct BufferedReader from InputStreamReader
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));

			// +,01-Jan-1800,470472,321001,INTERMEDIARY_OF
			String line = null;

			File foutE = null;
			FileOutputStream fosE = null;
			BufferedWriter bwE = null;

			while ((line = br.readLine()) != null) {
				String[] splittedLine = line.split(",");
				Date date = simpleDateFormat.parse(splittedLine[1]);
				// if (date.getMonth() > currentDate.getMonth() ||
				// (date.getYear() > currentDate.getYear())) {
				if (date.compareTo(currentDate) > 0) {
					i++;
					currentDate = date;

					// Gi
					bwG.close();
					foutG = new File(String.format("deltaEsEnron/G%03d.txt", i));
					fosG = new FileOutputStream(foutG, true);
					bwG = new BufferedWriter(new OutputStreamWriter(fosG));
//					bwG.write("TS:" + splittedLine[1]);
//					bwG.newLine();

					for (String historyLines : soFar) {
						bwG.write(historyLines);
						bwG.newLine();
					}

					// deltaE
					if (bwE != null)
						bwE.close();
					foutE = new File(String.format("deltaEsEnron/deltaE%03d.txt", i));
					fosE = new FileOutputStream(foutE, true);
					bwE = new BufferedWriter(new OutputStreamWriter(fosE));
//					bwE.write("TS:" + splittedLine[1]);
//					bwE.newLine();
				}

				String newLine = splittedLine[0] + " " + splittedLine[2] + " " + splittedLine[3] + " ";
				// bwG.write(newLine);
				// bwG.newLine();

				if (bwE != null) {
					bwE.write(newLine);
					bwE.newLine();
				}

				if (splittedLine[0].equals("+")) {
					soFar.add(splittedLine[2] + " " + splittedLine[3]);
				} else {
					soFar.remove(splittedLine[2] + " " + splittedLine[3]);
				}

			}

			br.close();

		}
		bwG.close();

	}

}
