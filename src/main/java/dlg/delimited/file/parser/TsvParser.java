package dlg.delimited.file.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dlg.delimited.file.parser.binder.BindedClass;
import dlg.delimited.file.parser.binder.BindedClasses;
import dlg.delimited.file.parser.exception.InvalidFieldValueException;
import dlg.delimited.file.parser.util.DateFormatUtils;

/**
 * 
 * @author dalcantara
 * @since 2016/05/18 
 */
@SuppressWarnings("rawtypes")
public class TsvParser {
	

	private Logger logger = LoggerFactory.getLogger(TsvParser.class);

	
	@SuppressWarnings("unchecked")
	public List parseContent(BufferedReader bufferedReader, Class clazz) {
		BindedClass bindedClass = BindedClasses.getBinder(clazz);
		if (bindedClass == null) {
			throw new IllegalArgumentException("No binding annotation defined for class " + clazz.getName());
		}
		List result = null;
		try {
			String line;
			int lineNumber = 1;
			Object record;
			result = new ArrayList();
			while (((line = bufferedReader.readLine()) != null)) {
				if (lineNumber == 1 && bindedClass.getIgnoreFirstLine()) {
					lineNumber++;
					continue;
				}
				if (StringUtils.trimToNull(line) != null) {

					try {
						record = bindedClass.getClazz().cast(getBindedClassDTOFromLine(line, lineNumber, bindedClass));
						if (record != null) {
							result.add(record);
						}
					} catch (Exception e) {
						if (e instanceof InvalidFieldValueException) {
							throw (InvalidFieldValueException) e;
						}
						logger.error("Error processing line, " + line, e);
					}
				}
				lineNumber++;
			}
		} catch (IOException e) {
			logger.error("Error reading content of file", e);
		} finally {
			try {
				bufferedReader.close();
			} catch (IOException e1) {
				logger.error("Error closing buffered reader", e1);
			}
		}
		logger.debug("Resulting DTOs {}: ", result);
		return result;
	}

	/**
	 * Generates an instance of your DTO by parsing the current string row accordingly to
	 * the specified indexes of fields provided in the constructor
	 * @param line
	 * @param clazz
	 * @param lineNumber
	 * @return
	 * @throws Exception 
	 */
	private Object getBindedClassDTOFromLine(String line,  int lineNumber, BindedClass bindedClass) throws InvalidFieldValueException {

		Object myIntance = null;
		try {
			Constructor[] constructors = bindedClass.getClazz().getDeclaredConstructors();
			Constructor init = constructors[0];
			init.setAccessible(true);

			Object[] initArgs = new Object[] {};
			myIntance = init.newInstance(initArgs);
			
			if (StringUtils.isNotEmpty(line)) {
				String[] split = split(line, bindedClass);
				int startWith = (bindedClass.getNaturalOrder()) ? bindedClass.getFirstColumn() - 1 : bindedClass.getFirstColumn();
				for (int inx = startWith; inx < split.length; inx++) {
					String stringValue = split[inx];
					if (StringUtils.isNotEmpty(stringValue)) {
						int mapColumnValue = (bindedClass.getNaturalOrder()) ? (inx + 1) : inx;
						String fieldName = bindedClass.getExistingFields().get(mapColumnValue);
						if (StringUtils.isNotEmpty(fieldName)) {
							Object value = getValue(stringValue, fieldName, bindedClass);
							if (bindedClass.getInheritedFields().contains(fieldName)) {
								setInheritedField(fieldName, bindedClass.getClazz().getSuperclass(), myIntance, value);
							} else {
								Field field = myIntance.getClass().getDeclaredField(fieldName);
								if (field != null) {
									field.setAccessible(true);
									field.set(myIntance, value);
								}
							}
							
						}
					}
				}
			}
			String lineNumberField = bindedClass.getLineNumberField();
			if (StringUtils.trimToNull(lineNumberField) != null) {
				if (bindedClass.getInheritedFields().contains(lineNumberField)) {
					setInheritedField(lineNumberField, bindedClass.getClazz().getSuperclass(), myIntance, lineNumber);
				} else {
					Field field = myIntance.getClass().getDeclaredField(lineNumberField);
					if (field != null) {
						field.setAccessible(true);
						field.set(myIntance, lineNumber);
					}
				}
			}
		} catch (Exception e) {
			if (e instanceof InvalidFieldValueException) {
				throw (InvalidFieldValueException) e;
			}
			logger.error("Error parsing line " + line, e);
		}
		return myIntance;
	}
	
