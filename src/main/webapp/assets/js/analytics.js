  'use strict';
  /* jshint  strict: true*/
  /* global $, Blob, document, window*/

  $(document).ready(function() {
      var reportPollIntervalMillis = 10000; //poll every 10 sec
      getTermsInfo();

      function getTermsInfo() {
          var url = 'report?action=getEnrollmentTerms';
          var termRequest = $.ajax({
              url: url,
              type: 'GET',
              dataType: 'json'
          });
          termRequest.done(function(data) {
              var terms = data.enrollment_terms;
              var newSelect = document.getElementById('termsSelect');
              var selectHTML = '';
              $.each(terms, function(index, term) {
                  selectHTML += '<option id="term' + term.id + '" value="' + term.id + '">' + term.name + '</option>';
              });
              newSelect.innerHTML = selectHTML;
              $('#termsSelect').on('change', function() {
                  $('.spinner').show();
                  var courseReportRequest = $.ajax({
                      url: 'report?action=getCoursesPublished&term=' + $(this).val() + '&termName=' + $('#termsSelect option:selected').text(),
                      type: 'GET',
                  });
                  courseReportRequest.done(function(response, status, xhr) {
                      var reportIntervId = setInterval(function() {
                          reportTimer(response);
                      }, reportPollIntervalMillis);

                      function reportTimer(response) {
                          $('.spinner').show();
                          var resultsMsg = $.ajax({
                              url: 'report?action=polling&thrdId=' + response,
                              type: 'get',
                          }).done(function(response, status, xhr) {
                              if (response !== "working") {
                                  clearInterval(reportIntervId);
                                  $('.spinner').hide();
                                  downloadFile(response, status, xhr);
                              }
                          }).fail(function(xhr) {
                              clearInterval(reportIntervId);
                              $('.spinner').hide();
                              if (xhr.status === 403) {
                                  $('#sessExpire').show();
                                  $('#sessExpire').fadeIn().delay(10000).fadeOut();
                              }
                              $('#errRes').html('<p>' + xhr.responseText + '</p>').show();
                              $('#errRes').fadeIn().delay(10000).fadeOut();
                          });
                      } // end of reportTimer()
                  });

                  courseReportRequest.fail(function(xhr) {
                      $('.spinner').hide();
                      if (xhr.status === 403) {
                          $('#sessExpire').show();
                          $('#sessExpire').fadeIn().delay(10000).fadeOut();
                      }
                      $('#errRes').html('<p>' + xhr.responseText + '</p>').show();
                      $('#errRes').fadeIn().delay(10000).fadeOut();
                  });

              });

          });
          termRequest.fail(function(xhr) {
              if (xhr.status === 403) {
                  $('#sessExpire').show();
                  $('#sessExpire').fadeIn().delay(10000).fadeOut();
              }
              $('#errRes').html('<p>' + xhr.responseText + '</p>').show();
              $('#errRes').fadeIn().delay(10000).fadeOut();
          });

      } //end of getTermsInfo()

      function downloadFile(response, status, xhr) {
          var filename = '';
          var disposition = xhr.getResponseHeader('Content-Disposition');
          if (disposition && disposition.indexOf('attachment') !== -1) {
              var filenameRegex = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/;
              var matches = filenameRegex.exec(disposition);
              if (matches !== null && matches[1]) filename = matches[1].replace(/['"]/g, '');
          }

          var type = xhr.getResponseHeader('Content-Type');
          var blob = new Blob([response], {
              type: type
          });

          if (typeof window.navigator.msSaveBlob !== 'undefined') {
              // IE workaround for "HTML7007: One or more blob URLs were revoked by closing the blob for which they were created. These URLs will no longer resolve as the data backing the URL has been freed."
              window.navigator.msSaveBlob(blob, filename);
          } else {
              var URL = window.URL || window.webkitURL;
              var downloadUrl = URL.createObjectURL(blob);

              if (filename) {
                  // use HTML5 a[download] attribute to specify filename
                  var a = document.createElement('a');
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

              setTimeout(function() {
                  URL.revokeObjectURL(downloadUrl);
              }, 100); // cleanup
          }
      }


  });