package dlg.parser.util;


import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DateFormatUtils {
	
	private static Logger logger = LoggerFactory.getLogger(DateFormatUtils.class);
	
	
	private static final String GENERIC_DATE_FORMAT_PATTERN = "^[0-9]{4}\\-[0-9]{2}\\-[0-9]{2}[T ][0-9]{2}[:][0-9]{2}([:][0-9]{2})?([\\+-][0-9]{2}([:]?[0-9]{2})?)?$";
	private static List<String[]> patterns = null;
	
	
	public static Date getDateFromString(String dateString) throws Exception {
		Pattern pattern = Pattern.compile(GENERIC_DATE_FORMAT_PATTERN);
		Matcher matcher = pattern.matcher(dateString);		
		if (matcher.matches()) {
			String dateFormat = getDate(dateString);
			if (StringUtils.isNotEmpty(dateFormat)) {

				DateTimeFormatter formatter = DateTimeFormat.forPattern(dateFormat);
				DateTime dateTime = formatter.parseDateTime(dateString);
				dateTime = dateTime.toDateTime(DateTimeZone.forID("EST"));
				return dateTime.toDate();
			

			}
		} else {
			logger.error("String date {} doesn't match the accepted patter: {}", dateString, GENERIC_DATE_FORMAT_PATTERN);
			throw new IllegalArgumentException("String date not accepted ");
		}
		return null;
	}
	
	private static String getDate(String dateString) {
		if (patterns == null) {
			initializePatterns();
		}
		Pattern pattern = null;
		String dateFormat = null;
		Matcher  matcher;
		Iterator<String[]> iterator = patterns.iterator();
		while (iterator.hasNext() && dateFormat == null) {
			String[] patternDate = iterator.next();
			pattern = Pattern.compile(patternDate[0]);
			matcher = pattern.matcher(dateString);;
		    if(matcher.matches()) {
		    	dateFormat = patternDate[1];
		    }			
		}
		return dateFormat;
	}

	private static void initializePatterns() {
		patterns = new ArrayList<String[]>();
		patterns.add(new String[]{"^[0-9]{4}\\-[0-9]{2}\\-[0-9]{2}T[0-9]{2}[:][0-9]{2}$", "yyyy-MM-dd'T'HH:mm"});//google format without time zone
		patterns.add(new String[]{"^[0-9]{4}\\-[0-9]{2}\\-[0-9]{2} [0-9]{2}[:][0-9]{2}$", "yyyy-MM-dd HH:mm"});//like google format without time zone, but without T
		patterns.add(new String[]{"^[0-9]{4}\\-[0-9]{2}\\-[0-9]{2}T[0-9]{2}[:][0-9]{2}[\\+-][0-9]{2}[:]?[0-9]{2}$", "yyyy-MM-dd'T'HH:mmZ"});//google format with time zone
		patterns.add(new String[]{"^[0-9]{4}\\-[0-9]{2}\\-[0-9]{2} [0-9]{2}[:][0-9]{2}[\\+-][0-9]{2}[:]?[0-9]{2}$", "yyyy-MM-dd HH:mmZ"});//like google format with time zone, but without T
		//same as before, but seconds included
		patterns.add(new String[]{"^[0-9]{4}\\-[0-9]{2}\\-[0-9]{2}T[0-9]{2}[:][0-9]{2}[:][0-9]{2}$", "yyyy-MM-dd'T'HH:mm:ss"});
		patterns.add(new String[]{"^[0-9]{4}\\-[0-9]{2}\\-[0-9]{2} [0-9]{2}[:][0-9]{2}[:][0-9]{2}$", "yyyy-MM-dd HH:mm:ss"});
		patterns.add(new String[]{"^[0-9]{4}\\-[0-9]{2}\\-[0-9]{2}T[0-9]{2}[:][0-9]{2}[:][0-9]{2}[\\+-][0-9]{2}[:]?[0-9]{2}$", "yyyy-MM-dd'T'HH:mm:ssZ"});
		patterns.add(new String[]{"^[0-9]{4}\\-[0-9]{2}\\-[0-9]{2} [0-9]{2}[:][0-9]{2}[:][0-9]{2}[\\+-][0-9]{2}[:]?[0-9]{2}$", "yyyy-MM-dd HH:mm:ssZ"});
		
	}

	/**
	 * Will compare the receive dates, by using joda DateTime, setting same
	 * timezone for both, and comparing just the date without minutes and
	 * seconds. Will return true if both dates belong to the same day.
	 * 
	 * @param dateOne
	 * @param dateTwo
	 * @return boolean
	 */
	public static boolean isSameDate(Date dateOne, Date dateTwo) {
	
		if (dateOne == null || dateTwo == null) {
			throw new IllegalArgumentException("One of the expected dates was null.");
		}

		DateTime dateTimeOne = new DateTime(dateOne);
		dateTimeOne = dateTimeOne.toDateTime(DateTimeZone.forID("EST"));
		
		DateTime dateTimeTwo = new DateTime(dateTwo);
		dateTimeTwo = dateTimeTwo.toDateTime(DateTimeZone.forID("EST"));
		
		
		return dateTimeOne.withTimeAtStartOfDay().isEqual(dateTimeTwo.withTimeAtStartOfDay());
	}
	
}
