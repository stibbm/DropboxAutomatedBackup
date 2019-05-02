package pack;

import java.util.Scanner;
import java.util.*;
import java.io.*;
import java.math.BigDecimal;

import com.dropbox.core.DbxApiException;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.UploadErrorException;
import com.dropbox.core.v2.users.FullAccount;

public class Main {

	static DbxRequestConfig config;
	static DbxClientV2 client;
	static ArrayList<String> failedUploads = new ArrayList<String>();
	static final String LOGFILE_PATH = "[insertDirectory where you want your logfile to be stored]";
	static final String BACKUPLOGFILE_PATH = "[Not used for anything yet]";
	static HashMap<String, Long> filesUploaded = new HashMap<String, Long>();
	static BigDecimal amountUploadedMB = new BigDecimal("0");
	static BigDecimal startTime;
	static FIFOList recentUploads = new FIFOList(30);

	public static void main(String[] args) throws IOException, DbxApiException, DbxException {


		long startTimeMillis = System.currentTimeMillis();
		String s_startTimeMillis = "" + startTimeMillis;
		startTime = new BigDecimal(s_startTimeMillis);

		// since I will always be running this from this computer I will just do the
		// tracking of whats been backed up already as a
		// locally stored file because that will make this much easier to do.

		// save full file paths of files that have been backed up to dropbox and store
		// the
		// corresponding last modified date of the version that was backup up to
		// dropbox.
		File logfile = new File(LOGFILE_PATH);
		File backupLogfile = new File(BACKUPLOGFILE_PATH);
		if (logfile.exists() == true) {
			// logfile.createNewFile();
			BufferedReader br = new BufferedReader(new FileReader(logfile));
			String line = null;
			while ((line = br.readLine()) != null) {
				String[] tokens = line.split("-:-:-");
				String fullPath = null;
				long lastModified = 0;
				if (tokens.length == 2) {
					try {
						fullPath = tokens[0];
						lastModified = Long.parseLong(tokens[1]);
					} catch (Exception e) {
						System.out.println("Error in format of logfile");
					}
					if (filesUploaded.keySet().contains(fullPath)) {
						System.out.println("Duplicate Keys found in logfile, this is an error.");
					} else {
						filesUploaded.put(fullPath, lastModified);
					}

				}
			}

		} else if (logfile.exists() == false) {
			System.out.println("Creating log file");
			logfile.createNewFile();

		}

		// else if (backupLogfile.exists() == true) {

		// }

		String token = getAccessToken();
		config = DbxRequestConfig.newBuilder("dropbox/java-tutorial").build();
		client = new DbxClientV2(config, token);
		FullAccount account = client.users().getCurrentAccount();
		System.out.println(account.getName().getDisplayName());
		String dir = System.getProperty("user.dir");
		String directory = dir;
		
		System.out.println("upload starting");

		// System.exit(0);
		if (args.length == 1) {
			directory = args[0];
			System.out.println(args[0]);
			System.out.println("Backing up directory : " + args[0]);
		}
		uploadFilesInDirectory(new File(directory), directory);

	}

	public static void uploadFilesInDirectory(File dir, String path)
			throws UploadErrorException, IOException, DbxException {
		try {
			if (dir.isDirectory()) {
				File[] files = dir.listFiles();
				if (files != null) {
					for (File file : files) {
						try {
							if (file.isDirectory()
									// optional flags to look for to avoid backing up things that you likely wouldn't 
									// actually care to have backed up
									/* ) { */ && (!file.getAbsolutePath().toLowerCase().contains(".config"))
									&& (!file.getAbsolutePath().toLowerCase().contains(".metadata"))
									&& (!file.getAbsolutePath().toLowerCase().contains(".dropbox"))
									&& (!file.getAbsolutePath().toLowerCase().contains("logs"))
									&& (!file.getAbsolutePath().toLowerCase().contains(".mysql"))
									&& (!file.getAbsolutePath().toLowerCase().contains("Dropbox"))
									&& (!file.getAbsolutePath().toLowerCase().contains(".iml"))
									&& (!file.getAbsolutePath().toLowerCase().contains("~lock"))
									&& (!file.getAbsolutePath().toLowerCase().contains("cache"))
									&& (!file.getAbsolutePath().toLowerCase().contains("encrypted"))) {
								System.out.println("running on directory : " + file.getName());
								uploadFilesInDirectory(file, path + "/" + file.getName());
							} else if ((!file.getAbsolutePath().toLowerCase().contains("snap"))
									&& (!file.getAbsolutePath().toLowerCase().contains(".config"))
									&& (!file.getAbsolutePath().toLowerCase().contains(".metadata"))
									&& (!file.getAbsolutePath().toLowerCase().contains(".dropbox"))
									&& (!file.getAbsolutePath().toLowerCase().contains("logs"))
									&& (!file.getAbsolutePath().toLowerCase().contains(".mysql"))
									&& (!file.getAbsolutePath().toLowerCase().contains("Dropbox"))
									&& (!file.getAbsolutePath().toLowerCase().contains(".iml"))
									&& (!file.getAbsolutePath().toLowerCase().contains("~lock"))
									&& (!file.getAbsolutePath().toLowerCase().contains("cache"))
									&& (!file.getAbsolutePath().toLowerCase().contains("encrypted"))) {
								// &&
								// file.getName().contains(".zip")) {
								// System.out.println(file.getName());
								uploadFile(file, path);
							}
						} catch (Exception e) {
							// e.printStackTrace();
							// System.out.println("file that failed was " + dir.getAbsolutePath());
							// failedUploads.add(dir.getAbsolutePath());
							// Thread.sleep(3000);

						}
					}
				}
			}
		} catch (Exception e) {
			// e.printStackTrace();
		}
		for (String key : filesUploaded.keySet()) {
			System.out.println("key = " + key);
		}
	}

