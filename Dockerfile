FROM tomcat:7-jre8
MAINTAINER Pushyami Gundala <pushyami@umich.edu>

RUN apt-get update \
 && apt-get install -y wget

#ENV JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64

WORKDIR /tmp

RUN wget http://limpkin.dsc.umich.edu:6660/job/CourseReportTool-Master/lastSuccessfulBuild/artifact/target/canvas_courses_report.master.20160226152728.war

RUN cp canvas_courses_report.master.20160226152728.war canvas_courses_report.war

RUN mv canvas_courses_report.war /usr/local/tomcat/webapps

WORKDIR /usr/local/tomcat/webapps

# Set Opts, including paths for the CCM properties.
ENV JAVA_OPTS="-server \
-Xmx1028m \
-Dorg.apache.jasper.compiler.Parser.STRICT_QUOTE_ESCAPING=false \
-Djava.awt.headless=true -Dcom.sun.management.jmxremote \
-Dsun.lang.ClassLoader.allowArraySyntax=true \
-Dfile.encoding=UTF-8 \
-DcanvasCourseReportPath=file:$CATALINA_HOME/conf/canvasreport.properties \
-Dlog4j.configuration=file:/usr/local/tomcat/conf/log4j.properties \
"
EXPOSE 8080
EXPOSE 8009
EXPOSE 5010
ENV JPDA_ADDRESS="5010"
ENV JPDA_TRANSPORT="dt_socket"

CMD cp /usr/share/ccr/log4j.properties /usr/local/tomcat/conf/log4j.properties /usr/share/ccr/canvasReport.properties /usr/local/tomcat/conf catalina.sh jpda run
