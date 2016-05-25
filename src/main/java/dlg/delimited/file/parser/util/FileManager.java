package dlg.delimited.file.parser.util;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;



public class FileManager {
	
	private Logger logger = Logger.getLogger(FileManager.class);
	
	
	private final String DATE_FORMAT = "yyyy-MM-dd_hhmmssSSS";
	
		
	/**
	 * Write a file to the disk, to the given path, with the specified fileName, 
	 * containing the received content
	 * @param path
	 * @param fileName
	 * @param content
	 * @return
	 */
	public File writeFileToDisk(final String path, final String fileName, String content) {
		
		// Concatenating parent's path with file name		
		String filePath = path.concat("/").concat(fileName);
		
		// Opening a new file with the indicated path and name
		File file = new File(filePath);
		Writer output = null;
		try {
			// Creating a file writer and a buffered writer
			output = new BufferedWriter(new FileWriter(file));
			// assigning the content of the file to the output writer
			output.write(content);
			
			// closing the connection with the file
			output.close();
			// returning the file
			return file;
		} catch (final IOException e) {
			logger.error("Error writing file " + filePath, e);
		}
		return null;
		
	}

	/**
	 * Removes the received file from the disk
	 * @param file
	 */
	public Boolean removeFile(File file) {
		if (file != null) {
			
			String fileName = file.getName();
			try {
				
				boolean succeed = file.delete();
				if (succeed) {
					logger.debug("File removed successdully from disk, " + fileName);
				} else {
					logger.error("File couldn't been removed from disk, " + fileName);
				}
				return succeed;
			} catch (Exception e) {
				logger.error("Error trying to remove file, " + fileName, e);
				
			}		
		}
		return false;
	}
	
	public File loadFileFromDisk(String filePath) {
		if (StringUtils.isEmpty(filePath)) {
			throw new IllegalArgumentException("Invalid File Name: " + filePath);
		}
		try {
			
			return new File(filePath);
			
		} catch (Exception e) {
			logger.error("Error trying to load file " + filePath, e);
			throw new IllegalArgumentException("Invalid File Name: " + filePath);
		}
	}
	
	public String getFileContent(String filePath) {
		try {
			File file = loadFileFromDisk(filePath);
			String fileContent = getFileContent(file);
			file = null;
			return fileContent;
		} catch (Exception e) {
			logger.error("Error trying to read file from disk " + filePath, e);
		}
		return null;
	}
	
