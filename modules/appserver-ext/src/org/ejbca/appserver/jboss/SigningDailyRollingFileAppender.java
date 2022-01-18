package org.ejbca.appserver.jboss;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.rolling.RollingFileManager;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.net.Advertiser;
import org.apache.logging.log4j.status.StatusLogger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.tsp.TSPAlgorithms;
import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.tsp.TSPValidationException;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampResponse;
import org.bouncycastle.util.encoders.Base64;
import org.cesecore.util.CryptoProviderTools;
import org.cesecore.util.FileTools;
import org.cesecore.util.log.NonFinalRollingFileAppender;

/**
 * A shameless copy of DailyRollingFileAppender from log4j and merged (also shamelessly)
 * with DailyRollingFileAppender from jboss.
 * 
 * This was the only way I could find to implement the desired functionality.
 */
@Plugin(name = SigningDailyRollingFileAppender.PLUGIN_NAME, category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class SigningDailyRollingFileAppender extends NonFinalRollingFileAppender {

    public static final String PLUGIN_NAME = "SigningDailyRollingFileAppender";
    
    protected final static StatusLogger log = StatusLogger.getLogger();
    
	private Thread signerThread; // NOPMD this is not run in the ejb app

	/**
	     The date pattern. By default, the pattern is set to
	     "'.'yyyy-MM-dd" meaning daily rollover.
	 */
	private String datePattern = "'.'yyyy-MM-dd";

	/** The method use to create a signature */
	private String signMethod;

	/** The URL to a TSA server used to create time stamps for rolled over log files */
	private String tsaUrl;
	
	/**
	     The log file will be renamed to the value of the
	     scheduledFilename variable when the next interval is entered. For
	     example, if the rollover period is one hour, the log file will be
	     renamed to the value of "scheduledFilename" at the beginning of
	     the next hour. 

	     The precise time when a rollover occurs depends on logging
	     activity. 
	 */
	private String scheduledFilename;

	/**
	     The next time we estimate a rollover should occur. */
	private long nextCheck = System.currentTimeMillis () - 1;

	Date now = new Date();

	SimpleDateFormat sdf;

	RollingCalendar rc = new RollingCalendar();

	int checkPeriod = RollingCalendar.TOP_OF_TROUBLE;

	// The gmtTimeZone is used only in computeCheckPeriod() method.
	static final TimeZone gmtTimeZone = TimeZone.getTimeZone("GMT");


	/** Constructor from superclass. */ 
    public SigningDailyRollingFileAppender(
            final String name, 
            final Layout<? extends Serializable> layout, 
            final Filter filter,
            final RollingFileManager manager, 
            final String fileName, 
            final String filePattern,
            final boolean ignoreExceptions, 
            final boolean immediateFlush, 
            final Advertiser advertiser,
            final Property[] properties) {
        super(name, layout, filter, manager, fileName, filePattern, ignoreExceptions, immediateFlush, advertiser, properties);
        activateOptions();
    }

	/**
	    Instantiate a <code>DailyRollingFileAppender</code> and open the
	    file designated by <code>filename</code>. The opened filename will
	    become the ouput destination for this appender.

	 */
    // EJBCAINTER-323 Fix constructor if required.
//	public SigningDailyRollingFileAppender (Layout layout, String filename,
//			String datePattern) throws IOException {
//		super(layout, filename, true);
//		this.datePattern = datePattern;
//		activateOptions();
//	}

	/** This is from org.jboss.logging.appender.DailyRollingFileAppender,
	 *  which will make the directory structure for the set log file. 
	 */
 // EJBCAINTER-323 Must be done with the RollingFileManager
//	public void setFile(final String filename){
//	    makePath(filename);
//		super.setFile(filename);
//	}

	/**
     * Copied from org.jboss.logging.appender.FileAppender.Helper.makePath(String filename);
     * 
     */
    private static void makePath(final String filename) {
        File dir;

        try {
            URL url = new URL(filename.trim());
            dir = new File(url.getFile()).getParentFile();
        } catch (MalformedURLException e) {
            dir = new File(filename.trim()).getParentFile();
        }

        if (!dir.exists()) {
            boolean success = dir.mkdirs();
            if (!success) {
                log.error("Failed to create directory structure: " + dir);
            }
        }
    }
	
	/**
	     The <b>DatePattern</b> takes a string in the same format as
	     expected by {@link SimpleDateFormat}. This options determines the
	     rollover schedule.
	 */
	public void setDatePattern(String pattern) {
		datePattern = pattern;
	}

	/** Returns the value of the <b>DatePattern</b> option. */
	public String getDatePattern() {
		return datePattern;
	}
	
	public void setSignMethod(String method) {
		signMethod = method;
	}
	public String getSignMethod() {
		return signMethod;
	}
	public void setTsaUrl(String url) {
		tsaUrl = url;
	}
	public String getTsaUrl() {
		return tsaUrl;
	}

	public void activateOptions() {
	    // EJBCAINTER Check this!
//		super.activateOptions();
		final String name = getName();
		final String fileName = getFileName();
		if(datePattern != null && fileName != null) {
			now.setTime(System.currentTimeMillis());
			sdf = new SimpleDateFormat(datePattern);
			int type = computeCheckPeriod();
			printPeriodicity(type);
			rc.setType(type);
			File file = new File(fileName);
			scheduledFilename = fileName+sdf.format(new Date(file.lastModified()));

		} else {
			log.error("Either File or DatePattern options are not set for appender ["+name+"].");
		}
		if (signMethod != null) {
			if (tsaUrl == null) {
				log.error("TsaUrl option is not set for appender ["+name+"].");				
			}
		} else {
			log.error("SignMethod option is not set for appender ["+name+"].");			
		}
		CryptoProviderTools.installBCProvider();
	}

	void printPeriodicity(int type) {
	    final String name = getName();
		switch(type) {
		case RollingCalendar.TOP_OF_MINUTE:
			log.debug("Appender ["+name+"] to be rolled every minute.");
			break;
		case RollingCalendar.TOP_OF_HOUR:
			log.debug("Appender ["+name
					+"] to be rolled on top of every hour.");
			break;
		case RollingCalendar.HALF_DAY:
			log.debug("Appender ["+name
					+"] to be rolled at midday and midnight.");
			break;
		case RollingCalendar.TOP_OF_DAY:
			log.debug("Appender ["+name
					+"] to be rolled at midnight.");
			break;
		case RollingCalendar.TOP_OF_WEEK:
			log.debug("Appender ["+name
					+"] to be rolled at start of week.");
			break;
		case RollingCalendar.TOP_OF_MONTH:
			log.debug("Appender ["+name
					+"] to be rolled at start of every month.");
			break;
		default:
			log.warn("Unknown periodicity for appender ["+name+"].");
		}
	}


	// This method computes the roll over period by looping over the
	// periods, starting with the shortest, and stopping when the r0 is
	// different from from r1, where r0 is the epoch formatted according
	// the datePattern (supplied by the user) and r1 is the
	// epoch+nextMillis(i) formatted according to datePattern. All date
	// formatting is done in GMT and not local format because the test
	// logic is based on comparisons relative to 1970-01-01 00:00:00
	// GMT (the epoch).

	int computeCheckPeriod() {
		RollingCalendar rollingCalendar = new RollingCalendar(gmtTimeZone, Locale.ENGLISH);
		// set sate to 1970-01-01 00:00:00 GMT
		Date epoch = new Date(0);
		if(datePattern != null) {
			for(int i = RollingCalendar.TOP_OF_MINUTE; i <= RollingCalendar.TOP_OF_MONTH; i++) {
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat(datePattern);
				simpleDateFormat.setTimeZone(gmtTimeZone); // do all date formatting in GMT
				String r0 = simpleDateFormat.format(epoch);
				rollingCalendar.setType(i);
				Date next = new Date(rollingCalendar.getNextCheckMillis(epoch));
				String r1 =  simpleDateFormat.format(next);
				//System.out.println("Type = "+i+", r0 = "+r0+", r1 = "+r1);
				if(r0 != null && r1 != null && !r0.equals(r1)) {
					return i;
				}
			}
		}
		return RollingCalendar.TOP_OF_TROUBLE; // Deliberately head for trouble...
	}

	/**
	     Rollover the current file to a new file.
	 */
// EJBCAINTER-323 Replace with custom SigningRollingFileManager.
//	@Override
//	void rollOver() throws IOException {
//	    final String fileName = getFileName();
//	    
//		/* Compute filename, but only if datePattern is specified */
//		if (datePattern == null) {
//			getHandler().error("Missing DatePattern option in rollOver().");
//			return;
//		}
//
//		String datedFilename = fileName+sdf.format(now);
//		// It is too early to roll over because we are still within the
//		// bounds of the current interval. Rollover will occur once the
//		// next interval is reached.
//		if (scheduledFilename.equals(datedFilename)) {
//			return;
//		}
//
//		// close current file, and rename it to datedFilename
//		this.closeFile();
//
//		File target  = new File(scheduledFilename);
//		if (target.exists()) {
//			target.delete();
//		}
//
//		File file = new File(fileName);
//		boolean result = file.renameTo(target);
//		if(result) {
//			log.debug(fileName +" -> "+ scheduledFilename);
//		} else {
//			log.error("Failed to rename ["+fileName+"] to ["+scheduledFilename+"].");
//		}
//
//		try {
//			// This will also close the file. This is OK since multiple
//			// close operations are safe.
//		    // EJBCAINTER-323 Check if required.
////			this.setFile(fileName, false, this.bufferedIO, this.bufferSize);
//		}
//		catch(IOException e) {
//		    getHandler().error("setFile("+fileName+", false) call failed.");
//		}
//		if (signMethod.equalsIgnoreCase("tsa")) {
//			if (tsaUrl != null) {
//				// Now do the actual signing
//				// Check first if an old instance of the thread is blocking
//				if ( (signerThread != null) && signerThread.isAlive() ) {
//					System.out.println("Stopping old hanging signerthread");
//					signerThread.interrupt();
//				}
//				signerThread = new Thread(new SignerThread(tsaUrl, scheduledFilename, scheduledFilename+".tsp")); // NOPMD this is not run in the ejb app
//				signerThread.start();							
//			} else {
//				System.out.println("No TsaUrl set, can not sign logs!");
//			}
//		}
//
//		scheduledFilename = datedFilename;
//	}

	/**
	 * This method differentiates DailyRollingFileAppender from its
	 * super class.
	 *
	 * <p>Before actually logging, this method will check whether it is
	 * time to do a rollover. If it is, it will schedule the next
	 * rollover time and then rollover.
	 * */
	// EJBCAINTER-323 append(event) is invoked and SigningRollingFileManager has do the work.
//	protected void subAppend(LogEvent event) {
//		long n = System.currentTimeMillis();
//		if (n >= nextCheck) {
//			now.setTime(n);
//			nextCheck = rc.getNextCheckMillis(now);
//			try {
//				rollOver();
//			}
//			catch(IOException ioe) {
//				log.error("rollOver() failed.", ioe);
//			}
//		}
//		super.append(event);
//	}

}

class SignerThread implements Runnable { // NOPMD this is not run in the ejb app
	private String urlstr;
	private String infile;
	private String outfile;
	public SignerThread(String urlstr, String infile, String outfile) {
		this.urlstr = urlstr;
		this.infile = infile;
		this.outfile = outfile;
	}
	public void run() {
		
		try {
			boolean base64 = true;
			TimeStampRequestGenerator timeStampRequestGenerator = new TimeStampRequestGenerator();

			Random rand = new Random();
			int nonce = rand.nextInt();
			byte[] digestBytes = new byte[20];
			if (infile != null) {
				digestBytes = FileTools.readFiletoBuffer(infile);
			}
			MessageDigest dig = MessageDigest.getInstance(TSPAlgorithms.SHA1.getId(), BouncyCastleProvider.PROVIDER_NAME);
			dig.update(digestBytes);
			byte[] digest = dig.digest();
			TimeStampRequest timeStampRequest = timeStampRequestGenerator.generate(TSPAlgorithms.SHA1, digest, BigInteger.valueOf(nonce));

            // create a singular HttpClient object
			CloseableHttpClient httpclient = HttpClients.createDefault();
            
            final HttpPost post = new HttpPost(urlstr);
            //establish a connection within 5 seconds
            final int timeoutMillis = 5000;
            final RequestConfig reqcfg = RequestConfig.custom()
                    .setConnectionRequestTimeout(timeoutMillis)
                    .setConnectTimeout(timeoutMillis)
                    .setSocketTimeout(timeoutMillis)
                    .build();
            post.setConfig(reqcfg);
            post.setHeader("Content-Type", "application/timestamp-query");
            
            InputStreamEntity reqEntity = new InputStreamEntity(
                    new ByteArrayInputStream(timeStampRequest.getEncoded()), -1, ContentType.APPLICATION_OCTET_STREAM);
            reqEntity.setChunked(true);
            post.setEntity(reqEntity);

            // POST and read input 
			byte[] replyBytes = null;            
			try (CloseableHttpResponse response = httpclient.execute(post)) {
				if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				    replyBytes = EntityUtils.toByteArray(response.getEntity());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			if ( (outfile != null) && (replyBytes != null) ) {
				// Store request
				byte[] outBytes;
				if (base64) {
					outBytes=Base64.encode(replyBytes);
				} else {
					outBytes = replyBytes;
				}
				FileOutputStream fos = null;
				try {
					fos = new FileOutputStream(outfile);
					fos.write(outBytes);					
				} finally {
					if (fos != null) {
						fos.close();
					}
				}
			}

			if (replyBytes != null) {
				try {
					TimeStampResponse timeStampResponse = new TimeStampResponse(replyBytes);
					timeStampResponse.validate(timeStampRequest);
				} catch (TSPValidationException e) {
				    SigningDailyRollingFileAppender.log.error("TimeStampResponse validation failed.", e);
					e.printStackTrace();
				} catch (TSPException e) {
				    SigningDailyRollingFileAppender.log.error("TimeStampResponse failed.", e);
					e.printStackTrace();
				}			
			} else {
			    SigningDailyRollingFileAppender.log.error("No reply bytes received, is TSA down?");
				System.out.println("SigningDailyRollingFileAppender: No reply bytes received, is TSA down?");
			}			
		} catch (Exception e) {
		    SigningDailyRollingFileAppender.log.error("Exception caught while signing log: ", e);
			e.printStackTrace();
		} 
		
	}
	
}