	/**
	 * Searches in the received superclass for the field to set, if not found
	 * will keep calling itself with the superclass of first received class
	 * until it finds it or there is not more super classes.
	 * NOTE this is in order to handle the setting of inherited properties,
	 * which camel-bindy didn't provide 
	 * @param fieldName
	 * @param superclass
	 * @param myIntance
	 * @param value
	 */
	private void setInheritedField(String fieldName, Class superclass, Object myIntance, Object value) {
		if (superclass != null) {
			Field field;
			try {
				field = superclass.getDeclaredField(fieldName);
				if (field != null) {
					field.setAccessible(true);
					field.set(myIntance, value);
				}
			} catch (NoSuchFieldException e) {
				setInheritedField(fieldName, superclass.getSuperclass(), myIntance, value);
			} catch (IllegalArgumentException e) {
				logger.warn("Trying to set {} with value {} for DTO {}", fieldName, value, superclass.getSuperclass());
				logger.error("Error setting field", e);
			} catch (IllegalAccessException e) {
				logger.warn("Trying to set {} with value {} for DTO {}", fieldName, value, superclass.getSuperclass());
				logger.error("Error setting field", e);
			}
		}
		
	}
	
	/**
	 * Selects the strategy to split columns, according to the quoting 
	 * character and the separator provided 
	 * @param line
	 * @return
	 */
	private String[] split(String line, BindedClass bindedClass) {
		String quoting = bindedClass.getQuoting();
		String separator = bindedClass.getSeparator();
		if (StringUtils.trimToNull(quoting) == null) {
			return splitWithNoQuoting(line, separator);
		} else {
			return splitWithQuoting(line, separator, quoting);
		}
	}
	
	/**
	 * Split by separator, assuming there is not quoting to indicate the 
	 * possibility of a separator character inside a text
	 * 
	 * @param line
	 * @param separator
	 * @return
	 */
	private String[] splitWithNoQuoting(String line, String separator) {
		String[] split = line.split(separator);
		List<String> columns = new ArrayList<String>(split.length + 1); 
		for (int i = 0; i < split.length; i++) {
			String columnValue = split[i];
				columns.add(columnValue);
		}
		return  columns.toArray(new String[]{});
	}
	
	/**
	 * Splits the line into columns, taking into account that inside quotings there might
	 * be a separator character that is only part of a text and not a an actual separator
	 * @param line
	 * @param separator
	 * @param quoting
	 * @return
	 */
	private String[] splitWithQuoting(String line, String separator, String quoting) {
		String[] split = line.split(separator);
		List<String> columns = new ArrayList<String>(split.length + 1); 
		for (int i = 0; i < split.length; i++) {
			String columnValue = split[i];
			if (columnValue != null && columnValue.startsWith(quoting)  && (!columnValue.endsWith(quoting) || columnValue.endsWith(quoting.concat(quoting)))) {
				StringBuilder column = new StringBuilder(columnValue);
				Boolean keepLooking = true;
				while (keepLooking) {
					column.append(separator);
					i++;
					if (i < split.length) {
						String otherColumn = split[i];
						column.append(otherColumn);
						if (otherColumn.endsWith(quoting) && !otherColumn.endsWith(quoting.concat(quoting))) {
							keepLooking = false;
						}
					} else {
						keepLooking = false;
					}
				}
				columns.add(column.toString());
			} else {
				columns.add(columnValue);
			}
		}
		
		
		return  columns.toArray(new String[]{});
	}

