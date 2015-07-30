$(document).ready(function(){
	getTermsInfo();
/*var newSelect=document.createElement('select');
    var selectHTML="";
   /* for(i=0; i<choices.length; i=i+1){
        selectHTML+= "<option value='"+choices[i]+"'>"+choices[i]+"</option>";
    }*/
   /* selectHTML+= "<option value='test'>test</option>";

    newSelect.innerHTML= selectHTML;
    document.getElementById('enrollment_terms').appendChild(newSelect);*/
    
    function getTermsInfo(){
    	console.log("TERMS info function");
    	var url="analytics?action=getEnrollmentTerms";
    	var request = $.ajax({
			url: url,
			type: "GET",			
			dataType: "json"
		});
    	request.done(function(data) {
    		var terms=data.enrollment_terms;
    		var newSelect=document.createElement('select');
    		 var selectHTML="";
    		$.each(terms, function(index, element) {
    			selectHTML+= "<option value='"+element.id+"'>"+element.name+"</option>";
    	    });	
    		newSelect.innerHTML= selectHTML;
    	    document.getElementById('enrollment_terms').appendChild(newSelect);
		});
    	request.fail(function( jqXHR, textStatus ) {
    		  alert( "Request failed: " + textStatus );
    	});
    	
    }
});