	public static void uploadFile(File file, String path) throws IOException, UploadErrorException, DbxException {
		String fullPath = null;
		long lastModified = 0;
		System.out.println(file.getPath());
		if (!filesUploaded.keySet().contains(file.getAbsolutePath())) {
			try (InputStream in = new FileInputStream(file)) {
				try {
					// System.out.println(path + file.getName());
					System.out.println("Uploading file " + file.getAbsolutePath());
					double fileSizeMB = ((double) file.length()) / (1024 * 1024);
					String s_fileSizeMB = "" + fileSizeMB;
					BigDecimal bdFileSizeMB = new BigDecimal(s_fileSizeMB);
					System.out.println("File Size (MB) = " + fileSizeMB);

					FileMetadata metadata = client.files().uploadBuilder(path + "/" + file.getName())
							.uploadAndFinish(in);

					recentUploads.add(fileSizeMB);
					double recentUploadRate = recentUploads.getSum() / recentUploads.getList().size();
					System.out.println("Recent Upload Rate = " + recentUploadRate);

					fullPath = file.getAbsolutePath();
					lastModified = file.lastModified();
					amountUploadedMB = amountUploadedMB.add(bdFileSizeMB);
					// System.out.println("Check 1");
					long currTimeMillis = System.currentTimeMillis();
					String s_currTimeMillis = "" + currTimeMillis;
					BigDecimal currTime = new BigDecimal(s_currTimeMillis);
					// System.out.println("check 2");
					BigDecimal milliseconds = currTime.subtract(startTime);
					BigDecimal seconds = milliseconds.divide(new BigDecimal("1000"), BigDecimal.ROUND_HALF_UP);
					// System.out.println("check 3");
					BigDecimal amountUploadedMb = amountUploadedMB.multiply(new BigDecimal("8"));
					BigDecimal uploadRate = amountUploadedMb.divide(seconds, BigDecimal.ROUND_HALF_UP);

					System.out.println("Overall upload rate = " + uploadRate.toString() + " Mbps");
					System.out.println("Total amount uploaded = " + amountUploadedMB.toString() + "MB");

				} catch (Exception e) {
					e.printStackTrace();
					System.out.println("ERROR OCCURRED");
					try {
						// Thread.sleep(3000);
					} catch (Exception ee) {
						ee.printStackTrace();
					}
				}

				if (!filesUploaded.keySet().contains(fullPath)) {
					filesUploaded.put(fullPath, lastModified);

				} else {
					// ERROR
					System.out.println("ERROR");
				}
			}
			FileWriter fileWriter = new FileWriter(LOGFILE_PATH, true);
			PrintWriter pw = new PrintWriter(fileWriter);
			String text = fullPath + "-:-:-" + lastModified;
			pw.println(text);
			pw.close();
			fileWriter.close();
			// i think the pw calling close will automatically close the filewriter as well.

		}
		// System.out.println("uploaded " + file.getName());
	}

	public static String getAccessToken() throws FileNotFoundException {
		Scanner s = new Scanner(new File("AccessNew.txt"));
		String ret = "";
		while (s.hasNextLine()) {
			String line = s.nextLine();
			if(line.length()>10) {
				ret += line;
			}
		}
		return ret;
	}

}
