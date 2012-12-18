package grails.plugin.springmvc;

/**
 * A JSP facade that delegates to the Grails resource tag.
 *
 * @author Burt Beckwith
 */
public class JspResourceTag extends JspInvokeGrailsSimpleTagLibTag {

	private static final long serialVersionUID = 1;

	private static final String TAG_NAME = "resource";

	private String base;
	private String contextPath;
	private String dir;
	private String file;
	private String absolute;

	public JspResourceTag() {
		setTagName(TAG_NAME);
	}

	public String getBase() {
		return base;
	}
	public void setBase(String base) {
		this.base = base;
	}

	public String getContextPath() {
		return contextPath;
	}
	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	public String getDir() {
		return dir;
	}
	public void setDir(String dir) {
		this.dir = dir;
	}

	public String getFile() {
		return file;
	}
	public void setFile(String file) {
		this.file = file;
	}

	public String getAbsolute() {
		return absolute;
	}
	public void setAbsolute(String absolute) {
		this.absolute = absolute;
	}
}
