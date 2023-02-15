/**
 * 
 * Copyright � MeritTrac Services Pvt. Ltd. All Rights Reserved.
 * 
 * Last modified by: 25-Nov-2013 4:04:05 pm - Madhukesh_G
 * 
 */
package com.merittrac.apollo.acs.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.merittrac.apollo.acs.constants.ACSConstants;
import com.merittrac.apollo.acs.constants.BackUpRelatedConstants;
import com.merittrac.apollo.acs.utility.ACSCommonUtility;
import com.merittrac.apollo.acs.utility.ProcessBuilderWrapper;
import com.merittrac.apollo.acs.utility.WinRegistry;
import com.merittrac.apollo.common.PBEEncryptor;
import com.merittrac.apollo.common.SecuredZipUtil;
import com.merittrac.apollo.common.ZipUtility;

import net.lingala.zip4j.exception.ZipException;

/**
 * 
 * Service to take backUp of APOLLO_HOME and ACS database
 * 
 * @author Madhukesh_G - MeritTrac Services Pvt. Ltd.
 * @since Apollo v2.0
 * @see
 */
public class BackUpService {
	/**
	 * 
	 */
	// private static final String ENCRYPTOR_PATH = "C:\\Program Files (x86)\\MeritTrac\\ACS\\PBEencryptor.exe";

	final static Logger logger = LoggerFactory.getLogger(ACSConstants.GENERAL_LOGGER);

	static String OUTPUT_DIR = "D:\\" + "APOLLO_BACK_UP" + "\\";
	boolean isInteractive;
	private static final List<String> messages = new ArrayList<>();

	/**
	 * @return the logger
	 */
	public static Logger getLogger() {
		return logger;
	}

	public BackUpService() {
		isInteractive = true;
	}

	public BackUpService(boolean isInteractive) {
		this.isInteractive = isInteractive;
	}

	public static List<String> getMessages() {
		return messages;
	}

