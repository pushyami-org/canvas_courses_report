# canvas_courses_report

## Build Directions
 
1. CourseReportTool$ <code>mvn clean install</code>
2. Copy to tomcat/webapp
3. Add the property file on linux box <code>coursereport.properties</code>,   
then in JAVA_OPTS add the  
<code>-DcanvasCourseReportPath=file:/file-path/coursereport.properties</code><br/>


4. Add the following 5 properties to coursereport.properties: 

    <code>canvas.admin.token=canvas token  
    canvas.url=target canvas server e.g. https://umich.test.instructure.com  
    use.test.url=true  
    ldap.server.url=ldap server e.g. ldap://ldap.itd.umich.edu:389/dc=umich,dc=edu  
    mcomm.group=group that can use this tool e.g. its-canvas-reports</code>


5. Run this in local  
<code>http://localhost:port/canvas_courses_report/?testUser=uniqname</code><br/>
i. testUser parameter is not allowed in Prod and this is controlled by property with value called <code>use.test.url=false</code><br/>
ii. We will enable the cosign for authentication the user so we will get remote user info through that.<br/>

6. Enable application level logging using the log4j.properties files. Put this file in tomcat/lib directory and add the content between the 
 
	```
log4j.rootLogger=INFO, A1
log4j.appender.A1=org.apache.log4j.ConsoleAppender
log4j.appender.A1.layout=org.apache.log4j.PatternLayout
# Print the date in ISO 8601 format
log4j.appender.A1.layout.ConversionPattern=%d [%t] %-5p %c - %m%n
# umich
#log4j.logger.edu.umich=INFO
log4j.logger.edu.umich=DEBUG 
```

7. For adding build information to the project release candidate, populate src/main/webapps/build.txt with information about the current build (GIT commit, build time, etc.).
    If using Jenkins to build the application, the following script placed in the "Execute Shell" part of the "Pre Steps" section would do the job:
    
    
	 ``` 
    cd src/main/webapp
    if [ -f "build.txt" ]; then
      echo "build.txt found."
      rm build.txt
      echo "Existing build.txt file removed."
    else
      echo "No existing build.txt file found."
    fi
    touch build.txt

    echo "$JOB_NAME | Build: $BUILD_NUMBER | $GIT_URL | $GIT_COMMIT | $GIT_BRANCH | $BUILD_ID" >> build.txt
 ```

## Notes

If <code>use.test.url</code> is true, users will be able to execute the tool as if authenticated as the user specified in the URL parameter <code>?testUser=uniqname</code>. In Production this variable is  false. Based on this property property testUser is not allowed in Production.

ldap is used for authorizing the user and he needs to be part of particular Mcommunity group to be authorized to use the tool.

## Know issues
1)Currently in application each request for generating a course report is placed on separate thread and all the list of threads created likewise are placed in memory(java MAP object). The application is  deployed to tlsupportqa/tlsupport application servers and all request are directed only to one webserver( e.g., tomcat). Using a map in memory will work fine as all the requests go back to the same web server. if we ever have multiple webservers, 
   a) one solution is to make the Load balancer session "sticky" so that the request always goes back to same server. 
   b) Based on the usage we make have to revisit this issue to have better solution to store the list of the Threads in Database or Session.






 

