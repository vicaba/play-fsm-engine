package infrastructure;


import play.mvc.Http;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class BodyParser {
	public static String parseRawBuffer(Http.RawBuffer rawBuffer) {
		String body = null;

		try {
			BufferedReader br = new BufferedReader(new FileReader(rawBuffer.asFile()));
			StringBuilder sb = new StringBuilder();

			br.lines().forEach(sb::append);

			body = sb.toString();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return body;
	}
}
