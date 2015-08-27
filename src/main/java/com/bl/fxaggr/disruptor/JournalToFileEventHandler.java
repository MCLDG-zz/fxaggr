package com.bl.fxaggr.disruptor;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static java.nio.file.StandardOpenOption.*;

import com.lmax.disruptor.EventHandler;

/**
 * Persists the event to the file system
 */
public class JournalToFileEventHandler implements EventHandler<PriceEvent> {
	Path fileP;
	OpenOption[] options;

	public JournalToFileEventHandler() {
		String filePath = "/home/ubuntu/workspace/files" + this.hashCode();
		fileP = Paths.get(filePath);
		options = new OpenOption[] { WRITE, CREATE, APPEND };
	}

	public void onEvent(PriceEvent event, long sequence, boolean endOfBatch) {
		// String JSON = event.getPriceJSON();
		// String msg = event.getPriceMsg();
		// // Convert the JSON to a Price entity instance. In this case - take a
		// // short cut
		this.journalToFile("Sequence: " + sequence + " contents: " + event.getPriceEntity().toString() + "\n");

		if (sequence == PriceEventMain.producerCount) {
	        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
	        Date dateEnd = new Date();
			System.out.println("JournalToFileEventHandler processed: " + PriceEventMain.producerCount +
				"events. Complete time: " + dateFormat.format(dateEnd)); 
		}
	}

	/** TODO: can we have multiple file journalers?
	 * 
	 */
	/**
	 * Write a small string to a File - Use a FileWriter
	 */
	private void journalToFile(String content) {
		try {
			Files.write(fileP, content.getBytes("utf-8"), options);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}