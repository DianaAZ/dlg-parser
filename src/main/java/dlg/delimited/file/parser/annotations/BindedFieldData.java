package dlg.delimited.file.parser.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Config indications for each field to be mapped
 * @author dalcantara
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface BindedFieldData {
	
	/**
	 * Column index of the value for the property.
	 * @return
	 */
	int readPosition() default -1;
	
	/**
	 * Column index in which the column should be written.
	 * @return
	 */
	int writePosition() default -1;
	
	/**
	 * By default, all fields are not required.
	 * Use it when you need it to be required.
	 * @return
	 */
	boolean required() default false;
		
	/**
	 * Indicates that the string value of the row-column position should 
	 * be trimmed before any treatment or assignment. 
	 * @return
	 */
	boolean trimValue() default false;
	
	/**
	 * if a date field has to be parsed using an particular pattern, 
	 * provide it with this attribute
	 * @return
	 */
	String dateStringPattern() default "";
	
	/**
	 * Use this attribute in your DTO if you have a field that represent
	 * the line number in the file content, and you wish it to be set.
	 * 
	 * @return
	 */
	boolean lineNumberField() default false;
	
}
