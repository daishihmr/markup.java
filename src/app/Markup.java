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

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ResourceHandler;

public class Markup {

	public static void main(String[] args) throws Exception {
		String input = null;
		String output = null;
		int port = 8080;

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
					.println("java -jar markup.jar -i <input.md> -o <output.html> [-p <port>] [-r]");
			System.out
					.println("\t-i, --input\t<input.md>\t[require] markdown file to input");
			System.out
					.println("\t-o, --output\t<output.html>\t[require] html file to output");
			System.out
					.println("\t-p, --port\t<port>\t\t[option]  web server port (default 8080)");
			return;
		}

		final File inputDir = new File(input);
		final File outputDir = new File(output);
		if (!outputDir.exists()) {
			outputDir.mkdirs();
		}

		scan(inputDir, new F<File>() {
			@Override
			public void apply(File f) throws Exception {
				if (f.getName().endsWith(".md")) {
					File outputFile = new File(outputDir, f.getName().replace(
							".md", ".html"));
					startChecker(f, outputFile);
				}
			}
		});

		Server server = new Server(port);
		ResourceHandler rh = new ResourceHandler();
		rh.setResourceBase(output);
		server.setHandler(rh);
		server.start();
		
		Thread.sleep(1000);
		Desktop.getDesktop().browse(new URI("http://localhost:8080/"));
	}

	private static interface F<T> {
		void apply(T t) throws Exception;
	}

	private static void scan(File file, F<File> f) throws Exception {
		if (file.isDirectory()) {
			for (File child : file.listFiles()) {
				scan(child, f);
			}
		} else {
			f.apply(file);
		}
	}

	private static void startChecker(final File inputFile, final File outputFile)
			throws IOException, InterruptedException {
		System.out.println("start check " + inputFile);

		Thread.sleep((long) (100 * Math.random()));
		new Thread() {
			public void run() {
				try {
					long lastModified = -1;
					while (true) {
						if (inputFile.lastModified() > lastModified) {
							String html = markup(inputFile);

							final PrintWriter writer = new PrintWriter(
									new OutputStreamWriter(
											new FileOutputStream(outputFile,
													false), "UTF-8"));
							try {
								writeHtml(outputFile, html, writer);
							} finally {
								writer.close();
							}

							lastModified = inputFile.lastModified();
						}

						Thread.sleep(100);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	private static String markup(File file) throws MalformedURLException,
			IOException, ProtocolException {
		System.out.println("markup " + file);

		URL url = new URL("https://api.github.com/markdown/raw");
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		try {
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.addRequestProperty("Content-Type", "text/plain");

			OutputStream out = conn.getOutputStream();
			try {
				FileInputStream in = new FileInputStream(file);
				try {
					byte[] buf = new byte[1024];
					int len;
					while ((len = in.read(buf)) != -1) {
						out.write(buf, 0, len);
					}
				} finally {
					in.close();
				}
				out.flush();
			} finally {
				out.close();
			}

			System.out.println("limit = " + conn.getHeaderField("X-RateLimit-Remaining")
					+ " / " + conn.getHeaderField("X-RateLimit-Limit"));

			BufferedReader in = new BufferedReader(new InputStreamReader(
					conn.getInputStream(), "UTF-8"));
			try {

				StringBuffer result = new StringBuffer();
				String line = null;
				while ((line = in.readLine()) != null) {
					result.append(line + "\n");
				}
				return result.toString();

			} finally {
				in.close();
			}
		} finally {
			conn.disconnect();
			System.out.println("markup success " + file);
		}
	}

	private static void writeHtml(File outputFile, String html,
			PrintWriter writer) {
		writer.println("<html>");
		writer.println("<head>");
		writer.println("<meta charset=UTF-8 />");
		writer.println("<link rel='stylesheet' href='https://gist.github.com/raw/4661054/2e66cabdafe1c9a7f354aa2ebf5bc38265e638e5/github.css'>");
		writer.println("</head>");
		writer.println("<body>");
		writer.println(html);
		writer.println("</body>");
		writer.println("</html>");
		writer.flush();
	}

}
