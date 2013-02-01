package app;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URL;
import java.util.Date;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ResourceHandler;

public class Markup {

	public static void main(String[] args) throws Exception {
		String input = null;
		String output = null;
		int port = 8080;
		boolean debug = true;

		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("--input") || args[i].equals("-i")) {
				input = args[i + 1];
			}
			if (args[i].equals("--output") || args[i].equals("-o")) {
				output = args[i + 1];
			}
			if (args[i].equals("--port") || args[i].equals("-p")) {
				port = Integer.parseInt(args[i + 1]);
			}
		}

		if (input == null || output == null) {
			System.out
					.println("java -jar markup.jar --input <input.md> --outout <output.html> [--port <port>]");
			System.out
					.println("\t--input <input.md>\t\trequire. markdown file to input");
			System.out
					.println("\t--output <output.html>\t\trequire. html file to output");
			System.out
					.println("\t--port <port>\t\t\toption.  web server port (default 8080)");
			return;
		}

		Server server = new Server(port);
		ResourceHandler rh = new ResourceHandler();
		rh.setResourceBase(".");
		server.setHandler(rh);
		server.start();

		File inputFile = new File(input);
		File outputFile = new File(output);
		long lastModified = -1;

		boolean first = true;
		while (true) {
			if (inputFile.lastModified() > lastModified) {
				String html = markup(inputFile);

				PrintWriter writer = new PrintWriter(new OutputStreamWriter(
						new FileOutputStream(outputFile, false), "UTF-8"));
				try {
					Date now = new Date();
					writer.println("<html>");
					writer.println("<head>");
					writer.println("<meta charset=UTF-8 />");
					writer.println("<link rel='stylesheet' href='https://gist.github.com/raw/4661054/2e66cabdafe1c9a7f354aa2ebf5bc38265e638e5/github.css'>");
					writer.println("</head>");
					writer.println("<body>");
					writer.println(html);
					if (debug) {
						writer.println("<script src='http://code.jquery.com/jquery-1.9.0.min.js'></script>");
						writer.println(String
								.format("<script>$(function(){var lm='%s';"
										+ "var c=function(){"
										+ "$.ajax({url:'./%s',type:'GET',dataType:'html'})"
										+ ".done(function(d){"
										+ "var l=$('<div>').append($.parseHTML(d)).find('#last');"
										+ "if(lm!==l.text())location.href='./%s'"
										+ "});" + "setTimeout(c, 2000)};"
										+ "c()})</script>", now, output, output));
						writer.println(String.format(
								"<div id='last' style='display:none'>%s</div>",
								now));
					}
					writer.println("</body>");
					writer.println("</html>");
					writer.flush();

					if (first) {
						Desktop.getDesktop().browse(
								new URI("http://localhost:" + port + "/"
										+ output));
						first = false;
					}

				} finally {
					try {
						writer.close();
					} catch (Exception e) {
					}
				}

				lastModified = inputFile.lastModified();
			}

			Thread.sleep(100);
		}
	}

	private static String markup(File file) throws MalformedURLException,
			IOException, ProtocolException {
		URL url = new URL("https://api.github.com/markdown/raw");
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		try {
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.addRequestProperty("Content-Type", "text/plain");

			OutputStream out = null;
			try {

				out = conn.getOutputStream();

				FileInputStream in = new FileInputStream(file);
				try {
					byte[] buf = new byte[1024];
					int len;
					while ((len = in.read(buf)) != -1) {
						out.write(buf, 0, len);
					}
				} finally {
					try {
						in.close();
					} catch (Exception e) {
					}
				}

				out.flush();

			} finally {
				try {
					out.close();
				} catch (Exception e) {
				}
			}

			for (String name : conn.getHeaderFields().keySet()) {
				if (name == null) {
					System.out.println(conn.getHeaderField(name));
				} else {
					System.out
							.println(name + " : " + conn.getHeaderField(name));
				}
			}
			System.out.println();

			BufferedReader in = null;
			try {

				in = new BufferedReader(new InputStreamReader(
						conn.getInputStream(), "UTF-8"));

				StringBuffer result = new StringBuffer();
				String line = null;
				while ((line = in.readLine()) != null) {
					result.append(line + "\n");
				}
				return result.toString();

			} finally {
				try {
					in.close();
				} catch (Exception e) {
				}
			}
		} finally {
			conn.disconnect();
		}
	}

}
