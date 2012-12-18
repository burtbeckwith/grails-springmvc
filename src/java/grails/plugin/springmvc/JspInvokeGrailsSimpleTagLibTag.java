package grails.plugin.springmvc;

import groovy.lang.Closure;
import groovy.lang.GroovyObject;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.DynamicAttributes;
import javax.servlet.jsp.tagext.TagSupport;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsTagLibClass;
import org.codehaus.groovy.grails.commons.TagLibArtefactHandler;
import org.codehaus.groovy.grails.web.pages.FastStringPrintWriter;
import org.codehaus.groovy.grails.web.pages.GroovyPage;
import org.codehaus.groovy.grails.web.servlet.DefaultGrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.context.ApplicationContext;
import org.springframework.web.util.ExpressionEvaluationUtils;

/**
 * Based on org.codehaus.groovy.grails.web.taglib.jsp.JspInvokeGrailsTagLibTag
 * but useful when calling tags that don't have a body.
 *
 * @author Burt Beckwith
 */
public class JspInvokeGrailsSimpleTagLibTag extends TagSupport implements DynamicAttributes {

	private static final long serialVersionUID = 1;
	private static final String NAME_ATTRIBUTE = "tagName";
	private static final String TAG_LIBS_ATTRIBUTE = "org.codehaus.groovy.grails.TAG_LIBS";
	private static final String OUT_PROPERTY = "out";
	private static final Pattern ATTRIBUTE_MAP = Pattern.compile("(\\s*(\\S+)\\s*:\\s*(\\S+?)(,|$){1}){1}");

	private String tagName;
	private FastStringPrintWriter sw;
	private PrintWriter out;
	private GrailsApplicationAttributes grailsAttributes;
	private GrailsApplication application;
	private ApplicationContext appContext;
	private Map<String, Object> attributes = new HashMap<String, Object>();
	private BeanWrapper bean = new BeanWrapperImpl(this);

	@Override
	public int doEndTag() throws JspException {
		initAttributes();
		invokeTaglib();
		writeContent();
		return EVAL_PAGE;
	}

	public String getTagName() {
		return tagName;
	}

	public void setTagName(String tagName) {
		this.tagName = tagName;
	}

	private void initAttributes() throws JspException {
		for (PropertyDescriptor pd : bean.getPropertyDescriptors()) {
			if (pd.getPropertyType() != String.class ||
			    pd.getName().equals(NAME_ATTRIBUTE) ||
			    !bean.isWritableProperty(pd.getName()) ||
			    !bean.isReadableProperty(pd.getName())) {
				continue;
			}

			String propertyValue = (String)bean.getPropertyValue(pd.getName());
			if (propertyValue == null) {
				continue;
			}

			String trimmed = propertyValue.trim();
			if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
				trimmed = trimmed.substring(1, trimmed.length() - 1);
				Matcher m = ATTRIBUTE_MAP.matcher(trimmed);
				Map<String, Object> attributeMap = new HashMap<String, Object>();
				while (m.find()) {
					String attributeName = m.group(1);
					String attributeValue = m.group(2);
					storeAttribute(attributeMap, attributeName, attributeValue);
				}
				attributes.put(pd.getName(), attributeMap);
			}
			else {
				storeAttribute(attributes, pd.getName(), propertyValue);
			}
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private GroovyObject getTagLib(String name) {
		initPageState();

		Map tagLibs = (Map)pageContext.getAttribute(TAG_LIBS_ATTRIBUTE);
		if (tagLibs == null) {
			tagLibs = new HashMap();
			pageContext.setAttribute(TAG_LIBS_ATTRIBUTE, tagLibs);
		}
		GrailsTagLibClass tagLibClass = (GrailsTagLibClass) application.getArtefactForFeature(
				TagLibArtefactHandler.TYPE, GroovyPage.DEFAULT_NAMESPACE + ':' + name);

		GroovyObject tagLib;
		if (tagLibs.containsKey(tagLibClass.getFullName())) {
			tagLib = (GroovyObject)tagLibs.get(tagLibClass.getFullName());
		}
		else {
			tagLib = (GroovyObject)appContext.getBean(tagLibClass.getFullName());
			tagLibs.put(tagLibClass.getFullName(), tagLib);
		}
		return tagLib;
	}

	@SuppressWarnings("rawtypes")
	private void invokeTaglib() {
		GroovyObject tagLib = getTagLib(getTagName());
		if (tagLib == null) {
			throw new GrailsTagException("Tag [" + getTagName() + "] does not exist. No tag library found.");
		}

		sw = new FastStringPrintWriter();
		out = sw;
		tagLib.setProperty(OUT_PROPERTY, out);
		Object tagLibProp;
		final Map tagLibProperties = DefaultGroovyMethods.getProperties(tagLib);
		if (tagLibProperties.containsKey(getTagName())) {
			tagLibProp = tagLibProperties.get(getTagName());
		}
		else {
			throw new GrailsTagException("Tag [" + getTagName() + "] does not exist in tag library [" +
					tagLib.getClass().getName() + "]");
		}

		if (!(tagLibProp instanceof Closure)) {
			throw new GrailsTagException("Tag [" + getTagName() + "] does not exist in tag library [" +
					tagLib.getClass().getName() + "]");
		}

		Closure<?> tag = (Closure)tagLibProp;
		if (tag.getParameterTypes().length == 1) {
			tag.call(new Object[] { attributes });
		}
		else {
			// TODO
		}
	}

	private void writeContent() throws JspTagException {
		String tagContent = sw.toString();
		try {
			pageContext.getOut().write(tagContent);
			out.close();
		}
		catch (IOException e) {
			throw new JspTagException("I/O error writing tag contents [" + tagContent + "] to response out");
		}
	}

	private void initPageState() {
		if (application == null) {
			grailsAttributes = new DefaultGrailsApplicationAttributes(pageContext.getServletContext());
			application = grailsAttributes.getGrailsApplication();
			appContext = grailsAttributes.getApplicationContext();
		}
	}

	private void storeAttribute(Map<String, Object> map, String name, String value) throws JspException {
		Object storedValue = value;
		if (ExpressionEvaluationUtils.isExpressionLanguage(value)) {
			storedValue = ExpressionEvaluationUtils.evaluate(name, value, Object.class, pageContext);
		}
		map.put(name, storedValue);
	}

	public final void setDynamicAttribute(String uri, String localName, Object value) throws JspException {
		if (!(value instanceof String)) {
			attributes.put(localName, value);
			return;
		}

		String stringValue = (String)value;
		String trimmed = stringValue.trim();
		if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
			trimmed = trimmed.substring(1, trimmed.length() - 1);
			Matcher m = ATTRIBUTE_MAP.matcher(trimmed);
			Map<String, Object> attributeMap = new HashMap<String, Object>();
			while (m.find()) {
				String attributeName = m.group(1);
				String attributeValue = m.group(2);
				storeAttribute(attributeMap, attributeName, attributeValue);
			}
			attributes.put(localName, attributeMap);
		}
		else {
			storeAttribute(attributes, localName, stringValue);
		}
	}
}