	/**
	 * Gets the value of the row-column string, and gets the corresponding value according
	 * to the field data type in your DTO
	 * @param value
	 * @param type
	 * @return
	 */
	protected Object getValue(String value, String fieldName, BindedClass bindedClass) throws InvalidFieldValueException {
		if (StringUtils.trimToNull(fieldName) != null) {
			String type = bindedClass.getFieldsDataType().get(fieldName);
			if (StringUtils.trimToNull(type) == null) {
				return null;
			}
			String quoting = bindedClass.getQuoting();
			if (StringUtils.trimToNull(quoting) != null) {
				String startWith = "^".concat(quoting);
				String endWith = quoting.concat("$");
				value = value.replaceAll(startWith, "").replaceAll(endWith, "");
			}
			if (bindedClass.hasToTrimField(fieldName)) {
				value = value.trim();
			}
			try {
				String lowerCasedType = type.toLowerCase();
				if ("String".equals(type)) {
					return value;
				} else {
					String cleanOfCommaNumber = value.replace(",", "");
					if ("Integer".equals(type) || "int".equals(type)) {
						return Integer.valueOf(cleanOfCommaNumber);
						
					} else if ("double".equals(lowerCasedType)) {
						return Double.valueOf(cleanOfCommaNumber);
						
					} else if ("BigDecimal".equals(type)) {
						return new BigDecimal(cleanOfCommaNumber);
						
					} else if ("short".equals(lowerCasedType)) {
						return Short.valueOf(cleanOfCommaNumber);
						
					} else if ("BigInteger".equals(type)) {
						return new BigInteger(cleanOfCommaNumber);
						
					} else if ("float".equals(lowerCasedType)) {
						return Float.valueOf(cleanOfCommaNumber);
						
					} else if ("boolean".equals(lowerCasedType)) {
						return getBoolean(value);
						
					} else if ("Date".equals(lowerCasedType)) {
						return  getDateFromString(value, type, fieldName, bindedClass);
					} else if ("Calendar".equals(type)) {
						Date date = getDateFromString(value, type, fieldName, bindedClass);
						if (date != null) {
							Calendar calendar = Calendar.getInstance();
							calendar.setTime(date);
							return calendar;
						}
					}
				}
			} catch (Exception e) {
				logger.error("{}, Error parsing string value '{}' for data type {}" , value,  type, e.getMessage());
				if (bindedClass.getFailAtWrongDataTypeException() || (failForField(fieldName, bindedClass))) {
					throw new InvalidFieldValueException(String.format("Null or Invalid data value: '%s',  for field '%s' (dataType '%s'). ", value, fieldName, type));
				}
				return null;
			}
			
		}
		return null;
	}

	private static List<String> trueValues = Arrays.asList(new String[]{"true", "yes", "y", "1"});
	private static List<String> falseValues = Arrays.asList(new String[]{"false", "no", "n", "0"});
	/**
	 * Gets a boolean value according to accepted boolean string representation. <br>
	 * TRUE: "true", "yes", "y", "1" <br>
	 * FALSE: "false", "no", "n", "0" <br>
	 * Throws an IllegalArgumentException if the value doesn't correspond to any <br> 
	 * of the accepted values
	 * @param value
	 * @return
	 */
	private Object getBoolean(String value) {
		String lowerCasedValue = value.toLowerCase();
		if (trueValues.contains(lowerCasedValue)) {
			return Boolean.TRUE;
		} else if (falseValues.contains(lowerCasedValue)) {
			return Boolean.FALSE;
		}
		throw new IllegalArgumentException("Invalid boolean data " + value);
	}

	/**
	 * Returns true if the whole process has to fail because of this field missing or with invalid value 
	 * @param fieldName
	 * @return
	 */
	private boolean failForField(String fieldName, BindedClass bindedClass) {
		
		return bindedClass.getThrowExceptionAtMissingData() && bindedClass.getRequiredFields().get(fieldName);
	}

	/**
	 * Parses a date string into a Date object, using the pattern assigned to the field, or the generic 
	 * google's feed accepted
	 * @param value
	 * @param type
	 * @param fieldName
	 * @return
	 * @throws Exception
	 */
	private Date getDateFromString(String value, String type, String fieldName, BindedClass bindedClass) throws Exception {
		String fieldDatePattern = bindedClass.getDateFormatPattern(fieldName);
		if (StringUtils.trimToNull(fieldDatePattern) != null) {
			return getDateFromString(value, fieldDatePattern);
		} else {
			return DateFormatUtils.getDateFromString(value);
		}
		
	}
	/**
	 * Gets a date from string value, using the assigned pattern
	 * @param value
	 * @return
	 * @throws ParseException 
	 */
	private Date getDateFromString(String value, String pattern) throws ParseException {
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
			return simpleDateFormat.parse(value);
	}

	

}
