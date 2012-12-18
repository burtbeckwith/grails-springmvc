import grails.plugin.springmvc.GrailsSpringMvcView

import org.springframework.orm.hibernate3.support.OpenSessionInViewInterceptor
import org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor
import org.springframework.web.servlet.view.UrlBasedViewResolver

class SpringmvcGrailsPlugin {
	String version = '0.2'
	String grailsVersion = '2.0 > *'
	String author = 'Burt Beckwith'
	String authorEmail = 'beckwithb@vmware.com'
	String title = 'Spring MVC Controller Plugin'
	String description = 'Enables the use of Spring MVC controllers'
	String documentation = 'http://grails.org/plugin/springmvc'

	String license = 'APACHE'
	def issueManagement = [system: 'JIRA', url: 'http://jira.grails.org/browse/GPSPRINGMVC']
	def scm = [url: 'https://github.com/burtbeckwith/grails-springmvc']

	def doWithWebDescriptor = { xml ->
		def config = application.config.springmvc
		String suffix = config.urlSuffix ?: 'action'

		def servlets = xml.servlet
		def lastServlet = servlets[servlets.size() - 1]
		lastServlet + {
			servlet {
				'servlet-name'('SpringMVC')
				'servlet-class'('org.springframework.web.servlet.DispatcherServlet')
				'load-on-startup'(1)
				'init-param' {
					'param-name'('detectAllHandlerExceptionResolvers')
					'param-value'('false')
				}
			}
		}

		def mappings = xml.'servlet-mapping'
		def lastMapping = mappings[mappings.size() - 1]
		lastMapping + {
			'servlet-mapping' {
				'servlet-name'('SpringMVC')
				'url-pattern'("*.$suffix")
			}
		}
	}

	def doWithSpring = {
		def config = application.config.springmvc

		// can't use these from Grails
		mvcOpenSessionInViewInterceptor(OpenSessionInViewInterceptor) {
			sessionFactory = ref('sessionFactory')
		}

		mvcLocaleChangeInterceptor(LocaleChangeInterceptor)

		def handlerInterceptors = [ref('mvcOpenSessionInViewInterceptor'), ref('mvcLocaleChangeInterceptor')]
		config.interceptors.each { interceptor ->
			handlerInterceptors << ref(interceptor)
		}

		mvcHandlerMapping(BeanNameUrlHandlerMapping) {
			detectHandlersInAncestorContexts = true
			order = 1
			interceptors = handlerInterceptors
		}

		mvcViewResolver(UrlBasedViewResolver) {
			viewClass = GrailsSpringMvcView
			order = 1
			prefix = '/WEB-INF/jsp/'
			suffix = '.jsp'
		}

		handlerExceptionResolver(SimpleMappingExceptionResolver) {
			order = 1
			defaultErrorView = config.defaultErrorView ?: 'error' // default to WEB-INF/error.jsp
			if (config.exceptionMappings) {
				exceptionMappings = new Properties()
				config.exceptionMappings.each { key, value ->
					exceptionMappings.setProperty key, value
				}
			}
		}

		def controllerPackages = config.controllerPackages
		if (controllerPackages instanceof Collection) {
			xmlns grailsContext:'http://grails.org/schema/context'
			grailsContext.'component-scan'('base-package': controllerPackages.join(','))
		}
	}
}