	public boolean takeBackUp() throws Exception {

		Properties properties = null;
		File hibernateFile = null;
		String tomcatInstallPath = null;
		String mysqlPath = null;
		String hibernateConfigFile = null;
		String userName;
		String password;
		String connectionURL;
		String msg = "ACS BACK-UP TOOL STARTED";
		getLogger().debug(msg);
		boolean isWindowsOs = true;
		// getLogger().debug(msg);

		try {
			// get the tomcat installation path
			try {

				isWindowsOs = ACSCommonUtility.isWindowsOs();
				tomcatInstallPath = checkTomcatPath(isWindowsOs);
				if (tomcatInstallPath == null || tomcatInstallPath.isEmpty()) {
					getLogger().error("tomcat path is not defined");
					throw new Exception("Tomcat installation not found");
				}
				if (isWindowsOs)
					mysqlPath = checkMySqlPath();
				getLogger().debug("MySql installation path is ", mysqlPath);
			} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
				getLogger().debug("Could not fetch Tomcat installation path");
				e.printStackTrace();
				return false;
			}

			// get the ACS hibernate config file.
			hibernateConfigFile = getHibernateConfigFile(tomcatInstallPath);
			getLogger().debug("Hibernate config file is " + hibernateConfigFile);
			// BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

			hibernateFile = new File(hibernateConfigFile);
			if (!hibernateFile.exists()) {

				msg = "Hibernate confg file does not exists at " + hibernateConfigFile;
				getLogger().debug(msg);
				throw new Exception(msg);
				// if (isInteractive) {
				// userName = getUserNameFromConsole(console);
				// password = getPasswordFromConsole(console);
				// } else {
				// userName = "root";
				// password = PropertiesUtil.getFailWord();
				// }
			} else {
				// read hibernate config file, get username password

				properties = fetchConfig(hibernateFile);

				if (properties == null || properties.isEmpty()) {
					msg = "Could not fetch the properties.. ";
					getLogger().debug(msg);
					throw new Exception(msg);

				} else {

					userName = properties.getProperty(BackUpRelatedConstants.USER_NAME);
					password = properties.getProperty(BackUpRelatedConstants.PASSWORD);
					connectionURL = properties.getProperty(BackUpRelatedConstants.DRIVER_CLASS);
				}
			}
			if (userName == null || userName.isEmpty() || password == null || password.isEmpty()) {
				msg = "Could not fetch username password from hibernate.cfg.xml";
				getLogger().error(msg);
				throw new Exception(msg);
			}
			String decryptedPassword = decryptPassword(password);
			File outputDir = getdumpOutputDirectory();
			String timeInString = getTimeInString().trim();
			File newoutputDir = new File(outputDir.getAbsoluteFile() + File.separator + timeInString);
			newoutputDir.mkdirs();
			String outputFile = newoutputDir + File.separator + BackUpRelatedConstants.DUMPFILE_NAME + ".sql";
			URI url = getDbURLFromConnectionURL(connectionURL);
			backupDB(userName, decryptedPassword, outputFile, mysqlPath, timeInString, url, isWindowsOs);
			zipSecurelyApolloHome(newoutputDir.getAbsolutePath(), "b!ju" + timeInString);
			msg = "***ACS BACK-UP SUCCESFUL***";
			getLogger().debug(msg);
			return true;

		} catch (Exception e) {
			String string = "Exception while executing takeBackUp...";
			// getLogger().debug(string);
			getLogger().error(string, e);
			throw new Exception(e);
		} finally {
			if (properties != null)
				properties.clear();
		}

	}

	/**
	 * @param connectionURL
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private URI getDbURLFromConnectionURL(String connectionURL) {
		URI uri = URI.create(connectionURL.substring(5));
		return uri;
	}

	/**
	 * 
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private void zipApolloHome(String outputDir) {
		getLogger().debug("Archiving the Apollo home");
		String apolloHome = System.getenv("APOLLO_HOME");
		getLogger().debug("Apollo_Home:" + apolloHome);
		if (apolloHome == null || apolloHome.isEmpty()) {
			getLogger().debug("APOLLO_HOME System variable does not exists! Aborting the backup");
			return;
		}
		File apolloHomeDir = new File(apolloHome);
		if (!apolloHomeDir.isDirectory()) {
			getLogger().debug("Apollo home directory does not exists!!");
			return;
		}
		ZipUtility zipUtility = new ZipUtility();
		String destination = outputDir + File.separator + "apolloHome";
		getLogger().debug("Archiving may take some time depending on the size of Apollo Home. Please Wait....");
		zipUtility.archiveDir(apolloHome, destination);

		String msg = "Apollo Home is archived as " + destination + ".zip";
		getLogger().debug(msg);
		// getLogger().debug(msg);

	}

	/**
	 * 
	 * 
	 * @throws ZipException
	 * @since Apollo v2.0
	 * @see
	 */
	private void zipSecurelyApolloHome(String outputDir, String salt) throws ZipException {
		getLogger().debug("Archiving the Apollo home");
		String apolloHome = System.getenv("APOLLO_HOME");
		getLogger().debug("Apollo_Home:" + apolloHome);
		if (apolloHome == null || apolloHome.isEmpty()) {
			getLogger().debug("APOLLO_HOME System variable does not exists! Aborting the backup");
			return;
		}
		File apolloHomeDir = new File(apolloHome);
		if (!apolloHomeDir.isDirectory()) {
			getLogger().debug("Apollo home directory does not exists!!");
			return;
		}
		String destination = outputDir + File.separator + "apolloHome";
		String msg = "Archiving may take some time depending on the size of Apollo Home. Please Wait....";
		getLogger().debug(msg);
		// getLogger().debug(msg);

		SecuredZipUtil.archiveFilesWithPassword(new String[] { apolloHome }, destination, salt);

		msg = "Apollo Home is archived as " + destination + ".zip";
		getLogger().debug(msg);
		// getLogger().debug(msg);

	}

	/**
	 * @param console
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private String getPasswordFromConsole(BufferedReader console) throws IOException {
		getLogger().debug("Password:");
		String password = console.readLine().trim();

		return password;
	}

	/**
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private String getUserNameFromConsole(BufferedReader console) throws IOException {
		getLogger().debug("Please enter username of the database..");
		String userName = console.readLine().trim();

		return userName;
	}

	/**
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private File getdumpOutputDirectory() throws Exception {
		String Apollo_Home = System.getenv("APOLLO_HOME");
		File apolloHome = new File(Apollo_Home);
		String apollo_home_parent = apolloHome.getParent();
		OUTPUT_DIR = apollo_home_parent + File.separator + BackUpRelatedConstants.APOLLO_BACK_UP;
		File outputDir = new File(OUTPUT_DIR);

		if (!outputDir.isDirectory()) {
			getLogger().debug("Creation of directory is done:" + OUTPUT_DIR);
			outputDir.mkdirs();
		}

		return outputDir;
	}

	/**
	 * @param console
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private String getOutPutDirFromConsole(BufferedReader console) {

		boolean isEnd = false;
		String dir = null;
		try {
			while (!isEnd) {
				getLogger().debug("Please enter the directory to dump..");

				dir = console.readLine().trim();
				if ((new File(dir).isDirectory())) {
					getLogger().debug("Directory to dump:" + dir);
					isEnd = true;
				} else {
					getLogger().debug("Directory entered is invalid!");
					getLogger().debug("Would you like to create one and retry?:(y/n) ");
					String input = console.readLine().trim();
					if (input.equalsIgnoreCase("n")) {
						isEnd = true;
					}

				}

			}
		} catch (Exception e) {
			getLogger().debug(":-(");
		}

		return dir;
	}

	/**
	 * @param console
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private boolean isSameOutputDirToUse(BufferedReader console) throws IOException {
		boolean isSameDir = true;
		getLogger().debug("The default output directory is " + OUTPUT_DIR);
		getLogger().debug("Would you like to proceed with the same (y/n)?");
		String input = console.readLine().trim();
		if (input.equalsIgnoreCase("n")) {
			isSameDir = false;
		}
		return isSameDir;
	}

	public static String getTimeInString() {
		Calendar presentTime = Calendar.getInstance();
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(BackUpRelatedConstants.DISPLAY_DATE_FORMAT);
		String dateFormat = simpleDateFormat.format(new Date(presentTime.getTimeInMillis()));
		return dateFormat;
	}

	/**
	 * @param password2
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public String decryptPassword(String encryptedPassword) {
		String decPassword = null;
		if (encryptedPassword.startsWith("ENC(")) {

			PBEEncryptor encryptor = new PBEEncryptor();
			encryptedPassword = encryptedPassword.replace("ENC(", "");
			StringBuilder builder = new StringBuilder(encryptedPassword);
			builder.deleteCharAt(builder.length() - 1);
			decPassword = encryptor.decryptUsingPBE(builder.toString());

		} else {
			getLogger().debug("Password is not encrypted");
			decPassword = encryptedPassword;
		}

		return decPassword;
	}

	/**
	 * @param tomcatInstallPath
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private String getHibernateConfigFile(String tomcatInstallPath) {
		return tomcatInstallPath + BackUpRelatedConstants.HIBERNATE_CONFIG_PATH + File.separator
				+ BackUpRelatedConstants.HIBERNATE_CONFIG_FILE;
	}

	private String checkTomcatPath(boolean isWindowsOs) throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException {
		getLogger().debug("isWindowsOs:{}", isWindowsOs);
		String path = null;
		if (isWindowsOs) {
			path =
					WinRegistry.readString(WinRegistry.HKEY_LOCAL_MACHINE, BackUpRelatedConstants.TOMCAT_KEY,
							"InstallPath");
		} else {
			path = System.getenv("CATALINA_HOME");
			if (path == null || path.isEmpty()) {
				getLogger().debug(
						"CATALINA_HOME is not set. Hence using default directory:"
								+ BackUpRelatedConstants.DEFAULT_TOMCATHOME_FOR_LINUX);
				path = BackUpRelatedConstants.DEFAULT_TOMCATHOME_FOR_LINUX;
			}
		}
		getLogger().debug("The tomcat is installed at " + path);

		return path;

	}

	private String checkMySqlPath() throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		String path =
				WinRegistry.readString(WinRegistry.HKEY_LOCAL_MACHINE, BackUpRelatedConstants.MYSQL_64BIT_KEY,
						"Location");
		if (path == null || path.isEmpty()) {
			path =
					WinRegistry.readString(WinRegistry.HKEY_LOCAL_MACHINE, BackUpRelatedConstants.MYSQL_32BIT_KEY,
							"Location");
		}
		getLogger().debug("The tomcat is installed at " + path);

		return path;

	}

	/**
	 * @param dbUserName
	 * @param dbPassword
	 * @param path
	 * @param mySqlPath
	 *            TODO
	 * @param timeinString
	 *            TODO
	 * @param url
	 *            TODO
	 * @param isWindowsOs
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public boolean backupDB(String dbUserName, String dbPassword, String path, String mySqlPath, String timeinString,
			URI url, boolean isWindowsOs) {

		String mysqlHome;
		if (isWindowsOs)
			mysqlHome = mySqlPath + "bin" + File.separator;
		else
			mysqlHome = "";
		String executeCmd = mysqlHome + "mysqldump";
		getLogger().debug("Command to take backUp:" + executeCmd);
		String dbName = StringUtils.remove(url.getPath(), "/");
		getLogger().debug("Database considered for backing up is {}", dbName);
		String[] backUpParams = { "-u", dbUserName, "-p" + dbPassword, "-h", url.getHost(), dbName };
		try {
			String msg = "Backing of Database may take some time. Please Wait....";
			getLogger().debug(msg);

			getLogger().debug((executeCmd + backUpParams).replaceFirst("-p" + dbPassword, "*****"));
			ArrayList<String> commandList = new ArrayList<>();
			commandList.add(executeCmd);
			commandList.addAll(Arrays.asList(backUpParams));
			ProcessBuilderWrapper processBuilderWrapper = new ProcessBuilderWrapper(commandList, timeinString, path);
			int processComplete = processBuilderWrapper.getStatus();

			if (processComplete == 0) {
				msg = "Backup created successfully:" + path;
				getLogger().debug(msg);
				return true;
			} else {
				msg = "Could not create the backup: " + processBuilderWrapper.getErrors();
				getLogger().error(msg);
			}
		} catch (Exception ex) {
			getLogger().error("Could not create backUp", ex);
		}

		return false;
	}

	public Properties fetchConfig(File configFile) {

		Configuration cfg = new Configuration();
		Properties properties = null;

		try {
			cfg = new Configuration();
			cfg.configure(configFile);

		} catch (Exception exception) {
			// ignore
			getLogger().debug("EXception" + exception);
		}
		properties = cfg.getProperties();
		getLogger().debug("properties:" + properties);

		getLogger().debug("----------------------------");

		return properties;
	}

	public static void main(String[] args) {
		BackUpService backUpService = new BackUpService();
		backUpService.backupDB("root", "root", "D:\\Temp\\test.sql", "C:\\Program Files\\MySQL\\MySQL Server 5.5\\",
				"25092014165374", null, true);

	}
}