	public String getFileContent(File file) {
		if (file != null) {
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(file);					
				
				return syncReadText(fis, "UTF-8");			
			
			} catch (IOException e) {
				logger.error("Error trying to read content from a file " + file.getName(), e);
			} finally {
				if (fis != null) {
					try {
						fis.close();
					} catch (IOException e) {
						logger.error("Error closing file input stream for file: ", e);
					}
				}
			}
		}
		return null;
	}
	
	public static synchronized String syncReadText(InputStream is, String charset) throws IOException {
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    byte[] bytes = new byte[4096];
	    for(int len;(len = is.read(bytes))>0;)
	        baos.write(bytes, 0, len);
	    return new String(baos.toByteArray(), charset);
	}
	
	public String readTextFromInputStream(InputStream is, String charset) throws IOException {
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    byte[] bytes = new byte[4096];
	    for(int len;(len = is.read(bytes))>0;)
	        baos.write(bytes, 0, len);
	    return new String(baos.toByteArray(), charset);
	}
	
	public void moveFileTo(File file, String newFolderPath, Boolean addTimeStamp) {
		try {
			String name = getName(file.getName(), addTimeStamp);
						
			String fullPath = newFolderPath.concat("/").concat(name);
			if (file.renameTo(new File(fullPath.replaceAll("/{2,}","/")))) {
				logger.debug("File is moved successful!");
			} else {
				logger.error("File is failed to move!");
			}

		} catch (Exception e) {
			logger.error("Error trying to move file to " + newFolderPath
					+ " folder", e);
		}
	}
	
	
	private String getName(String name, Boolean addTimeStamp) {
		if (addTimeStamp) {
			final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
			Calendar calendar = Calendar.getInstance();
			String formattedDate = sdf.format(calendar.getTime());
			String[] split = name.split("\\.");
			int length = split.length;
			String extension = "";
			StringBuilder sb = new StringBuilder();
			if (length > 1) {
				extension = split[length - 1];				
			}
			if (StringUtils.isEmpty(extension)) {
				sb.append(name).append("_").append(formattedDate);
			} else {
				sb.append(name.replace(".".concat(extension), "")).append("_").append(formattedDate).append(".").append(extension);
			}
			return sb.toString();			
		}
		return name;
	}

	public Boolean copyFile(File originalFile, String newFolderPath, Boolean addTimeStamp) {
		InputStream inStream = null;
		OutputStream outStream = null;

		try {
			String name = getName(originalFile.getName(), addTimeStamp);
			
			String fullPath = newFolderPath.concat("/").concat(name);
			File endFile = new File(fullPath.replaceAll("/{2,}","/"));

			inStream = new FileInputStream(originalFile);
			outStream = new FileOutputStream(endFile);

			byte[] buffer = new byte[1024];

			int length;
			// copy the file content in bytes
			while ((length = inStream.read(buffer)) > 0) {

				outStream.write(buffer, 0, length);

			}

			inStream.close();
			outStream.close();
			return true;
		} catch (IOException e) {
			logger.error("Error copying file to new folder", e);
		}
		return false;
	}
	
	public Boolean copyAndRemoveFile(File originalFile, String newFolderPath, Boolean addTimeStamp) {
		InputStream inStream = null;
		OutputStream outStream = null;

		try {
			String name = getName(originalFile.getName(), addTimeStamp);
			
			String fullPath = newFolderPath.concat("/").concat(name);
			File endFile = new File(fullPath.replaceAll("/{2,}","/"));

			inStream = new FileInputStream(originalFile);
			outStream = new FileOutputStream(endFile);

			byte[] buffer = new byte[1024];

			int length;
			// copy the file content in bytes
			while ((length = inStream.read(buffer)) > 0) {

				outStream.write(buffer, 0, length);

			}

			inStream.close();
			outStream.close();

			Boolean removedFile = removeFile(originalFile);
			if (removedFile) {
				logger.debug("File has been copied to new folder and removed from old one successful!");				
			} else {
				logger.error("Failed to remove file: " + originalFile.getName());
			}
			return removedFile;
		} catch (IOException e) {
			logger.error("Error moving file to new folder", e);
		}
		return false;
	}
	
	
	
	/**
	 * Retrieves a list of all the files found in the given folders,
	 * including their folder path, i.e.: /src/somefolder/file.name
	 * JUST FILEs, it will ignore any sub-folder
	 * @param folderList
	 * @return
	 */
	public List<String> getFilesNameInFolders(List<String> folderList) {
		if (folderList.isEmpty()) {
			return new ArrayList<String>(0);
		}
		List<String> foundFiles = new ArrayList<String>();
		for (String folderPath : folderList) {			
			foundFiles.addAll(getFilesNameInFolder(folderPath));
		}
		return foundFiles;
	}
	
	public Map<String, Long> getFilesAndSizeNameInFolders(List<String> folderList) {
		if (folderList.isEmpty()) {
			return Collections.EMPTY_MAP;
		}
		Map<String, Long> foundFiles = new HashMap<String, Long>();
		for (String folderPath : folderList) {			
			foundFiles.putAll(getFilesNameAndSizeInFolder(folderPath));
		}
		return foundFiles;
	}

	/**
	 * Retrieves a list of fileNames, including their path, in the given folder.
	 * JUST FILEs, it will ignore any sub-folder.
	 * If checkLocked is true, it will check if the files are being written before 
	 * adding them to the result list.
	 * @param folderPath
	 * @param checkLocked
	 * @return
	 */
	public List<String> getFilesNameInFolder(String folderPath) {
		File folder = loadFileFromDisk(folderPath);
		List<String> foundFiles = new ArrayList<String>();
		String name;
		if (folder != null) {
			File[] filesList = folder.listFiles();
			if (filesList != null && filesList.length > 0) {
				for (File file : filesList) {
					if (file.isFile()) {						
						name = file.getName();
						foundFiles.add(folderPath + "/" + name);						
						
					}
				}
			}
		}
		return foundFiles;
	}	
	
	public Map<String, Long> getFilesNameAndSizeInFolder(String folderPath) {
		File folder = loadFileFromDisk(folderPath);
		Map<String, Long> foundFiles = new HashMap<String, Long>();
		String name;
		if (folder != null) {
			File[] filesList = folder.listFiles();
			if (filesList != null && filesList.length > 0) {
				
				Long size;
				for (File file : filesList) {
					size = 0L;
										
					if (file.isFile()) {		
						name = file.getName();						
						size = file.length();
						foundFiles.put(folderPath + "/" + name, size);						
					}
				}
			}
		}
		return foundFiles;
	}	

	public static String[] getLines(String fileContent) {
		String[] split = fileContent.split("\n\r");
		if (split.length == 1) {
			split = fileContent.split("\r\n");
		}
		if (split.length == 1) {
			split = fileContent.split("\n");
		}
		if (split.length == 1) {
			split = fileContent.split("\r");
		}
		return split;
	}
	
	public String[] getLinesFromContent(String fileContent) {
		return FileManager.getLines(fileContent);
	}
	
}
