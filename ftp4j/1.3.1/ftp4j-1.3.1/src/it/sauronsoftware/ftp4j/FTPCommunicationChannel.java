/*
 * ftp4j - A pure Java FTP client library
 * 
 * Copyright (C) 2008 Carlo Pelliccia (www.sauronsoftware.it)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.sauronsoftware.ftp4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * This class is used to represent a communication channel with a FTP server.
 * 
 * @author Carlo Pelliccia
 * @version 1.1
 */
public class FTPCommunicationChannel {

	/**
	 * The FTPCommunicationListener objects registered on the channel.
	 */
	private ArrayList communicationListeners = new ArrayList();

	/**
	 * The connection.
	 */
	private FTPConnection connection = null;

	/**
	 * The stream-reader channel established with the remote server.
	 */
	private NVTASCIIReader reader = null;

	/**
	 * The stream-writer channel established with the remote server.
	 */
	private NVTASCIIWriter writer = null;

	/**
	 * It builds a FTP communication channel.
	 * 
	 * @param connection
	 *            The underlying connection.
	 * @param charsetName
	 *            The name of the charset that has to be used to encode and
	 *            decode the communication.
	 * @throws IOException
	 *             If a I/O error occurs.
	 */
	public FTPCommunicationChannel(FTPConnection connection, String charsetName)
			throws IOException {
		this.connection = connection;
		InputStream inStream = connection.getInputStream();
		OutputStream outStream = connection.getOutputStream();
		// Wrap the streams into reader and writer objects.
		reader = new NVTASCIIReader(inStream, charsetName);
		writer = new NVTASCIIWriter(outStream, charsetName);
	}

	/**
	 * This method adds a FTPCommunicationListener to the object.
	 * 
	 * @param listener
	 *            The listener.
	 */
	public void addCommunicationListener(FTPCommunicationListener listener) {
		communicationListeners.add(listener);
	}

	/**
	 * This method removes a FTPCommunicationListener previously added to the
	 * object.
	 * 
	 * @param listener
	 *            The listener to be removed.
	 */
	public void removeCommunicationListener(FTPCommunicationListener listener) {
		communicationListeners.remove(listener);
	}

	/**
	 * Closes the channel.
	 */
	public void close() {
		try {
			connection.close();
		} catch (Exception e) {
			;
		}
	}

	/**
	 * This method returns a list with all the FTPCommunicationListener used by
	 * the client.
	 * 
	 * @return A list with all the FTPCommunicationListener used by the client.
	 */
	public FTPCommunicationListener[] getCommunicationListeners() {
		int size = communicationListeners.size();
		FTPCommunicationListener[] ret = new FTPCommunicationListener[size];
		for (int i = 0; i < size; i++) {
			ret[i] = (FTPCommunicationListener) communicationListeners.get(i);
		}
		return ret;
	}

	/**
	 * This method reads a line from the remote server.
	 * 
	 * @return The string read.
	 * @throws IOException
	 *             If an I/O error occurs during the operation.
	 */
	private String read() throws IOException {
		// Read the line from the server.
		String line = reader.readLine();
		if (line == null) {
			throw new IOException("FTPConnection closed");
		}
		// Call received() method on every communication listener
		// registered.
		for (Iterator iter = communicationListeners.iterator(); iter.hasNext();) {
			FTPCommunicationListener l = (FTPCommunicationListener) iter.next();
			l.received(line);
		}
		// Return the line read.
		return line;
	}

	/**
	 * This method sends a command line to the server.
	 * 
	 * @param command
	 *            The command to be sent.
	 * @throws IOException
	 *             If an I/O error occurs.
	 */
	public void sendFTPCommand(String command) throws IOException {
		writer.writeLine(command);
		for (Iterator iter = communicationListeners.iterator(); iter.hasNext();) {
			FTPCommunicationListener l = (FTPCommunicationListener) iter.next();
			l.sent(command);
		}
	}

	/**
	 * This method reads and parses a FTP reply statement from the server.
	 * 
	 * @return The reply from the server.
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws FTPIllegalReplyException
	 *             If the server doesn't reply in a FTP-compliant way.
	 */
	public FTPReply readFTPReply() throws IOException, FTPIllegalReplyException {
		int code = 0;
		ArrayList messages = new ArrayList();
		do {
			String statement = read();
			int l = statement.length();
			if (code == 0 && l < 3) {
				throw new FTPIllegalReplyException();
			}
			int aux;
			try {
				aux = Integer.parseInt(statement.substring(0, 3));
			} catch (Exception e) {
				if (code == 0) {
					throw new FTPIllegalReplyException();
				} else {
					aux = 0;
				}
			}
			if (code != 0 && aux != 0 && aux != code) {
				throw new FTPIllegalReplyException();
			}
			if (code == 0) {
				code = aux;
			}
			if (aux > 0 && l > 3) {
				char s = statement.charAt(3);
				String message = statement.substring(4, l);
				messages.add(message);
				if (s == ' ') {
					break;
				} else if (s == '-') {
					continue;
				} else {
					throw new FTPIllegalReplyException();
				}
			} else {
				messages.add(statement);
			}
		} while (true);
		int size = messages.size();
		String[] m = new String[size];
		for (int i = 0; i < size; i++) {
			m[i] = (String) messages.get(i);
		}
		return new FTPReply(code, m);
	}

	/**
	 * Changes the current charset.
	 * 
	 * @param charsetName
	 *            The new charset.
	 * @throws IOException
	 *             If I/O error occurs.
	 * @since 1.1
	 */
	public void changeCharset(String charsetName) throws IOException {
		reader.changeCharset(charsetName);
		writer.changeCharset(charsetName);
	}

}