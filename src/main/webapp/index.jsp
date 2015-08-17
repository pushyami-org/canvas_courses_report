<!DOCTYPE html>
<%
String canvasURL=(String)request.getAttribute("canvasURL");
%>
<html lang="en">
<head>
<meta charset="utf-8">
<title>Course Report Tool</title>
<link rel="stylesheet" href="assets/vendor/bootstrap/bootstrap.min.css">
<link href="assets/css/report.css" rel="stylesheet">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<meta http-equiv="Cache-control" content="no-cache">
<meta http-equiv="X-UA-Compatible" content="IE=9">
<script src="assets/vendor/jquery/jquery-1.11.0.min.js" type="text/javascript"></script>
<script src="assets/js/analytics.js" type="text/javascript"></script>
</head>
<body>
<div class="container">
<p></p>
<div class="well well-sm">
<h1>Course Report Tool </h1>
<span id="serverURL" class="alert"><%=canvasURL%></span>
</div>
<div class="panel panel-default">
<div class="panel-body">
<label for="termsSelect"></label>
  <select id="termsSelect">
  </select>
  <div class="spinner" style="display:none"></div>
</div>
</div>
<div id="sessExpire" class="well well-sm" style="display:none">Session Expired, Please reload the page</div>
<div id="errRes" class="well well-sm" style="display:none"></div>
</div>
 
</body>
</html>
