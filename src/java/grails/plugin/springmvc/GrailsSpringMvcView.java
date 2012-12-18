package grails.plugin.springmvc;

import java.util.Locale;

import org.springframework.core.io.Resource;
import org.springframework.web.servlet.view.JstlView;

public class GrailsSpringMvcView extends JstlView {

	@Override
	public boolean checkResource(Locale locale) throws Exception {
		String url = getUrl();
		if (url.startsWith("/")) {
			url = url.substring(1);
		}
		Resource resource = getApplicationContext().getResource(url);
		return resource != null && resource.exists();
	}
}
