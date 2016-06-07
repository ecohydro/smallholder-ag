package edu.indiana.d2i.textit;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.indiana.d2i.textit.utils.TextItUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public final class TextItClient {
	private static Logger logger = Logger.getLogger(TextItClient.class);

	/** properties loaded from the properties file */
	private final String TOKEN;
	private final String TIMEZONE;
	private final int WORKER_NUM, NO_OF_DAYS;
	private final String OUTPUT_DIRECTORY;
	private final URL TEXTIT_BASE_URL;
	private final URL GET_FLOWS_URL;
	private final URL GET_RUNS_URL;
	private final URL GET_CONTACTS_URL;

	/** utilities for parsing responses from TextIt */
	private final TextItUtils utils;

	/** multi-threaded downloader */
	class ThreadedDownloader {
		private final BlockingQueue<String> queue;
		private final Thread[] workers;
		private final int expectedCount;
		private AtomicInteger finishedCount = new AtomicInteger(0);

		private String flowParam = "?flow_uuid=";

		class Worker implements Runnable {
			@Override
			public void run() {
				String flowid = null;
				while ((flowid = queue.poll()) != null) {
					try {

						URL target = new URL(GET_RUNS_URL.toString()
								+ flowParam + flowid);
						final String id = flowid;
						logger.info("Try to download flow " + flowid);

						final String timestamp = new SimpleDateFormat(
								"yyyyMMdd").format(new Date());
						utils.processData(target,
								new TextItUtils.IJsonProcessor() {
									@Override
									public void process(
											Map<String, Object> data,
											int pageNum) throws IOException {
										ObjectMapper objectMapper = new ObjectMapper();
										objectMapper
												.writeValue(
														Paths.get(
																OUTPUT_DIRECTORY,
																String.format(
																		"%s-%s-%d-runs.json",
																		timestamp,
																		id,
																		pageNum))
																.toFile(), data);
									}
								});

						finishedCount.incrementAndGet();
					} catch (IOException e) {
						logger.warn("There are errors while downloading flow "
								+ flowid, e);
					}
				}
			}
		}

		public ThreadedDownloader(List<String> flowIDs, int workerNum,
				String param) {
			queue = new LinkedBlockingQueue<>(flowIDs);
			workers = new Thread[workerNum];
			expectedCount = flowIDs.size();
			if (param != null) {
				flowParam = param;
			}
		}

		public void execute() throws InterruptedException {
			long startT = System.currentTimeMillis();
			for (int i = 0; i < workers.length; i++) {
				workers[i] = new Thread(new Worker());
				workers[i].start();
			}

			for (Thread worker : workers) {
				worker.join();
			}

			long duration = System.currentTimeMillis() - startT;
			logger.info(String
					.format("Expected %d flows, actually downloaded %d, took %f seconds",
							expectedCount, finishedCount.get(),
							duration / 1000.0));
		}
	}

	private TextItClient(String token, String outputDir, String epr,
			int workerNum, String timezone, int no_of_days) throws IOException {
		TOKEN = token;
		TIMEZONE = timezone;
		NO_OF_DAYS = no_of_days;

		Date date = new Date();
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		df.setTimeZone(TimeZone.getTimeZone("timezone"));
		String final_dir = outputDir + df.format(date);
		OUTPUT_DIRECTORY = final_dir;

		String textitEpr = epr;
		WORKER_NUM = workerNum;
		if (TOKEN == null) {
			throw new IllegalArgumentException("Textit token is missing");
		}
		if (TIMEZONE == null) {
			throw new IllegalArgumentException("Textit timezone is missing");
		}

		TEXTIT_BASE_URL = new URL(textitEpr.replaceAll("/$", ""));
		GET_FLOWS_URL = new URL(TEXTIT_BASE_URL.getProtocol(),
				TEXTIT_BASE_URL.getHost(), TEXTIT_BASE_URL.getPort(),
				TEXTIT_BASE_URL.getFile() + "/flows.json", null);
		GET_RUNS_URL = new URL(TEXTIT_BASE_URL.getProtocol(),
				TEXTIT_BASE_URL.getHost(), TEXTIT_BASE_URL.getPort(),
				TEXTIT_BASE_URL.getFile() + "/runs.json", null);
		GET_CONTACTS_URL = new URL(TEXTIT_BASE_URL.getProtocol(),
				TEXTIT_BASE_URL.getHost(), TEXTIT_BASE_URL.getPort(),
				TEXTIT_BASE_URL.getFile() + "/contacts.json", null);

		Files.createDirectories(Paths.get(OUTPUT_DIRECTORY));

		utils = TextItUtils.createUtils(TOKEN, TIMEZONE);
	}

	@SuppressWarnings("unchecked")
	protected List<String> getFlowIDs() throws IOException {
		final List<String> res = new ArrayList<String>();

		Date date = new Date();
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		df.setTimeZone(TimeZone.getTimeZone("timezone"));

		final String timestamp = df.format(new DateTime(df.format(date))
				.minusDays(NO_OF_DAYS).toDate());
		final String timestamp_1 = df.format(new DateTime(df.format(date))
				.minusDays(1).toDate());

		logger.info("No of Days " + NO_OF_DAYS);
		URL target = new URL(GET_FLOWS_URL.toString() + "?after=" + timestamp
				+ "T00:00:00.000" + "&&" + "before=" + timestamp_1
				+ "T23:59:59.000");
		final String timestamp_final = timestamp.replace("-", "") + "-"
				+ timestamp_1.replace("-", "");

		utils.processData(target, new TextItUtils.IJsonProcessor() {
			@Override
			public void process(Map<String, Object> data, int pageNum)
					throws IOException {
				List<Object> results = (List<Object>) data.get("results");
				for (Object result : results) {
					Map<Object, Object> map = (Map<Object, Object>) result;
					if (map.get("uuid") != null) {
						res.add((String) map.get("uuid"));
					}
				}
				ObjectMapper objectMapper = new ObjectMapper();
				objectMapper.writeValue(
						Paths.get(
								OUTPUT_DIRECTORY,
								String.format("%s-%d-flows.json",
										timestamp_final, pageNum)).toFile(),
						data);

			}
		});

		return res;
	}

	@SuppressWarnings("unchecked")
	protected List<String> getContactInfo() throws IOException {
		final List<String> res = new ArrayList<String>();

		Date date = new Date();
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		df.setTimeZone(TimeZone.getTimeZone("timezone"));

		final String timestamp = df.format(new DateTime(df.format(date))
				.minusDays(NO_OF_DAYS).toDate());
		final String timestamp_1 = df.format(new DateTime(df.format(date))
				.minusDays(1).toDate());
		URL target = new URL(GET_CONTACTS_URL.toString() + "?after="
				+ timestamp + "T00:00:00.000" + "&&" + "before=" + timestamp_1
				+ "T23:59:59.000");
		final String timestamp_final = timestamp.replace("-", "") + "-"
				+ timestamp_1.replace("-", "");

		utils.processData(target, new TextItUtils.IJsonProcessor() {
			@Override
			public void process(Map<String, Object> data, int pageNum)
					throws IOException {
				List<Object> results = (List<Object>) data.get("results");
				for (Object result : results) {
					Map<Object, Object> map = (Map<Object, Object>) result;
					if (map.get("uuid") != null) {
						res.add((String) map.get("uuid"));
					}
				}
				ObjectMapper objectMapper = new ObjectMapper();
				objectMapper.writeValue(
						Paths.get(
								OUTPUT_DIRECTORY,
								String.format("%s-%d-contacts.json",
										timestamp_final, pageNum)).toFile(),
						data);

			}
		});

		return res;
	}

	protected void downloadData(String param, List<String> flowIDs)
			throws IOException {
		ThreadedDownloader downloader = new ThreadedDownloader(flowIDs,
				WORKER_NUM, param);
		try {
			downloader.execute();
		} catch (InterruptedException e) {
			throw new IOException(e);
		}
	}

	/** unit test purpose */
	protected static TextItClient createClient(Properties properties)
			throws IOException {
		String token = properties.getProperty("token");
		String outputDir = properties.getProperty(
				"outputdir", "./output");
		String textitEpr = properties.getProperty("textit.epr",
				"https://textit.in/api/v1");
		String timezone = properties.getProperty("timezone");
		int no_of_days = Integer.valueOf(properties
				.getProperty("download_no_of_days"));

		int workerNum = Integer.valueOf(properties.getProperty(
				"workernum", "1"));

		TextItClient instance = new TextItClient(token, outputDir, textitEpr,
				workerNum, timezone, no_of_days);
		return instance;
	}

	public static class TextItClientBuilder {
		private String token, outputDir, textitEpr, timezone;
		private int workerNum, no_of_days;

		private Properties getProperties() throws IOException {

			final Properties properties = new Properties();
			InputStream stream = TextItClient.class.getClassLoader()
					.getResourceAsStream("config.properties");
			if (stream == null) {
				throw new RuntimeException("config.properties is not found!");
			}
			properties.load(stream);
			stream.close();

			return properties;
		}

		public TextItClientBuilder() throws IOException {

			Properties properties = getProperties();
			token = properties.getProperty("token");
			outputDir = properties.getProperty("outputdir",
					"./output");
			textitEpr = properties.getProperty("textit.epr",
					"https://textit.in/api/v1");
			workerNum = Integer.valueOf(properties.getProperty(
					"workernum", "1"));
			timezone = properties.getProperty("timezone");
			no_of_days = Integer.valueOf(properties
					.getProperty("download_no_of_days"));

		}

		public TextItClient build() throws IOException {
			return new TextItClient(token, outputDir, textitEpr, workerNum,
					timezone, no_of_days);
		}

		public TextItClientBuilder setWorkerNum(int workerNum) {
			this.workerNum = workerNum;
			return this;
		}

		public TextItClientBuilder setProperties(Properties properties) {
			token = properties.getProperty("token");
			outputDir = properties.getProperty("outputdir",
					"./output");
			textitEpr = properties.getProperty("textit.epr",
					"https://textit.in/api/v1");
			workerNum = Integer.valueOf(properties.getProperty(
					"workernum", "1"));
			timezone = properties.getProperty("timezone");
			no_of_days = Integer.valueOf(properties
					.getProperty("download_no_of_days"));

			return this;
		}
	}

	public static TextItClientBuilder custom() throws IOException {
		return new TextItClientBuilder();
	}

	public static TextItClient createClient() throws IOException {
		return new TextItClientBuilder().build();
	}

	public void downloadRunsByFlowUUID(String flowID) throws IOException {
		List<String> flowIDs = new ArrayList<String>();
		flowIDs.add(flowID);
		downloadData(null, flowIDs);
	}

	public void downloadRunsByFlowID(String flowID) throws IOException {
		List<String> flowIDs = new ArrayList<String>();
		flowIDs.add(flowID);
		downloadData("?flow=", flowIDs);
	}

	public void downloadRuns() throws IOException {

		List<String> flowIDs = getFlowIDs();
		List<String> contactInfo = getContactInfo();

		downloadData(null, flowIDs);
	}

	public void close() throws IOException {
		utils.close();
	}
}
