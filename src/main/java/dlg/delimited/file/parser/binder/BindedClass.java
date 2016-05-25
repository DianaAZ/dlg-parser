package dlg.delimited.file.parser.binder;


import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import dlg.delimited.file.parser.annotations.BindedFieldData;
import dlg.delimited.file.parser.annotations.BinderClassData;
import dlg.delimited.file.parser.annotations.LineNumberField;
import dlg.delimited.file.parser.annotations.BinderClassData.Quoting;


@SuppressWarnings("rawtypes")
public class BindedClass {

	private final Map<Integer, String> existingFields;
	private final List<String> inheritedFields;
	private final Map<String, String> fieldsDataTypes;
	/**
	 * Calculated. Indicates which one is the column to start parsing the line
	 */
	private Integer firstColumn = null;

	/**
	 * If true, indicates that mapping of columns start with 1 (natural order,
	 * first column = 1) if false, indicates that the mapping of columns uses
	 * progamming order (first column = 0)
	 */
	private Boolean naturalOrder = true;
	/**
	 * use to specify what is the name of lineNumber object, is your DTO
	 * contains that field
	 */
	private String lineNumberField = null;

	/**
	 * For now, just TSV won't allow other parsing time such are , or ;, because
	 * I still don't have a strategy to handle them inside a string field
	 * probably regex are going to help me out on this.
	 */
	private String separator;

	private String quoting;

	/**
	 * set false if first row doesn't contain headers
	 */
	private Boolean ignoreFirstLine = true;

	/**
	 * set it to define the pattern to parse date string, otherwise will use
	 * google's patterns
	 */
	private final Map<String, String> dateFormatPattern;

	private final Map<String, Boolean> requiredFields;
	private final Map<String, Boolean> trimFieldRequired;

	/**
	 * set to true if it's desired to trim column values before assigning it
	 */
	private Boolean trimFields = false;

	private Boolean throwExceptionAtMissingData = false;

	private Boolean failAtWrongDataTypeException = false;

	private Class clazz;

	/**
	 * 
	 * @param clazz
	 *            (Class of your DTO)
	 * @param existingFields
	 *            (map containing the index of column->fieldName to map)
	 */
	@SuppressWarnings("unchecked")
	public BindedClass(Class clazz) {
		if (clazz == null) {
			throw new NullPointerException("Invalid parameters: class is mandatory");
		}
		inheritedFields = new ArrayList<String>();
		this.clazz = clazz;
		try {
			BinderClassData annotation = (BinderClassData) this.clazz
					.getAnnotation(dlg.delimited.file.parser.annotations.BinderClassData.class);
			this.naturalOrder = annotation.naturalOrder();
			this.ignoreFirstLine = annotation.ignoreFirstRow();
			this.trimFields = annotation.trimValue();
			this.throwExceptionAtMissingData = annotation.throwExceptionAtMissingData();
			this.failAtWrongDataTypeException = annotation.failAtWrongDataTypeException();
			this.separator = annotation.separtor();
			Quoting quotingType = annotation.quoting();
			this.quoting = null;
			if (Quoting.SINGLE.equals(quotingType)) {
				this.quoting = "'";
			} else if (Quoting.DOUBLE.equals(quotingType)) {
				this.quoting = "\"";
			}

		} catch (Exception e) {
			
		}
		fieldsDataTypes = new HashMap<String, String>();
		dateFormatPattern = new HashMap<String, String>();
		existingFields = new HashMap<Integer, String>();
		requiredFields = new HashMap<String, Boolean>();
		trimFieldRequired = new HashMap<String, Boolean>();

		setFieldsDataType();

		Set<Integer> keySet = existingFields.keySet();
		for (Integer columnNumber : keySet) {
			if (firstColumn == null || firstColumn > columnNumber) {
				firstColumn = columnNumber;
			}
		}
		if (firstColumn == 0) {
			naturalOrder = false;
		}
	}

	/**
	 * Chechs the fields of you DTO and the data types of each fields
	 */
	private void setFieldsDataType() {
		Field[] declaredFields = clazz.getDeclaredFields();
		if (declaredFields != null && declaredFields.length > 0) {
			for (Field field : declaredFields) {
				getAnnotation(field);
			}
		}
		getInheritedProperties(clazz.getSuperclass());
	}

