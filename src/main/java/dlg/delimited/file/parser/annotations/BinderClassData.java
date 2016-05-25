package dlg.delimited.file.parser.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 
 * @author dalcantara
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface BinderClassData {
	/**
	 * Quoting characters accepted
	 * @author dalcantara
	 *
	 */
	enum Quoting {SINGLE, DOUBLE, NONE};
	
	
	/**
	 * Quoting character, used to enclose text that might contain the separator character
	 * Possible values, <br>
	 * SINGLE: ' <br>
	 * DOUBLE: " <br>
	 * NONE:
	 * @return
	 */
	Quoting quoting() default Quoting.DOUBLE; 
	
	/**
	 * Default value tab ("\t"). Set it when you need to indicate a different
	 * separator. <br>
	 * @return
	 */
	String separtor() default "\t";
	
	/**
	 * Default true. Indicates that first row has to be ignored, because
	 * it's the header or some other reason.
	 * @return
	 */
	boolean ignoreFirstRow() default true;
	
	/**
	 * When true (default),indicates that the position of column indicated is
	 * natural a natural sequence: starting with 1. <br>
	 * When false (you have to specify it), indicates that the order of
	 * columns provided is like programming order: starting with 0
	 * 
	 * @return
	 */
	boolean naturalOrder() default true;
	
	/**
	 * Default false. If true, will trim all values of all columns, overwriting any specification 
	 * in the fields. If you just want to trim certain fields, use {@link FieldData.trimValue()} 
	 * @return
	 */
	boolean trimValue() default false;
	
	/**
	 * Default false. If set to true, will interrupt the whole parsing process and throw an exception if a 
	 * field indicated required is null ( see {@link BindedFieldData.required}
	 * @return
	 */
	boolean throwExceptionAtMissingData() default false;
	
	/**
	 * Default false. If set to true, will interrupt the whole parsing process and throw an exception if
	 * it a wrong data is found in field mapped to a property (i.e. string value of field: "45645A" and the column 
	 * is mapped to a Double property)
	 * @return
	 */
	boolean failAtWrongDataTypeException() default false;
}
