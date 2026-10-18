package Utils;

import com.microsoft.playwright.Download;
import com.microsoft.playwright.Page;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.testng.Reporter;


//Handles file downloads triggered by Playwright — saves them to test-output/Downloads/.
public class DownloadUtils {

    private static final Logger log = LogManager.getLogger(DownloadUtils.class);
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

  //File download verify by file name ends with
  	public static boolean isFileAvailable(String filename) throws IOException {
  		String absolutepath=new File("FilesDownload").getAbsolutePath();
  		File folder = new File(absolutepath);
  		File [] listOfFiles=folder.listFiles();
  		boolean isFileAvailable=false;
  		for(File listOfFile : listOfFiles ) {
  			if(listOfFile.isFile()) {
  				String fileName= listOfFile.getName();
  				Reporter.log("File Name ----- "+fileName);
  				if(fileName.endsWith(filename)) {
  					Reporter.log("File downloaded Successfully");
  					isFileAvailable=true;
  				}
  			}
  		}
  		return isFileAvailable;
  	}
  	
  //File download verify by file name start with
  	public static boolean IsFileAvailable(String filename) throws IOException {
  		String absolutepath=new File("FilesDownload").getAbsolutePath();
  		File folder = new File(absolutepath);
  		File [] listOfFiles=folder.listFiles();
  		boolean isFileAvailable=false;
  		for(File listOfFile : listOfFiles ) {
  			if(listOfFile.isFile()) {
  				String fileName= listOfFile.getName();
  				Reporter.log("File Name ----- "+fileName);
  				if(fileName.startsWith(filename)) {
  					Reporter.log("File downloaded Successfully");
  					isFileAvailable=true;
  				}
  			}
  		}
  		return isFileAvailable;
  	}
  //Delete file from downloaded folder
  	public void DeleteDownloadedFileFromFoder() throws IOException {
  		String absolutepath=new File("FilesDownload").getAbsolutePath();
  		File directory = new File(absolutepath);
  		FileUtils.cleanDirectory(directory);
  	}
}
