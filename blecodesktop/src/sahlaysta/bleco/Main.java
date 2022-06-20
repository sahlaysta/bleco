package sahlaysta.bleco;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import sahlaysta.bleco.ui.GUI;

/** Bleco GUI main class.
 * @author sahlaysta */
public final class Main {
	
	private Main() {}
	
	/** Entry point for Bleco GUI.
	 * @param args unused */
	public static void main(String[] args) {
		new GUI().setVisible(true);
	}
	
	/** Returns the resource file input stream of the filename.
	 * @param resourceName the filename of the resource
	 * @return the resource file of the filename
	 * @throws IOException if an I/O exception occurs
	 * */
	public static InputStream getResource(String resourceName) throws IOException {
		return Main.class.getResource("resources/" + resourceName).openStream();
	}
	
	/** Returns all the bytes from a resource file.
	 * @param resourceName the filename of the resource
	 * @return a byte array containing the bytes
	 * read from the resource file
	 * @throws UncheckedIOException if an I/O exception occurs
	 * */
	public static byte[] getResourceBytes(String resourceName) {
		try {
			InputStream is = getResource(resourceName);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			
			byte[] buffer = new byte[8192];
			int bytesRead;
			while ((bytesRead = is.read(buffer)) != -1)
				baos.write(buffer, 0, bytesRead);
			
			return baos.toByteArray();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}