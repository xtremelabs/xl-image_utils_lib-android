package com.xtremelabs.imageutils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class GenericDiskOperations {
	public static void clearDirectory(File directory) {
		deleteDirectory(directory);
	}

	public static void deleteFile(File directory, String name) {
		File file = getFile(directory, name);
		if (!file.isDirectory()) {
			file.delete();
		} else {
			deleteDirectory(file);
		}
	}

	public static File getFile(File directory, String filename) {
		return new File(directory, filename);
	}

	public static boolean isOnDisk(File directory, String filename) {
		File file = new File(directory, filename);
		return file.exists();
	}

	public static void loadStreamToFile(InputStream inputStream, File directory, String filename) throws IOException {
		File file = new File(directory, filename);
		FileOutputStream fileOutputStream = null;

		try {
			fileOutputStream = new FileOutputStream(file);
			byte[] buffer = new byte[1024];
			int bytesRead;
			while ((bytesRead = inputStream.read(buffer)) > 0) {
				fileOutputStream.write(buffer, 0, bytesRead);
			}
		} catch (IOException e) {
			file.delete();
			throw e;
		} catch (OutOfMemoryError e) {
			file.delete();
			throw e;
		} finally {
			try {
				if (fileOutputStream != null) {
					fileOutputStream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			try {
				if (inputStream != null) {
					inputStream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static void deleteDirectory(File directory) {
		File[] files = directory.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				deleteDirectory(file);
			}
			file.delete();
		}
	}
}
