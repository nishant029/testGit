package com.backbase.website.regression;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public class BackbaseProperties extends Properties {


	private static final long serialVersionUID = 1L;
	
	public static final String BASE_URL_KEY = "baseUrl";
	public static final String DEFAULT_USER_KEY = "defaultUser";
	public static final String DEFAULT_PASSWORD_KEY = "defaultPassword";
	public static final String DRIVER_TYPE_KEY = "driverType";

	public BackbaseProperties(){
		super();
	}

	public void loadProperties(String filename, boolean isOptional) {
		InputStream istream = null;

		try {

			// first look for file as a resource
			istream = ClassLoader.getSystemResourceAsStream(filename);

			// if not found, look for it as a file system file
			if (istream == null) {
				File file = new File(filename);

				if (!file.exists()) {

					if (isOptional) {
						return;
					}

					throw new IllegalArgumentException("properties file not found: " + filename);
				}

				istream = FileUtils.openInputStream(file);
			}

			super.load(istream);

		} catch (IOException ex) {
			throw new RuntimeException(ex);
		} finally {
			IOUtils.closeQuietly(istream);
		}
	}
	
	public String getRequiredProperty(String propertyName) {
	      String value = findProperty(propertyName);

	      if (value == null || value.isEmpty()) {
	         throw new IllegalStateException("Required property not defined: " + propertyName);
	      }

	      return value;
	   }
	
	/**
	    * Looks for the value of a named property. Will return null if not found. A system property
	    * value is returned if one exists. Otherwise, a value from the uitest.properties files is
	    * returned.
	    *
	    * @return may be null, if no value is found for the property
	    */
	   public String findProperty(String propertyName) {
	      String value = System.getProperty(propertyName);

	      if (value == null) {
	         value = super.getProperty(propertyName);
	      }

	      if (value != null) {
	         value = value.trim();
	      }

	      return value;
	   }
}
