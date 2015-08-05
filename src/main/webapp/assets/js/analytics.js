$(document).ready(function(){
	var term='term';
	getTermsInfo();
    
    function getTermsInfo(){
    	var url="analytics?action=getEnrollmentTerms";
    	var termRequest = $.ajax({
			url: url,
			type: "GET",			
			dataType: "json"
		});
    	termRequest.done(function(data) {
    		var terms=data.enrollment_terms;
    		var newSelect=document.createElement('select');
    		newSelect.setAttribute('id', 'termsSelect');
    		 var selectHTML="";
    		$.each(terms, function(index, term) {
    			selectHTML+= '<option id="term'+term.id+'" value="'+term.id+'">'+term.name+'</option>';
    	    });	
    		newSelect.innerHTML= selectHTML;
    	    document.getElementById('enrollment_terms').appendChild(newSelect);
    	    $('#termsSelect').on('change',function() {
    	    	var courseReportRequest = $.ajax({
    				url: 'analytics?action=getCoursesPublished&term='+$(this).val()+'&termName='+$('#termsSelect option:selected').text(),
    				type: "GET",			
    			});
    	    	courseReportRequest.done(function(response, status, xhr){
    	    		var filename = "";
    	            var disposition = xhr.getResponseHeader('Content-Disposition');
    	            if (disposition && disposition.indexOf('attachment') !== -1) {
    	                var filenameRegex = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/;
    	                var matches = filenameRegex.exec(disposition);
    	                if (matches != null && matches[1]) filename = matches[1].replace(/['"]/g, '');
    	            }

    	            var type = xhr.getResponseHeader('Content-Type');
    	            var blob = new Blob([response], { type: type });

    	            if (typeof window.navigator.msSaveBlob !== 'undefined') {
    	                // IE workaround for "HTML7007: One or more blob URLs were revoked by closing the blob for which they were created. These URLs will no longer resolve as the data backing the URL has been freed."
    	                window.navigator.msSaveBlob(blob, filename);
    	            } else {
    	                var URL = window.URL || window.webkitURL;
    	                var downloadUrl = URL.createObjectURL(blob);

    	                if (filename) {
    	                    // use HTML5 a[download] attribute to specify filename
    	                    var a = document.createElement("a");
    	                    // safari doesn't support this yet
    	                    if (typeof a.download === 'undefined') {
    	                        window.location = downloadUrl;
    	                    } else {
    	                        a.href = downloadUrl;
    	                        a.download = filename;
    	                        document.body.appendChild(a);
    	                        a.click();
    	                    }
    	                } else {
    	                    window.location = downloadUrl;
    	                }

    	                setTimeout(function () { URL.revokeObjectURL(downloadUrl); }, 100); // cleanup
    	            }
    	    	});
    	    	
    	    	courseReportRequest.fail(function( jqXHR, textStatus ) {
    	    		  alert( "Terms request failed: " + textStatus );
    	    	});
    	    	
    	    });
    	    
		});
    	termRequest.fail(function( jqXHR, textStatus ) {
    		  alert( "Terms request failed: " + textStatus );
    	});
    	
    }
    
});

