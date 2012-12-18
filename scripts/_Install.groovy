String webInfDir = "$basedir/web-app/WEB-INF"
String jspDir = "$webInfDir/jsp"
String tldDir = "$webInfDir/tld"

ant.mkdir(dir: jspDir)
ant.mkdir(dir: tldDir)

if (!new File(webInfDir, 'SpringMVC-servlet.xml').exists()) {
	ant.copy(file: "$springmvcPluginDir/src/resources/SpringMVC-servlet.xml", todir: webInfDir, verbose: true)
}

if (!new File(jspDir, 'error.jsp').exists()) {
	ant.copy(file: "$springmvcPluginDir/src/resources/error.jsp", todir: jspDir, verbose: true)
}

if (!new File(webInfDir, 'grails-new.tld').exists()) {
	ant.copy(file: "$springmvcPluginDir/src/resources/grails-new.tld", todir: tldDir, verbose: true)
}