	/**
	 * Checks for binded properties in the parent class that weren't already
	 * specified, and call itself recursively until there is no more parent
	 * class.
	 * 
	 * @param clazz
	 */
	private void getInheritedProperties(Class clazz) {
		if (clazz != null) {
			Field[] declaredFields = clazz.getDeclaredFields();
			if (declaredFields != null && declaredFields.length > 0) {
				for (Field field : declaredFields) {
					if (getAnnotation(field)) {
						inheritedFields.add(field.getName());
					}
				}
			}
			Class superclass = clazz.getSuperclass();
			getInheritedProperties(superclass);
		}
	}

	/**
	 * For the received field, checks if there is a binding property
	 * that indicates whether it's a field to bind to a column, 
	 * in that case to which column, if required, what kind of data type it is, 
	 * if it has to be trimmed and other stuffs, all by searching for the
	 * {@link BindedFieldData} annotation and reading its properties
	 * 
	 * @param field
	 * @return
	 */
	private boolean getAnnotation(Field field) {
		String fieldName = field.getName();
		try {
			String type = field.getType().getSimpleName();
			if (this.lineNumberField == null) {
				LineNumberField lineNumberAnnotation = field
						.getAnnotation(dlg.delimited.file.parser.annotations.LineNumberField.class);
				if (lineNumberAnnotation != null) {
					boolean isLineNumberField = lineNumberAnnotation.setLineNumber();
					if (isLineNumberField && StringUtils.trimToNull(this.lineNumberField) == null
							&& ("Integer".equals(type) || "int".equals(type))) {
						this.lineNumberField = fieldName;
						return true;
					}
				}
			}

			BindedFieldData annotation = field.getAnnotation(dlg.delimited.file.parser.annotations.BindedFieldData.class);
			if (annotation != null) {
				if (!existingFields.containsValue(fieldName)) {
					int position = annotation.readPosition();
					if (position > -1) {

						existingFields.put(position, fieldName);
						fieldsDataTypes.put(fieldName, type);
						String datepattern = annotation.dateStringPattern();
						if (StringUtils.trimToNull(datepattern) != null) {
							this.dateFormatPattern.put(fieldName, datepattern);
						}
						boolean required = annotation.required();
						this.requiredFields.put(fieldName, Boolean.valueOf(required));

						boolean trimValue = annotation.trimValue();
						this.trimFieldRequired.put(fieldName, Boolean.valueOf(trimValue));
					}
					return true;
				}
			}
		} catch (Exception e) {

		}
		return false;
	}

	public Boolean getIgnoreFirstLine() {
		return ignoreFirstLine;
	}

	public Map<Integer, String> getExistingFields() {
		return existingFields;
	}

	public String getDateFormatPattern(String fieldName) {
		return dateFormatPattern.get(fieldName);
	}

	public Boolean hasToTrimField(String fieldName) {
		return (trimFields || this.trimFieldRequired.get(fieldName));
	}

	public String getLineNumberField() {
		return lineNumberField;
	}

	public Boolean getNaturalOrder() {
		return naturalOrder;
	}

	public List<String> getInheritedFields() {
		return inheritedFields;
	}

	public Map<String, String> getFieldsDataType() {
		return fieldsDataTypes;
	}

	public Integer getFirstColumn() {
		return firstColumn;
	}

	public String getSeparator() {
		return separator;
	}

	public Map<String, String> getFieldsDataTypes() {
		return fieldsDataTypes;
	}

	public Map<String, String> getDateFormatPattern() {
		return dateFormatPattern;
	}

	public Map<String, Boolean> getRequiredFields() {
		return requiredFields;
	}

	public boolean getThrowExceptionAtMissingData() {
		return throwExceptionAtMissingData;
	}

	public boolean getFailAtWrongDataTypeException() {
		return failAtWrongDataTypeException;
	}

	public Class getClazz() {
		return clazz;
	}

	public String getQuoting() {
		return quoting;
	}

}
